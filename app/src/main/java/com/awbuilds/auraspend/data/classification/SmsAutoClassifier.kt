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

data class ClassifiedSms(
    val sms: SmsInfo,
    val parsed: ParsedBankMessage,
    val isSaved: Boolean = false
)

object SmsAutoClassifier {

    private val bankKeywords = listOf(
        "HDFC", "ICICI", "SBI", "Axis", "Kotak", "Yes Bank",
        "PNB", "Canara", "BOB", "UPI", "credited", "debited",
        "A/c", "account", "transaction", "spent", "paid",
        "INR", "Rs.", "withdrawal", "deposit", "balance"
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
        return messages.map { sms ->
            val parsed = TransactionClassifier.classify(sms.body)
            ClassifiedSms(sms = sms, parsed = parsed)
        }
    }

    fun toTransaction(classified: ClassifiedSms, categoryId: String? = null): Transaction {
        val parsed = classified.parsed
        val effectiveCategory = categoryId ?: parsed.categoryId ?: "cat_other"
        return Transaction(
            amount = parsed.amount ?: 0.0,
            categoryId = effectiveCategory,
            note = parsed.note ?: parsed.rawMessage.take(100),
            merchant = parsed.merchant,
            bankName = parsed.bankName,
            date = parsed.date ?: LocalDateTime.now(),
            type = parsed.type ?: TransactionType.EXPENSE
        )
    }

    private fun queryBankSms(
        context: Context,
        sinceTimestamp: Long,
        maxMessages: Int
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
}
