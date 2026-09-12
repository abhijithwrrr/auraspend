package com.awbuilds.auraspend.data.classification

import android.content.Context
import android.provider.Telephony
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

    // Patterns to filter out OTP messages. Structural: an OTP keyword plus a
    // standalone 4-8 digit code, or an explicit one-time-password phrase.
    private val otpPatterns = listOf(
        Regex("(?i)\\b(otp|one\\s*time\\s*(password|code)|security\\s+code)\\b"),
        Regex("(?i)\\b\\d{4,8}\\b\\s*(?:is|was|to)\\s+(?:your|the|a)\\s+(?:OTP|password|code|PIN)")
    )

    /**
     * Non-transaction notice patterns. A match only skips the message when the
     * amount/verb rescue below does not fire (i.e. no completed transaction).
     */
    private val alertPatterns = listOf(
        Regex("(?i)\\b(approved|approval|confirmation)\\b(?!.*(?:debited|credited|spent|paid))"),
        Regex("(?i)\\b(alert|notification|notify)\\b"),
        Regex("(?i)\\b(data\\s+usage|data\\s+plan|gb\\s+data|internet\\s+usage)\\b"),
        Regex("(?i)\\b(maintenance|system\\s+update|upgrade|server)\\b"),
        Regex("(?i)\\b(avail(?:able)?\\s+balance|bal(ance)?\\s+(?:amount|is|of)|current\\s+balance)\\b"),
        Regex("(?i)\\b(due|deadline|maturity|interest\\s+(?:charged|posted)|annual\\s+fee)\\b"),
        Regex("(?i)\\b(congratulations|welcome|thank\\s+you)\\b"),
        Regex("(?i)\\b(limit\\s+(?:exceed|reached)|threshold|invalid|failed|declined)\\b"),
        Regex("(?i)\\b(application|document|status|pending|submitted)\\b"),
        Regex("(?i)\\b(have\\s+used|utilization|remaining|offer|promo|%\\s*off|discount|sale|coupon)\\b"),
        Regex("(?i)\\b(kyc|re-?kyc|block(?:ed)?|unblock|token|de-?register)\\b"),
        Regex("(?i)\\b(fraud|scam|phishing|report\\s+cyber)\\b"),
        Regex("(?i)\\b(win|won|lucky\\s+draw|prize|lottery)\\b")
    )

    /**
     * Hard vetoes: these prove NO completed transaction regardless of any amount or
     * debit/credit wording present. They bypass the amount+verb rescue below.
     *
     * NOTE: bare "fraud"/"report cyber fraud" is deliberately NOT a hard veto — Canara
     * appends "Dial 1930 to report cyber fraud" boilerplate to every genuine transaction
     * SMS. Only conditional/unauthorised framing and advance notices are unconditional.
     */
    private val hardVetoPatterns = listOf(
        // Conditional/unauthorised framing ("Rs.X debited? report cyber fraud if not done by you").
        Regex("(?i)\\b(if\\s+not\\s+(?:done\\s+)?by\\s+you|if\\s+(?:it['’]?s?\\s+)?not\\s+you|if\\s+you\\s+(?:have\\s+)?not\\s+(?:done|authorised|initiated)|not\\s+authorised\\s+by\\s+you|unauthorised\\s+transaction)\\b"),
        // Advance mandate registrations: money has NOT moved yet ("will be debited on <date>").
        Regex("(?i)\\bupcoming\\s+mandate\\b"),
        Regex("(?i)\\be-?mandate\\b[\\s\\S]{0,120}?(?:has\\s+been\\s+registered|set\\s+at|limit\\s+amount)"),
        // Payment reminders: "Ignore if paid" proves this SMS is not the payment itself.
        Regex("(?i)\\bignore\\s+if\\s+paid\\b"),
        Regex("(?i)\\b(?:installment|emi|bill)\\b[\\s\\S]{0,60}?\\bis\\s+due\\b")
    )

    /**
     * Converts a parsed message into a savable transaction. All ingestion flows through the
     * persistent Room queue (SmsIngestor -> sms_messages -> SmsPipelineProcessor), which takes
     * messages one at a time — never parse the inbox directly.
     */
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

                if (looksLikeBankSender(addr) ||
                    bankKeywords.any { body.contains(it, ignoreCase = true) }
                ) {
                    messages.add(SmsInfo(id = id, address = addr, body = body, timestamp = date))
                }
            }
        }

        return messages.sortedByDescending { it.timestamp }
    }

    /**
     * Indian banks send via DLT alphabetic sender IDs, commonly with a
     * two-letter channel prefix: "AD-HDFCBK", "VM-ICICIB", "JD-SBICRD".
     */
    private val bankSenderRegex = Regex(
        "(?i)(?:^[A-Z]{2}-?)?(hdfc|icici|sbic?|sbin|axisb?|kotak|yesb|pnbn?|canara|barb|" +
            "idfc|idbi|indusinb?k?|unionbnk|indbnk|federal|rblbnk|aubank|bandhan|paytm|airtel)"
    )

    internal fun looksLikeBankSender(address: String): Boolean =
        address.isNotBlank() && bankSenderRegex.containsMatchIn(address.trim())

    fun isOtpOrAlertMessage(message: String): Boolean {
        // OTPs are always skipped.
        if (otpPatterns.any { it.containsMatchIn(message) }) {
            return true
        }

        // Fraud warnings quote amounts and debit verbs to look real — no rescue.
        if (hardVetoPatterns.any { it.containsMatchIn(message) }) {
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
