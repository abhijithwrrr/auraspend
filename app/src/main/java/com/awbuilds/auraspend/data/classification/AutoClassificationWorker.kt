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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AutoClassificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val prefs = applicationContext.getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
        val lastScan = prefs.getLong("last_sms_scan", 0L)

        val classified = SmsAutoClassifier.readAndClassify(
            context = applicationContext,
            sinceTimestamp = lastScan,
            maxMessages = 20
        )

        if (classified.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_sms_scan", now).apply()

        showNotification(classified.size)

        return Result.success()
    }

    private fun showNotification(count: Int) {
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
            .setContentTitle("$count new transaction${if (count > 1) "s" else ""} detected")
            .setContentText("Open AuraSpend to review and save.")
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
    }
}
