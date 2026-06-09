package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BankMessageParserTest {

    @Test
    fun `parse HDFC debit message`() {
        val msg = "Rs.1,500.00 debited from HDFC Bank A/c XX1234 at Amazon on 15/06/2026"
        val result = BankMessageParser.parse(msg)
        assertEquals(1500.0, result.amount!!, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("Amazon", result.merchant)
        assertTrue(result.confidence >= 0.80f)
    }

    @Test
    fun `parse ICICI credit message`() {
        val msg = "Your A/c XX5678 is credited with INR 50,000.00 from Salary on 01/06/2026 - ICICI Bank"
        val result = BankMessageParser.parse(msg)
        assertEquals(50000.0, result.amount!!, 0.001)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("ICICI Bank", result.bankName)
        assertEquals("Salary", result.merchant)
    }

    @Test
    fun `parse SBI message with amount prefix`() {
        val msg = "INR 2000.00 spent at Swiggy via UPI - SBI"
        val result = BankMessageParser.parse(msg)
        assertEquals(2000.0, result.amount!!, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("SBI", result.bankName)
    }

    @Test
    fun `parse UPI reference`() {
        val msg = "INR 500.00 debited from HDFC Bank VPA: user@paytm for purchase at Zomato"
        val result = BankMessageParser.parse(msg)
        assertEquals(500.0, result.amount!!, 0.001)
        assertEquals("user", result.merchant)
    }

    @Test
    fun `return nulls for unrecognized message`() {
        val msg = "Hello, this is a test message with no financial information"
        val result = BankMessageParser.parse(msg)
        assertNull(result.amount)
        assertNull(result.type)
        assertNull(result.bankName)
    }

    @Test
    fun `low confidence for amount-only messages`() {
        val msg = "INR 500.00 processed"
        val result = BankMessageParser.parse(msg)
        assertEquals(0.40f, result.confidence)
    }

    @Test
    fun `merchant extraction with purchase at`() {
        val msg = "INR 899.00 purchase at Myntra on 10/06/2026 - Axis Bank"
        val result = BankMessageParser.parse(msg)
        assertEquals("Axis Bank", result.bankName)
        assertEquals("Myntra", result.merchant)
    }

    @Test
    fun `amount with plain number pattern`() {
        val msg = "Amount 1234.56 is debited from your account"
        val result = BankMessageParser.parse(msg)
        assertEquals(1234.56, result.amount!!, 0.001)
    }

    @Test
    fun `transaction type from pattern keyword`() {
        val creditMsg = "INR 10000.00 has been credited to your account"
        assertEquals(TransactionType.INCOME, BankMessageParser.parse(creditMsg).type)

        val debitMsg = "INR 500.00 has been debited from your account"
        assertEquals(TransactionType.EXPENSE, BankMessageParser.parse(debitMsg).type)
    }

    @Test
    fun `merchant with payment to pattern`() {
        val msg = "INR 1200.00 payment to Flipkart on 05/06/2026 - Kotak Mahindra"
        val result = BankMessageParser.parse(msg)
        assertEquals("Kotak Mahindra", result.bankName)
        assertEquals("Flipkart", result.merchant)
    }

    @Test
    fun `date detection with dd/mm format`() {
        val msg = "INR 500.00 debited at Swiggy on 15/06/2026"
        val result = BankMessageParser.parse(msg)
        assertNotNull(result.date)
        assertEquals(2026, result.date!!.year)
        assertEquals(6, result.date!!.monthValue)
        assertEquals(15, result.date!!.dayOfMonth)
    }

    @Test
    fun `date detection with relative today`() {
        val msg = "INR 500.00 paid today at Uber"
        val result = BankMessageParser.parse(msg)
        assertNotNull(result.date)
    }
}
