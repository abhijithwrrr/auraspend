package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.domain.model.Transaction
import kotlin.math.abs

object DuplicateDetector {

    /**
     * Detect potential duplicate transactions
     * Returns a list of duplicate pairs with similarity score
     */
    fun detectDuplicates(
        newTransaction: Transaction,
        existingTransactions: List<Transaction>
    ): List<DuplicateTransaction> {
        val duplicates = mutableListOf<DuplicateTransaction>()

        existingTransactions.forEach { existing ->
            val similarity = calculateSimilarity(newTransaction, existing)
            if (similarity >= 0.85f) { // 85% match threshold
                duplicates.add(
                    DuplicateTransaction(
                        originalId = existing.id,
                        duplicateId = newTransaction.id,
                        similarity = similarity,
                        reason = buildDuplicateReason(newTransaction, existing, similarity)
                    )
                )
            }
        }

        return duplicates.sortedByDescending { it.similarity }
    }

    /**
     * Calculate similarity score between two transactions (0.0 - 1.0)
     */
    fun calculateSimilarity(t1: Transaction, t2: Transaction): Float {
        var score = 0f
        var weightSum = 0f

        // Amount similarity (30% weight) - exact match or within 2%
        val amountDiff = abs(t1.amount - t2.amount)
        val amountSimilarity = if (amountDiff < 0.1) {
            1f
        } else {
            (1f - (amountDiff / maxOf(t1.amount, t2.amount, 1.0)).toFloat()).coerceIn(0f, 1f)
        }
        score += amountSimilarity * 0.30f
        weightSum += 0.30f

        // Merchant similarity (40% weight)
        val merchantSimilarity = if (t1.merchant != null && t2.merchant != null) {
            stringSimilarity(t1.merchant, t2.merchant)
        } else if (t1.merchant == t2.merchant) {
            1f
        } else {
            0f
        }
        score += merchantSimilarity * 0.40f
        weightSum += 0.40f

        // Date similarity (30% weight) - same day or within 1 hour
        val timeDiff = abs(
            t1.date.toLocalDate().toEpochDay() - t2.date.toLocalDate().toEpochDay()
        )
        val dateSimilarity = when {
            timeDiff == 0L -> 1f
            timeDiff == 1L -> 0.8f
            timeDiff <= 2L -> 0.6f
            else -> 0f
        }
        score += dateSimilarity * 0.30f
        weightSum += 0.30f

        return if (weightSum > 0) score / weightSum else 0f
    }

    /**
     * Levenshtein distance-based string similarity (0.0 - 1.0)
     */
    private fun stringSimilarity(s1: String, s2: String): Float {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1f

        val distance = levenshteinDistance(s1.lowercase(), s2.lowercase())
        return 1f - (distance.toFloat() / maxLength.toFloat())
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    private fun buildDuplicateReason(t1: Transaction, t2: Transaction, similarity: Float): String {
        val reasons = mutableListOf<String>()

        if (t1.amount == t2.amount) {
            reasons.add("Same amount: ₹${t1.amount}")
        }

        if (t1.merchant != null && t2.merchant != null &&
            t1.merchant.equals(t2.merchant, ignoreCase = true)) {
            reasons.add("Same merchant: ${t1.merchant}")
        }

        if (t1.date.toLocalDate() == t2.date.toLocalDate()) {
            reasons.add("Same date: ${t1.date.toLocalDate()}")
        }

        return reasons.joinToString(", ")
    }

    /**
     * Check if a transaction is likely a duplicate of another in the list
     * Returns the most similar transaction if similarity >= threshold
     */
    fun findMostLikelyDuplicate(
        transaction: Transaction,
        existingTransactions: List<Transaction>,
        threshold: Float = 0.85f
    ): Pair<Transaction, Float>? {
        return existingTransactions
            .map { existing -> Pair(existing, calculateSimilarity(transaction, existing)) }
            .filter { it.second >= threshold }
            .maxByOrNull { it.second }
    }
}
