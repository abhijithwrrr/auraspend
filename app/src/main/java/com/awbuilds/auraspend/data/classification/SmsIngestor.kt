package com.awbuilds.auraspend.data.classification

import android.content.Context
import com.awbuilds.auraspend.data.local.dao.SmsMessageDao
import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.data.privacy.SensitiveDataMasker

/**
 * Snapshots bank-like messages from the device inbox into the persistent [SmsMessageEntity] queue.
 *
 * Ingestion is idempotent (the queue is keyed on the provider's `_id`, inserted with `OR IGNORE`),
 * so re-scanning is always safe. A lightweight pref watermark keeps repeated scans to the newest
 * messages only; the watermark is set one ms below the newest ingested message so a message sharing
 * the exact boundary timestamp is never permanently missed (re-inserts are ignored).
 *
 * PRIVACY: bodies are passed through [SensitiveDataMasker] BEFORE storage, so phone numbers,
 * reference ids and account fragments never touch the database (or anything derived from it).
 */
object SmsIngestor {

    private const val PREF_FILE = "auraspend_prefs"
    private const val PREF_INGEST_SINCE = "sms_ingest_since"

    /** Returns how many new messages were enqueued. */
    suspend fun ingest(
        context: Context,
        dao: SmsMessageDao,
        maxMessages: Int = 500
    ): Int {
        val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val since = prefs.getLong(PREF_INGEST_SINCE, 0L)

        val messages = SmsAutoClassifier.queryBankSms(
            context = context,
            sinceTimestamp = since,
            maxMessages = maxMessages
        )
        if (messages.isEmpty()) return 0

        dao.insertAll(messages.map { message ->
            SmsMessageEntity(
                id = message.id,
                address = message.address,
                body = SensitiveDataMasker.mask(message.body),
                receivedAt = message.timestamp,
                status = SmsMessageStatus.NEW.name
            )
        })

        val newest = messages.maxOf { it.timestamp }
        prefs.edit().putLong(PREF_INGEST_SINCE, newest - 1).apply()
        return messages.size
    }
}