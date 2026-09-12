package com.awbuilds.auraspend.core

import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * Single logging entry point for the app.
 *
 * Hard rule (see AGENTS.md): every IO / platform boundary catches its
 * exceptions, logs them through here, and degrades gracefully. Nothing may
 * crash the UI because a bank parser, a download or the LLM failed.
 */
object AuraLog {
    private const val PREFIX = "AuraSpend"

    fun d(tag: String, message: String) = Log.d("$PREFIX/$tag", message)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        Log.w("$PREFIX/$tag", message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e("$PREFIX/$tag", message, throwable)
}

/**
 * Runs [block] and returns its value, or [fallback] when it throws.
 * [CancellationException] is never swallowed — coroutine cancellation must
 * propagate so structured concurrency keeps working.
 */
inline fun <T> boundary(tag: String, fallback: T, block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuraLog.e(tag, "Boundary failed, using fallback", e)
        fallback
    }

/**
 * Like [boundary] but for cases with no sensible fallback. Returns null and
 * logs instead of letting the exception reach the UI.
 */
inline fun <T> boundaryOrNull(tag: String, block: () -> T): T? =
    boundary(tag, null, block)
