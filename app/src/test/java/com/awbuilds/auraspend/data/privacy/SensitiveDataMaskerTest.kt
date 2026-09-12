package com.awbuilds.auraspend.data.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Privacy contract: real leak patterns found in the production Room database
 * (60 phone numbers, 99 reference ids, a half-revealed account "57791190XXXX")
 * must never survive masking — while amounts, dates and bank-pre-masked
 * card/account fragments stay intact for parsing and UX.
 */
class SensitiveDataMaskerTest {

    @Test
    fun `merchant block-request phone number is masked`() {
        val input = "Not you? SMS BLOCK 9054 to 919951860002"
        val out = SensitiveDataMasker.mask(input)
        assertFalse(out.contains("919951860002"))
        assertTrue(out.contains("BLOCK 9054 to"))
    }

    @Test
    fun `bare ten digit phone is masked`() {
        val out = SensitiveDataMasker.mask("Call support at 8843364491 now")
        assertFalse(out.contains("8843364491"))
    }

    @Test
    fun `upi reference number is masked`() {
        val out = SensitiveDataMasker.mask("Sent Rs.299 UPI Ref no 649362919592 -HDFC")
        assertFalse(out.contains("649362919592"))
        assertTrue(out.contains("Ref"))
    }

    @Test
    fun `utr is masked`() {
        val out = SensitiveDataMasker.mask("towards NEFT by Sender ACME, UTR HDFCH01163278288, HDFC BANK")
        assertFalse(out.contains("HDFCH01163278288"))
        assertTrue(out.contains("UTR"))
    }

    @Test
    fun `folio number is masked`() {
        val out = SensitiveDataMasker.mask("Your SIP Purchase of Rs.999.95 in Folio 25514044")
        assertFalse(out.contains("25514044"))
        // The amount survives.
        assertTrue(out.contains("999.95"))
    }

    @Test
    fun `half revealed account fragment is masked`() {
        val out = SensitiveDataMasker.mask("Transfer from a/c 57791190XXXX successful")
        // The exposed first-8 digits must not survive verbatim.
        assertFalse(out.contains("57791190"))
    }

    @Test
    fun `hash like token is masked`() {
        val out = SensitiveDataMasker.mask("Rs 500 debited at 0cd87d2e3e094eb78d56d4d283530a04 ok")
        assertFalse(out.contains("0cd87d2e3e094eb78d56d4d283530a04"))
    }

    // ─── Things that must SURVIVE ─────────────────────────────────────────────

    @Test
    fun `amounts are preserved`() {
        val input = "An amount of INR 54,215.00 has been credited. Total Avail.bal INR 55420.73"
        val out = SensitiveDataMasker.mask(input)
        assertTrue(out.contains("54,215.00"))
        assertTrue(out.contains("55420.73"))
    }

    @Test
    fun `dates and times are preserved`() {
        val input = "Spent INR 25 Axis Bank Card no. XX9054 16-08-26 11:33:30 IST"
        val out = SensitiveDataMasker.mask(input)
        assertTrue(out.contains("16-08-26"))
        assertTrue(out.contains("11:33:30"))
    }

    @Test
    fun `bank premasked card and account fragments are preserved`() {
        val input = "A/c XXXX0007 linked to card XXXX1751 debited Rs.500. XX2643 credited back."
        val out = SensitiveDataMasker.mask(input)
        assertTrue(out.contains("XXXX0007"))
        assertTrue(out.contains("XXXX1751"))
        assertTrue(out.contains("XX2643"))
    }

    @Test
    fun `ifsc code is preserved for bank detection`() {
        val out = SensitiveDataMasker.mask("NEFT IFSC HDFC0000240 sender info")
        assertTrue(out.contains("HDFC0000240"))
    }

    @Test
    fun `masking is idempotent`() {
        val input = "UPI Ref no 649362919592 call 8843364491 folio 25514044"
        val once = SensitiveDataMasker.mask(input)
        val twice = SensitiveDataMasker.mask(once)
        assertEquals(once, twice)
    }

    @Test
    fun `plain transaction text passes through unchanged`() {
        val input = "Rs 250 spent at SWIGGY on 23-08-26. Avl bal Rs 25,000.00 - SBI"
        assertEquals(input, SensitiveDataMasker.mask(input))
    }
}
