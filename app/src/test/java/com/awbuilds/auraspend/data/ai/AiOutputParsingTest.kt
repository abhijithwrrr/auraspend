package com.awbuilds.auraspend.data.ai

import com.awbuilds.auraspend.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the hardened LLM output handling: balanced-brace JSON extraction that survives
 * markdown fences / prose / nested braces, and fuzzy category resolution.
 */
class AiOutputParsingTest {

    private val categories = mapOf(
        "cat_food" to "Food & Dining",
        "cat_transport" to "Transport",
        "cat_salary" to "Salary",
        "cat_subscription" to "Subscriptions"
    )

    private fun categorizer() = QwenMessageCategorizer(llm = null, modelFile = java.io.File("/tmp/t.gguf"))

    // ── extractJsonObject ──────────────────────────────────────────────────────

    @Test
    fun `extracts plain object`() {
        assertEquals("""{"a":1}""", extractJsonObject("""{"a":1}"""))
    }

    @Test
    fun `extracts from prose before and after`() {
        val out = """Sure! Here is the result: {"is_transaction":true} hope that helps"""
        assertEquals("""{"is_transaction":true}""", extractJsonObject(out))
    }

    @Test
    fun `extracts through markdown fences`() {
        val out = "```json\n{\"type\":\"expense\"}\n```"
        assertEquals("""{"type":"expense"}""", extractJsonObject(out))
    }

    @Test
    fun `braces inside string values do not break extraction`() {
        val out = """{"merchant":"ACME {corp}","ok":true} trailing"""
        assertEquals("""{"merchant":"ACME {corp}","ok":true}""", extractJsonObject(out))
    }

    @Test
    fun `escaped quotes inside strings are handled`() {
        val out = """{"merchant":"say \"hi\" now"}"""
        assertEquals("""{"merchant":"say \"hi\" now"}""", extractJsonObject(out))
    }

    @Test
    fun `returns null when no braces`() {
        assertNull(extractJsonObject("no json here at all"))
    }

    @Test
    fun `returns null for unbalanced braces`() {
        assertNull(extractJsonObject("""{"a":{"b":1}"""))
    }

    // ── parse with messy real-world model output ──────────────────────────────

    @Test
    fun `parses fenced output`() {
        val out = "```json\n{\"is_transaction\":true,\"type\":\"expense\",\"category\":\"Transport\",\"merchant\":\"Uber\",\"subscription\":false}\n```"
        val r = categorizer().parse(out, categories)
        assertNotNull(r)
        assertEquals("cat_transport", r!!.categoryId)
        assertEquals(TransactionType.EXPENSE, r.type)
    }

    @Test
    fun `parses output wrapped in prose`() {
        val out = """The message is a transaction. {"is_transaction":true,"type":"income","category":"salary","merchant":null,"subscription":false} Done."""
        val r = categorizer().parse(out, categories)
        assertNotNull(r)
        assertTrue(r!!.isTransaction)
        assertEquals(TransactionType.INCOME, r.type)
    }

    // ── fuzzy category matching ───────────────────────────────────────────────

    @Test
    fun `case and punctuation differences still resolve`() {
        val r = categorizer().parse("""{"category":"FOOD & DINING!"}""", categories)
        assertEquals("cat_food", r!!.categoryId)
    }

    @Test
    fun `partial names resolve by containment`() {
        val r = categorizer().parse("""{"category":"food"}""", categories)
        assertEquals("cat_food", r!!.categoryId)
    }

    @Test
    fun `word overlap resolves salary`() {
        val r = categorizer().parse("""{"category":"monthly salary income"}""", categories)
        assertEquals("cat_salary", r!!.categoryId)
    }

    @Test
    fun `none maps to other not subscription`() {
        val r = categorizer().parse("""{"category":"none","subscription":false}""", categories)
        assertEquals("cat_other", r!!.categoryId)
        assertFalse(r.isSubscription)
    }

    @Test
    fun `unknown name falls back to other`() {
        val r = categorizer().parse("""{"category":"quantum entanglement"}""", categories)
        assertEquals("cat_other", r!!.categoryId)
    }
}
