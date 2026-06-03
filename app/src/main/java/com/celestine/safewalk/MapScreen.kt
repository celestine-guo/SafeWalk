package com.celestine.safewalk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MapDestinationSuggestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val distanceText: String,
    val latitude: Double,
    val longitude: Double,
)

data class MapRouteOption(
    val id: String,
    val title: String,
    val distanceText: String,
    val durationText: String,
)

data class MapUiState(
    val permissionGranted: Boolean,
    val destinationQuery: String = "",
    val destinationSuggestions: List<MapDestinationSuggestion> = emptyList(),
    val isSearchingDestination: Boolean = false,
    val isSearchingRoute: Boolean = false,
    val routeOptions: List<MapRouteOption> = emptyList(),
    val selectedRouteId: String? = null,
    val isGuarding: Boolean = false,
    val isCreatingTrip: Boolean = false,
    val statusMessage: String,
    val syncMessage: String? = null,
)

@Composable
fun MapScreenContent(
    state: MapUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onDestinationSuggestionSelected: (MapDestinationSuggestion) -> Unit,
    onRouteSelected: (String) -> Unit,
    onResetSelection: () -> Unit,
    onStartGuard: () -> Unit,
    onStopGuard: () -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeWalkPalette.Night)
    ) {
        mapContent()

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 18.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SafeWalkPalette.NightSurface.copy(alpha = 0.86f))
                        .testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = SafeWalkPalette.DarkText
                    )
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_status_banner"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.88f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SafeWalkPalette.Sky.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = SafeWalkPalette.Sky
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.statusMessage,
                                color = SafeWalkPalette.DarkText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.syncMessage ?: stringResource(id = R.string.map_search_destination_hint),
                                color = SafeWalkPalette.DarkTextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            DestinationSearchPanel(
                state = state,
                onDestinationQueryChange = onDestinationQueryChange,
                onDestinationSuggestionSelected = onDestinationSuggestionSelected,
            )
        }

        if (!state.permissionGranted) {
            PermissionPrompt(
                onRequestPermission = onRequestPermission,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }

        if (state.routeOptions.isNotEmpty()) {
            RouteActionPanel(
                state = state,
                onRouteSelected = onRouteSelected,
                onResetSelection = onResetSelection,
                onStartGuard = onStartGuard,
                onStopGuard = onStopGuard,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_permission_prompt"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SafeWalkPalette.Lavender.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = SafeWalkPalette.Lavender)
            }
            Text(
                text = stringResource(id = R.string.map_permission_title),
                color = SafeWalkPalette.DarkText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.map_permission_desc),
                color = SafeWalkPalette.DarkTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeWalkPalette.Lavender)
            ) {
                Text(text = stringResource(id = R.string.map_permission_action), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DestinationSearchPanel(
    state: MapUiState,
    onDestinationQueryChange: (String) -> Unit,
    onDestinationSuggestionSelected: (MapDestinationSuggestion) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_destination_search"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.destinationQuery,
                onValueChange = onDestinationQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_search_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = SafeWalkPalette.DarkText),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.map_destination_search_placeholder),
                        color = SafeWalkPalette.DarkTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = SafeWalkPalette.DarkTextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafeWalkPalette.Sky,
                    unfocusedBorderColor = SafeWalkPalette.NightPanel,
                    focusedContainerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.96f),
                    unfocusedContainerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.96f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (state.isSearchingDestination) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SafeWalkPalette.Sky,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(id = R.string.map_destination_searching),
                        color = SafeWalkPalette.DarkTextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.destinationSuggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.destinationSuggestions, key = { it.id }) { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDestinationSuggestionSelected(suggestion) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SafeWalkPalette.NightPanel)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = SafeWalkPalette.Sky
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.title,
                                        color = SafeWalkPalette.DarkText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (suggestion.subtitle.isNotBlank()) {
                                        Text(
                                            text = suggestion.subtitle,
                                            color = SafeWalkPalette.DarkTextMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Text(
                                    text = suggestion.distanceText,
                                    color = SafeWalkPalette.Sky,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteActionPanel(
    state: MapUiState,
    onRouteSelected: (String) -> Unit,
    onResetSelection: () -> Unit,
    onStartGuard: () -> Unit,
    onStopGuard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRoute = state.routeOptions.firstOrNull { it.id == state.selectedRouteId }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_route_summary"),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        colors = CardDefaults.cardColors(containerColor = SafeWalkPalette.NightSurface.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.map_route_ready),
                        color = SafeWalkPalette.DarkText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedRoute?.let {
                            stringResource(
                                id = R.string.map_route_summary_format,
                                it.distanceText,
                                it.durationText
                            )
                        } ?: stringResource(id = R.string.map_select_route_to_guard),
                        color = SafeWalkPalette.DarkTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.isSearchingRoute) {
                        Text(
                            text = stringResource(id = R.string.map_route_searching),
                            color = SafeWalkPalette.Sky,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SafeWalkPalette.Sky.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = selectedRoute?.let { "ETA ${it.durationText}" } ?: "待选择",
                        color = SafeWalkPalette.Sky,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.routeOptions.forEach { option ->
                    val selected = option.id == state.selectedRouteId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRouteSelected(option.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) SafeWalkPalette.Sky.copy(alpha = 0.2f) else SafeWalkPalette.NightPanel
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.Navigation,
                                contentDescription = null,
                                tint = if (selected) SafeWalkPalette.Sky else SafeWalkPalette.DarkTextMuted
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    color = SafeWalkPalette.DarkText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.map_route_summary_format,
                                        option.distanceText,
                                        option.durationText
                                    ),
                                    color = SafeWalkPalette.DarkTextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = if (state.isGuarding) onStopGuard else onStartGuard,
                    enabled = !state.isCreatingTrip && !state.isSearchingRoute &&
                        (state.isGuarding || state.selectedRouteId != null),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("guard_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isGuarding) SafeWalkPalette.Danger else SafeWalkPalette.Lavender,
                        disabledContainerColor = SafeWalkPalette.NightPanel,
                        disabledContentColor = SafeWalkPalette.DarkTextMuted
                    )
                ) {
                    Icon(
                        imageVector = if (state.isGuarding) Icons.Filled.Warning else Icons.Filled.Navigation,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            state.isCreatingTrip -> stringResource(id = R.string.map_creating_trip)
                            state.isGuarding -> stringResource(id = R.string.map_stop_guarding)
                            else -> stringResource(id = R.string.map_start_guarding)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onResetSelection,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeWalkPalette.NightPanel)
                ) {
                    Text("重选", color = SafeWalkPalette.DarkText)
                }
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeWalkPalette.Danger)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("紧急 SOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MapPlaceholder() {
    NightMapBackdrop()
}
