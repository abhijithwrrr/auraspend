package com.awbuilds.auraspend.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.BudgetSpending
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
            // Recompute spent from live transactions so budgets reflect auto-saved
            // SMS expenses and deletions without requiring a manual re-save.
            val transactions = repository.getAllTransactions().first()
            _state.update {
                it.copy(
                    budgets = BudgetSpending.withFreshSpent(budgets, transactions),
                    categories = categories
                )
            }
        }
    }

    private fun saveBudget() {
        viewModelScope.launch {
            try {
                val s = _state.value
                val amount = s.limitAmount.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid amount")
                val transactions = repository.getAllTransactions().first()
                val budget = Budget(
                    id = s.editingBudget?.id ?: java.util.UUID.randomUUID().toString(),
                    categoryId = s.selectedCategoryId,
                    limitAmount = amount,
                    spentAmount = 0.0, // recomputed on load from live transactions
                    period = s.selectedPeriod
                )
                repository.saveBudget(
                    budget.copy(spentAmount = BudgetSpending.spentFor(budget, transactions))
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
