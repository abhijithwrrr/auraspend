package com.awbuilds.auraspend.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val categoryId: String,
    val note: String,
    val merchant: String? = null,
    val bankName: String? = null,
    val dateTimestamp: Long,
    val type: String,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: String? = null,
    val nextDueDateTimestamp: Long? = null,
    val subscriptionName: String? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Int,
    val isDefault: Boolean
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val period: String
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val categoryId: String,
    val billingCycle: String,
    val nextBillingDateTimestamp: Long,
    val active: Boolean = true
)
