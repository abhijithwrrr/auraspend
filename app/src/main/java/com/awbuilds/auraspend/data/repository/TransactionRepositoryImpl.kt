package com.awbuilds.auraspend.data.repository

import com.awbuilds.auraspend.data.local.toDomain
import com.awbuilds.auraspend.data.local.toEntity
import com.awbuilds.auraspend.core.boundaryOrNull
import com.awbuilds.auraspend.data.privacy.SensitiveDataMasker
import com.awbuilds.auraspend.data.local.dao.BudgetDao
import com.awbuilds.auraspend.data.local.dao.CategoryDao
import com.awbuilds.auraspend.data.local.dao.SavingsGoalDao
import com.awbuilds.auraspend.data.local.dao.SubscriptionDao
import com.awbuilds.auraspend.data.local.dao.TransactionDao
import com.awbuilds.auraspend.domain.model.*
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val subscriptionDao: SubscriptionDao,
    private val savingsGoalDao: SavingsGoalDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { it.map { e -> e.toDomain() } }

    override fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end).map { it.map { e -> e.toDomain() } }

    override fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query).map { it.map { e -> e.toDomain() } }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId).map { it.map { e -> e.toDomain() } }

    override fun getRecurringTransactions(): Flow<List<Transaction>> =
        transactionDao.getRecurringTransactions().map { it.map { e -> e.toDomain() } }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { it.map { e -> e.toDomain() } }

    override fun observeSummary(start: Long, end: Long): Flow<TransactionSummary> =
        transactionDao.observeSummary(start, end).map {
            TransactionSummary(
                income = it.income,
                expense = it.expense,
                incomeCount = it.incomeCount,
                expenseCount = it.expenseCount
            )
        }

    override fun observeBalance(): Flow<Double> = transactionDao.observeBalance()

    override fun observeExpenseByCategory(start: Long, end: Long): Flow<List<CategoryTotal>> =
        transactionDao.observeExpenseByCategory(start, end).map { rows ->
            rows.map { CategoryTotal(categoryId = it.categoryId, amount = it.total) }
        }

    override fun observeDailyExpense(start: Long, end: Long): Flow<List<DayTotal>> =
        transactionDao.observeDailyExpense(start, end).map { rows ->
            rows.mapNotNull { row ->
                val date = boundaryOrNull("TransactionRepository") { java.time.LocalDate.parse(row.day) }
                date?.let { DayTotal(it, row.total) }
            }
        }

    /**
     * PRIVACY: notes and merchant names are passed through [SensitiveDataMasker] before
     * touching the database. This is the single choke-point for every write path —
     * SMS pipeline, Smart Add, manual entry, CSV import and Drive restore all land here.
     */
    private fun Transaction.sanitized(): Transaction = copy(
        note = SensitiveDataMasker.mask(note),
        merchant = merchant?.let { SensitiveDataMasker.mask(it) }
    )

    override suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.sanitized().toEntity())
    }

    override suspend fun saveTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { it.sanitized().toEntity() })
    }

    override suspend fun deleteTransaction(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { it.map { e -> e.toDomain() } }

    override suspend fun getCategoryById(id: String): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun saveCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    override fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets().map { it.map { e -> e.toDomain() } }

    override suspend fun getBudgetByCategory(categoryId: String): Budget? =
        budgetDao.getBudgetByCategory(categoryId)?.toDomain()

    override suspend fun saveBudget(budget: Budget) {
        budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun updateBudgetSpent(categoryId: String, spent: Double) {
        budgetDao.updateSpentAmount(categoryId, spent)
    }

    override suspend fun deleteBudget(budgetId: String) {
        budgetDao.deleteBudgetById(budgetId)
    }

    override fun getAllSavingsGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getAllSavingsGoals().map { it.map { e -> e.toDomain() } }

    override suspend fun saveSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.insertSavingsGoal(goal.toEntity())
    }

    override suspend fun deleteSavingsGoal(goalId: String) {
        savingsGoalDao.deleteSavingsGoalById(goalId)
    }

    override suspend fun updateSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.updateSavingsGoal(goal.toEntity())
    }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> =
        subscriptionDao.getActiveSubscriptions().map { it.map { e -> e.toDomain() } }

    override suspend fun saveSubscription(subscription: Subscription) {
        subscriptionDao.insertSubscription(subscription.toEntity())
    }

    override suspend fun deleteSubscription(id: String, active: Boolean) {
        subscriptionDao.setSubscriptionActive(id, active)
    }
}
