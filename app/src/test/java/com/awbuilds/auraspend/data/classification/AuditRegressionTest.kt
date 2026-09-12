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
            "Spent INR 25 Axis Bank Card no. XX9054 16-08-26 11:33:30 IST YAS MART"
        )
        assertTrue(parsed.merchant?.contains("YAS") == true)
    }

    @Test
    fun `axis MULTILINE device SMS extracts merchant from its own line`() {
        // Exact structure of real Axis SMS (literal newlines) that failed in production.
        val parsed = BankMessageParser.parse(
            "Spent INR 25\nAxis Bank Card no. XX9054\n16-08-26 11:33:30 IST\nYasar\nAvl Limit: INR 48058.57\nNot you? SMS BLOCK 9054 to 919951860002"
        )
        assertEquals("Yasar", parsed.merchant)

        val parsed2 = BankMessageParser.parse(
            "Spent INR 2299\nAxis Bank Card no. XX9054\n15-08-26 14:19:43 IST\nCLASSIC SIL\nAvl Limit: INR 50381.35"
        )
        assertEquals("CLASSIC SIL", parsed2.merchant)
    }

    @Test
    fun `canara NEFT credit extracts sender as merchant`() {
        val parsed = BankMessageParser.parse(
            "An amount of INR 54,215.00 has been credited to XXXX0007 on 01/08/2026 towards NEFT by Sender DIGITIDE SOLUTIONS LIMITED, IFSC HDFC0000240, Sender A/c XXXX5020, HDFC BANK, MUMBAI"
        )
        assertTrue(parsed.merchant?.startsWith("DIGITIDE") == true)
        assertEquals(com.awbuilds.auraspend.domain.model.TransactionType.INCOME, parsed.type)
    }

    @Test
    fun `axis card message with longer merchant name`() {
        val parsed = BankMessageParser.parse(
            "Spent INR 3059 Axis Bank Card no. XX9054 11-08-26 19:33:50 IST MCDONALDS KORAMANGALA"
        )
        assertTrue(parsed.merchant?.startsWith("MCDONALDS") == true)
    }

    @Test
    fun `merchant extraction never yields boilerplate fragments from prod data`() {
        val junkMessages = listOf(
            // Old parser extracted "report cyber fraud- Canara Bank" as merchant:
            "Rs.10,000 debited.. report cyber fraud- Canara Bank if not done by you",
            // Old parser extracted these phrases as merchants:
            "Rs 706.82 debited enjoy uninterrupted services recharge now",
            "Rs 706.82 paid pay your bill online today"
        )
        for (msg in junkMessages) {
            assertNull("extracted junk from: $msg", BankMessageParser.parse(msg).merchant)
        }
    }

    @Test
    fun `hash-like merchant fragments are rejected`() {
        val parsed = BankMessageParser.parse(
            "Rs 500 debited at 0cd87d2e3e094eb78d56d4d283530a04 ref 88123"
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
                "An amount of INR 25.00 has been DEBITED to your account XXXX0007 on 20/06/2026. Total Avail.bal INR 6,475.77.Dial 1930 to report cyber fraud - Canara Bank"
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
                "Your Installment of Rs.3000 against Scheme XXX23704 is due on 10-09-26. Pay to avoid penalty."
            )
        )
    }

    @Test
    fun `genuine card spend is NOT caught by reminder filter`() {
        assertFalse(
            SmsAutoClassifier.isOtpOrAlertMessage(
                "Spent INR 25 Axis Bank Card no. XX9054 16-08-26 11:33:30 IST YAS MART"
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
