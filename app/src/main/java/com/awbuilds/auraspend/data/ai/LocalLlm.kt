package com.awbuilds.auraspend.data.ai

import java.io.File

/**
 * Minimal abstraction over the on-device GGUF runtime (llama.cpp Android library).
 *
 * Everything in the app talks to this interface, keeping the categoriser decoupled from the
 * exact API of the vendored llama.cpp Android module. The concrete adapter touches the native
 * library only.
 */
interface LocalLlm {

    /** True when the given model file appears complete and loadable. */
    fun isModelAvailable(modelFile: File): Boolean

    /**
     * Runs [prompt] through the loaded model and returns the generated completion text.
     * Returns null when the model is not loaded or inference failed.
     */
    fun generate(prompt: String): String?

    /** Releases native resources. Safe to call multiple times. */
    fun close()
}
