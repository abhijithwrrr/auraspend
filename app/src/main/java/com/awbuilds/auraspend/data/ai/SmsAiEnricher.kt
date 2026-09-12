package com.awbuilds.auraspend.data.ai

import android.content.Context
import android.util.Log
import com.awbuilds.auraspend.data.classification.AiSignalFusion
import com.awbuilds.auraspend.data.classification.ClassificationMemory
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.domain.model.TransactionType

/**
 * Enriches heuristic classification results, in order of trust:
 *
 *  1. **Learned memory** - when this merchant/note was categorized before, apply the remembered
 *     category instantly and skip the LLM entirely (fast path; also keeps results stable).
 *  2. **On-device LLM** - when the model is downloaded and the runtime is linked, fuse its verdict
 *     with the regex parse via [AiSignalFusion].
 *  3. **Regex only** - fall back to the heuristic result untouched.
 */
class SmsAiEnricher(
    context: Context,
    private val memory: ClassificationMemory? = null
) {

    private val categorizer: QwenMessageCategorizer? = run {
        if (ModelConstants.isDownloaded(context) && LocalLlmProvider.get() !is UnavailableLlm) {
            QwenMessageCategorizer(
                llm = LocalLlmProvider.get(),
                modelFile = ModelConstants.modelFile(context)
            )
        } else {
            null
        }
    }

    suspend fun enrich(
        classified: ClassifiedSms,
        categoryIdByName: Map<String, String>
    ): ClassifiedSms {
        // 1. Memory fast path.
        if (memory != null) {
            memory.lookup(classified.parsed.merchant, classified.parsed.note)?.let { hit ->
                val rememberedType = hit.type?.let { type ->
                    runCatching { TransactionType.valueOf(type) }.getOrNull()
                }
                return classified.copy(
                    parsed = classified.parsed.copy(
                        categoryId = hit.categoryId,
                        type = classified.parsed.type ?: rememberedType,
                        confidence = maxOf(classified.parsed.confidence, MEMORY_CONFIDENCE)
                    ),
                    isSubscription = hit.categoryId == "cat_subscription"
                )
            }
        }

        // 2. LLM fusion when available; regex-only fusion otherwise. Both paths apply
        //    recurring-keyword / known-subscription-service detection.
        val cat = categorizer
        val ai = if (cat != null) {
            try {
                cat.categorise(classified.sms.body, categoryIdByName)
            } catch (e: Exception) {
                Log.w(TAG, "LLM categorisation crashed; using regex result", e)
                null
            }
        } else null

        val fused = AiSignalFusion.fuse(classified.parsed, ai)
        return classified.copy(parsed = fused.parsed, isSubscription = fused.isSubscription)
    }

    private companion object {
        const val TAG = "SmsAiEnricher"
        const val MEMORY_CONFIDENCE = 0.92f
    }
}
