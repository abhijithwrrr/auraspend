package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.data.ai.AiCategorisation
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSignalFusionTest {

    private fun parsed(
        amount: Double? = 250.0,
        type: TransactionType? = TransactionType.EXPENSE,
        merchant: String? = null,
        categoryId: String? = null,
        raw: String = "INR 250 debited"
    ) = ParsedBankMessage(
        amount = amount,
        type = type,
        merchant = merchant,
        categoryId = categoryId,
        confidence = 0.85f,
        rawMessage = raw
    )

    private fun ai(
        isTransaction: Boolean = true,
        type: TransactionType? = TransactionType.EXPENSE,
        categoryId: String? = "cat_food",
        isSubscription: Boolean = false,
        merchant: String? = null
    ) = AiCategorisation(isTransaction, type, categoryId, isSubscription, merchant, "{}")

    @Test
    fun `llm veto wipes amount and type`() {
        val fused = AiSignalFusion.fuse(parsed(), ai(isTransaction = false))
        assertNull(fused.parsed.amount)
        assertNull(fused.parsed.type)
        assertFalse(fused.isSubscription)
        assertTrue(fused.usedAi)
    }

    @Test
    fun `regex keyword wins on type conflict`() {
        val fused = AiSignalFusion.fuse(
            parsed(type = TransactionType.EXPENSE),
            ai(type = TransactionType.INCOME)
        )
        assertEquals(TransactionType.EXPENSE, fused.parsed.type)
    }

    @Test
    fun `ai fills missing type`() {
        val fused = AiSignalFusion.fuse(
            parsed(type = null, amount = 10.0),
            ai(type = TransactionType.INCOME)
        )
        assertEquals(TransactionType.INCOME, fused.parsed.type)
    }

    @Test
    fun `curated regex category survives when not a subscription`() {
        val fused = AiSignalFusion.fuse(
            parsed(categoryId = "cat_grocery"),
            ai(categoryId = "cat_other")
        )
        assertEquals("cat_grocery", fused.parsed.categoryId)
    }

    @Test
    fun `ai category used when regex has none`() {
        val fused = AiSignalFusion.fuse(
            parsed(categoryId = null),
            ai(categoryId = "cat_transport")
        )
        assertEquals("cat_transport", fused.parsed.categoryId)
    }

    @Test
    fun `ai subscription forces subscription category`() {
        val fused = AiSignalFusion.fuse(
            parsed(categoryId = "cat_entertainment"),
            ai(isSubscription = true, categoryId = "cat_subscription")
        )
        assertTrue(fused.isSubscription)
        assertEquals("cat_subscription", fused.parsed.categoryId)
    }

    @Test
    fun `recurring keywords force subscription even without ai`() {
        val fused = AiSignalFusion.fuse(
            parsed(raw = "Rs 199 auto-debit for HOTSTAR renewal"),
            ai = null
        )
        assertTrue(fused.isSubscription)
        assertEquals("cat_subscription", fused.parsed.categoryId)
        assertFalse(fused.usedAi)
    }

    @Test
    fun `ai merchant fills gap only`() {
        val keep = AiSignalFusion.fuse(parsed(merchant = "Swiggy"), ai(merchant = "Swiggy Order"))
        assertEquals("Swiggy", keep.parsed.merchant)

        val fill = AiSignalFusion.fuse(parsed(merchant = null), ai(merchant = "Netflix"))
        assertEquals("Netflix", fill.parsed.merchant)
    }

    @Test
    fun `agreement boosts confidence and conflict reduces it`() {
        val agree = AiSignalFusion.fuse(parsed(), ai(type = TransactionType.EXPENSE))
        val disagree = AiSignalFusion.fuse(parsed(), ai(type = TransactionType.INCOME))
        assertTrue(agree.parsed.confidence > disagree.parsed.confidence)
    }
}
