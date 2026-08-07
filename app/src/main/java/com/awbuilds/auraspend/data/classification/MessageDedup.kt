package com.awbuilds.auraspend.data.classification

import android.content.Context

/**
 * Tracks which SMS ids have already been saved as transactions, so the same message is never saved
 * twice (e.g. by both Smart Add and the background auto-detect worker). The hard guarantee against
 * double-saves is the unique `sourceSmsId` index on the transactions table; this prefs-backed
 * helper is a fast-path check and the UI source of truth for per-message saved/ignored state.
 *
 * A separate "ignored" set records messages the worker scanned but that can never become a
 * transaction (no amount/type detected). They are skipped on later passes without blocking the
 * scan cursor, but Smart Add can still save them manually.
 */
object MessageDedup {

    private const val PREF_FILE = "auraspend_prefs"
    private const val KEY_SAVED_IDS = "saved_sms_ids"
    private const val KEY_IGNORED_IDS = "ignored_sms_ids"

    fun isAlreadySaved(context: Context, smsId: String): Boolean =
        if (smsId.isBlank()) false
        else getSavedIds(context).contains(smsId)

    fun markSaved(context: Context, smsId: String) {
        if (smsId.isBlank()) return
        val ids = getSavedIds(context).toMutableSet()
        if (ids.add(smsId)) {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_SAVED_IDS, ids).apply()
        }
    }

    fun markManySaved(context: Context, smsIds: List<String>) {
        val ids = getSavedIds(context).toMutableSet()
        var changed = false
        for (id in smsIds) {
            if (id.isNotBlank() && ids.add(id)) changed = true
        }
        if (changed) {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_SAVED_IDS, ids).apply()
        }
    }

    fun isIgnored(context: Context, smsId: String): Boolean =
        smsId.isNotBlank() && getIgnoredIds(context).contains(smsId)

    fun markManyIgnored(context: Context, smsIds: List<String>) {
        val ids = getIgnoredIds(context).toMutableSet()
        var changed = false
        for (id in smsIds) {
            if (id.isNotBlank() && ids.add(id)) changed = true
        }
        if (changed) {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_IGNORED_IDS, ids).apply()
        }
    }

    private fun getSavedIds(context: Context): Set<String> =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY_SAVED_IDS, emptySet()) ?: emptySet()

    private fun getIgnoredIds(context: Context): Set<String> =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY_IGNORED_IDS, emptySet()) ?: emptySet()
}
