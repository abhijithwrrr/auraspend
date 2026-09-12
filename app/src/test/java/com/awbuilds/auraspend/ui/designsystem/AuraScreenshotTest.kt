package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.awbuilds.auraspend.ui.theme.AppThemeMode
import com.awbuilds.auraspend.ui.theme.AuraSpendTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshot tests for the Aurora design system.
 *
 * These render real resources (including the bundled font) via Robolectric and
 * write PNGs under `build/screenshots`. They are the visual-regression net for
 * the design system; CI can diff them with Roborazzi's verify task.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-420dpi",
    application = com.awbuilds.auraspend.TestApplication::class
)
class AuraScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun componentGalleryLight() = capture(AppThemeMode.LIGHT, "light")

    @Test
    fun componentGalleryDark() = capture(AppThemeMode.DARK, "dark")

    @Test
    fun componentGalleryAmoled() = capture(AppThemeMode.AMOLED, "amoled")

    private fun capture(mode: AppThemeMode, name: String) {
        compose.setContent {
            AuraSpendTheme(themeMode = mode, dynamicColor = false) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(AuraSpacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)
                ) {
                    Text(
                        "Aurora components",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
                        AuraCard(style = AuraCardStyle.Outlined, modifier = Modifier.weight(1f)) {
                            Text("Outlined", style = MaterialTheme.typography.labelMedium)
                            AnimatedMoney(
                                amount = 12_345.67,
                                style = AuraType.moneyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                animate = false
                            )
                        }
                        AuraCard(style = AuraCardStyle.Tonal, modifier = Modifier.weight(1f)) {
                            Text("Tonal", style = MaterialTheme.typography.labelMedium)
                            AuraProgressRing(
                                progress = 0.65f,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp),
                                stroke = 6.dp
                            )
                        }
                    }
                    AuraSegmentedControl(
                        options = listOf("This Month", "30 Days", "All Time"),
                        selectedIndex = 0,
                        onSelect = {}
                    )
                    AuraEmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "No transactions yet",
                        message = "Add your first transaction and AuraSpend will start building your picture."
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/aurora_$name.png")
    }
}
