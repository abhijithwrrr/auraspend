package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.domain.model.BudgetPeriod
import com.awbuilds.auraspend.domain.model.RecurrenceFrequency
import com.awbuilds.auraspend.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The read path must never throw on stored data.
 *
 * Every *write* path already validated its enums — `BackupSerializer` skips bad
 * rows with `enumOrNull ?: return`, `CsvManager` try/catches, `SmsAiEnricher`
 * uses `runCatching`. The read path did not, and that asymmetry was a live bug:
 * one subscription row with `billingCycle = "ANNUAL"` threw
 * `IllegalArgumentException` out of a Room `Flow.map`, propagated through the
 * dashboard aggregation, and blanked the whole home screen with no way back short
 * of reinstalling. Observed on-device 2026-09-29.
 *
 * These tests pin the fail-soft behaviour, because the failure mode is silent
 * and a regression here costs users their app.
 */
class MappersEnumSafetyTest {

    @Test
    fun `transaction with an unknown type falls back to EXPENSE, not a crash`() {
        val entity = transactionEntity(type = "SPEND")
        assertEquals(TransactionType.EXPENSE, entity.toDomain().type)
    }

    @Test
    fun `valid transaction type is preserved`() {
        assertEquals(TransactionType.INCOME, transactionEntity(type = "INCOME").toDomain().type)
    }

    @Test
    fun `a null recurrence frequency stays null`() {
        // Defaulting this would make every non-recurring transaction look like
        // it recurs monthly.
        assertNull(transactionEntity(recurrenceFrequency = null).toDomain().recurrenceFrequency)
    }

    @Test
    fun `an unknown recurrence frequency falls back rather than throwing`() {
        assertEquals(
            RecurrenceFrequency.MONTHLY,
            transactionEntity(recurrenceFrequency = "FORTNIGHTLY").toDomain().recurrenceFrequency
        )
    }

    @Test
    fun `subscription with the wrong billing cycle still maps`() {
        // The exact row that bricked the dashboard.
        val entity = subscriptionEntity(billingCycle = "ANNUAL")
        assertEquals(RecurrenceFrequency.MONTHLY, entity.toDomain().billingCycle)
    }

    @Test
    fun `valid subscription billing cycle is preserved`() {
        assertEquals(
            RecurrenceFrequency.YEARLY,
            subscriptionEntity(billingCycle = "YEARLY").toDomain().billingCycle
        )
    }

    @Test
    fun `budget with an unknown period falls back to MONTHLY`() {
        val entity = com.awbuilds.auraspend.data.local.entities.BudgetEntity(
            id = "b1", categoryId = "cat_food", limitAmount = 100.0, period = "WEEKLYISH"
        )
        assertEquals(BudgetPeriod.MONTHLY, entity.toDomain().period)
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun transactionEntity(
        type: String = "EXPENSE",
        recurrenceFrequency: String? = null
    ) = com.awbuilds.auraspend.data.local.entities.TransactionEntity(
        id = "t1", amount = 10.0, categoryId = "cat_food", note = "",
        dateTimestamp = 0L, type = type, recurrenceFrequency = recurrenceFrequency
    )

    private fun subscriptionEntity(billingCycle: String) =
        com.awbuilds.auraspend.data.local.entities.SubscriptionEntity(
            id = "s1", name = "Netflix", amount = 649.0, categoryId = "cat_entertainment",
            billingCycle = billingCycle, nextBillingDateTimestamp = 0L, active = true
        )
}
