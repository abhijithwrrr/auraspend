package com.awbuilds.auraspend.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class BudgetSpendingTest {

    private fun txn(
        amount: Double,
        categoryId: String,
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDateTime
    ) = Transaction(
        amount = amount,
        categoryId = categoryId,
        note = "",
        date = date,
        type = type
    )

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 23, 15, 0)

    @Test
    fun `monthly period covers whole august`() {
        val (start, end) = BudgetSpending.periodRange(BudgetPeriod.MONTHLY, now)
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), start)
        assertEquals(LocalDateTime.of(2026, 8, 31, 23, 59, 59, 999_999_999), end)
    }

    @Test
    fun `weekly period is monday to sunday`() {
        // 2026-08-23 is a Sunday; the week runs Mon 17 Aug .. Sun 23 Aug.
        val (start, end) = BudgetSpending.periodRange(BudgetPeriod.WEEKLY, now)
        assertEquals(LocalDateTime.of(2026, 8, 17, 0, 0), start)
        assertEquals(LocalDateTime.of(2026, 8, 23, 23, 59, 59, 999_999_999), end)
    }

    @Test
    fun `spent sums only matching category expenses inside period`() {
        val budget = Budget(categoryId = "cat_food", limitAmount = 1000.0, period = BudgetPeriod.MONTHLY)
        val transactions = listOf(
            txn(250.0, "cat_food", date = LocalDateTime.of(2026, 8, 5, 10, 0)),
            txn(125.50, "cat_food", date = LocalDateTime.of(2026, 8, 20, 22, 30)),
            txn(900.0, "cat_transport", date = LocalDateTime.of(2026, 8, 10, 9, 0)),
            // Income in the same category never counts against a spending budget.
            txn(500.0, "cat_food", type = TransactionType.INCOME, date = LocalDateTime.of(2026, 8, 12, 9, 0)),
            // Last month's food spend must not leak into this month.
            txn(400.0, "cat_food", date = LocalDateTime.of(2026, 7, 28, 9, 0))
        )
        assertEquals(375.50, BudgetSpending.spentFor(budget, transactions, now), 0.001)
    }

    @Test
    fun `withFreshSpent recomputes stale stored amounts`() {
        val stale = Budget(categoryId = "cat_food", limitAmount = 500.0, spentAmount = 999.0, period = BudgetPeriod.MONTHLY)
        val transactions = listOf(
            txn(120.0, "cat_food", date = LocalDateTime.of(2026, 8, 2, 9, 0))
        )
        val fresh = BudgetSpending.withFreshSpent(listOf(stale), transactions, now)
        assertEquals(120.0, fresh.single().spentAmount, 0.001)
    }
}
