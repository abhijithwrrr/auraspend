package com.awbuilds.auraspend.data.classification.bank

import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType

/**
 * Axis Bank's transaction SMS format.
 *
 * The layout that motivates per-bank parsing, taken verbatim from the golden
 * corpus (`axis_multiline_yasar`):
 *
 * ```
 * Spent INR 25
 * Axis Bank Card
 * no. XX9054
 * 16-08-26 11:33:30 IST
 * Yasar
 * Avl Limit: INR 48058.57
 * Not you? SMS BLOCK 9054 to 919951860002
 * ```
 *
 * Line 1 carries the real amount; the second-to-last line carries a decoy that
 * is an order of magnitude larger and would win under "last amount wins". The
 * merchant is the line between the timestamp and the balance. So the amount is
 * taken from the *first* money line only, and the merchant from a position, not
 * from a keyword.
 */
class AxisBankParser : BankParser {

    override val bankName = "Axis Bank"

    private val senderIds = setOf("AXISBK", "AXISBANK", "AXISB", "AXIS")

    // DLT-registered templates: "<CHANNEL>-AXISBK-S", "VI-AXISBANK", etc.
    private val dltTemplate = Regex("^[A-Z]{2}-AXIS(BK|BANK|B)?-?S?$")

    override fun canHandle(sender: String): Boolean =
        senderIds.contains(sender) || dltTemplate.containsMatchIn(sender)

    // "Spent INR 25" / "INR 2299" / "Rs.3059" — money spent on a card.
    private val spend = Regex(
        """(?:Spent|INR|Rs\.?)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // The decoy. Recognised so it can be *excluded*, never so it can win.
    private val availableLimit = Regex(
        """Avl\s+Limit\s*:\s*(?:INR|Rs\.?)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val cardLine = Regex("""^no\.\s*[X\d]{2,}""", RegexOption.IGNORE_CASE)
    private val timestamp = Regex("""\d{2}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+IST""")
    private val date = Regex("""(\d{2})-(\d{2})-(\d{2})""")

    override fun extract(rawMessage: String): ParsedBankMessage {
        val lines = rawMessage.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return empty(rawMessage)

        // The first money line that is not a balance is the transaction amount.
        val amountLine = lines.firstOrNull { line ->
            (spend.containsMatchIn(line) || availableLimit.containsMatchIn(line)) &&
                !availableLimit.containsMatchIn(line)
        }
        val amount = amountLine
            ?.let { spend.find(it)?.groupValues?.get(1) }
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val isCredit = rawMessage.contains(Regex("""\b(credited|received|refund)""", RegexOption.IGNORE_CASE))
        val isDebit = rawMessage.contains(Regex("""\b(spent|debited|paid)""", RegexOption.IGNORE_CASE))
        val type = when {
            isDebit -> TransactionType.EXPENSE
            isCredit -> TransactionType.INCOME
            else -> null
        }

        // The merchant is positional: the line after the timestamp and before any
        // trailing notice (balance, limit, "Not you?").
        val timestampIndex = lines.indexOfFirst { timestamp.containsMatchIn(it) }
        val merchant = if (timestampIndex >= 0 && timestampIndex + 1 < lines.size) {
            lines[timestampIndex + 1].takeIf { candidate ->
                candidate.isNotBlank() &&
                    !cardLine.containsMatchIn(candidate) &&
                    !availableLimit.containsMatchIn(candidate) &&
                    !candidate.startsWith("Avl", ignoreCase = true) &&
                    !candidate.startsWith("Not you", ignoreCase = true)
            }
        } else {
            null
        }

        return ParsedBankMessage(
            amount = amount,
            type = type,
            merchant = merchant,
            bankName = bankName,
            date = parseDate(rawMessage),
            note = rawMessage.take(100),
            // Confidence is deliberately conservative: this parser is only
            // chosen when the sender is already known to be Axis, but the layout
            // is still inferred, and a wrong category here is worse than none.
            confidence = if (amount != null && type != null) 0.9f else 0.3f,
            rawMessage = rawMessage
        )
    }

    private fun parseDate(raw: String) = date.find(raw)
        ?.groupValues
        ?.let { g ->
            runCatching {
                java.time.LocalDateTime.of(
                    // Axis writes dd-MM-yy; the corpus is 2026, so 26 -> 2026.
                    2000 + g[3].toInt(),
                    g[2].toInt(),
                    g[1].toInt(),
                    0, 0
                )
            }.getOrNull()
        }

    private fun empty(raw: String) = ParsedBankMessage(rawMessage = raw, confidence = 0f)
}
