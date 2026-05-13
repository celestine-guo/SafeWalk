package com.celestine.safewalk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView

@Composable
fun AuthGate(
    signedInContent: @Composable () -> Unit,
) {
    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
    val clerkInitializationError by Clerk.initializationError.collectAsStateWithLifecycle()
    val appInitializationError by SafeWalkAuthInitialization.initializationError.collectAsStateWithLifecycle()
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    val state = resolveAuthGateState(
        hasPublishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY.isNotBlank(),
        isInitialized = isInitialized,
        initializationError = appInitializationError ?: clerkInitializationError,
        isSignedIn = user != null
    )

    AuthGateContent(state = state, signedInContent = signedInContent)
}

enum class AuthGateState {
    MissingKey,
    InitializationFailed,
    Loading,
    SignedOut,
    SignedIn
}

fun resolveAuthGateState(
    hasPublishableKey: Boolean,
    isInitialized: Boolean,
    initializationError: Throwable?,
    isSignedIn: Boolean,
): AuthGateState = when {
    !hasPublishableKey -> AuthGateState.MissingKey
    initializationError != null -> AuthGateState.InitializationFailed
    !isInitialized -> AuthGateState.Loading
    !isSignedIn -> AuthGateState.SignedOut
    else -> AuthGateState.SignedIn
}

@Composable
fun AuthGateContent(
    state: AuthGateState,
    signedInContent: @Composable () -> Unit,
    signedOutContent: @Composable () -> Unit = {
        AuthView(
            modifier = Modifier.fillMaxSize(),
            onAuthComplete = {}
        )
    },
) {
    when (state) {
        AuthGateState.MissingKey -> CenteredMessage(text = stringResource(id = R.string.auth_missing_key))
        AuthGateState.InitializationFailed -> CenteredMessage(text = stringResource(id = R.string.auth_initialization_failed))
        AuthGateState.Loading -> AuthLoading()
        AuthGateState.SignedOut -> signedOutContent()
        AuthGateState.SignedIn -> signedInContent()
    }
}

@Composable
private fun AuthLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(id = R.string.auth_loading),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
