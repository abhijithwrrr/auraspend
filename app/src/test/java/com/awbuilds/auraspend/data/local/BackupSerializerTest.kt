package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.domain.model.Budget
import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.Subscription
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks in backup round-trips: transactions keep their source SMS id, and the message queue
 * survives serialize -> deserialize so a restore never re-classifies (or double-saves) messages
 * the user already handled.
 */
class BackupSerializerTest {

    private val transaction = Transaction(
        id = "tx-1",
        amount = 149.0,
        categoryId = "cat_food",
        note = "Swiggy order",
        merchant = "Swiggy",
        date = LocalDateTime.of(2026, 8, 1, 12, 0),
        type = TransactionType.EXPENSE,
        sourceSmsId = "sms-9"
    )

    private val message = SmsMessageEntity(
        id = "sms-9",
        address = "HDFCBank",
        body = "INR 149 debited at Swiggy",
        receivedAt = 1_782_000_000_000L,
        status = SmsMessageStatus.SAVED.name,
        amount = 149.0,
        type = "EXPENSE",
        merchant = "Swiggy",
        categoryId = "cat_food",
        isSubscription = false,
        confidence = 0.9f,
        attempts = 1,
        updatedAt = 1_782_000_000_001L
    )

    private val backup = BackupData(
        transactions = listOf(transaction),
        categories = listOf(
            Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0, isDefault = true)
        ),
        budgets = listOf(
            Budget(id = "b1", categoryId = "cat_food", limitAmount = 5000.0, period = BudgetPeriod.MONTHLY)
        ),
        subscriptions = listOf(
            Subscription(
                id = "sub1", name = "Netflix", amount = 199.0, categoryId = "cat_ent",
                billingCycle = com.awbuilds.auraspend.domain.model.RecurrenceFrequency.MONTHLY,
                nextBillingDate = LocalDateTime.of(2026, 9, 1, 0, 0)
            )
        ),
        smsMessages = listOf(message)
    )

    @Test
    fun `transactions round trip with source sms id`() {
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(backup))
        assertEquals("sms-9", restored.transactions.single().sourceSmsId)
        assertEquals(149.0, restored.transactions.single().amount, 0.0)
    }

    @Test
    fun `message queue round trips preserving terminal status`() {
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(backup))
        val m = restored.smsMessages.single()
        assertEquals("sms-9", m.id)
        assertEquals(SmsMessageStatus.SAVED.name, m.status)
        assertEquals(149.0, m.amount!!, 0.0)
        assertEquals("EXPENSE", m.type)
        assertEquals("Swiggy", m.merchant)
        assertEquals("cat_food", m.categoryId)
        assertEquals(1, m.attempts)
    }

    @Test
    fun `null amount and merchant survive round trip`() {
        val withNulls = backup.copy(
            smsMessages = listOf(
                message.copy(amount = null, merchant = null, type = null, categoryId = null)
            )
        )
        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(withNulls))
        val m = restored.smsMessages.single()
        assertNull(m.amount)
        assertNull(m.merchant)
        assertNull(m.type)
        assertNull(m.categoryId)
    }

    @Test
    fun `unknown status falls back to NEW`() {
        val json = BackupSerializer.serialize(
            backup.copy(smsMessages = listOf(message.copy(status = "NOT_A_STATUS")))
        )
        val restored = BackupSerializer.deserialize(json)
        assertEquals(SmsMessageStatus.NEW.name, restored.smsMessages.single().status)
    }

    @Test
    fun `legacy backup without smsMessages still deserializes`() {
        val legacyJson = BackupSerializer.serialize(backup.copy(smsMessages = emptyList()))
        val restored = BackupSerializer.deserialize(legacyJson)
        assertEquals(0, restored.smsMessages.size)
        assertEquals(1, restored.transactions.size)
    }
}
