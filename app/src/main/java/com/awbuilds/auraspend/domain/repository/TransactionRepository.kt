package com.awbuilds.auraspend.domain.repository

import com.awbuilds.auraspend.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    fun getRecurringTransactions(): Flow<List<Transaction>>
    suspend fun saveTransaction(transaction: Transaction)
    suspend fun saveTransactions(transactions: List<Transaction>)
    suspend fun deleteTransaction(id: String)

    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun saveCategories(categories: List<Category>)
    suspend fun deleteCategory(id: String)

    fun getAllBudgets(): Flow<List<Budget>>
    suspend fun getBudgetByCategory(categoryId: String): Budget?
    suspend fun saveBudget(budget: Budget)
    suspend fun updateBudgetSpent(categoryId: String, spent: Double)

    fun getActiveSubscriptions(): Flow<List<Subscription>>
    suspend fun saveSubscription(subscription: Subscription)
    suspend fun deleteSubscription(id: String, active: Boolean)
}
