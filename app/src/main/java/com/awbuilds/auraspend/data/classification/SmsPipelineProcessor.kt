package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.data.local.dao.SmsMessageDao
import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.ui.classification.SmsInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield

/**
 * Result of a single pipeline run.
 */
data class SmsProcessResult(
    val saved: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val attempted: Int = 0
)

/**
 * Persistent-queue classifier. The worker feeds [SmsMessageEntity] rows in one at a time
 * (idempotent ingestion keyed on the provider `_id`), and this processor advances each row to a
 * terminal state:
 *
 *  - [SmsMessageStatus.SAVED]  - a transaction was written (unique `sourceSmsId` prevents dupes)
 *  - [SmsMessageStatus.SKIPPED] - not a transaction (OTP/alert, or no amount/type could be parsed)
 *  - [SmsMessageStatus.FAILED]  - save errored and retry attempts were exhausted
 *  - stays [SmsMessageStatus.NEW] - transient error, retried next run (attempts incremented)
 *
 * The message body is never modified; every row is kept in the queue so the UI can show live
 * status and the user can re-save skipped messages manually from Smart Add.
 */
class SmsPipelineProcessor(
    private val dao: SmsMessageDao,
    private val transactionRepository: TransactionRepository,
    private val enrich: (ClassifiedSms, Map<String, String>) -> ClassifiedSms = { c, _ -> c },
    private val categoryResolver: (String?) -> String? = { merchant ->
        merchant?.let { getCategoryIdForKeyword(it) }
    },
    private val maxAttempts: Int = 3
) {

    private enum class Outcome { SAVED, SKIPPED, FAILED, RETRY }

    /** Process at most [maxMessages] pending messages, one at a time, newest first. */
    suspend fun processPending(maxMessages: Int = 10): SmsProcessResult {
        var saved = 0
        var skipped = 0
        var failed = 0
        var attempted = 0
        val categories = transactionRepository.getAllCategories().first()
            .associate { it.id to it.name }

        repeat(maxMessages) {
            val message = dao.getPending(SmsMessageStatus.NEW.name, 1).firstOrNull()
                ?: return SmsProcessResult(saved, skipped, failed, attempted)
            attempted++
            when (processOne(message, categories)) {
                Outcome.SAVED -> saved++
                Outcome.SKIPPED -> skipped++
                Outcome.FAILED -> failed++
                Outcome.RETRY -> {} // row left NEW for the next run
            }
        }
        return SmsProcessResult(saved, skipped, failed, attempted)
    }

    private suspend fun processOne(
        message: SmsMessageEntity,
        categories: Map<String, String>
    ): Outcome {
        yield()
        val now = System.currentTimeMillis()

        if (SmsAutoClassifier.isOtpOrAlertMessage(message.body)) {
            dao.update(message.copy(
                status = SmsMessageStatus.SKIPPED.name,
                updatedAt = now
            ))
            return Outcome.SKIPPED
        }

        val classified = ClassifiedSms(
            sms = SmsInfo(
                id = message.id,
                address = message.address,
                body = message.body,
                timestamp = message.receivedAt
            ),
            parsed = TransactionClassifier.classify(message.body)
        )
        val enriched = enrich(classified, categories)

        if (enriched.parsed.amount == null || enriched.parsed.type == null) {
            dao.update(message.copy(
                status = SmsMessageStatus.SKIPPED.name,
                updatedAt = now
            ))
            return Outcome.SKIPPED
        }

        val categoryId = enriched.parsed.categoryId
            ?: categoryResolver(enriched.parsed.merchant)
        val transaction = SmsAutoClassifier.toTransaction(enriched, categoryId)

        return try {
            transactionRepository.saveTransaction(transaction)
            dao.update(message.copy(
                status = SmsMessageStatus.SAVED.name,
                amount = transaction.amount,
                type = transaction.type.name,
                merchant = transaction.merchant,
                categoryId = transaction.categoryId,
                isSubscription = transaction.isRecurring,
                confidence = enriched.parsed.confidence,
                updatedAt = now
            ))
            Outcome.SAVED
        } catch (e: Exception) {
            // The unique sourceSmsId index makes a duplicate write a constraint violation rather
            // than a double-save; treat it as terminal and done.
            if (e.message?.contains("UNIQUE constraint failed", ignoreCase = true) == true) {
                dao.update(message.copy(
                    status = SmsMessageStatus.SKIPPED.name,
                    updatedAt = now
                ))
                Outcome.SKIPPED
            } else if (message.attempts + 1 >= maxAttempts) {
                dao.update(message.copy(
                    status = SmsMessageStatus.FAILED.name,
                    attempts = message.attempts + 1,
                    updatedAt = now
                ))
                Outcome.FAILED
            } else {
                dao.update(message.copy(
                    status = SmsMessageStatus.NEW.name,
                    attempts = message.attempts + 1,
                    updatedAt = now
                ))
                Outcome.RETRY
            }
        }
    }
}
