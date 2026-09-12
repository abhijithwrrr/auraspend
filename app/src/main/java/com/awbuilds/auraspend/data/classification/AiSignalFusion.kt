package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.data.ai.AiCategorisation
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType

/**
 * Multi-signal fusion of the regex parser result with the on-device LLM verdict.
 *
 * Priority rules:
 *  - an explicit LLM "not a transaction" veto wins (wipes amount/type so the pipeline skips);
 *  - explicit debit/credit keywords from the regex parser beat an LLM type guess on conflict;
 *  - curated merchant-keyword categories survive unless a subscription signal fires;
 *  - recurring-payment keywords or a known-subscription service force subscription classification,
 *    even without AI.
 */
object AiSignalFusion {

    data class Fused(
        val parsed: ParsedBankMessage,
        val isSubscription: Boolean,
        val usedAi: Boolean
    )

    /** Mandates / auto-debit machinery that always implies a recurring charge. */
    private val recurringKeywords = Regex(
        "(?i)\\b(renewal|renews?|auto[- ]?pay(?:ment)?|autopay|nach|e[- ]?mandate|emandate|" +
            "mandate|standing\\s+instruction|si\\s+debit|recurring|subscription|plan\\s+renewal|billing\\s+cycle)\\b"
    )

    /**
     * Services that are subscriptions by identity. Matched against merchant and
     * note text with token boundaries so "amazon" alone never matches while
     * "amazon prime" does.
     */
    private val subscriptionServices = listOf(
        "netflix", "spotify", "prime video", "amazon prime", "hotstar", "jiocinema",
        "sony liv", "zee5", "youtube premium", "youtube music", "google one",
        "google ai", "icloud", "dropbox", "office 365", "microsoft 365", "adobe",
        "chatgpt", "openai", "claude", "gemini advanced", "notion", "canva",
        "duolingo", "gaana", "wynk", "jiosaavn", "audible", "kindle unlimited",
        "disney+", "crunchyroll", "linkedin premium", "zoom pro", "figma",
        "lastpass", "1password", "tata play", "times prime", "cult fit"
    )

    /** True when [text] clearly names a subscription service. */
    fun looksLikeSubscriptionService(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val haystack = " " + text.lowercase().replace(Regex("[^a-z0-9&+ ]+"), " ")
            .replace(Regex("\\s+"), " ") + " "
        return subscriptionServices.any { svc -> haystack.contains(" $svc ") }
    }

    fun fuse(regexParsed: ParsedBankMessage, ai: AiCategorisation?): Fused {
        val base = regexParsed

        if (ai != null && !ai.isTransaction) {
            // LLM veto: not a transaction (OTP, promo, balance alert...).
            return Fused(base.copy(amount = null, type = null), isSubscription = false, usedAi = true)
        }

        val resolvedType = when {
            ai?.type == null -> base.type
            base.type == null -> ai.type
            // Both present but conflicting: keyword signals ("credited"/"debited") are explicit.
            else -> base.type
        }

        val recurringByKeyword = recurringKeywords.containsMatchIn(base.rawMessage)
        val subscriptionMerchant = resolvedType == TransactionType.EXPENSE && (
            looksLikeSubscriptionService(base.merchant) ||
                (base.merchant == null && looksLikeSubscriptionService(base.note)) ||
                // Last resort: a debit naming a subscription service in its body.
                (base.merchant == null && base.note == null &&
                    looksLikeSubscriptionService(base.rawMessage))
            )
        val isSubscription = (ai?.isSubscription == true) || recurringByKeyword || subscriptionMerchant

        val resolvedCategory = when {
            isSubscription -> "cat_subscription"
            base.categoryId != null -> base.categoryId
            else -> ai?.categoryId
        }

        val resolvedMerchant = base.merchant ?: ai?.merchant

        val confidence = computeConfidence(base.confidence, base.type, resolvedType, ai)

        return Fused(
            parsed = base.copy(
                amount = base.amount,
                type = resolvedType,
                merchant = resolvedMerchant,
                categoryId = resolvedCategory,
                confidence = confidence
            ),
            isSubscription = isSubscription,
            usedAi = ai != null
        )
    }

    private fun computeConfidence(
        regexConfidence: Float,
        regexType: TransactionType?,
        fusedType: TransactionType?,
        ai: AiCategorisation?
    ): Float {
        var confidence = regexConfidence
        when {
            ai == null -> Unit // heuristic only
            regexType != null && ai.type != null && regexType == ai.type ->
                confidence = (confidence + 0.08f).coerceAtMost(0.99f) // signals agree
            regexType != null && ai.type != null ->
                confidence = (confidence - 0.10f).coerceAtLeast(0.30f) // conflict
            else -> Unit
        }
        if (fusedType == null) confidence = confidence.coerceAtMost(0.35f)
        return confidence
    }
}
