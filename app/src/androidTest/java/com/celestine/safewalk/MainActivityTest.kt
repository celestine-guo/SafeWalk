package com.celestine.safewalk

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launchContent(onWalkingModeSelected: () -> Unit = {}) {
        composeRule.setContent {
            SafeWalkApp {
                SafeWalkScreen(onWalkingModeSelected = onWalkingModeSelected)
            }
        }
    }

    @Test
    fun homeScreen_displaysCriticalUiElements() {
        launchContent()

        composeRule.onNodeWithText("夜间安全守护").assertIsDisplayed()
        composeRule.onNodeWithText("系统运行中").assertIsDisplayed()
        composeRule.onNodeWithText("开始夜路守护").assertIsDisplayed()
        composeRule.onNodeWithText("位置共享").assertIsDisplayed()
        composeRule.onNodeWithText("警报").assertIsDisplayed()
        composeRule.onNodeWithTag("sos_button").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
    }

    @Test
    fun clickingWalkingMode_invokesMapNavigationCallback() {
        var clickCount = 0

        launchContent {
            clickCount += 1
        }

        composeRule.onNodeWithTag("mode_card_开始夜路守护").performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun clickingTaxiMode_showsPlaceholderMessage() {
        launchContent()

        composeRule.onNodeWithTag("mode_card_安心打车").performClick()

        composeRule.onNodeWithTag("mode_card_安心打车")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
            )
        composeRule.onNodeWithTag("mode_placeholder_message").assertIsDisplayed()
        composeRule.onNodeWithText("打车模式即将支持").assertIsDisplayed()
    }

    @Test
    fun clickingSosButton_togglesStateDescription() {
        launchContent()

        composeRule.onNodeWithTag("sos_button")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "idle"
                )
            )

        composeRule.onNodeWithTag("sos_button").performClick()

        composeRule.onNodeWithTag("sos_button")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "activated"
                )
            )
    }
}
