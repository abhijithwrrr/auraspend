package com.awbuilds.auraspend.data.classification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.awbuilds.auraspend.AuraSpendApp
import com.awbuilds.auraspend.data.ai.SmsAiEnricher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

/**
 * Periodically enqueues new bank SMS into the persistent [SmsMessageEntity] queue and processes
 * them one-by-one in the background. Each run is idempotent:
 *
 *  1. [SmsIngestor] snapshots new device messages (keyed on the provider `_id`, `OR IGNORE`).
 *  2. [SmsPipelineProcessor] advances every `NEW` row to SAVED / SKIPPED / FAILED, one at a time.
 *  3. A notification summarises how many transactions were auto-saved.
 *
 * Because the queue is the source of truth, a message saved here can never collide with a manual
 * Smart Add save (unique `sourceSmsId` index), and no inbox cursor / prefs dedup sets are needed.
 */
class AutoClassificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        // Only run when the user opted in to automatic classification.
        if (!AutoDetect.isEnabled(applicationContext)) return Result.success()

        // SMS query + LLM enrichment are heavy (each AI pass reads a 400 MB model), so run on IO
        // rather than WorkManager's Default dispatcher to keep the shared CPU pool responsive.
        return withContext(Dispatchers.IO) {
            doProcess().let { if (it) Result.success() else Result.retry() }
        }
    }

    private suspend fun doProcess(): Boolean {
        val app = applicationContext as? AuraSpendApp ?: return true
        val dao = app.database.smsMessageDao()

        // 1. Snapshot new device messages into the queue (idempotent).
        SmsIngestor.ingest(context = applicationContext, dao = dao)

        // 2. Process pending rows one at a time.
        val enricher = SmsAiEnricher(applicationContext)
        val processor = SmsPipelineProcessor(
            dao = dao,
            transactionRepository = app.transactionRepository,
            enrich = { classified, categories -> enricher.enrich(classified, categories) }
        )
        val result = processor.processPending(maxMessages = 10)

        if (result.saved > 0) {
            showNotification(result.saved)
        }

        // RETRY rows still exist (transient failures) but a successful pass is not a job failure;
        // the periodic schedule will pick them up again.
        return true
    }

    private fun showNotification(saved: Int) {
        val channelId = "auto_classification"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transaction Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for auto-detected bank transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$saved new transaction${if (saved > 1) "s" else ""} saved")
            .setContentText("Detected from your bank messages. Open AuraSpend to review.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext).notify(1001, notification)
            }
        } else {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        }
    }

    companion object {
        private const val WORK_NAME = "auto_classification"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoClassificationWorker>(
                6, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Runs a single classification pass immediately (replaces any pending one-shot run so we
         * never queue duplicates). Used right after consent / SMS permission / turning the toggle on.
         */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutoClassificationWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONESHOT,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private const val WORK_NAME_ONESHOT = "auto_classification_now"
    }
}