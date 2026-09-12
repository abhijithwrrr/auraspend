package com.awbuilds.auraspend.data.classification

import android.content.Context
import com.awbuilds.auraspend.core.AuraLog
import java.util.Locale

/**
 * Curated merchant knowledge base with noise-tolerant resolution.
 *
 * Bank SMS rarely contain a clean merchant string: "SWIGGY*Zomato/BLR",
 * "IOCL PETROL PUMP 2234" and "CAFE COFFEE DAY PVT LTD" must all resolve.
 * Resolution order in [resolveMerchant]:
 *
 *  1. Exact match on the MemoryKeys-normalized input (strip *, @, digits,
 *     legal suffixes, bank boilerplate).
 *  2. Longest distinctive-token match — the normalized input tokens are
 *     looked up in a token -> merchant index built from names, aliases and
 *     the keywords column. "iocl" alone identifies Indian Oil.
 *  3. Phrase containment for multi-word brands ("cafe coffee day" inside
 *     longer noise).
 *
 * Generic words ("order", "delivery", "payment", …) are never indexed as
 * identity tokens, so "Amazon delivery" cannot resolve to Swiggy.
 */
object MerchantRepository {
    private const val TAG = "MerchantRepository"

    private var merchantDatabase: Map<String, MerchantInfo> = emptyMap()
    private var merchantTokenMap: Map<String, MerchantInfo> = emptyMap()
    private var merchantPhrases: List<Pair<String, MerchantInfo>> = emptyList()

    /** Words that carry no merchant identity and must never resolve one. */
    private val genericTokens = setOf(
        "order", "orders", "delivery", "payment", "payments", "booking", "bookings",
        "subscription", "store", "stores", "shop", "mobile", "card", "bank", "banking",
        "recharge", "wallet", "bill", "bills", "shopping", "clothing", "shoes",
        "medicine", "medicines", "grocery", "supermarket", "station", "service",
        "services", "india", "music", "video", "app", "online", "day", "devices",
        "ride", "rides", "trip", "fuel", "petrol", "storage", "workspace",
        "first", "web", "tata", "sky", "products", "premium", "more",
        "one", "new", "balance", "big", "direct",
        // Brand-umbrella tokens whose wallet/payment products must not inherit
        // the parent brand's category ("Google Pay" is not Google One).
        "google", "pay"
    )

    fun initialize(context: Context) {
        if (merchantDatabase.isNotEmpty()) return
        synchronized(this) {
            if (merchantDatabase.isNotEmpty()) return

            try {
                val lines = context.assets.open("merchant_database.csv")
                    .bufferedReader().use { it.readLines() }
                install(lines)
            } catch (e: Exception) {
                // The KB is an accuracy booster, not a dependency: an unread
                // asset degrades to keyword heuristics instead of crashing.
                AuraLog.e(TAG, "merchant_database.csv could not be loaded", e)
            }
        }
    }

    /** Pure installer, internal so unit tests can exercise parsing directly. */
    internal fun install(lines: List<String>) {
        val merchants = mutableMapOf<String, MerchantInfo>()
        val tokens = mutableMapOf<String, MerchantInfo>()
        val phrases = mutableListOf<Pair<String, MerchantInfo>>()

        fun addKey(key: String?, info: MerchantInfo) {
            if (key.isNullOrBlank()) return
            merchants.putIfAbsent(key, info)
        }

        fun addToken(token: String, info: MerchantInfo) {
            if (token.length < 3) return
            if (token in genericTokens) return
            if (token in MemoryKeys.stopWords) return
            tokens.putIfAbsent(token, info)
        }

        fun addPhrase(raw: String, info: MerchantInfo) {
            val phrase = MemoryKeys.normalize(raw) ?: return
            if (phrase.length >= 5) phrases.add(phrase to info)
        }

        for (line in lines.drop(1)) { // skip header
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.size < 6) continue

            val info = MerchantInfo(
                merchantName = parts[0],
                merchantAlias = parts[1],
                category = parts[2],
                subcategory = parts[3],
                keywords = parts[4],
                confidence = parts[5].toFloatOrNull() ?: 0.8f
            )

            addKey(info.merchantName.lowercase(Locale.ENGLISH), info)
            addKey(MemoryKeys.normalize(info.merchantName), info)
            addKey(info.merchantAlias.lowercase(Locale.ENGLISH), info)
            addKey(MemoryKeys.normalize(info.merchantAlias), info)

            for (token in tokensOf(info.merchantName) + tokensOf(info.merchantAlias)) {
                addToken(token, info)
            }
            // The keywords column holds space-separated distinctive words.
            for (token in tokensOf(info.keywords)) {
                addToken(token, info)
            }

            addPhrase(info.merchantName, info)
            addPhrase(info.merchantAlias, info)
        }

        merchantDatabase = merchants
        merchantTokenMap = tokens
        merchantPhrases = phrases.sortedByDescending { it.first.length }
    }

    private fun tokensOf(phrase: String): List<String> =
        phrase.lowercase(Locale.ENGLISH)
            .replace(Regex("[^a-z& ]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    /**
     * Resolves the best merchant for a noisy input string, or null when
     * nothing distinctive matches.
     */
    fun resolveMerchant(input: String): MerchantInfo? {
        if (merchantDatabase.isEmpty()) return null
        val normalized = MemoryKeys.normalize(input) ?: return null

        merchantDatabase[normalized]?.let { return it }

        var best: Pair<String, MerchantInfo>? = null
        for (token in normalized.split(' ')) {
            val hit = merchantTokenMap[token] ?: continue
            val current = best
            if (current == null ||
                token.length > current.first.length ||
                (token.length == current.first.length && hit.confidence > current.second.confidence)
            ) {
                best = token to hit
            }
        }
        best?.let { return it.second }

        return merchantPhrases.firstOrNull { (phrase, _) ->
            normalized.length >= 5 &&
                (normalized.contains(phrase) || phrase.contains(normalized))
        }?.second
    }

    /** Look up a merchant by name or alias (noise-tolerant). */
    fun lookupMerchant(merchantName: String): MerchantInfo? =
        resolveMerchant(merchantName)

    /** Search merchants by keyword (case-insensitive, token identity). */
    fun searchByKeyword(keyword: String): List<MerchantInfo> {
        val direct = resolveMerchant(keyword)?.let { listOf(it) } ?: emptyList()
        val normalized = MemoryKeys.normalize(keyword)
        val byPhrase = if (normalized != null) {
            merchantPhrases.filter { (phrase, _) -> phrase.contains(normalized) }
                .map { it.second }
        } else emptyList()
        return (direct + byPhrase).distinctBy { it.merchantName }
            .sortedByDescending { it.confidence }
    }

    /** Get all categories with subcategories. */
    fun getAllCategories(): Map<String, List<String>> {
        val categories = mutableMapOf<String, MutableSet<String>>()
        merchantDatabase.values.forEach { info ->
            categories.computeIfAbsent(info.category) { mutableSetOf() }.add(info.subcategory)
        }
        return categories.mapValues { it.value.sorted() }
    }

    /** Suggest a (category, confidence) pair for a noisy merchant string. */
    fun suggestCategory(merchantName: String): Pair<String, Float>? =
        resolveMerchant(merchantName)?.let { it.category to it.confidence }

    /** Suggest (category, subcategory, confidence) for a noisy merchant string. */
    fun suggestSubcategory(merchantName: String): Triple<String, String, Float>? =
        resolveMerchant(merchantName)?.let {
            Triple(it.category, it.subcategory, it.confidence)
        }

    internal fun isLoaded(): Boolean = merchantDatabase.isNotEmpty()
}
