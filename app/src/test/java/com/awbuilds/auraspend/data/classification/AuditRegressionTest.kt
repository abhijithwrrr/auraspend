package com.awbuilds.auraspend.data.classification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests built from REAL failure cases found by auditing the production
 * Room database (Aug 2026): Axis "IST <merchant>" endings, hash-like merchant
 * fragments, cyber-fraud alerts saved as expenses, and due reminders saved as
 * payments.
 */
class AuditRegressionTest {

    // ─── Axis card messages: merchant trails after "IST" ────────────────────

    @Test
    fun `axis card message extracts trailing merchant after IST`() {
        val parsed = BankMessageParser.parse(
            "Spent INR 135 Axis Bank Card no. XX4821 05-07-26 10:15:22 IST TULIP MART"
        )
        assertTrue(parsed.merchant?.contains("TULIP") == true)
    }

    @Test
    fun `axis MULTILINE device SMS extracts merchant from its own line`() {
        // Exact structure of real Axis SMS (literal newlines) that failed in production.
        val parsed = BankMessageParser.parse(
            "Spent INR 135\nAxis Bank Card no. XX4821\n05-07-26 10:15:22 IST\nTulip\nAvl Limit: INR 42731.09\nNot you? SMS BLOCK 4821 to 919900000000"
        )
        assertEquals("Tulip", parsed.merchant)

        val parsed2 = BankMessageParser.parse(
            "Spent INR 1849\nAxis Bank Card no. XX4821\n04-07-26 18:42:07 IST\nAMBER LEAF\nAvl Limit: INR 41209.80"
        )
        assertEquals("AMBER LEAF", parsed2.merchant)
    }

    @Test
    fun `canara NEFT credit extracts sender as merchant`() {
        val parsed = BankMessageParser.parse(
            "An amount of INR 46,800.00 has been credited to XXXX6021 on 28/06/2026 towards NEFT by Sender ACME SOLUTIONS LIMITED, IFSC HDFC0000240, Sender A/c XXXX1134, HDFC BANK, MUMBAI"
        )
        assertTrue(parsed.merchant?.startsWith("ACME") == true)
        assertEquals(com.awbuilds.auraspend.domain.model.TransactionType.INCOME, parsed.type)
    }

    @Test
    fun `axis card message with longer merchant name`() {
        val parsed = BankMessageParser.parse(
            "Spent INR 2745 Axis Bank Card no. XX4821 03-07-26 20:11:45 IST SUNRISE DINER"
        )
        assertTrue(parsed.merchant?.startsWith("SUNRISE") == true)
    }

    @Test
    fun `merchant extraction never yields boilerplate fragments from prod data`() {
        val junkMessages = listOf(
            // Old parser extracted "report cyber fraud- Canara Bank" as merchant:
            "Rs.10,000 debited.. report cyber fraud- Canara Bank if not done by you",
            // Old parser extracted these phrases as merchants:
            "Rs 749.50 debited enjoy uninterrupted services recharge now",
            "Rs 749.50 paid pay your bill online today"
        )
        for (msg in junkMessages) {
            assertNull("extracted junk from: $msg", BankMessageParser.parse(msg).merchant)
        }
    }

    @Test
    fun `hash-like merchant fragments are rejected`() {
        val parsed = BankMessageParser.parse(
            "Rs 500 debited at 9f3a1c7b2d8e4f6091a2b3c4d5e6f708 ref 88123"
        )
        assertNull(parsed.merchant)
    }

    // ─── Alert/reminder messages that old engine saved as expenses ──────────

    @Test
    fun `cyber fraud warnings are skipped`() {
        // Long conditional variant.
        assertTrue(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "Dear Customer, Rs.10,000 has been debited? report cyber fraud- Canara Bank if not done by you. Phishing alert."
            )
        )
        // Short conditional variant seen in production data.
        assertTrue(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "INR 10,000 debited.. report cyber fraud- Canara Bank if not you"
            )
        )
    }

    @Test
    fun `genuine canara debit with fraud helpline boilerplate is NOT skipped`() {
        // Canara appends "Dial 1930 to report cyber fraud" to EVERY real debit —
        // the boilerplate must never veto a completed transaction.
        assertFalse(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "An amount of INR 135.00 has been DEBITED to your account XXXX6021 on 18/05/2026. Total Avail.bal INR 7,250.44.Dial 1930 to report cyber fraud - Canara Bank"
            )
        )
    }

    @Test
    fun `mandate registration notices are skipped even though they quote amounts`() {
        // The ₹200,000 production case: e-Mandate LIMIT, not a payment.
        assertTrue(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "Dear Cardholder, your payment e-Mandate set at merchant platform has been registered with the following details. Merchant: Microsoft Businesses, e-Mandate Limit Amount (INR): 200000.00, Frequency: As Presented."
            )
        )
        // Advance-notice debits: money will move LATER.
        assertTrue(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "For the upcoming mandate set for 02/05/2026 ,your account will be debited with Rs.3168.00 towards BAJAJ FINANCE LTD for the CREATE MANDATE."
            )
        )
    }

    @Test
    fun `installment due reminders are skipped`() {
        assertTrue(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "Your Installment of Rs.3000 against Scheme XXX58190 is due on 10-09-26. Pay to avoid penalty."
            )
        )
    }

    @Test
    fun `genuine card spend is NOT caught by reminder filter`() {
        assertFalse(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "Spent INR 135 Axis Bank Card no. XX4821 05-07-26 10:15:22 IST TULIP MART"
            )
        )
    }

    // ─── DuplicateDetector granularity (two coffees vs one payment twice) ───

    @Test
    fun `same merchant same day different hour scores below auto-skip threshold`() {
        val morning = com.awbuilds.auraspend.domain.model.Transaction(
            amount = 350.0,
            categoryId = "cat_food",
            note = "",
            merchant = "Starbucks",
            date = java.time.LocalDateTime.of(2026, 8, 23, 9, 30),
            type = com.awbuilds.auraspend.domain.model.TransactionType.EXPENSE
        )
        val afternoon = morning.copy(date = java.time.LocalDateTime.of(2026, 8, 23, 17, 10))
        val score = DuplicateDetector.calculateSimilarity(morning, afternoon)
        assertTrue("score $score should be below 0.96", score < 0.96f)
    }

    @Test
    fun `re-delivered copies of one payment score above threshold`() {
        val original = com.awbuilds.auraspend.domain.model.Transaction(
            amount = 999.95,
            categoryId = "cat_other",
            note = "SIP Purchase",
            date = java.time.LocalDateTime.of(2026, 8, 23, 10, 0),
            type = com.awbuilds.auraspend.domain.model.TransactionType.EXPENSE
        )
        val copy = original.copy(id = "other-id", date = java.time.LocalDateTime.of(2026, 8, 23, 10, 20))
        val score = DuplicateDetector.calculateSimilarity(original, copy)
        assertTrue("score $score should be >= 0.96", score >= 0.96f)
    }
}
