package com.awbuilds.auraspend.ui.home

import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction

data class DashboardViewState(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val activeSubscriptions: List<Subscription> = emptyList(),
    val totalSubscriptionCost: Double = 0.0,
    val dailySpending: List<Pair<Long, Double>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class DashboardViewIntent {
    object LoadDashboard : DashboardViewIntent()
    data class RefreshTransactions(val force: Boolean) : DashboardViewIntent()
}

data class SpendingChartData(
    val days: List<String>,
    val amounts: List<Double>
)
