package com.awbuilds.auraspend.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "Recurring monthly" was computed two different ways on two screens, so the
 * Dashboard and the Plan hub showed the user two different numbers for the same
 * question: **₹3,745/mo** and **₹2,370.90/mo** with one annual subscription
 * among five monthly ones. The Dashboard was summing raw amounts, counting the
 * yearly charge twelve times over.
 *
 * Both now call [RecurringCost], so the maths and the inactivity filter are
 * pinned here rather than in whichever screen happens to break first.
 */
class RecurringCostTest {

    @Test
    fun `a monthly subscription costs its face value`() {
        assertEquals(649.0, RecurringCost.monthlyEquivalent(sub("Netflix", 649.0, RecurrenceFrequency.MONTHLY)), 0.001)
    }

    @Test
    fun `an annual subscription is divided by twelve`() {
        // 1499 / 12 = 124.916..., which is what made the old Plan figure
        // "₹2,370.90" and the Dashboard's "₹3,745" disagree.
        assertEquals(124.916, RecurringCost.monthlyEquivalent(sub("M365", 1499.0, RecurrenceFrequency.YEARLY)), 0.01)
    }

    @Test
    fun `inactive subscriptions are excluded from the total`() {
        val subs = listOf(
            sub("Netflix", 649.0, RecurrenceFrequency.MONTHLY, active = true),
            sub("Prime", 299.0, RecurrenceFrequency.MONTHLY, active = false)
        )
        assertEquals(649.0, RecurringCost.monthlyTotal(subs), 0.001)
    }

    @Test
    fun `a mixed set totals to the monthly-equivalent, not the raw sum`() {
        val subs = listOf(
            sub("Netflix", 649.0, RecurrenceFrequency.MONTHLY),
            sub("Spotify", 149.0, RecurrenceFrequency.MONTHLY),
            sub("GoogleOne", 149.0, RecurrenceFrequency.MONTHLY),
            sub("iCloud", 1299.0, RecurrenceFrequency.MONTHLY),
            sub("M365", 1499.0, RecurrenceFrequency.YEARLY)
        )
        val rawSum = subs.sumOf { it.amount }
        val monthly = RecurringCost.monthlyTotal(subs)

        // The whole bug in one assertion: these must not be the same number.
        assertEquals(3745.0, rawSum, 0.001)
        assertEquals(2370.916, monthly, 0.01)
    }

    @Test
    fun `an empty list totals to zero rather than throwing`() {
        assertEquals(0.0, RecurringCost.monthlyTotal(emptyList()), 0.001)
    }

    private fun sub(
        name: String,
        amount: Double,
        cycle: RecurrenceFrequency,
        active: Boolean = true
    ) = Subscription(
        id = name, name = name, amount = amount, categoryId = "cat_subscription",
        billingCycle = cycle, nextBillingDate = java.time.LocalDateTime.now(), active = active
    )
}
