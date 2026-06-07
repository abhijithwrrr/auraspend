package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object BankMessageParser {

    // Indian bank patterns
    private val bankPatterns = mapOf(
        Regex("HDFC\\s*Bank|HDFCB|HDFC", RegexOption.IGNORE_CASE) to "HDFC Bank",
        Regex("ICICI\\s*Bank|ICICI|ICICIB", RegexOption.IGNORE_CASE) to "ICICI Bank",
        Regex("SBI\\s*Bank|SBI|SBIB|State\\s*Bank", RegexOption.IGNORE_CASE) to "SBI",
        Regex("Axis\\s*Bank|AXIS|AXISB", RegexOption.IGNORE_CASE) to "Axis Bank",
        Regex("Kotak\\s*Bank|KOTAK|KOTAKB", RegexOption.IGNORE_CASE) to "Kotak Mahindra",
        Regex("Yes\\s*Bank|YESB|YES\\s*Bank", RegexOption.IGNORE_CASE) to "Yes Bank",
        Regex("PNB|Punjab\\s*National\\s*Bank", RegexOption.IGNORE_CASE) to "PNB",
        Regex("Canara\\s*Bank|CANARA", RegexOption.IGNORE_CASE) to "Canara Bank",
        Regex("BOB|Bank\\s*of\\s*Baroda", RegexOption.IGNORE_CASE) to "Bank of Baroda"
    )

    // Amount patterns - ₹ or INR
    private val amountPattern = Regex(
        """(?:Rs\.?|INR|₹)\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val amountPatternAlt = Regex(
        """(\d{1,3}(?:,\d{3})*(?:\.\d{1,2}))\s*(?:Rs\.?|INR|₹|only)""",
        RegexOption.IGNORE_CASE
    )

    private val amountPatternPlain = Regex(
        """[A-Za-z]+\s+(\d{1,3}(?:,\d{3})*(?:\.\d{2}))\s+is""",
        RegexOption.IGNORE_CASE
    )

    // Transaction type keywords
    private val creditPatterns = listOf(
        Regex("credited|Cr[.]?|credit|received|deposited", RegexOption.IGNORE_CASE),
        Regex("has been credited", RegexOption.IGNORE_CASE)
    )

    private val debitPatterns = listOf(
        Regex("debited|Dr[.]?|debit|spent|paid|purchased", RegexOption.IGNORE_CASE),
        Regex("has been debited", RegexOption.IGNORE_CASE),
        Regex("is debited", RegexOption.IGNORE_CASE)
    )

    // Merchant extraction patterns
    private val merchantPatterns = listOf(
        Regex("""at\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+at\s+\d|\s+via|\s+ref|\.|$|\s+A/c|\s+Available)""", RegexOption.IGNORE_CASE),
        Regex("""to\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+at\s+\d|\s+via|\s+ref|\.|$|\s+A/c)""", RegexOption.IGNORE_CASE),
        Regex("""from\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+ref|\.|$)""", RegexOption.IGNORE_CASE),
        Regex("""payment to\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+ref|\.|$|\s+via)""", RegexOption.IGNORE_CASE),
        Regex("""purchase at\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+ref|\.|$)""", RegexOption.IGNORE_CASE),
        Regex("""withdrawal at\s+([A-Za-z0-9\s&.]+?)(?:\s+on|\s+ref|\.|$)""", RegexOption.IGNORE_CASE)
    )

    // Date patterns commonly used in Indian bank SMS
    private val datePatterns = listOf(
        Regex("""(\d{2}/\d{2}/\d{4})"""),
        Regex("""(\d{2}-\d{2}-\d{4})"""),
        Regex("""(\d{2}/\d{2}/\d{2})"""),
        Regex("""on\s+(\d{1,2}\s+[A-Z][a-z]+\s+\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""dated:\s*(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
    )

    fun parse(rawMessage: String): ParsedBankMessage {
        val message = rawMessage.trim()

        val bankName = detectBank(message)
        val amount = detectAmount(message)
        val type = detectTransactionType(message)
        val merchant = detectMerchant(message)
        val date = detectDate(message)
        val note = buildNote(merchant, bankName, message)
        val confidence = calculateConfidence(amount, type, message)
        val categoryId = merchant?.let { getCategoryIdForKeyword(it) }

        return ParsedBankMessage(
            amount = amount,
            type = type,
            merchant = merchant,
            bankName = bankName,
            date = date,
            note = note,
            categoryId = categoryId,
            confidence = confidence,
            rawMessage = message
        )
    }

    private fun detectBank(message: String): String? {
        for ((pattern, name) in bankPatterns) {
            if (pattern.containsMatchIn(message)) return name
        }
        return null
    }

    private fun detectAmount(message: String): Double? {
        for (pattern in listOf(amountPattern, amountPatternAlt, amountPatternPlain)) {
            val match = pattern.find(message)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "")
                return raw.toDoubleOrNull()
            }
        }

        val plainNumber = Regex("""\b(\d{3,}\.\d{2})\b""").find(message)
        if (plainNumber != null) {
            val raw = plainNumber.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull()
            if (value != null && value < 10000000 && value > 0) return value
        }

        return null
    }

    private fun detectTransactionType(message: String): TransactionType? {
        val isCredit = creditPatterns.any { it.containsMatchIn(message) }
        val isDebit = debitPatterns.any { it.containsMatchIn(message) }
        return when {
            isCredit && !isDebit -> TransactionType.INCOME
            isDebit && !isCredit -> TransactionType.EXPENSE
            else -> null
        }
    }

    private fun detectMerchant(message: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.length in 2..50) return name
            }
        }

        // Try to detect UPIs like merchant@axisbank etc
        val upiPattern = Regex("""[Vv][Pp][Aa]\s*:\s*(\w+?)(?:@|\s)""", RegexOption.IGNORE_CASE)
        val upiMatch = upiPattern.find(message)
        if (upiMatch != null) return upiMatch.groupValues[1]

        return null
    }

    private fun detectDate(message: String): LocalDateTime? {
        for (pattern in datePatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val dateStr = match.groupValues[1]
                val formats = listOf(
                    "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy",
                    "d MMM yyyy", "d MMMM yyyy"
                )
                for (fmt in formats) {
                    try {
                        return LocalDateTime.parse(
                            dateStr,
                            DateTimeFormatter.ofPattern(fmt, Locale.ENGLISH)
                        ).withHour(12)
                    } catch (_: DateTimeParseException) { }
                }
            }
        }

        // Check for relative dates
        if (Regex("today", RegexOption.IGNORE_CASE).containsMatchIn(message)) return LocalDateTime.now()
        if (Regex("yesterday", RegexOption.IGNORE_CASE).containsMatchIn(message)) return LocalDateTime.now().minusDays(1)

        return null
    }

    private fun buildNote(merchant: String?, bankName: String?, rawMessage: String): String? {
        return merchant ?: truncateMessage(rawMessage)
    }

    private fun truncateMessage(message: String): String? {
        val cleaned = message
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (cleaned.length > 120) cleaned.take(120) + "..." else cleaned
    }

    private fun calculateConfidence(amount: Double?, type: TransactionType?, message: String): Float {
        val hasBothAmountAndType = amount != null && type != null
        val hasBank = bankPatterns.any { (pattern, _) -> pattern.containsMatchIn(message) }
        val hasMerchant = merchantPatterns.any { pattern -> pattern.containsMatchIn(message) }

        return when {
            hasBothAmountAndType && hasBank && hasMerchant -> 0.95f
            hasBothAmountAndType && hasBank -> 0.85f
            hasBothAmountAndType && hasMerchant -> 0.80f
            hasBothAmountAndType -> 0.70f
            amount != null && hasBank -> 0.60f
            amount != null -> 0.40f
            else -> 0.20f
        }
    }
}
