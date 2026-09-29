package com.awbuilds.auraspend.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.awbuilds.auraspend.data.local.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO interfaces for Room database access.
 */

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTimestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateTimestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /**
     * Paged Activity list. Filtering happens in SQL so no filter combination
     * ever loads more than a page of rows; every ORDER BY is newest-first.
     * NULL parameters mean "no constraint" ([query] may also be blank).
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (
              :query IS NULL OR :query = ''
              OR note LIKE '%' || :query || '%'
              OR merchant LIKE '%' || :query || '%'
          )
        ORDER BY dateTimestamp DESC
        """
    )
    fun pagedTransactions(
        query: String?,
        type: String?,
        categoryId: String?
    ): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1")
    fun getRecurringTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateTimestamp BETWEEN :start AND :end ORDER BY dateTimestamp DESC")
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' OR merchant LIKE '%' || :query || '%' ORDER BY dateTimestamp DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateTimestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND dateTimestamp BETWEEN :start AND :end")
    fun getTotalByTypeInRange(type: String, start: Long, end: Long): Flow<Double?>

    // ─── SQL aggregates (dashboard / activity summaries) ─────────────────────

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense,
               SUM(CASE WHEN type = 'INCOME' THEN 1 ELSE 0 END) AS incomeCount,
               SUM(CASE WHEN type = 'EXPENSE' THEN 1 ELSE 0 END) AS expenseCount
        FROM transactions
        WHERE dateTimestamp BETWEEN :start AND :end
        """
    )
    fun observeSummary(start: Long, end: Long): Flow<TransactionSummaryRow>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) FROM transactions")
    fun observeBalance(): Flow<Double>

    @Query(
        """
        SELECT categoryId, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE' AND dateTimestamp BETWEEN :start AND :end
        GROUP BY categoryId
        """
    )
    fun observeExpenseByCategory(start: Long, end: Long): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT date(dateTimestamp / 1000, 'unixepoch', 'localtime') AS day, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE' AND dateTimestamp BETWEEN :start AND :end
        GROUP BY day
        """
    )
    fun observeDailyExpense(start: Long, end: Long): Flow<List<DayTotalRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    /** Used only by the atomic Drive restore, which replaces the table wholesale. */
    @Query("DELETE FROM transactions")
    suspend fun clear()
}

/** Aggregate row for a date range: totals plus counts per type. */
data class TransactionSummaryRow(
    val income: Double,
    val expense: Double,
    val incomeCount: Int,
    val expenseCount: Int
)

/** Aggregate row: total expense for one category. */
data class CategoryTotalRow(
    val categoryId: String,
    val total: Double
)

/** Aggregate row: total expense for one local calendar day ("YYYY-MM-DD"). */
data class DayTotalRow(
    val day: String,
    val total: Double
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    /** Used only by the atomic Drive restore, which replaces the table wholesale. */
    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId")
    suspend fun getBudgetByCategory(categoryId: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: String)

    @Query("UPDATE budgets SET spentAmount = :spent WHERE categoryId = :categoryId")
    suspend fun updateSpentAmount(categoryId: String, spent: Double)

    /** Used only by the atomic Drive restore, which replaces the table wholesale. */
    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteSavingsGoalById(goalId: String)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    /** Used only by the atomic Drive restore, which replaces the table wholesale. */
    @Query("DELETE FROM savings_goals")
    suspend fun clear()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE active = 1")
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDateTimestamp ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    @Query("UPDATE subscriptions SET active = :active WHERE id = :id")
    suspend fun setSubscriptionActive(id: String, active: Boolean)

    /** Used only by the atomic Drive restore, which replaces the table wholesale. */
    @Query("DELETE FROM subscriptions")
    suspend fun clear()
}

/**
 * Bank messages the app could not turn into a transaction.
 *
 * Written only when a message carries a movement verb and an amount marker but
 * no parser could read it — i.e. the message plausibly is a real transaction.
 * Pure OTPs and promotions are filtered earlier and never reach this table.
 */
@Dao
interface UnrecognizedSmsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<UnrecognizedSmsEntity>)

    @Query("SELECT * FROM unrecognized_sms ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<UnrecognizedSmsEntity>>

    @Query("SELECT COALESCE(COUNT(*), 0) FROM unrecognized_sms")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM unrecognized_sms WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM unrecognized_sms")
    suspend fun clear()

    /** Retention sweep: rows older than [cutoff] are not worth surfacing. */
    @Query("DELETE FROM unrecognized_sms WHERE createdAt < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long)

    @Query("SELECT * FROM unrecognized_sms")
    suspend fun getAll(): List<UnrecognizedSmsEntity>
}

@Dao
interface SmsMessageDao {

    /** Idempotent ingest: a message already in the queue is ignored, never duplicated. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<SmsMessageEntity>)

    @Query("SELECT * FROM sms_messages WHERE status = :status ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun getPending(status: String, limit: Int): List<SmsMessageEntity>

    @Query("SELECT * FROM sms_messages WHERE id = :id")
    suspend fun getById(id: String): SmsMessageEntity?

    @Query("SELECT * FROM sms_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<SmsMessageEntity>>

    @Update
    suspend fun update(message: SmsMessageEntity)

    @Query("SELECT COALESCE(MAX(receivedAt), 0) FROM sms_messages")
    suspend fun getMaxReceivedAt(): Long

    /**
     * Used only by the atomic Drive restore. The queue is cleared and re-inserted
     * so a restored row keeps the status the backup recorded — restoring with
     * INSERT OR IGNORE would leave locally-newer rows untouched and never retry
     * messages the backup still had pending.
     */
    @Query("DELETE FROM sms_messages")
    suspend fun clear()
}
