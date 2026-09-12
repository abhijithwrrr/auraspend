package com.awbuilds.auraspend.data.ai

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * [LocalLlm] backed by the bundled llama.cpp Android runtime (the vendored `:llama` module).
 *
 * The engine keeps a single chat session; each classification is sent as one self-contained user
 * prompt so the model always has the full instruction + message in context.
 *
 * Hardened for background use:
 *  - every generation runs under a wall-clock timeout so a hung native call can never stall the
 *    classification pipeline;
 *  - an engine that failed to initialise short-circuits instead of blocking for the full retry
 *    window;
 *  - a failed [InferenceEngine.loadModel] resets the cached path so the next call retries.
 */
class LlamaCppLlm(context: Context) : LocalLlm {

    private val appContext = context.applicationContext
    private val engine: InferenceEngine = AiChat.getInferenceEngine(appContext)
    private val loadMutex = Mutex()

    @Volatile
    private var loadedModelPath: String? = null

    override fun isModelAvailable(modelFile: File): Boolean =
        modelFile.exists() && modelFile.isFile && modelFile.length() >= ModelConstants.MIN_SIZE_BYTES

    override fun generate(prompt: String): String? = runBlocking {
        try {
            withTimeout(ModelConstants.GENERATION_TIMEOUT_MS) {
                if (!ensureLoaded(ModelConstants.modelFile(appContext))) return@withTimeout null
                engine.setSystemPrompt(SYSTEM_BASE)

                val sb = StringBuilder()
                engine.sendUserPrompt(prompt, ModelConstants.MAX_OUTPUT_TOKENS).collect { sb.append(it) }
                sb.toString().takeIf { it.isNotBlank() }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Generation timed out after ${ModelConstants.GENERATION_TIMEOUT_MS}ms")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Generation failed", e)
            null
        }
    }

    /** Loads [modelFile] if needed. Returns false when the engine cannot accept work. */
    private suspend fun ensureLoaded(modelFile: File): Boolean = loadMutex.withLock {
        if (loadedModelPath == modelFile.absolutePath) return@withLock true
        if (!isModelAvailable(modelFile)) return@withLock false
        awaitReady() ?: return@withLock false
        try {
            engine.loadModel(modelFile.absolutePath)
            loadedModelPath = modelFile.absolutePath
            true
        } catch (e: Exception) {
            Log.w(TAG, "loadModel failed; will retry on next call", e)
            loadedModelPath = null
            false
        }
    }

    /**
     * Waits until the native library finishes loading. Returns false if the engine ended in an
     * error state (retrying later is pointless within this call).
     */
    private suspend fun awaitReady(): Boolean {
        repeat(ENGINE_READY_POLLS) {
            when (val s = engine.state.value) {
                is InferenceEngine.State.Error -> {
                    Log.w(TAG, "Inference engine error during init: ${s.exception.message}")
                    return false
                }
                is InferenceEngine.State.Uninitialized,
                is InferenceEngine.State.Initializing -> delay(POLL_INTERVAL_MS)
                else -> return true
            }
        }
        // Timed out waiting - treat as unavailable unless it actually became ready.
        return engine.state.value !is InferenceEngine.State.Uninitialized &&
            engine.state.value !is InferenceEngine.State.Initializing &&
            engine.state.value !is InferenceEngine.State.Error
    }

    override fun close() {
        try {
            engine.destroy()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "LlamaCppLlm"
        const val SYSTEM_BASE = "You are a personal finance categorisation assistant. " +
            "Respond with exactly the JSON object the user requests and nothing else."
        const val ENGINE_READY_POLLS = 75
        const val POLL_INTERVAL_MS = 200L
    }
}
