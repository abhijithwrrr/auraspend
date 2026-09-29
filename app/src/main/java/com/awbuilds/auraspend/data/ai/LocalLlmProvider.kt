package com.awbuilds.auraspend.data.ai

import android.content.Context
import android.util.Log

/**
 * Supplies the [OnDeviceClassifier] used by the classification flow.
 *
 * This is the single place a runtime is chosen, and the single place the app
 * degrades. When the native runtime cannot be linked it returns
 * [UnavailableClassifier] rather than throwing, so the app keeps working on
 * regex-only classification — the property that makes swapping runtimes safe.
 *
 * Cached permanently once built: a failed native link is a build/packaging fact,
 * not a transient one, so retrying it on every message would only add log noise.
 */
object LocalLlmProvider {

    private const val TAG = "LocalLlmProvider"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var delegate: OnDeviceClassifier? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** True when a downloaded, usable model is present **and** a runtime is linked. */
    fun isReady(context: Context): Boolean =
        ModelConstants.isDownloaded(context) && get() !is UnavailableClassifier

    /** Returns the shared classifier, building it lazily. Never null. */
    fun get(): OnDeviceClassifier {
        delegate?.let { return it }
        synchronized(this) {
            delegate?.let { return it }
            val ctx = appContext
            if (ctx == null) {
                Log.w(TAG, "LocalLlmProvider used before init()")
                return UnavailableClassifier
            }
            val classifier = build(ctx)
            delegate = classifier
            return classifier
        }
    }

    private fun build(context: Context): OnDeviceClassifier = try {
        LlamaCppClassifier(context).also { Log.i(TAG, "on-device classifier ready: ${it.id}") }
    } catch (e: UnsatisfiedLinkError) {
        Log.w(TAG, "Native runtime unavailable; using regex fallback", e)
        UnavailableClassifier
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to initialise on-device runtime; using regex fallback", e)
        UnavailableClassifier
    }

    /** Test seam: swap the runtime without touching production wiring. */
    internal fun overrideForTest(classifier: OnDeviceClassifier?) {
        delegate = classifier
    }
}
