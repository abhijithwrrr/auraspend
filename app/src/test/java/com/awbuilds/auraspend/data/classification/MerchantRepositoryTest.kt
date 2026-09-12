package com.awbuilds.auraspend.data.classification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for the noise-tolerant merchant knowledge base: normalization of
 * messy SMS merchant strings, token-identity resolution, and the integrity
 * of the committed CSV (every category must map to a local category id).
 */
class MerchantRepositoryTest {

    @Before
    fun installFixture() {
        MerchantRepository.install(
            listOf(
                "merchant_name,merchant_alias,category,subcategory,keywords,confidence",
                "Swiggy,swiggy food delivery,\"Food\",\"Food Delivery\",\"swiggy delivery order\",0.95",
                "Instamart,swiggy instamart,\"Groceries\",\"Quick Commerce\",\"instamart grocery\",0.92",
                "Indian Oil,iocl,\"Transport\",\"Auto Fuel\",\"indian oil petrol\",0.95",
                "Cafe Coffee Day,ccd,\"Food\",\"Coffee\",\"cafe coffee day\",0.92"
            )
        )
    }

    @Test
    fun `noisy swiggy string resolves by normalization`() {
        val hit = MerchantRepository.resolveMerchant("SWIGGY*Zomato/BLR")
        assertEquals("Swiggy", hit?.merchantName)
    }

    @Test
    fun `iocl petrol pump resolves via alias token`() {
        val hit = MerchantRepository.resolveMerchant("IOCL PETROL PUMP 2234")
        assertEquals("Indian Oil", hit?.merchantName)
        assertEquals("Transport", hit?.category)
    }

    @Test
    fun `instamart beats swiggy for grocery strings`() {
        val hit = MerchantRepository.resolveMerchant("SWIGGY INSTAMART BLR")
        assertEquals("Instamart", hit?.merchantName)
        assertEquals("Groceries", hit?.category)
    }

    @Test
    fun `generic words never resolve to a merchant`() {
        assertNull(MerchantRepository.resolveMerchant("order payment"))
        assertNull(MerchantRepository.resolveMerchant("delivery order"))
        assertNull(MerchantRepository.resolveMerchant(""))
    }

    @Test
    fun `numbers-only input resolves to nothing`() {
        assertNull(MerchantRepository.resolveMerchant("9123456789"))
    }

    @Test
    fun `suggest category returns category and confidence`() {
        val suggestion = MerchantRepository.suggestCategory("swiggy")
        assertEquals("Food", suggestion?.first)
        assertEquals(0.95f, suggestion?.second)
    }

    // ─── Committed asset integrity ──────────────────────────────────────────

    private fun realCsvLines(): List<String>? =
        listOf("src/main/assets/merchant_database.csv", "app/src/main/assets/merchant_database.csv")
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?.readLines()

    @Test
    fun `committed csv is well formed and every category maps to a local id`() {
        val lines = realCsvLines() ?: return // asset not on the unit-test path in some environments
        assertTrue("merchant csv unexpectedly small", lines.size > 200)

        MerchantRepository.install(lines)

        val categories = MerchantRepository.getAllCategories().keys
        assertTrue(categories.isNotEmpty())
        for (category in categories) {
            assertTrue(
                "merchant category '$category' has no local category mapping",
                mapMerchantCategoryToLocal(category) != null
            )
        }

        for ((index, line) in lines.drop(1).withIndex()) {
            if (line.isBlank()) continue
            val parts = line.split(",")
            assertTrue("row ${index + 2} has ${parts.size} fields", parts.size >= 6)
            val confidence = parts[5].trim().toFloatOrNull()
            assertTrue("row ${index + 2} has invalid confidence", confidence != null && confidence in 0f..1f)
        }
    }

    @Test
    fun `wallet and card-network names no longer force wrong categories`() {
        val lines = realCsvLines() ?: return
        MerchantRepository.install(lines)
        assertNull(MerchantRepository.resolveMerchant("PhonePe"))
        assertNull(MerchantRepository.resolveMerchant("Google Pay"))
        assertNull(MerchantRepository.resolveMerchant("Visa"))
        assertNull(MerchantRepository.resolveMerchant("UPI Transfer"))
    }
}
