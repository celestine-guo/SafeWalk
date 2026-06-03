package com.celestine.safewalk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapScreen_withoutPermission_showsPermissionPrompt() {
        composeRule.setContent {
            SafeWalkApp {
                MapScreenContent(
                    state = MapUiState(
                        permissionGranted = false,
                        statusMessage = "请先授予定位权限，再开始守护",
                    ),
                    onBack = {},
                    onRequestPermission = {},
                    onDestinationQueryChange = {},
                    onDestinationSuggestionSelected = {},
                    onRouteSelected = {},
                    onResetSelection = {},
                    onStartGuard = {},
                    onStopGuard = {},
                    mapContent = { MapPlaceholder() }
                )
            }
        }

        composeRule.onNodeWithTag("map_permission_prompt").assertIsDisplayed()
        composeRule.onNodeWithText("需要定位权限").assertIsDisplayed()
        composeRule.onNodeWithText("授权定位").assertIsDisplayed()
    }
}
