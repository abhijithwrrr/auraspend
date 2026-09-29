package com.awbuilds.auraspend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.awbuilds.auraspend.ui.designsystem.AuraEmptyState
import com.awbuilds.auraspend.ui.designsystem.AuraErrorState
import com.awbuilds.auraspend.ui.designsystem.AuraSpacing
import com.awbuilds.auraspend.ui.designsystem.AuraStatTile
import com.awbuilds.auraspend.ui.designsystem.formatMoney
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.theme.AuraSpendTheme
import com.awbuilds.auraspend.ui.theme.extendedColors
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual-regression coverage for the *states a user actually sees*.
 *
 * The previous suite only rendered a hand-built component gallery, so the whole
 * screen layer sat outside the net: a clipped value, a broken layout or a colour
 * that fails in AMOLED could ship with no test noticing. These render real
 * components with realistic data, in all three themes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-420dpi",
    application = com.awbuilds.auraspend.TestApplication::class
)
class ScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    // ── Empty state ──────────────────────────────────────────────────────────
    @Test
    fun emptyLight() = capture(AppThemeMode.LIGHT, "empty_light", ::EmptyFixture)
    @Test
    fun emptyDark() = capture(AppThemeMode.DARK, "empty_dark", ::EmptyFixture)
    @Test
    fun emptyAmoled() = capture(AppThemeMode.AMOLED, "empty_amoled", ::EmptyFixture)

    // ── Error state ──────────────────────────────────────────────────────────
    @Test
    fun errorLight() = capture(AppThemeMode.LIGHT, "error_light", ::ErrorFixture)
    @Test
    fun errorDark() = capture(AppThemeMode.DARK, "error_dark", ::ErrorFixture)
    @Test
    fun errorAmoled() = capture(AppThemeMode.AMOLED, "error_amoled", ::ErrorFixture)

    // ── Stat tiles (the money grid) ──────────────────────────────────────────
    @Test
    fun statsLight() = capture(AppThemeMode.LIGHT, "stats_light", ::StatsFixture)
    @Test
    fun statsDark() = capture(AppThemeMode.DARK, "stats_dark", ::StatsFixture)
    @Test
    fun statsAmoled() = capture(AppThemeMode.AMOLED, "stats_amoled", ::StatsFixture)

    private fun capture(mode: AppThemeMode, name: String, fixture: @Composable () -> Unit) {
        compose.setContent {
            AuraSpendTheme(themeMode = mode, dynamicColor = false) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) { fixture() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/state_$name.png")
    }
}

@Composable
private fun EmptyFixture() {
    AuraEmptyState(
        icon = Icons.Default.ReceiptLong,
        title = "No transactions yet",
        message = "Add your first transaction and AuraSpend will start building your picture.",
        actionLabel = "Add transaction",
        onAction = {}
    )
}

@Composable
private fun ErrorFixture() {
    AuraErrorState(
        icon = Icons.Default.CloudOff,
        title = "Could not load transactions",
        message = "We could not read your local database. Your data is safe — try again.",
        actionLabel = "Try again",
        onAction = {}
    )
}

@Composable
private fun StatsFixture() {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = Modifier.padding(AuraSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)
    ) {
        // Values chosen to exercise grouping: a lakh figure, a plain thousands
        // figure and a negative balance all render differently.
        AuraStatTile(
            label = "Total balance",
            value = formatMoney(1_23_456.78),
            supporting = "Across 42 transactions",
            modifier = Modifier.fillMaxWidth()
        )
        AuraStatTile(
            label = "Spent this month",
            value = formatMoney(4_280.50),
            valueColor = extended.expenseAmount,
            supporting = "18% more than last month",
            modifier = Modifier.fillMaxWidth()
        )
        AuraStatTile(
            label = "Overdrawn",
            value = formatMoney(-2_150.00),
            valueColor = extended.expenseAmount,
            supporting = "Across 2 accounts",
            modifier = Modifier.fillMaxWidth()
        )
        AuraStatTile(
            label = "Income this month",
            value = formatMoney(85_000.00),
            valueColor = extended.incomeAmount,
            supporting = "Salary credit",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
