package com.awbuilds.auraspend.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.auraspend.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigation_splashToOnboarding() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigation_onboardingPages() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigation_getStarted() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.waitForIdle()
    }
}
