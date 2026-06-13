package com.awbuilds.auraspend.data.repository

import com.awbuilds.auraspend.data.local.toDomain
import com.awbuilds.auraspend.data.local.toEntity
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

    override suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun saveTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { it.toEntity() })
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
