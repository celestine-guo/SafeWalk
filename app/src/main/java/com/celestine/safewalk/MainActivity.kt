package com.celestine.safewalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeWalkApp {
                AuthGate {
                    SafeWalkScreen(
                        onWalkingModeSelected = {
                            startActivity(MapActivity.createIntent(this, Mode.WALKING))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SafeWalkApp(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF3B82F6),
            secondary = Color(0xFF7C3AED),
            tertiary = Color(0xFF10B981),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            error = Color(0xFFEF4444)
        )
    ) {
        content()
    }
}

@Composable
fun SafeWalkScreen(
    onWalkingModeSelected: () -> Unit = {},
) {
    var selectedMode by remember { mutableStateOf(Mode.WALKING) }
    var isSosActivated by remember { mutableStateOf(false) }
    var modeMessage by remember { mutableStateOf<String?>(null) }
    val taxiPlaceholder = stringResource(id = R.string.mode_taxi_placeholder)

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 顶部安全状态指示器
            item {
                StatusIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 应用标题
            item {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 安全提示横幅
            item {
                SafetyTipCard()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 模式选择区
            item {
                ModeSelection(
                    selectedMode = selectedMode,
                    onModeSelected = {
                        selectedMode = it
                        if (it == Mode.WALKING) {
                            modeMessage = null
                            onWalkingModeSelected()
                        } else {
                            modeMessage = taxiPlaceholder
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            modeMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mode_placeholder_message"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 最近联系人
            item {
                RecentContacts()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 底部空间
            item {
                Spacer(modifier = Modifier.height(104.dp))
            }
        }

        // 设置按钮
        FloatingActionButton(
            onClick = { /* 打开设置 */ },
            modifier = Modifier
                .testTag("settings_fab")
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset(y = (-88).dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(id = R.string.settings))
        }

        // 底部悬浮SOS按钮
        SosButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            isActivated = isSosActivated,
            onActivate = { isSosActivated = true },
            onDeactivate = { isSosActivated = false }
        )
    }
}

@Composable
fun StatusIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 网络连接状态
        StatusItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Wifi,
            text = stringResource(id = R.string.status_connected)
        )

        // 定位服务状态
        StatusItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.LocationOn,
            text = stringResource(id = R.string.status_location_on)
        )

        // 守护状态
        StatusItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Security,
            text = stringResource(id = R.string.status_not_guarding)
        )
    }
}

@Composable
fun StatusItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SafetyTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "安全提示",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.safety_tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = { /* 打开设置 */ }) {
                Text(
                    text = stringResource(id = R.string.safety_tip_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class Mode {
    WALKING,
    TAXI
}

@Composable
fun ModeSelection(selectedMode: Mode, onModeSelected: (Mode) -> Unit) {
    Column {
        // 走夜路模式
        ModeCard(
            title = stringResource(id = R.string.mode_walking),
            description = stringResource(id = R.string.mode_walking_desc),
            icon = Icons.Filled.DirectionsWalk,
            isSelected = selectedMode == Mode.WALKING,
            onSelect = { onModeSelected(Mode.WALKING) }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 打车模式
        ModeCard(
            title = stringResource(id = R.string.mode_taxi),
            description = stringResource(id = R.string.mode_taxi_desc),
            icon = Icons.Filled.DirectionsCar,
            isSelected = selectedMode == Mode.TAXI,
            onSelect = { onModeSelected(Mode.TAXI) }
        )
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("mode_card_${title}")
            .semantics { selected = isSelected }
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun RecentContacts() {
    Column {
        Text(
            text = stringResource(id = R.string.recent_contacts),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            ContactItem(
                modifier = Modifier.weight(1f),
                name = "妈妈",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            ContactItem(
                modifier = Modifier.weight(1f),
                name = "爸爸",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ContactItem(modifier: Modifier = Modifier, name: String, color: Color) {
    Card(
        modifier = modifier
            .height(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SosButton(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = { if (!isActivated) onActivate() else onDeactivate() },
            modifier = Modifier
                .testTag("sos_button")
                .semantics {
                    stateDescription = if (isActivated) "activated" else "idle"
                }
                .size(72.dp)
                .align(Alignment.Center),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ),
            shape = androidx.compose.foundation.shape.CircleShape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.sos_button_text),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SafeWalkScreenPreview() {
    SafeWalkApp {
        SafeWalkScreen()
    }
}
