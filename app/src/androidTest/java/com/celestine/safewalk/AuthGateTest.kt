package com.celestine.safewalk

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingPublishableKeyShowsMissingKeyMessage() {
        composeRule.setContent {
            AuthGateContent(
                state = AuthGateState.MissingKey,
                signedInContent = { Box(modifier = Modifier.testTag("signed_in_content")) },
                signedOutContent = { Box(modifier = Modifier.testTag("signed_out_content")) }
            )
        }

        composeRule.onNodeWithText("请先配置 CLERK_PUBLISHABLE_KEY").assertIsDisplayed()
    }

    @Test
    fun initializationFailureShowsLocalizedErrorMessage() {
        composeRule.setContent {
            AuthGateContent(
                state = AuthGateState.InitializationFailed,
                signedInContent = { Box(modifier = Modifier.testTag("signed_in_content")) },
                signedOutContent = { Box(modifier = Modifier.testTag("signed_out_content")) }
            )
        }

        composeRule.onNodeWithText("登录服务初始化失败，请稍后重试").assertIsDisplayed()
    }

    @Test
    fun loadingShowsProgressMessage() {
        composeRule.setContent {
            AuthGateContent(
                state = AuthGateState.Loading,
                signedInContent = { Box(modifier = Modifier.testTag("signed_in_content")) },
                signedOutContent = { Box(modifier = Modifier.testTag("signed_out_content")) }
            )
        }

        composeRule.onNodeWithText("正在检查登录状态…").assertIsDisplayed()
    }

    @Test
    fun signedOutShowsAuthContent() {
        composeRule.setContent {
            AuthGateContent(
                state = AuthGateState.SignedOut,
                signedInContent = { Box(modifier = Modifier.testTag("signed_in_content")) },
                signedOutContent = { Text("AuthView", modifier = Modifier.testTag("signed_out_content")) }
            )
        }

        composeRule.onNodeWithTag("signed_out_content").assertIsDisplayed()
    }

    @Test
    fun signedInShowsSignedInContent() {
        composeRule.setContent {
            AuthGateContent(
                state = AuthGateState.SignedIn,
                signedInContent = { Text("Signed in", modifier = Modifier.testTag("signed_in_content")) },
                signedOutContent = { Box(modifier = Modifier.testTag("signed_out_content")) }
            )
        }

        composeRule.onNodeWithTag("signed_in_content").assertIsDisplayed()
    }
}
