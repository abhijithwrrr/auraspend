package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class BackupSerializerTest {

    private val testTransaction = Transaction(
        id = "tx-1",
        amount = 1500.0,
        categoryId = "cat-food",
        note = "Lunch",
        merchant = "Restaurant A",
        bankName = "HDFC Bank",
        date = LocalDateTime.of(2026, 6, 1, 12, 30),
        type = TransactionType.EXPENSE,
        isRecurring = false
    )

    private val testCategory = Category(
        id = "cat-food", name = "Food", icon = "🍕", color = 0xFFFF5722.toInt(), isDefault = true
    )

    private val testBudget = Budget(
        id = "bgt-1", categoryId = "cat-food", limitAmount = 10000.0, spentAmount = 2500.0, period = BudgetPeriod.MONTHLY
    )

    private val testSubscription = Subscription(
        id = "sub-1", name = "Netflix", amount = 499.0, categoryId = "cat-ent",
        billingCycle = RecurrenceFrequency.MONTHLY,
        nextBillingDate = LocalDateTime.of(2026, 7, 1, 0, 0),
        active = true
    )

    @Test
    fun `serialize and deserialize round-trip`() {
        val data = BackupData(
            transactions = listOf(testTransaction),
            categories = listOf(testCategory),
            budgets = listOf(testBudget),
            subscriptions = listOf(testSubscription)
        )

        val json = BackupSerializer.serialize(data)
        assertTrue(json.contains("tx-1"))
        assertTrue(json.contains("cat-food"))
        assertTrue(json.contains("Netflix"))
        assertTrue(json.contains("MONTHLY"))

        val restored = BackupSerializer.deserialize(json)
        assertEquals(1, restored.transactions.size)
        assertEquals(1, restored.categories.size)
        assertEquals(1, restored.budgets.size)
        assertEquals(1, restored.subscriptions.size)

        assertEquals(1500.0, restored.transactions[0].amount, 0.001)
        assertEquals("Food", restored.categories[0].name)
        assertEquals(10000.0, restored.budgets[0].limitAmount, 0.001)
        assertEquals("Netflix", restored.subscriptions[0].name)
    }

    @Test
    fun `deserialize empty arrays`() {
        val json = BackupSerializer.serialize(BackupData(emptyList(), emptyList(), emptyList(), emptyList()))
        val restored = BackupSerializer.deserialize(json)
        assertTrue(restored.transactions.isEmpty())
        assertTrue(restored.categories.isEmpty())
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.subscriptions.isEmpty())
    }

    @Test
    fun `deserialize null arrays returns empty lists`() {
        val json = """{"version":1}"""
        val restored = BackupSerializer.deserialize(json)
        assertTrue(restored.transactions.isEmpty())
        assertTrue(restored.categories.isEmpty())
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.subscriptions.isEmpty())
    }

    @Test
    fun `serialize recurring transaction with recurrence`() {
        val t = Transaction(
            id = "tx-recur", amount = 999.0, categoryId = "cat-sub",
            note = "Subscription", date = LocalDateTime.now(), type = TransactionType.EXPENSE,
            isRecurring = true, recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = LocalDateTime.now().plusMonths(1), subscriptionName = "Spotify"
        )
        val data = BackupData(listOf(t), emptyList(), emptyList(), emptyList())
        val json = BackupSerializer.serialize(data)
        assertTrue(json.contains("Spotify"))
        assertTrue(json.contains("MONTHLY"))

        val restored = BackupSerializer.deserialize(json)
        assertTrue(restored.transactions[0].isRecurring)
        assertEquals(RecurrenceFrequency.MONTHLY, restored.transactions[0].recurrenceFrequency)
        assertEquals("Spotify", restored.transactions[0].subscriptionName)
    }

    @Test
    fun `version field is present in serialized output`() {
        val json = BackupSerializer.serialize(BackupData(emptyList(), emptyList(), emptyList(), emptyList()))
        assertTrue(json.contains("\"version\""))
    }
}
