package com.awbuilds.auraspend.data.ai

import com.awbuilds.auraspend.domain.model.TransactionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenMessageCategorizerTest {

    private val categories = mapOf(
        "cat_food" to "Food & Dining",
        "cat_transport" to "Transport",
        "cat_subscription" to "subscription"
    )

    private fun categorizer(llm: LocalLlm? = null) =
        QwenMessageCategorizer(llm = llm, modelFile = File("/tmp/test-model.gguf"))

    @Test
    fun `parse maps income type`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"income","category":"Salary","merchant":"none","subscription":false}""", categories)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertFalse(result.isSubscription)
        assertNull(result.merchant)
    }

    @Test
    fun `parse maps expense to a real category id`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"Food & Dining","merchant":"Swiggy","subscription":false}""", categories)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
        assertEquals("cat_food", result.category)
        assertEquals("Swiggy", result.merchant)
        assertFalse(result.isSubscription)
    }

    @Test
    fun `parse detects subscription`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"subscription","merchant":"Netflix","subscription":true}""", categories)
        assertNotNull(result)
        assertTrue(result!!.isSubscription)
        assertEquals("cat_subscription", result.category)
    }

    @Test
    fun `parse falls back to other for unknown category`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"Mystery","merchant":"x","subscription":false}""", categories)
        assertNotNull(result)
        assertEquals("cat_other", result!!.category)
    }

    @Test
    fun `parse returns null for non json`() {
        val cat = categorizer()
        assertNull(cat.parse("the model rambled on without json", categories))
    }

    @Test
    fun `buildPrompt includes categories and message`() {
        val cat = categorizer()
        val prompt = cat.buildPrompt("INR 500 debited at Swiggy", categories)
        assertTrue(prompt.contains("Food & Dining"))
        assertTrue(prompt.contains("Swiggy"))
        assertTrue(prompt.contains("subscription"))
    }

    @Test
    fun `categorise returns null when no llm`() {
        val cat = categorizer(llm = null)
        assertNull(cat.categorise("INR 500 at Swiggy", categories))
    }

    @Test
    fun `categorise uses llm output`() {
        val fake = FakeLlm("""{"type":"expense","category":"Food & Dining","merchant":"Swiggy","subscription":false}""")
        val cat = categorizer(llm = fake)
        val result = cat.categorise("INR 500 at Swiggy", categories)
        assertNotNull(result)
        assertEquals("cat_food", result!!.category)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `unparsable model output returns null instead of a false veto`() {
        // Regression: garbage generations used to become isTransaction=false, which the
        // fusion layer treated as a veto and discarded a good regex parse.
        val fake = FakeLlm("I could not understand that message, sorry!")
        val cat = categorizer(llm = fake)
        assertNull(cat.categorise("Rs 500 debited", categories))
    }

    @Test
    fun `parse infers transaction from type when flag missing`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"Food & Dining","merchant":"Swiggy"}""", categories)
        assertNotNull(result)
        assertTrue(result!!.isTransaction)
    }

    @Test
    fun `explicit false still vetoes`() {
        val cat = categorizer()
        val result = cat.parse("""{"is_transaction":false,"type":"none","category":"Other","merchant":"none","subscription":false}""", categories)
        assertNotNull(result)
        assertFalse(result!!.isTransaction)
    }

    @Test
    fun `bank-style type words are understood`() {
        val cat = categorizer()
        val credited = cat.parse("""{"is_transaction":true,"type":"credited","category":"Salary","merchant":"Acme"}""", categories)
        assertEquals(TransactionType.INCOME, credited!!.type)

        val debited = cat.parse("""{"is_transaction":true,"type":"debited","category":"Shopping","merchant":"Amazon"}""", categories)
        assertEquals(TransactionType.EXPENSE, debited!!.type)
    }

    @Test
    fun `camelCase flag variant parses`() {
        val cat = categorizer()
        val result = cat.parse("""{"isTransaction":true,"type":"expense","category":"Transport","merchant":"Uber","subscription":false}""", categories)
        assertNotNull(result)
        assertTrue(result!!.isTransaction)
        assertEquals("cat_transport", result.category)
    }

    @Test
    fun `prompt carries strict schema rules and stays within context budget`() {
        val cat = categorizer()
        val prompt = cat.buildPrompt(
            "INR 250 debited A/c XX1234 at SWIGGY on 01-07",
            mapOf(
                "cat_food" to "Food & Dining",
                "cat_transport" to "Transport",
                "cat_subscription" to "Subscriptions",
                "cat_transfer" to "Transfer",
                "cat_other" to "Other"
            )
        )

        // Schema + critical rules must be present.
        assertTrue(prompt.contains("\"is_transaction\""))
        assertTrue(prompt.contains("\"type\":\"income|expense|none\"") || prompt.contains("income|expense|none"))
        assertTrue(prompt.contains("EXACTLY one of"))
        assertTrue(prompt.contains("Food & Dining"))
        assertTrue(prompt.contains("\"Transfer\""))
        assertTrue(prompt.contains("Refund"))
        assertTrue(prompt.contains("auto-pay/NACH/e-mandate/auto-debit"))
        assertTrue(prompt.contains("NEVER copy account numbers"))
        assertTrue(prompt.contains("No explanations, no markdown"))

        // Failure-mode few-shots must all be covered.
        assertTrue(prompt.contains("SWIGGY"))
        assertTrue(prompt.contains("rahim@ybl"))
        assertTrue(prompt.contains("salary credited"))
        assertTrue(prompt.contains("refunded"))
        assertTrue(prompt.contains("AutoPay"))
        assertTrue(prompt.contains("Your OTP is"))
        assertTrue(prompt.contains("Avl balance"))

        // The whole prompt must comfortably fit the 2048-token context (~4 chars/token).
        assertTrue("prompt too long: ${prompt.length} chars", prompt.length < 3200)
    }
}

private class FakeLlm(private val response: String) : LocalLlm {
    var lastPrompt: String? = null
    override fun isModelAvailable(modelFile: File): Boolean = true
    override fun generate(prompt: String): String? {
        lastPrompt = prompt
        return response
    }

    override fun close() = Unit
}
