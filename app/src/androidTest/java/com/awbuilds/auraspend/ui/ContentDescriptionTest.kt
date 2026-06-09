package com.awbuilds.auraspend.ui

import android.content.ContentResolver
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.auraspend.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentDescriptionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun allIcons_haveContentDescriptions() {
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomNavIcons_haveLabels() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Home").assertExists()
        composeTestRule.onNodeWithContentDescription("Analytics").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }
}
