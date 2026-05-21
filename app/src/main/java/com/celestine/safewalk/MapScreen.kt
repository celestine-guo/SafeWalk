package com.celestine.safewalk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MapRouteSummary(
    val distanceText: String,
    val durationText: String,
)

data class MapUiState(
    val permissionGranted: Boolean,
    val isSearchingRoute: Boolean = false,
    val routeSummary: MapRouteSummary? = null,
    val isGuarding: Boolean = false,
    val isCreatingTrip: Boolean = false,
    val statusMessage: String,
    val syncMessage: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    state: MapUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onStartGuard: () -> Unit,
    onStopGuard: () -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.map_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("map_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            mapContent()

            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .testTag("map_status_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        state.syncMessage?.let { syncMessage ->
                            Text(
                                text = syncMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!state.permissionGranted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth()
                        .testTag("map_permission_prompt"),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.map_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.map_permission_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onRequestPermission) {
                            Text(text = stringResource(id = R.string.map_permission_action))
                        }
                    }
                }
            }

            state.routeSummary?.let { summary ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .testTag("map_route_summary"),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.map_route_ready),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                id = R.string.map_route_summary_format,
                                summary.distanceText,
                                summary.durationText
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.isSearchingRoute) {
                            Text(
                                text = stringResource(id = R.string.map_route_searching),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = if (state.isGuarding) onStopGuard else onStartGuard,
                            enabled = !state.isCreatingTrip && !state.isSearchingRoute,
                            modifier = Modifier.testTag("guard_button")
                        ) {
                            Text(
                                text = when {
                                    state.isCreatingTrip -> stringResource(id = R.string.map_creating_trip)
                                    state.isGuarding -> stringResource(id = R.string.map_stop_guarding)
                                    else -> stringResource(id = R.string.map_start_guarding)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
    )
}
