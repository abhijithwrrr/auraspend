package com.awbuilds.auraspend.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ModelsTest {

    @Test
    fun `transaction defaults to random UUID`() {
        val t1 = Transaction(amount = 100.0, categoryId = "cat1", date = LocalDateTime.now(), type = TransactionType.EXPENSE)
        val t2 = Transaction(amount = 100.0, categoryId = "cat1", date = LocalDateTime.now(), type = TransactionType.EXPENSE)
        assertNotEquals(t1.id, t2.id)
    }

    @Test
    fun `transaction expense type`() {
        val t = Transaction(amount = 50.0, categoryId = "food", date = LocalDateTime.now(), type = TransactionType.EXPENSE)
        assertEquals(TransactionType.EXPENSE, t.type)
        assertEquals(50.0, t.amount, 0.001)
    }

    @Test
    fun `transaction income type`() {
        val t = Transaction(amount = 1000.0, categoryId = "salary", date = LocalDateTime.now(), type = TransactionType.INCOME)
        assertEquals(TransactionType.INCOME, t.type)
        assertEquals(1000.0, t.amount, 0.001)
    }

    @Test
    fun `transaction optional fields default to null`() {
        val t = Transaction(amount = 25.0, categoryId = "cat1", note = "", date = LocalDateTime.now(), type = TransactionType.EXPENSE)
        assertNull(t.merchant)
        assertNull(t.bankName)
        assertFalse(t.isRecurring)
        assertNull(t.recurrenceFrequency)
        assertNull(t.nextDueDate)
        assertNull(t.subscriptionName)
    }

    @Test
    fun `category defaults`() {
        val c = Category(name = "Food", icon = "🍕", color = 0xFF0000FF.toInt())
        assertNotNull(c.id)
        assertFalse(c.isDefault)
        assertEquals("Food", c.name)
        assertEquals("🍕", c.icon)
    }

    @Test
    fun `budget creation`() {
        val b = Budget(categoryId = "cat1", limitAmount = 5000.0, period = BudgetPeriod.MONTHLY)
        assertNotNull(b.id)
        assertEquals(0.0, b.spentAmount, 0.001)
        assertEquals(BudgetPeriod.MONTHLY, b.period)
        assertEquals(5000.0, b.limitAmount, 0.001)
    }

    @Test
    fun `subscription defaults to active`() {
        val s = Subscription(
            name = "Netflix", amount = 199.0, categoryId = "entertainment",
            billingCycle = RecurrenceFrequency.MONTHLY,
            nextBillingDate = LocalDateTime.now()
        )
        assertTrue(s.active)
        assertEquals(RecurrenceFrequency.MONTHLY, s.billingCycle)
    }

    @Test
    fun `parsed bank message defaults`() {
        val msg = ParsedBankMessage()
        assertNull(msg.amount)
        assertNull(msg.type)
        assertNull(msg.merchant)
        assertEquals(0f, msg.confidence)
        assertEquals("", msg.rawMessage)
    }

    @Test
    fun `recurrenceFrequency enum values`() {
        assertTrue(RecurrenceFrequency.entries.containsAll(
            listOf(RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY)
        ))
    }

    @Test
    fun `budgetPeriod enum values`() {
        assertTrue(BudgetPeriod.entries.containsAll(
            listOf(BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY, BudgetPeriod.YEARLY)
        ))
    }

    @Test
    fun `transaction type enum values`() {
        assertTrue(TransactionType.entries.containsAll(
            listOf(TransactionType.INCOME, TransactionType.EXPENSE)
        ))
    }
}
