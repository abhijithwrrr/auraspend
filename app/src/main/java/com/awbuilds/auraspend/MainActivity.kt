package com.awbuilds.auraspend

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.awbuilds.auraspend.ui.navigation.AuraSpendNavHost
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.theme.AuraSpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val app = application as AuraSpendApp

        setContent {
            val prefs = getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
            var themeMode by remember { mutableStateOf(
                AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name)
            ) }
            var dynamicColor by remember { mutableStateOf(
                prefs.getBoolean("dynamic_color", true)
            ) }

            AuraSpendTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AuraSpendNavHost(
                    repository = app.transactionRepository,
                    themeMode = themeMode,
                    onThemeChanged = { mode ->
                        themeMode = mode
                        prefs.edit().putString("theme_mode", mode.name).apply()
                    },
                    dynamicColor = dynamicColor,
                    onDynamicColorChanged = { enabled ->
                        dynamicColor = enabled
                        prefs.edit().putBoolean("dynamic_color", enabled).apply()
                    }
                )
            }
        }
    }
}
