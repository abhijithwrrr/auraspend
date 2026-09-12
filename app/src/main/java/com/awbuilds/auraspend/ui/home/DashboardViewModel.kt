package com.awbuilds.auraspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetSpending
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null

    fun handleIntent(intent: DashboardViewIntent) {
        when (intent) {
            is DashboardViewIntent.LoadDashboard -> loadDashboard()
            is DashboardViewIntent.RefreshTransactions -> loadDashboard()
        }
    }

    /**
     * Reactive dashboard: every write (transaction, budget, category,
     * subscription) re-aggregates automatically, so saving from Quick Add or
     * the SMS pipeline is reflected without leaving the screen.
     */
    private fun loadDashboard() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true) }

        loadJob = viewModelScope.launch {
            combine(
                repository.getAllTransactions(),
                repository.getAllCategories(),
                repository.getAllBudgets(),
                repository.getActiveSubscriptions()
            ) { transactions, categories, budgets, subscriptions ->
                DashboardInputs(transactions, categories, budgets, subscriptions)
            }
                .catch { e ->
                    AuraLog.e(TAG, "Dashboard aggregation failed", e)
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { inputs -> aggregate(inputs) }
        }
    }

    private fun aggregate(inputs: DashboardInputs) {
        val now = LocalDateTime.now()
        val monthStart = now.withDayOfMonth(1).with(LocalTime.MIN)
        val monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
        val monthStartEpoch = monthStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndEpoch = monthEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val weekStart = now.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        ).with(LocalTime.MIN)

        val transactions = inputs.transactions
        val monthlyTransactions = transactions.filter { t ->
            val ts = t.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ts in monthStartEpoch..monthEndEpoch
        }

        // Single pass for month totals + counts.
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
        val dailyTotals = HashMap<LocalDate, Double>()
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

        val budgets = BudgetSpending.withFreshSpent(inputs.budgets, transactions)
        val totalSubscriptionCost = inputs.subscriptions.sumOf { it.amount }

        _state.update {
            it.copy(
                totalBalance = balance,
                monthlyIncome = income,
                monthlyExpense = expense,
                monthlyIncomeCount = incomeCount,
                monthlyExpenseCount = expenseCount,
                recentTransactions = transactions.take(20),
                categories = inputs.categories,
                budgets = budgets,
                activeSubscriptions = inputs.subscriptions,
                totalSubscriptionCost = totalSubscriptionCost,
                dailySpending = dailySpending,
                categoryMonthTotals = categoryTotals,
                isLoading = false,
                error = null
            )
        }
    }

    private data class DashboardInputs(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val budgets: List<Budget>,
        val subscriptions: List<Subscription>
    )

    private companion object {
        const val TAG = "Dashboard"
    }
}
