package com.awbuilds.auraspend.data.ai

import java.io.File

/**
 * Central configuration for the on-device Qwen model used for message categorisation.
 *
 * The model is a quantised GGUF (Q4_K_M) of Qwen2.5-0.5B-Instruct, downloaded once (after
 * explicit user consent) and then run fully on-device via the bundled llama.cpp runtime.
 */
object ModelConstants {

    /** Human friendly name shown to the user. */
    const val MODEL_LABEL = "Qwen 2.5 0.5B (Local AI)"

    /** File name of the downloaded model. */
    const val MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"

    /** Source of the model. Apache-2.0 licensed. */
    const val MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"

    /**
     * Exact size of the published file, in bytes (468.6 MiB).
     *
     * The previous constant claimed 400 MiB, which is simply wrong — the
     * consent dialog has been telling users the wrong download size, and the
     * loose `MIN_SIZE_BYTES` below let a truncated file through.
     */
    val EXPECTED_SIZE_BYTES: Long = 491_400_032L

    /** Minimum acceptable size for a complete model (bytes). */
    val MIN_SIZE_BYTES: Long = 450 * 1024 * 1024L

    /**
     * SHA-256 of the published file, taken from the upstream `x-linked-etag`
     * header. Verified after download, before the model is moved into place.
     *
     * A size check alone cannot tell a truncated download from a corrupt one —
     * or a substituted file of the right length. This matters more than usual
     * here: the model is fetched over the network and then executed on-device.
     */
    const val EXPECTED_SHA256: String =
        "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db"

    const val PREF_CONSENT_GIVEN = "ai_model_consent_given"

    /** Maximum number of tokens each classification call may generate. */
    const val MAX_OUTPUT_TOKENS = 128

    /** Wall-clock budget for a single classification generation. */
    const val GENERATION_TIMEOUT_MS: Long = 90_000L

    fun baseModelsDir(context: android.content.Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(context: android.content.Context): File =
        File(baseModelsDir(context), MODEL_FILE_NAME)

    fun isDownloaded(context: android.content.Context): Boolean {
        val file = modelFile(context)
        return file.exists() && file.length() >= MIN_SIZE_BYTES
    }
}
