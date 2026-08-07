package com.awbuilds.auraspend.data.ai

import android.content.Context
import com.awbuilds.auraspend.data.classification.ClassifiedSms

/**
 * Shared helper that enriches heuristic classification results with the on-device LLM when the
 * model is downloaded and the runtime is linked. Falls back to the heuristic result otherwise.
 */
class SmsAiEnricher(context: Context) {

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

    fun enrich(
        classified: ClassifiedSms,
        categoryIdByName: Map<String, String>
    ): ClassifiedSms {
        val cat = categorizer ?: return classified
        val ai = cat.categorise(classified.sms.body, categoryIdByName) ?: return classified

        // If AI explicitly says it's not a transaction, we wipe the amount so the worker ignores it.
        // We also record if it was an AI-confirmed transaction.
        val parsed = if (ai.isTransaction) {
            classified.parsed.copy(
                categoryId = ai.categoryId ?: classified.parsed.categoryId,
                type = ai.type ?: classified.parsed.type,
                merchant = ai.merchant ?: classified.parsed.merchant
            )
        } else {
            classified.parsed.copy(amount = null, type = null)
        }
        return classified.copy(parsed = parsed, isSubscription = ai.isSubscription)
    }
}
