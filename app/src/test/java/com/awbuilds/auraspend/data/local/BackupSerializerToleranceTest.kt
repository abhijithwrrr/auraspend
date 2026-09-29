package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.ClassificationMemoryEntity
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.model.SavingsGoal
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A malformed or older-schema backup must never crash the app.
 *
 * Restore is user-initiated, and it previously called `getString` / `getDouble` /
 * `enum.valueOf` on every field with no guarding. A truncated or hand-edited backup
 * therefore threw out of `deserialize` into an unguarded `scope.launch` and killed
 * the app. Deserialization now skips individual unreadable entries and keeps the rest,
 * which these tests pin down.
 */
class BackupSerializerToleranceTest {

    private val validTransaction = """
        {"id":"tx-1","amount":149.0,"categoryId":"cat_food","note":"Swiggy",
         "merchant":"Swiggy","bankName":"HDFC","date":"2026-08-01T12:00",
         "type":"EXPENSE","isRecurring":false,"sourceSmsId":"sms-9"}
    """.trimIndent()

    private fun json(vararg sections: Pair<String, String>) =
        sections.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }

    @Test
    fun `a transaction missing a required field is skipped, not thrown on`() {
        val payload = json(
            "version" to "3",
            "transactions" to """[{"amount":10.0,"categoryId":"c","date":"2026-08-01T12:00","type":"EXPENSE"}]"""
        )
        val result = BackupSerializer.deserialize(payload)
        assertTrue("transaction without an id must be skipped", result.transactions.isEmpty())
    }

    @Test
    fun `an unknown transaction type is skipped without failing the whole restore`() {
        val payload = json(
            "version" to "3",
            "transactions" to "[$validTransaction,{\"id\":\"tx-2\",\"amount\":10.0," +
                "\"categoryId\":\"c\",\"date\":\"2026-08-01T12:00\",\"type\":\"SIDEWAYS\"}]"
        )
        val result = BackupSerializer.deserialize(payload)
        assertEquals("the valid transaction must still load", 1, result.transactions.size)
        assertEquals("tx-1", result.transactions.first().id)
    }

    @Test
    fun `a non-numeric amount is treated as absent rather than as zero`() {
        val payload = json(
            "version" to "3",
            "transactions" to """[{"id":"tx-2","amount":"abc","categoryId":"c",""" +
                """"date":"2026-08-01T12:00","type":"EXPENSE"}]"""
        )
        val result = BackupSerializer.deserialize(payload)
        assertTrue("a 0.0 amount is worse than dropping the row", result.transactions.isEmpty())
    }

    @Test
    fun `an unparseable date skips the entry`() {
        val payload = json(
            "version" to "3",
            "transactions" to """[{"id":"tx-2","amount":10.0,"categoryId":"c",""" +
                """"date":"31/08/2026","type":"EXPENSE"}]"""
        )
        assertTrue(BackupSerializer.deserialize(payload).transactions.isEmpty())
    }

    @Test
    fun `a non-object entry in the array is skipped`() {
        val payload = json(
            "version" to "3",
            "transactions" to """[42,"junk",$validTransaction]"""
        )
        val result = BackupSerializer.deserialize(payload)
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun `a missing section yields an empty list rather than an error`() {
        val result = BackupSerializer.deserialize(json("version" to "1"))
        assertTrue(result.transactions.isEmpty())
        assertTrue(result.categories.isEmpty())
        assertTrue(result.budgets.isEmpty())
        assertTrue(result.subscriptions.isEmpty())
    }

    @Test
    fun `an unknown budget period is skipped`() {
        val payload = json(
            "version" to "3",
            "budgets" to """[{"id":"b-1","categoryId":"cat_food","limitAmount":1000.0,"period":"FORTNIGHTLY"}]"""
        )
        assertTrue(BackupSerializer.deserialize(payload).budgets.isEmpty())
    }

    @Test
    fun `an unknown subscription cycle is skipped`() {
        val payload = json(
            "version" to "3",
            "subscriptions" to """[{"id":"s-1","name":"Netflix","amount":649.0,""" +
                """"categoryId":"cat_ent","billingCycle":"EVERY_FORTNIGHT",""" +
                """"nextBillingDate":"2026-09-01T10:00","active":true}]"""
        )
        assertTrue(BackupSerializer.deserialize(payload).subscriptions.isEmpty())
    }

    // ── New sections ─────────────────────────────────────────────────────────
    // Savings goals and classification memory were not in the backup at all, so a
    // restore silently discarded every goal and everything the user had taught the app.

    @Test
    fun `savings goals round trip`() {
        val original = BackupData(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            subscriptions = emptyList(),
            savingsGoals = listOf(
                SavingsGoal(
                    id = "g-1",
                    name = "New laptop",
                    targetAmount = 120_000.0,
                    currentAmount = 35_000.0,
                    deadline = 1_800_000_000_000L
                )
            )
        )
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(original))
        assertEquals(1, restored.savingsGoals.size)
        val goal = restored.savingsGoals.first()
        assertEquals("New laptop", goal.name)
        assertEquals(120_000.0, goal.targetAmount, 0.001)
        assertEquals(35_000.0, goal.currentAmount, 0.001)
        assertEquals(1_800_000_000_000L, goal.deadline)
    }

    @Test
    fun `a goal with no deadline round trips as null`() {
        val original = BackupData(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            subscriptions = emptyList(),
            savingsGoals = listOf(SavingsGoal(id = "g-2", name = "Trip", targetAmount = 5_000.0))
        )
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(original))
        assertNull(restored.savingsGoals.first().deadline)
    }

    @Test
    fun `classification memory round trips`() {
        val original = BackupData(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            subscriptions = emptyList(),
            classificationMemory = listOf(
                ClassificationMemoryEntity(
                    memoryKey = "swiggy",
                    categoryId = "cat_food",
                    type = "EXPENSE",
                    source = "user_save",
                    hits = 7,
                    updatedAt = 1_782_000_000_000L
                )
            )
        )
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(original))
        assertEquals(1, restored.classificationMemory.size)
        val memory = restored.classificationMemory.first()
        assertEquals("swiggy", memory.memoryKey)
        assertEquals("cat_food", memory.categoryId)
        assertEquals("EXPENSE", memory.type)
        assertEquals(7, memory.hits)
    }

    @Test
    fun `a full backup with every section round trips`() {
        val original = BackupData(
            transactions = listOf(
                Transaction(
                    id = "tx-1",
                    amount = 149.0,
                    categoryId = "cat_food",
                    note = "Swiggy order",
                    merchant = "Swiggy",
                    date = LocalDateTime.of(2026, 8, 1, 12, 0),
                    type = TransactionType.EXPENSE,
                    sourceSmsId = "sms-9"
                )
            ),
            categories = listOf(Category(id = "cat_food", name = "Food", icon = "🍔", color = 1)),
            budgets = listOf(
                Budget(
                    id = "b-1",
                    categoryId = "cat_food",
                    limitAmount = 5_000.0,
                    spentAmount = 1_200.0,
                    period = BudgetPeriod.MONTHLY
                )
            ),
            subscriptions = listOf(
                Subscription(
                    id = "s-1",
                    name = "Netflix",
                    amount = 649.0,
                    categoryId = "cat_ent",
                    billingCycle = RecurrenceFrequency.MONTHLY,
                    nextBillingDate = LocalDateTime.of(2026, 9, 1, 10, 0)
                )
            ),
            savingsGoals = listOf(SavingsGoal(id = "g-1", name = "Laptop", targetAmount = 100_000.0))
        )
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(original))
        assertEquals(1, restored.transactions.size)
        assertEquals("sms-9", restored.transactions.first().sourceSmsId)
        assertEquals(1, restored.categories.size)
        assertEquals(1, restored.budgets.size)
        assertEquals(1, restored.subscriptions.size)
        assertEquals(1, restored.savingsGoals.size)
        assertNotNull(restored)
    }

    @Test
    fun `an older backup without the new sections still deserializes`() {
        // A v2 backup taken before goals/memory existed must load cleanly.
        val legacy = """
            {"version":2,"transactions":[$validTransaction],"categories":[],
             "budgets":[],"subscriptions":[],"smsMessages":[]}
        """.trimIndent()
        val result = BackupSerializer.deserialize(legacy)
        assertEquals(1, result.transactions.size)
        assertTrue(result.savingsGoals.isEmpty())
        assertTrue(result.classificationMemory.isEmpty())
    }
}
