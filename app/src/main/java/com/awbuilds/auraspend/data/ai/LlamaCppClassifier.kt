package com.awbuilds.auraspend.data.ai

import android.content.Context

/**
 * The llama.cpp-backed runtime.
 *
 * A thin composition of the two pieces that already existed: [LlamaCppLlm] (the
 * vendored native runtime) and [QwenMessageCategorizer] (the prompt and the
 * output parser). Neither knows about the other, and a future grammar-constrained
 * runtime will implement [OnDeviceClassifier] directly and leave both untouched.
 */
class LlamaCppClassifier(context: Context) : OnDeviceClassifier {

    private val llm = LlamaCppLlm(context)

    private val categorizer = QwenMessageCategorizer(
        llm = llm,
        modelFile = ModelConstants.modelFile(context)
    )

    override val id: String = "llama.cpp"

    override suspend fun extract(
        smsBody: String,
        categoryIdByName: Map<String, String>
    ): SmsExtraction? = categorizer.categorise(smsBody, categoryIdByName)

    override fun close() {
        categorizer.close()
    }
}
