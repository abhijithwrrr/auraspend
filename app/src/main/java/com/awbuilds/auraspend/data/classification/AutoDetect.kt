package com.awbuilds.auraspend.data.classification

import android.content.Context

/**
 * Prefs + helpers for "Auto-categorize messages": after the user consents to the on-device AI and
 * grants SMS access, the app reads new device messages in the background, classifies them into
 * income / expense (with category + subscription), and saves them automatically.
 */
object AutoDetect {

    const val PREF_FILE = "auraspend_prefs"
    const val KEY_ENABLED = "auto_detect_sms"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
