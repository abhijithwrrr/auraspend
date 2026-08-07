package com.awbuilds.auraspend.data.ai

import android.content.Context
import android.util.Log

/**
 * Provides the single [LocalLlm] used by the classification flow.
 *
 * It returns the llama.cpp-backed adapter when the native runtime is linked; otherwise it safely
 * returns [UnavailableLlm] so the app keeps working with regex-based classification.
 */
object LocalLlmProvider {

    private const val TAG = "LocalLlmProvider"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var delegate: LocalLlm? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** True when a downloaded, usable model is present. */
    fun isReady(context: Context): Boolean =
        ModelConstants.isDownloaded(context) && get() !is UnavailableLlm

    /**
     * Returns the shared runtime, building it lazily. Never returns null; falls back to
     * [UnavailableLlm] when the native library cannot be loaded (e.g. not built into the APK).
     */
    fun get(): LocalLlm {
        delegate?.let { return it }
        synchronized(this) {
            delegate?.let { return it }
            val ctx = appContext
                ?: run { Log.w(TAG, "LocalLlmProvider used before init()"); return UnavailableLlm }
            val llm = try {
                LlamaCppLlm(ctx).also { Log.i(TAG, "llama.cpp runtime initialised") }
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native llama.cpp runtime unavailable; using regex fallback", e)
                UnavailableLlm
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to initialise llama.cpp runtime; using regex fallback", e)
                UnavailableLlm
            }
            delegate = llm
            return llm
        }
    }
}
