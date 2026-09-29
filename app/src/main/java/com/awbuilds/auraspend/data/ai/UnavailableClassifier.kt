package com.awbuilds.auraspend.data.ai

/**
 * The null object returned when no native runtime is available.
 *
 * Every call declines rather than throwing, so callers never need a null check on
 * the runtime itself — they check the *result* for null, which the fusion layer
 * already handles by keeping the regex parse.
 */
object UnavailableClassifier : OnDeviceClassifier {

    override val id: String = "unavailable"

    override suspend fun extract(
        smsBody: String,
        categoryIdByName: Map<String, String>
    ): SmsExtraction? = null

    override fun close() = Unit
}
