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
                        spentAmount = s.editingBudget?.spentAmount ?: 0.0,
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
            repository.saveBudget(budget.copy(limitAmount = 0.0))
            loadBudgets()
        }
    }
}
