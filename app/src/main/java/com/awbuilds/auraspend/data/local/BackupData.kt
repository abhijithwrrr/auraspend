package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction

data class BackupData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val subscriptions: List<Subscription>
)
