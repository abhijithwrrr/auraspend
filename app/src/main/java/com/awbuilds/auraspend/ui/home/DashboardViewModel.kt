package com.awbuilds.auraspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

                val income = monthlyTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }

                val expense = monthlyTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                val balance = transactions
                    .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }

                val dailySpending = (0..6).map { dayOffset ->
                    val day = weekStart.plusDays(dayOffset.toLong())
                    val dayStart = day.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val dayEnd = day.with(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val spent = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .filter { t ->
                            val ts = t.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            ts in dayStart..dayEnd
                        }
                        .sumOf { t -> t.amount }
                    Pair(dayStart, spent)
                }

                val categories = repository.getAllCategories().first()
                val budgets = repository.getAllBudgets().first()
                val subscriptions = repository.getActiveSubscriptions().first()
                val totalSubscriptionCost = subscriptions.sumOf { it.amount }

                _state.update {
                    it.copy(
                        totalBalance = balance,
                        monthlyIncome = income,
                        monthlyExpense = expense,
                        recentTransactions = transactions.take(20),
                        categories = categories,
                        budgets = budgets,
                        activeSubscriptions = subscriptions,
                        totalSubscriptionCost = totalSubscriptionCost,
                        dailySpending = dailySpending,
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
