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
    if (BuildConfig.CLERK_PUBLISHABLE_KEY.isBlank()) {
        CenteredMessage(text = stringResource(id = R.string.auth_missing_key))
        return
    }

    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    when {
        !isInitialized -> AuthLoading()
        user == null -> AuthView(
            modifier = Modifier.fillMaxSize(),
            onAuthComplete = {}
        )
        else -> signedInContent()
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
