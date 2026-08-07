package com.awbuilds.auraspend.data.ai

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * [LocalLlm] backed by the bundled llama.cpp Android runtime (the vendored `:llama` module).
 *
 * The engine keeps a single chat session; each classification is sent as one self-contained user
 * prompt so the model always has the full instruction + message in context.
 */
class LlamaCppLlm(context: Context) : LocalLlm {

    private val appContext = context.applicationContext
    private val engine: InferenceEngine = AiChat.getInferenceEngine(appContext)
    private val loadMutex = Mutex()
    private var loadedModelPath: String? = null

    override fun isModelAvailable(modelFile: File): Boolean =
        modelFile.exists() && modelFile.isFile && modelFile.length() >= ModelConstants.MIN_SIZE_BYTES

    override fun generate(prompt: String): String? = runBlocking {
        try {
            ensureLoaded(ModelConstants.modelFile(appContext))
            engine.setSystemPrompt(SYSTEM_BASE)

            val sb = StringBuilder()
            engine.sendUserPrompt(prompt, ModelConstants.MAX_OUTPUT_TOKENS).collect { sb.append(it) }
            sb.toString().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun ensureLoaded(modelFile: File) = loadMutex.withLock {
        if (loadedModelPath == modelFile.absolutePath) return@withLock
        awaitReady()
        engine.loadModel(modelFile.absolutePath)
        loadedModelPath = modelFile.absolutePath
    }

    /** Waits until the native library has finished loading into [InferenceEngine.State.Initialized]. */
    private suspend fun awaitReady() {
        repeat(100) {
            when (val s = engine.state.value) {
                is InferenceEngine.State.Uninitialized,
                is InferenceEngine.State.Initializing,
                is InferenceEngine.State.Error -> delay(200)
                else -> return
            }
            delay(200)
        }
    }

    override fun close() {
        try {
            engine.destroy()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val SYSTEM_BASE = "You are a personal finance categorisation assistant. " +
            "Respond with exactly the JSON object the user requests and nothing else."
    }
}
