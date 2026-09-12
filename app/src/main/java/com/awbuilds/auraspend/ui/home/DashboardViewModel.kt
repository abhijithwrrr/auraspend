package com.awbuilds.auraspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.domain.model.BudgetSpending
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardViewState())
    val state: StateFlow<DashboardViewState> = _state.asStateFlow()

    fun handleIntent(intent: DashboardViewIntent) {
        when (intent) {
            is DashboardViewIntent.LoadDashboard -> loadDashboard()
            is DashboardViewIntent.RefreshTransactions -> loadDashboard()
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val now = LocalDateTime.now()
                val monthStart = now.withDayOfMonth(1).with(LocalTime.MIN)
                val monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                val monthStartEpoch = monthStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val monthEndEpoch = monthEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val weekStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN)

                val transactions = repository.getAllTransactions().first()
                val monthlyTransactions = transactions.filter { t ->
                    val ts = t.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    ts in monthStartEpoch..monthEndEpoch
                }

                // Single pass for month totals + counts (was 5 separate passes).
                var income = 0.0
                var expense = 0.0
                var incomeCount = 0
                var expenseCount = 0
                val categoryTotalsMap = HashMap<String, Double>()
                monthlyTransactions.forEach { t ->
                    if (t.type == TransactionType.INCOME) {
                        income += t.amount
                        incomeCount++
                    } else {
                        expense += t.amount
                        expenseCount++
                        categoryTotalsMap[t.categoryId] = (categoryTotalsMap[t.categoryId] ?: 0.0) + t.amount
                    }
                }
                val categoryTotals = categoryTotalsMap.entries
                    .map { it.key to it.value }
                    .sortedByDescending { it.second }

                var balance = 0.0
                transactions.forEach { t ->
                    balance += if (t.type == TransactionType.INCOME) t.amount else -t.amount
                }

                // Weekly chart: one pass, bucketed by local date (DST-safe).
                val weekDates = (0..6).map { weekStart.plusDays(it.toLong()).toLocalDate() }
                val weekEndEpoch = weekDates.last()
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val weekStartEpoch = weekStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dailyTotals = HashMap<java.time.LocalDate, Double>()
                transactions.forEach { t ->
                    if (t.type != TransactionType.EXPENSE) return@forEach
                    val ts = t.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    if (ts in weekStartEpoch..weekEndEpoch) {
                        val day = t.date.toLocalDate()
                        dailyTotals[day] = (dailyTotals[day] ?: 0.0) + t.amount
                    }
                }
                val dailySpending = weekDates.map { day ->
                    day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to
                        (dailyTotals[day] ?: 0.0)
                }

                val categories = repository.getAllCategories().first()
                val budgets = BudgetSpending.withFreshSpent(
                    repository.getAllBudgets().first(),
                    transactions
                )
                val subscriptions = repository.getActiveSubscriptions().first()
                val totalSubscriptionCost = subscriptions.sumOf { it.amount }

                _state.update {
                    it.copy(
                        totalBalance = balance,
                        monthlyIncome = income,
                        monthlyExpense = expense,
                        monthlyIncomeCount = incomeCount,
                        monthlyExpenseCount = expenseCount,
                        recentTransactions = transactions.take(20),
                        categories = categories,
                        budgets = budgets,
                        activeSubscriptions = subscriptions,
                        totalSubscriptionCost = totalSubscriptionCost,
                        dailySpending = dailySpending,
                        categoryMonthTotals = categoryTotals,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
