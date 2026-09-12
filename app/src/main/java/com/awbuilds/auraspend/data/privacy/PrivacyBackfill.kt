package com.awbuilds.auraspend.data.privacy

import android.content.Context
import com.awbuilds.auraspend.AuraSpendApp
import kotlinx.coroutines.flow.first

/**
 * One-time retrofit: rows written before [SensitiveDataMasker] existed may still
 * contain phone numbers / reference ids in `sms_messages.body` and
 * `transactions.note`/`merchant`. This pass masks everything already stored and
 * records completion so it never runs twice.
 *
 * Safe to re-run: masking is idempotent (already-masked values don't change), and
 * the pref gate short-circuits subsequent launches anyway.
 */
object PrivacyBackfill {

    private const val PREF_FILE = "auraspend_prefs"
    private const val KEY_DONE = "pii_backfill_done_v1"

    suspend fun runIfNeeded(applicationContext: Context) {
        val prefs = applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return

        val app = applicationContext as? AuraSpendApp ?: return
        val smsDao = app.database.smsMessageDao()

        // 1. Queue bodies.
        smsDao.observeAll().first()
            .filter { SensitiveDataMasker.mask(it.body) != it.body }
            .forEach { row ->
                smsDao.update(row.copy(body = SensitiveDataMasker.mask(row.body)))
            }

        // 2. Transaction notes + merchants. saveTransaction uses REPLACE on the id,
        //    so re-saving overwrites in place.
        app.transactionRepository.getAllTransactions().first()
            .filter { txn ->
                SensitiveDataMasker.mask(txn.note) != txn.note ||
                    txn.merchant?.let { SensitiveDataMasker.mask(it) != it } == true
            }
            .forEach { txn ->
                app.transactionRepository.saveTransaction(
                    txn.copy(
                        note = SensitiveDataMasker.mask(txn.note),
                        merchant = txn.merchant?.let { SensitiveDataMasker.mask(it) }
                    )
                )
            }

        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }
}
