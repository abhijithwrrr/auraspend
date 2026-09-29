package com.awbuilds.auraspend.data.classification.bank

import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType

/**
 * State Bank of India's transaction SMS format.
 *
 * SBI sends from `SBIN`, and its DLT templates carry the same
 * `<CHANNEL>-<ID>-S` shape as the other banks. The distinguishing feature is the
 * `A/c XXXX1234` fragment, which also appears on the balance and interest lines
 * that must not be read as transactions.
 */
class SbiBankParser : BankParser {

    override val bankName = "State Bank of India"

    private val senderIds = setOf("SBIN", "SBI", "SBICRD")
    private val dltTemplate = Regex("^[A-Z]{2}-SBI(N|CRD)?-?S?$")

    override fun canHandle(sender: String): Boolean =
        senderIds.contains(sender) || dltTemplate.containsMatchIn(sender)

    // A completed movement, which is what distinguishes a transaction from a
    // balance or interest notice.
    private val movement = Regex(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(debited|credited)""",
        RegexOption.IGNORE_CASE
    )

    private val accountTail = Regex("""A/c\s*X?X?X?X?(\d{2,})""", RegexOption.IGNORE_CASE)

    // "at MERCHANT on 12-08-2026" / "towards MERCHANT"
    private val atMerchant = Regex("""\b(?:at|to)\s+([A-Za-z0-9&.\-' ]{2,48}?)\s+(?:on|dated|trxn)""", RegexOption.IGNORE_CASE)
    private val towardsMerchant = Regex("""\btowards\s+([A-Za-z0-9&.\-' ]{2,48}?)(?:\s+on\b|\s*$|[.])""", RegexOption.IGNORE_CASE)
    private val avlLine = Regex("""\bAVL\s*[:.]?\s*(?:INR|Rs\.?)?\s*([0-9,.]+)""", RegexOption.IGNORE_CASE)

    override fun extract(rawMessage: String): ParsedBankMessage {
        val match = movement.find(rawMessage)
            ?: return ParsedBankMessage(rawMessage = rawMessage, bankName = bankName)

        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            ?: return ParsedBankMessage(rawMessage = rawMessage, bankName = bankName)

        val isCredit = match.groupValues[2].equals("credited", ignoreCase = true)
        val type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE

        // A credit's counterparty is the sender, not a merchant.
        val merchant = if (isCredit) {
            Regex("""\bfrom\s+([A-Za-z0-9&.\-' ]{3,48}?)(?:\s+A/c|\s+on\b|$)""", RegexOption.IGNORE_CASE)
                .find(rawMessage)?.groupValues?.get(1)?.trim()
        } else {
            atMerchant.find(rawMessage)?.groupValues?.get(1)?.trim()
                ?: towardsMerchant.find(rawMessage)?.groupValues?.get(1)?.trim()
        }

        return ParsedBankMessage(
            amount = amount,
            type = type,
            merchant = merchant?.takeIf { it.isNotBlank() && !avlLine.containsMatchIn(it) },
            bankName = bankName,
            note = rawMessage.take(100),
            confidence = 0.8f,
            rawMessage = rawMessage
        )
    }
}
