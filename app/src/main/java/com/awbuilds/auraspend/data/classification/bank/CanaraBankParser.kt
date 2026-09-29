package com.awbuilds.auraspend.data.classification.bank

import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType

/**
 * Canara Bank's transaction SMS format.
 *
 * The important property of this parser is what it *must not* match. Canara
 * appends `Dial 1930 to report cyber fraud` to genuine debits, so any
 * fraud-keyword heuristic drops real money. The global parser's hard fraud veto
 * exists for the same reason and is deliberately unconditional — this parser
 * simply never looks at the footer, and reads the verb Canara actually uses
 * (`DEBITED` / `credited`) to decide the direction.
 */
class CanaraBankParser : BankParser {

    override val bankName = "Canara Bank"

    private val senderIds = setOf("CANARA", "CNRB", "CANARABK")
    private val dltTemplate = Regex("^[A-Z]{2}-(CANARA|CNRB)-?S?$")

    override fun canHandle(sender: String): Boolean =
        senderIds.contains(sender) || dltTemplate.containsMatchIn(sender)

    private val movement = Regex(
        """(?:INR|Rs\.?)\s*([0-9,]+(?:\.\d{1,2})?)\s+has\s+been\s+(DEBITED|CREDITED)""",
        RegexOption.IGNORE_CASE
    )

    private val alternativeAmount = Regex(
        """(?:Rs\.?|INR)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:debited|credited)""",
        RegexOption.IGNORE_CASE
    )

    // "from ACME SOLUTIONS LTD NEFT" — the payee on a credit.
    private val fromPayee = Regex("""\bfrom\s+([A-Za-z0-9&.\- ]{3,60}?)\s+(?:NEFT|IMPS|RTGS|UPI|IMPS)""", RegexOption.IGNORE_CASE)

    private val upiHandle = Regex("""(?:UPI\s*)?([a-z0-9._-]{2,64}@[a-z]{2,})""", RegexOption.IGNORE_CASE)

    override fun extract(rawMessage: String): ParsedBankMessage {
        val match = movement.find(rawMessage)
        val (amountText, direction) = when {
            match != null -> match.groupValues[1] to match.groupValues[2]
            else -> {
                val alt = alternativeAmount.find(rawMessage)
                if (alt == null) return ParsedBankMessage(rawMessage = rawMessage, bankName = bankName)
                val isCredit = rawMessage.contains(Regex("""\bcredited""", RegexOption.IGNORE_CASE))
                alt.groupValues[1] to if (isCredit) "CREDITED" else "DEBITED"
            }
        }
        val amount = amountText.replace(",", "").toDoubleOrNull()
            ?: return ParsedBankMessage(rawMessage = rawMessage, bankName = bankName)

        val type = if (direction.equals("CREDITED", ignoreCase = true)) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val merchant = fromPayee.find(rawMessage)?.groupValues?.get(1)?.trim()
            ?: upiHandle.find(rawMessage)?.groupValues?.get(1)?.trim()

        return ParsedBankMessage(
            amount = amount,
            type = type,
            merchant = merchant,
            bankName = bankName,
            date = date.find(rawMessage)?.groupValues?.let { g ->
                runCatching {
                    java.time.LocalDateTime.of(g[3].toInt(), g[2].toInt(), g[1].toInt(), 0, 0)
                }.getOrNull()
            },
            note = rawMessage.take(100),
            confidence = if (merchant != null) 0.85f else 0.75f,
            rawMessage = rawMessage
        )
    }

    private val date = Regex("""on\s+(\d{2})-(\d{2})-(\d{4})""")
}
