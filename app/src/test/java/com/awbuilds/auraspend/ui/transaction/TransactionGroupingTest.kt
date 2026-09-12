package com.awbuilds.auraspend.ui.transaction

import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The old grouping used epoch-math (`now - now % 86400000`), which bucketed by
 * UTC days — wrong for IST evenings/mornings. These tests lock in LOCAL calendar
 * grouping with an injectable "today".
 */
class TransactionGroupingTest {

    private fun txn(amount: Double, type: TransactionType, date: LocalDateTime) = Transaction(
        amount = amount,
        categoryId = "cat_other",
        note = "",
        date = date,
        type = type
    )

    // Wednesday 2026-08-19; the 1st was a Saturday.
    private val today: LocalDate = LocalDate.of(2026, 8, 19)

    @Test
    fun `buckets follow the local calendar`() {
        val groups = groupTransactionsByDate(
            listOf(
                txn(10.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 19, 9, 0)),   // today
                txn(20.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 18, 23, 30)), // yesterday
                txn(30.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 17, 12, 0)),  // Mon, this week
                txn(40.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 5, 12, 0)),   // earlier this month
                txn(50.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 7, 20, 12, 0))   // older
            ),
            today = today
        )

        assertEquals(
            listOf("Today", "Yesterday", "This Week", "This Month", "Older"),
            groups.map { it.label }
        )
        assertEquals(listOf(1, 1, 1, 1, 1), groups.map { it.transactions.size })
    }

    @Test
    fun `group net is income positive expense negative`() {
        val groups = groupTransactionsByDate(
            listOf(
                txn(100.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 19, 8, 0)),
                txn(250.0, TransactionType.INCOME, LocalDateTime.of(2026, 8, 19, 9, 0)),
                txn(40.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 19, 10, 0))
            ),
            today = today
        )

        val todayGroup = groups.single()
        assertEquals("Today", todayGroup.label)
        assertEquals(250.0 - 140.0, todayGroup.netAmount, 0.001)
    }

    @Test
    fun `late night transactions near midnight land in their own local day`() {
        // 00:15 today is TODAY even though epoch-day math once pushed it around.
        val groups = groupTransactionsByDate(
            listOf(txn(5.0, TransactionType.EXPENSE, LocalDateTime.of(2026, 8, 19, 0, 15))),
            today = today
        )
        assertEquals("Today", groups.single().label)
    }

    @Test
    fun `empty input yields no groups`() {
        assertEquals(0, groupTransactionsByDate(emptyList(), today = today).size)
    }
}
