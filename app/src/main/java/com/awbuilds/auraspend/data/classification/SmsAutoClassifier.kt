package com.awbuilds.auraspend.data.classification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.ui.classification.SmsInfo
import java.time.LocalDateTime

/** Live status of a message while it is being categorized, shown in the UI. */
enum class SmsStatus { PENDING, CLASSIFYING, CLASSIFIED, SAVED, FAILED }

data class ClassifiedSms(
    val sms: SmsInfo,
    val parsed: ParsedBankMessage,
    val isSaved: Boolean = false,
    val isSubscription: Boolean = false,
    val status: SmsStatus = SmsStatus.PENDING
)

object SmsAutoClassifier {

    private val bankKeywords = listOf(
        "HDFC", "ICICI", "SBI", "Axis", "Kotak", "Yes Bank",
        "PNB", "Canara", "BOB", "UPI", "credited", "debited",
        "A/c", "account", "transaction", "spent", "paid",
        "INR", "Rs.", "withdrawal", "deposit", "balance"
    )

    // Patterns to filter out OTP and alert messages
    private val otpPatterns = listOf(
        Regex("OTP|one\\s*time\\s*password|password|verify", RegexOption.IGNORE_CASE),
        Regex("\\d{4,6}\\s+(?:is|was|to)\\s+(?:your|the|a)\\s+(?:OTP|password|code|PIN)", RegexOption.IGNORE_CASE)
    )

    private val alertPatterns = listOf(
        Regex("approved|approval|confirmation", RegexOption.IGNORE_CASE),
        Regex("alert|notification|notify", RegexOption.IGNORE_CASE),
        Regex("data\\s+usage|data\\s+plan|gb\\s+data|internet\\s+usage", RegexOption.IGNORE_CASE),
        Regex("maintenance|system|update|upgrade|server", RegexOption.IGNORE_CASE),
        Regex("available\\s+balance|balance\\s+amount|current\\s+balance", RegexOption.IGNORE_CASE),
        Regex("due|deadline|maturity|interest|annual|charge", RegexOption.IGNORE_CASE),
        Regex("congratulations|success|welcome|thank\\s+you", RegexOption.IGNORE_CASE),
        Regex("limit|exceed|threshold|invalid|failed", RegexOption.IGNORE_CASE),
        Regex("application|document|status|pending|submitted", RegexOption.IGNORE_CASE),
        Regex("have\\s+used|utilization|remaining|offer|promo", RegexOption.IGNORE_CASE)
    )

    fun readAndClassify(
        context: Context,
        sinceTimestamp: Long = 0L,
        maxMessages: Int = 50
    ): List<ClassifiedSms> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val messages = queryBankSms(context, sinceTimestamp, maxMessages)
        return messages
            .filter { !isOtpOrAlertMessage(it.body) }
            .map { sms ->
                val parsed = TransactionClassifier.classify(sms.body)
                ClassifiedSms(sms = sms, parsed = parsed)
            }
    }

    fun toTransaction(classified: ClassifiedSms, categoryId: String? = null): Transaction {
        val parsed = classified.parsed
        val subscriptionCategory = if (classified.isSubscription) "cat_subscription" else null
        val effectiveCategory = subscriptionCategory ?: categoryId ?: parsed.categoryId ?: "cat_other"
        return Transaction(
            amount = parsed.amount ?: 0.0,
            categoryId = effectiveCategory,
            note = parsed.note ?: parsed.rawMessage.take(100),
            merchant = parsed.merchant,
            bankName = parsed.bankName,
            date = parsed.date ?: LocalDateTime.now(),
            type = parsed.type ?: TransactionType.EXPENSE,
            isRecurring = classified.isSubscription,
            sourceSmsId = classified.sms.id.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Next scan cursor: the newest timestamp in the batch. The worker only calls this after every
     * message in the batch is terminal (saved, already saved, or recorded as ignored), so nothing
     * is silently stranded below the cursor and unparsed messages are never re-read forever.
     */
    fun nextScanCursor(messages: List<ClassifiedSms>): Long? =
        messages.maxOfOrNull { it.sms.timestamp }

    /** Latest bank-like messages from the device inbox, newest first. */
    fun queryBankSms(
        context: Context,
        sinceTimestamp: Long = 0L,
        maxMessages: Int = 500
    ): List<SmsInfo> {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        val selection = buildString {
            append("(${bankKeywords.joinToString(" OR ") { "${Telephony.Sms.Inbox.BODY} LIKE '%$it%'" }})")
            if (sinceTimestamp > 0L) {
                append(" AND ${Telephony.Sms.Inbox.DATE} > $sinceTimestamp")
            }
        }
        val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC LIMIT $maxMessages"

        val cursor = context.contentResolver.query(uri, projection, selection, null, sortOrder)

        val messages = mutableListOf<SmsInfo>()
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(Telephony.Sms.Inbox._ID)
            val addrIdx = c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.Inbox.DATE)

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getString(idIdx) else ""
                val addr = if (addrIdx >= 0) c.getString(addrIdx) else ""
                val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

                if (bankKeywords.any { body.contains(it, ignoreCase = true) || addr.contains(it, ignoreCase = true) }) {
                    messages.add(SmsInfo(id = id, address = addr, body = body, timestamp = date))
                }
            }
        }

        return messages.sortedByDescending { it.timestamp }
    }

    fun isOtpOrAlertMessage(message: String): Boolean {
        // Check for OTP patterns
        if (otpPatterns.any { it.containsMatchIn(message) }) {
            return true
        }

        // Check for alert patterns
        if (alertPatterns.any { it.containsMatchIn(message) }) {
            // But make sure it's not a transaction (has amount + debit/credit pattern)
            val hasAmount = Regex("""(?:Rs\.?|INR|₹)\s*\d|₹\s*\d|\d+\s*(?:Rs|INR)""").containsMatchIn(message)
            val hasTransactionKeyword = Regex(
                "(?:debited|credited|spent|paid|transferred|withdrawn)",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(message)

            // If it has both amount and transaction keyword, it's likely a real transaction
            if (hasAmount && hasTransactionKeyword) {
                return false
            }

            // Otherwise it's an alert
            return true
        }

        return false
    }
}
