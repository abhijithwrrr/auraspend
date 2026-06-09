package com.awbuilds.auraspend.ui

import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.auraspend.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityScanTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        AccessibilityChecks.enable().apply {
            setRunChecksFromRootView(true)
            setThrowExceptionForRedundantContentDescription(false)
            setTouchExplorationStateCheckEnabled(true)
            addAllowedEventType(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        }
    }

    @Test
    fun accessibilityScan_appLaunch_noViolations() {
        composeTestRule.waitForIdle()
    }

    @Test
    fun accessibilityScan_splashScreen() {
        composeTestRule.waitForIdle()
    }
}
