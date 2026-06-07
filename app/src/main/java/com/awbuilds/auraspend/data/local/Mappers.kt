package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.*
import com.awbuilds.auraspend.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    categoryId = categoryId,
    note = note,
    merchant = merchant,
    bankName = bankName,
    date = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTimestamp), ZoneId.systemDefault()),
    type = TransactionType.valueOf(type),
    isRecurring = isRecurring,
    recurrenceFrequency = recurrenceFrequency?.let { RecurrenceFrequency.valueOf(it) },
    nextDueDate = nextDueDateTimestamp?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    },
    subscriptionName = subscriptionName
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    note = note,
    merchant = merchant,
    bankName = bankName,
    dateTimestamp = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    type = type.name,
    isRecurring = isRecurring,
    recurrenceFrequency = recurrenceFrequency?.name,
    nextDueDateTimestamp = nextDueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    subscriptionName = subscriptionName
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    spentAmount = spentAmount,
    period = BudgetPeriod.valueOf(period)
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    spentAmount = spentAmount,
    period = period.name
)

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    billingCycle = RecurrenceFrequency.valueOf(billingCycle),
    nextBillingDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(nextBillingDateTimestamp), ZoneId.systemDefault()
    ),
    active = active
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    billingCycle = billingCycle.name,
    nextBillingDateTimestamp = nextBillingDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    active = active
)
