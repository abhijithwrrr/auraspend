package com.awbuilds.auraspend.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetViewState())
    val state: StateFlow<BudgetViewState> = _state.asStateFlow()

    fun handleIntent(intent: BudgetViewIntent) {
        when (intent) {
            is BudgetViewIntent.LoadBudgets -> loadBudgets()
            is BudgetViewIntent.SelectCategory -> _state.update { it.copy(selectedCategoryId = intent.categoryId) }
            is BudgetViewIntent.AmountChanged -> _state.update { it.copy(limitAmount = intent.amount) }
            is BudgetViewIntent.PeriodChanged -> _state.update { it.copy(selectedPeriod = intent.period) }
            is BudgetViewIntent.SaveBudget -> saveBudget()
            is BudgetViewIntent.DeleteBudget -> deleteBudget(intent.budgetId)
            is BudgetViewIntent.StartAdd -> {
                _state.update { it.copy(editingBudget = null, selectedCategoryId = "", limitAmount = "", selectedPeriod = BudgetPeriod.MONTHLY, isAdding = true) }
            }
            is BudgetViewIntent.CancelEdit -> _state.update { it.copy(editingBudget = null, isAdding = false) }
        }
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            val budgets = repository.getAllBudgets().first()
            val categories = repository.getAllCategories().first()
            _state.update { it.copy(budgets = budgets, categories = categories) }
        }
    }

    private fun saveBudget() {
        viewModelScope.launch {
            try {
                val s = _state.value
                val amount = s.limitAmount.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid amount")
                repository.saveBudget(
                    Budget(
                        id = s.editingBudget?.id ?: java.util.UUID.randomUUID().toString(),
                        categoryId = s.selectedCategoryId,
                        limitAmount = amount,
                        // Calculate spent amount based on transactions in the selected period
                        spentAmount = run {
                            // Determine period start and end timestamps
                            val now = java.time.LocalDateTime.now()
                            val periodStart = when (s.selectedPeriod) {
                                BudgetPeriod.WEEKLY -> now.with(java.time.DayOfWeek.MONDAY).with(java.time.LocalTime.MIN)
                                BudgetPeriod.MONTHLY -> now.withDayOfMonth(1).with(java.time.LocalTime.MIN)
                                BudgetPeriod.YEARLY -> now.withDayOfYear(1).with(java.time.LocalTime.MIN)
                            }
                            val periodEnd = when (s.selectedPeriod) {
                                BudgetPeriod.WEEKLY -> now.with(java.time.DayOfWeek.SUNDAY).with(java.time.LocalTime.MAX)
                                BudgetPeriod.MONTHLY -> now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).with(java.time.LocalTime.MAX)
                                BudgetPeriod.YEARLY -> now.with(java.time.temporal.TemporalAdjusters.lastDayOfYear()).with(java.time.LocalTime.MAX)
                            }
                            val startEpoch = periodStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val endEpoch = periodEnd.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            // Sum expenses for the selected category within period
                            repository.getTransactionsInRange(startEpoch, endEpoch)
                                .first()
                                .filter { it.categoryId == s.selectedCategoryId && it.type == com.awbuilds.auraspend.domain.model.TransactionType.EXPENSE }
                                .sumOf { it.amount }
                        },
                        period = s.selectedPeriod
                    )
                )
                _state.update { it.copy(editingBudget = null, selectedCategoryId = "", limitAmount = "", isAdding = false, isSaving = false) }
                loadBudgets()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }

    private fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val budget = _state.value.budgets.find { it.id == budgetId } ?: return@launch
            repository.deleteBudget(budgetId)
            loadBudgets()
        }
    }
}
