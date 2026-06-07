package com.awbuilds.auraspend.premium

import android.content.Context
import com.awbuilds.auraspend.billing.BillingManager

object PremiumGate {
    private var _isPremium: Boolean? = null

    fun isPremium(context: Context): Boolean {
        if (_isPremium == null) {
            _isPremium = BillingManager.isPremium(context)
        }
        return _isPremium ?: false
    }

    fun invalidate() {
        _isPremium = null
    }
}
