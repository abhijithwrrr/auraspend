package com.awbuilds.auraspend.ui.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

object AdManager {
    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    fun createBannerAd(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
            loadAd(AdRequest.Builder().build())
        }
    }
}
