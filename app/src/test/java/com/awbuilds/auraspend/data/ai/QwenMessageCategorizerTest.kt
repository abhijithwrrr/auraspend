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
        assertEquals("cat_food", result.categoryId)
        assertEquals("Swiggy", result.merchant)
        assertFalse(result.isSubscription)
    }

    @Test
    fun `parse detects subscription`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"subscription","merchant":"Netflix","subscription":true}""", categories)
        assertNotNull(result)
        assertTrue(result!!.isSubscription)
        assertEquals("cat_subscription", result.categoryId)
    }

    @Test
    fun `parse falls back to other for unknown category`() {
        val cat = categorizer()
        val result = cat.parse("""{"type":"expense","category":"Mystery","merchant":"x","subscription":false}""", categories)
        assertNotNull(result)
        assertEquals("cat_other", result!!.categoryId)
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
        assertEquals("cat_food", result!!.categoryId)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `categorise keeps raw output when parse fails`() {
        val fake = FakeLlm("garbage output")
        val cat = categorizer(llm = fake)
        val result = cat.categorise("x", categories)
        assertNotNull(result)
        assertNull(result!!.categoryId)
        assertEquals("garbage output", result.rawModelOutput)
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
