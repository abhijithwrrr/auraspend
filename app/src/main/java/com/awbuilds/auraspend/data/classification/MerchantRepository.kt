package com.awbuilds.auraspend.data.classification

import android.content.Context
import java.io.BufferedReader

object MerchantRepository {
    private var merchantDatabase: Map<String, MerchantInfo> = emptyMap()
    private var merchantKeywordMap: Map<String, MerchantInfo> = emptyMap()

    fun initialize(context: Context) {
        if (merchantDatabase.isNotEmpty()) return
        synchronized(this) {
            if (merchantDatabase.isNotEmpty()) return

            val merchants = mutableMapOf<String, MerchantInfo>()
            val keywords = mutableMapOf<String, MerchantInfo>()

            try {
                val inputStream = context.assets.open("merchant_database.csv")
                inputStream.bufferedReader().use { reader ->
                    reader.readLine() // Skip header
                    reader.forEachLine { line ->
                        val parts = line.split(",")
                        if (parts.size >= 6) {
                            val merchantName = parts[0].trim()
                            val merchantAlias = parts[1].trim()
                            val category = parts[2].trim()
                            val subcategory = parts[3].trim()
                            val keywordString = parts[4].trim()
                            val confidence = parts[5].trim().toFloatOrNull() ?: 0.8f

                            val info = MerchantInfo(
                                merchantName = merchantName,
                                merchantAlias = merchantAlias,
                                category = category,
                                subcategory = subcategory,
                                keywords = keywordString,
                                confidence = confidence
                            )

                            // Index by merchant name
                            merchants[merchantName.lowercase()] = info
                            merchants[merchantAlias.lowercase()] = info

                            // Index by keywords
                            keywordString.lowercase().split("|").forEach { keyword ->
                                if (keyword.isNotBlank()) {
                                    keywords[keyword.trim()] = info
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            merchantDatabase = merchants
            merchantKeywordMap = keywords
        }
    }

    /**
     * Look up a merchant by name or alias
     */
    fun lookupMerchant(merchantName: String): MerchantInfo? {
        if (merchantDatabase.isEmpty()) return null
        return merchantDatabase[merchantName.lowercase()]
    }

    /**
     * Search for merchants by keyword (case-insensitive)
     */
    fun searchByKeyword(keyword: String): List<MerchantInfo> {
        if (merchantDatabase.isEmpty()) return emptyList()
        val results = mutableListOf<MerchantInfo>()
        val searchTerm = keyword.lowercase()

        merchantDatabase.values.forEach { info ->
            if (info.merchantName.lowercase().contains(searchTerm) ||
                info.merchantAlias.lowercase().contains(searchTerm) ||
                info.keywords.lowercase().contains(searchTerm)
            ) {
                if (!results.any { it.merchantName == info.merchantName }) {
                    results.add(info)
                }
            }
        }

        return results.sortedByDescending { it.confidence }
    }

    /**
     * Get all categories with subcategories
     */
    fun getAllCategories(): Map<String, List<String>> {
        val categories = mutableMapOf<String, MutableSet<String>>()
        merchantDatabase.values.forEach { info ->
            categories.computeIfAbsent(info.category) { mutableSetOf() }.add(info.subcategory)
        }
        return categories.mapValues { it.value.sorted() }
    }

    /**
     * Suggest category for a merchant with confidence score
     */
    fun suggestCategory(merchantName: String): Pair<String, Float>? {
        val merchant = lookupMerchant(merchantName) ?: return null
        return Pair(merchant.category, merchant.confidence)
    }

    /**
     * Suggest subcategory for a merchant
     */
    fun suggestSubcategory(merchantName: String): Triple<String, String, Float>? {
        val merchant = lookupMerchant(merchantName) ?: return null
        return Triple(merchant.category, merchant.subcategory, merchant.confidence)
    }
}
