package com.awbuilds.auraspend.data.local

import androidx.room.withTransaction
import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.core.boundary
import com.awbuilds.auraspend.data.local.toEntity
import com.awbuilds.auraspend.data.privacy.SensitiveDataMasker
import com.awbuilds.auraspend.domain.model.Transaction

/**
 * Applies a Drive backup to the local database.
 *
 * Two properties the previous inline implementation in `NavGraph` did not have:
 *
 * 1. **Atomic.** Every table is cleared and re-inserted inside one Room
 *    transaction. Previously five independent DAO calls ran outside any
 *    transaction, so a process death mid-restore left the database half
 *    restored with no way back.
 * 2. **A true replace, not a merge.** Nothing was deleted before, so restoring
 *    an old backup on top of newer local data kept both. Worse, the SMS queue
 *    was re-inserted with `INSERT OR IGNORE`, so rows already present kept their
 *    *local* status and messages the backup still had pending were never retried.
 *
 * Transaction rows are re-masked with [SensitiveDataMasker] on the way in, which
 * keeps restore behind the same privacy choke-point as every other write path.
 */
class BackupRestoreManager(private val database: AppDatabase) {

    private companion object {
        const val TAG = "BackupRestore"
    }

    /**
     * @return a summary of what was written, or null when the restore failed. A
     *   failure is never thrown at the UI: the database is left untouched.
     */
    suspend fun restore(backup: BackupData): RestoreSummary? = boundary(TAG, null) {
        database.withTransaction {
            database.transactionDao().clear()
            database.categoryDao().clear()
            database.budgetDao().clear()
            database.subscriptionDao().clear()
            database.savingsGoalDao().clear()
            database.smsMessageDao().clear()
            database.classificationMemoryDao().clear()

            database.transactionDao().insertTransactions(
                backup.transactions.map { it.masked().toEntity() }
            )
            if (backup.categories.isNotEmpty()) {
                database.categoryDao().insertCategories(backup.categories.map { it.toEntity() })
            }
            backup.budgets.forEach { database.budgetDao().insertBudget(it.toEntity()) }
            backup.subscriptions.forEach { database.subscriptionDao().insertSubscription(it.toEntity()) }
            backup.savingsGoals.forEach { database.savingsGoalDao().insertSavingsGoal(it.toEntity()) }
            if (backup.smsMessages.isNotEmpty()) {
                // Safe now: the table was cleared above, so every row in the backup
                // lands with the status the backup recorded.
                database.smsMessageDao().insertAll(backup.smsMessages)
            }
            backup.classificationMemory.forEach { database.classificationMemoryDao().upsert(it) }

            AuraLog.i(
                TAG,
                "Restored ${backup.transactions.size} transactions, " +
                    "${backup.categories.size} categories, ${backup.smsMessages.size} SMS rows"
            )
            RestoreSummary(
                transactions = backup.transactions.size,
                categories = backup.categories.size,
                budgets = backup.budgets.size,
                subscriptions = backup.subscriptions.size,
                savingsGoals = backup.savingsGoals.size,
                smsMessages = backup.smsMessages.size,
                classificationMemory = backup.classificationMemory.size
            )
        }
    }

    private fun Transaction.masked(): Transaction = copy(
        note = SensitiveDataMasker.mask(note),
        merchant = merchant?.let { SensitiveDataMasker.mask(it) }
    )

    data class RestoreSummary(
        val transactions: Int,
        val categories: Int,
        val budgets: Int,
        val subscriptions: Int,
        val savingsGoals: Int,
        val smsMessages: Int,
        val classificationMemory: Int
    )
}
