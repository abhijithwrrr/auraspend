package com.awbuilds.auraspend.domain.model

/**
 * Normalises recurring charges to a monthly figure.
 *
 * This exists because two screens were computing "recurring monthly" two
 * different ways and showing the user two different answers for the same
 * question. On 2026-09-29, with one ANNUAL subscription of ₹1,499 among five
 * monthly ones, the Dashboard read **₹3,745/mo** and the Plan hub read
 * **₹2,370.90/mo**.
 *
 * The Dashboard was wrong: it summed raw amounts, so a yearly charge was
 * counted twelve times over. The Plan hub was right about the maths but wrong
 * about the presentation — it divided first and then printed the fraction, so a
 * figure that is conceptually "about ₹2,371 a month" rendered as
 * `₹2,370.90`.
 *
 * Both now call [monthlyTotal]. The rounding to whole rupees is deliberate and
 * is the caller's to apply via `formatMoney(..., fractionDigits = 0)`, matching
 * every other summary figure in the app.
 */
object RecurringCost {

    /**
     * What one [subscription] costs per month.
     *
     * Uses 30-day months and 52-week years, which is the same approximation the
     * rest of the app already uses for period maths.
     */
    fun monthlyEquivalent(subscription: Subscription): Double =
        subscription.amount * when (subscription.billingCycle) {
            RecurrenceFrequency.DAILY -> 30.0
            RecurrenceFrequency.WEEKLY -> 52.0 / 12.0
            RecurrenceFrequency.MONTHLY -> 1.0
            RecurrenceFrequency.YEARLY -> 1.0 / 12.0
        }

    /** Combined monthly cost of [subscriptions]. Inactive ones are excluded. */
    fun monthlyTotal(subscriptions: List<Subscription>): Double =
        subscriptions.filter { it.active }.sumOf { monthlyEquivalent(it) }
}
