package com.awbuilds.auraspend.domain.model

import java.util.UUID
import java.time.LocalDateTime

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val categoryId: String,
    val note: String,
    val merchant: String? = null,
    val bankName: String? = null,
    val date: LocalDateTime,
    val type: TransactionType,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val nextDueDate: LocalDateTime? = null,
    val subscriptionName: String? = null
)

enum class TransactionType {
    INCOME, EXPENSE
}

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Int,
    val isDefault: Boolean = false
)

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val period: BudgetPeriod
)

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY
}

data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val categoryId: String,
    val billingCycle: RecurrenceFrequency,
    val nextBillingDate: LocalDateTime,
    val active: Boolean = true
)

data class ParsedBankMessage(
    val amount: Double? = null,
    val type: TransactionType? = null,
    val merchant: String? = null,
    val bankName: String? = null,
    val date: LocalDateTime? = null,
    val note: String? = null,
    val categoryId: String? = null,
    val confidence: Float = 0f,
    val rawMessage: String = ""
)
