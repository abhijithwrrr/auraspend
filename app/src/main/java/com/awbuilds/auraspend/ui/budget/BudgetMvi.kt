package com.awbuilds.auraspend.ui.budget

import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.Category

data class BudgetViewState(
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    val editingBudget: Budget? = null,
    val selectedCategoryId: String = "",
    val limitAmount: String = "",
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val isAdding: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed class BudgetViewIntent {
    object LoadBudgets : BudgetViewIntent()
    data class SelectCategory(val categoryId: String) : BudgetViewIntent()
    data class AmountChanged(val amount: String) : BudgetViewIntent()
    data class PeriodChanged(val period: BudgetPeriod) : BudgetViewIntent()
    object SaveBudget : BudgetViewIntent()
    data class DeleteBudget(val budgetId: String) : BudgetViewIntent()
    object StartAdd : BudgetViewIntent()
    object CancelEdit : BudgetViewIntent()
}
