package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.ClassificationMemoryEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.UnrecognizedSmsEntity
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.SavingsGoal
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction

data class BackupData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val subscriptions: List<Subscription>,
    val smsMessages: List<SmsMessageEntity> = emptyList(),
    /**
     * Savings goals and learned classifications are user data. They were previously
     * absent from the backup, so a restore silently dropped both.
     */
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val classificationMemory: List<ClassificationMemoryEntity> = emptyList(),
    /**
     * Bank messages the app could not read. User-visible data: it is the only
     * record that a transaction was *missed*, so a restore that dropped it would
     * hide the gap. Defaults to empty so a v3 payload — which predates the table
     * — still restores.
     */
    val unrecognizedSms: List<UnrecognizedSmsEntity> = emptyList()
)
