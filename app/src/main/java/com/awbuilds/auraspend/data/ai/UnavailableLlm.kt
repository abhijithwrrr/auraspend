package com.awbuilds.auraspend.data.ai

import java.io.File

/**
 * A neutral [LocalLlm] used when the native llama.cpp runtime is not linked into the APK.
 * Classification then falls back to the existing regex-based classifier, so the app keeps
 * working even before the native runtime is configured on a build machine.
 */
object UnavailableLlm : LocalLlm {
    override fun isModelAvailable(modelFile: File): Boolean = false
    override fun generate(prompt: String): String? = null
    override fun close() = Unit
}
