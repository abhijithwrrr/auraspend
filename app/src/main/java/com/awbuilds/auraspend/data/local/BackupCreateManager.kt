package com.awbuilds.auraspend.data.local

import androidx.room.withTransaction
import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.core.boundary

/**
 * Builds the JSON payload a Drive backup uploads.
 *
 * The mirror image of [BackupRestoreManager], which takes a payload apart: this
 * one puts it together from every user-owned table. Two properties matter:
 *
 * 1. **One transaction.** Every read happens inside a single Room transaction,
 *    so the backup is a consistent snapshot rather than a mix of rows from
 *    before and after a concurrent write — the auto-classification worker can
 *    be advancing the SMS queue while the user taps "Back up".
 * 2. **Every table.** [BackupRestoreManager.restore] replaces each table
 *    wholesale, so a section this class forgets to read is not left as it was
 *    on a later restore — it is deleted. `BackupCreateManagerTest` seeds one row
 *    in each table and fails if any section comes back empty.
 */
class BackupCreateManager(private val database: AppDatabase) {

    private companion object {
        const val TAG = "BackupCreate"
    }

    /**
     * @return the backup JSON, or null when it could not be built. A failure is
     *   never thrown at the UI; the caller reports a generic "backup failed".
     */
    suspend fun createBackupJson(): String? = boundary(TAG, null) {
        val data = database.withTransaction {
            BackupData(
                transactions = database.transactionDao().getAllOnce().map { it.toDomain() },
                categories = database.categoryDao().getAllOnce().map { it.toDomain() },
                budgets = database.budgetDao().getAllOnce().map { it.toDomain() },
                subscriptions = database.subscriptionDao().getAllOnce().map { it.toDomain() },
                savingsGoals = database.savingsGoalDao().getAllOnce().map { it.toDomain() },
                smsMessages = database.smsMessageDao().getAllOnce(),
                classificationMemory = database.classificationMemoryDao().getAll(),
                unrecognizedSms = database.unrecognizedSmsDao().getAll()
            )
        }

        AuraLog.i(
            TAG,
            "Backup built: ${data.transactions.size} transactions, " +
                "${data.categories.size} categories, ${data.smsMessages.size} SMS rows"
        )

        // Serialization is CPU-only JSON building; it stays outside the
        // transaction so the database lock is held for reads alone.
        BackupSerializer.serialize(data)
    }
}
