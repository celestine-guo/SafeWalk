package com.celestine.safewalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SafeWalkPalette {
    val Night = Color(0xFF080B12)
    val NightSurface = Color(0xFF121826)
    val NightSurfaceVariant = Color(0xFF171D2B)
    val NightPanel = Color(0xFF1B2434)
    val Lavender = Color(0xFF7C8CFF)
    val LavenderSoft = Color(0xFFA5B1FF)
    val Violet = Color(0xFF8057F5)
    val Sky = Color(0xFF89C4FF)
    val Mint = Color(0xFF62DDBB)
    val Danger = Color(0xFFE74E5D)
    val DangerDeep = Color(0xFFB62C4D)
    val DarkText = Color(0xFFF7F8FB)
    val DarkTextMuted = Color(0xFFB4BED0)
    val LightBackground = Color(0xFFF3F5FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceSoft = Color(0xFFF9FAFF)
    val LightText = Color(0xFF101626)
    val LightTextMuted = Color(0xFF62708A)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var useDarkTheme by rememberSaveable { mutableStateOf(true) }

            SafeWalkApp(darkTheme = useDarkTheme) {
                AuthGate {
                    SafeWalkScreen(
                        isDarkTheme = useDarkTheme,
                        onThemeToggle = { useDarkTheme = !useDarkTheme },
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
fun SafeWalkApp(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = SafeWalkPalette.Lavender,
            onPrimary = Color.White,
            secondary = SafeWalkPalette.Sky,
            tertiary = SafeWalkPalette.Mint,
            background = SafeWalkPalette.Night,
            onBackground = SafeWalkPalette.DarkText,
            surface = SafeWalkPalette.NightSurface,
            onSurface = SafeWalkPalette.DarkText,
            surfaceVariant = SafeWalkPalette.NightPanel,
            onSurfaceVariant = SafeWalkPalette.DarkTextMuted,
            error = SafeWalkPalette.Danger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = SafeWalkPalette.Lavender,
            onPrimary = Color.White,
            secondary = SafeWalkPalette.Sky,
            tertiary = SafeWalkPalette.Mint,
            background = SafeWalkPalette.LightBackground,
            onBackground = SafeWalkPalette.LightText,
            surface = SafeWalkPalette.LightSurface,
            onSurface = SafeWalkPalette.LightText,
            surfaceVariant = SafeWalkPalette.LightSurfaceSoft,
            onSurfaceVariant = SafeWalkPalette.LightTextMuted,
            error = SafeWalkPalette.Danger,
            onError = Color.White
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

enum class Mode {
    WALKING,
    TAXI
}

private enum class MainDestination(
    val label: String,
    val icon: ImageVector,
) {
    HOME("首页", Icons.Filled.Home),
    GUARD("守护", Icons.Filled.Map),
    CHAT("AI", Icons.AutoMirrored.Filled.Chat),
}

private enum class EmergencySurface {
    SOS,
    FAKE_CALL,
    ALARM,
    SETTINGS,
}

@Composable
fun SafeWalkScreen(
    onWalkingModeSelected: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
) {
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    var selectedMode by rememberSaveable { mutableStateOf(Mode.WALKING) }
    var isSosActivated by rememberSaveable { mutableStateOf(false) }
    var modeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var emergencySurface by rememberSaveable { mutableStateOf<EmergencySurface?>(null) }
    val taxiPlaceholder = stringResource(id = R.string.mode_taxi_placeholder)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightDashboardBackground(isDarkTheme)
    ) {
        when (selectedDestination) {
            MainDestination.HOME -> HomeDashboard(
                isDarkTheme = isDarkTheme,
                modeMessage = modeMessage,
                selectedMode = selectedMode,
                onThemeToggle = onThemeToggle,
                onSettings = { emergencySurface = EmergencySurface.SETTINGS },
                onModeSelected = { mode ->
                    selectedMode = mode
                    if (mode == Mode.WALKING) {
                        modeMessage = null
                        onWalkingModeSelected()
                    } else {
                        modeMessage = taxiPlaceholder
                    }
                },
                onFakeCall = { emergencySurface = EmergencySurface.FAKE_CALL },
                onAlarm = { emergencySurface = EmergencySurface.ALARM }
            )

            MainDestination.GUARD -> GuardDashboard(
                onThemeToggle = onThemeToggle,
                onSettings = { emergencySurface = EmergencySurface.SETTINGS }
            )

            MainDestination.CHAT -> CompanionChat(
                onThemeToggle = onThemeToggle,
                onSettings = { emergencySurface = EmergencySurface.SETTINGS }
            )
        }

        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("bottom_navigation"),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 10.dp
        ) {
            MainDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = selectedDestination == destination,
                    onClick = { selectedDestination = destination },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                )
            }
        }

        SosButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            isActivated = isSosActivated,
            onActivate = {
                isSosActivated = true
                emergencySurface = EmergencySurface.SOS
            },
            onDeactivate = {
                isSosActivated = false
                emergencySurface = null
            }
        )

        when (emergencySurface) {
            EmergencySurface.SOS -> SosEmergencyScreen(
                onCancel = {
                    isSosActivated = false
                    emergencySurface = null
                }
            )

            EmergencySurface.FAKE_CALL -> FakeCallScreen(
                onClose = { emergencySurface = null }
            )

            EmergencySurface.ALARM -> AlarmScreen(
                onClose = { emergencySurface = null }
            )

            EmergencySurface.SETTINGS -> SettingsScreen(
                onClose = { emergencySurface = null }
            )

            null -> Unit
        }
    }
}

@Composable
private fun HomeDashboard(
    isDarkTheme: Boolean,
    modeMessage: String?,
    selectedMode: Mode,
    onThemeToggle: () -> Unit,
    onSettings: () -> Unit,
    onModeSelected: (Mode) -> Unit,
    onFakeCall: () -> Unit,
    onAlarm: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 148.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            DashboardHeader(
                title = "夜间安全守护",
                subtitle = "今晚 21:42 · 位置共享待命",
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onSettings = onSettings
            )
        }

        item {
            SystemStatusCard()
        }

        item {
            ModeSelection(
                selectedMode = selectedMode,
                onModeSelected = onModeSelected
            )
        }

        modeMessage?.let { message ->
            item {
                AssistMessageCard(message)
            }
        }

        item {
            QuickToolsGrid(
                onFakeCall = onFakeCall,
                onAlarm = onAlarm
            )
        }

        item {
            RecentContacts()
        }
    }
}

@Composable
private fun DashboardHeader(
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 0.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )
        }

        IconButton(
            onClick = onThemeToggle,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("theme_toggle")
        ) {
            Icon(
                imageVector = Icons.Filled.Brightness6,
                contentDescription = if (isDarkTheme) "切换浅色主题" else "切换深色主题",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(id = R.string.settings),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatusIndicator() {
    SystemStatusCard()
}

@Composable
private fun SystemStatusCard() {
    DashboardCard(shape = RoundedCornerShape(26.dp)) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(
                icon = Icons.Filled.Security,
                tint = SafeWalkPalette.Mint,
                modifier = Modifier.size(54.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = "系统运行中", color = SafeWalkPalette.Mint)
                    StatusChip(text = "定位在线", color = SafeWalkPalette.Sky)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前状态",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "AI 陪伴、位置共享和紧急联系人均已待命",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary)
        Text(text = text, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SafetyTipCard() {
    DashboardCard(shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = Icons.Filled.CheckCircle, tint = SafeWalkPalette.Sky)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.safety_tip),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = R.string.safety_tip_action),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ModeSelection(selectedMode: Mode, onModeSelected: (Mode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeCard(
            title = "开始夜路守护",
            description = stringResource(id = R.string.mode_walking_desc),
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            isSelected = selectedMode == Mode.WALKING,
            primary = true,
            onSelect = { onModeSelected(Mode.WALKING) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                modifier = Modifier.weight(1f),
                title = "安心打车",
                description = stringResource(id = R.string.mode_taxi_desc),
                icon = Icons.Filled.DirectionsCar,
                isSelected = selectedMode == Mode.TAXI,
                primary = false,
                onSelect = { onModeSelected(Mode.TAXI) }
            )
            SecondaryActionCard(
                modifier = Modifier.weight(1f),
                title = "AI 陪伴",
                description = "持续对话",
                icon = Icons.AutoMirrored.Filled.Chat,
                tint = SafeWalkPalette.Mint
            )
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val cardModifier = modifier
        .height(if (primary) 138.dp else 132.dp)
        .testTag("mode_card_${title}")
        .semantics { selected = isSelected }
        .clickable { onSelect() }

    if (primary) {
        Card(
            modifier = cardModifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = if (isSelected) BorderStroke(1.dp, SafeWalkPalette.LavenderSoft) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SafeWalkPalette.Violet,
                                SafeWalkPalette.Lavender,
                                SafeWalkPalette.Sky
                            )
                        )
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusChip(text = "推荐", color = Color.White)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconBadge(icon = icon, tint = Color.White, backgroundAlpha = 0.18f, modifier = Modifier.size(58.dp))
            }
        }
    } else {
        DashboardCard(
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color,
) {
    DashboardCard(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconBadge(icon = icon, tint = tint)
            Column {
                Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AssistMessageCard(message: String) {
    DashboardCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mode_placeholder_message"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = Icons.Filled.DirectionsCar, tint = SafeWalkPalette.Sky)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickToolsGrid(
    onFakeCall: () -> Unit,
    onAlarm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "快捷协助",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "位置共享",
                subtitle = "3 位联系人",
                icon = Icons.Filled.Share,
                tint = SafeWalkPalette.Sky
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "安全计时",
                subtitle = "15 分钟",
                icon = Icons.Filled.Timer,
                tint = SafeWalkPalette.Mint
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "模拟来电",
                subtitle = "一键脱身",
                icon = Icons.Filled.Call,
                tint = SafeWalkPalette.Mint,
                onClick = onFakeCall
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "警报",
                subtitle = "高亮声光",
                icon = Icons.Filled.Notifications,
                tint = SafeWalkPalette.Danger,
                urgent = true,
                onClick = onAlarm
            )
        }
    }
}

@Composable
private fun QuickToolCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    urgent: Boolean = false,
    onClick: () -> Unit = {},
) {
    DashboardCard(
        modifier = modifier
            .height(132.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        containerColor = if (urgent) SafeWalkPalette.Danger.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconBadge(icon = icon, tint = tint)
            Column {
                Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RecentContacts() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.recent_contacts),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        DashboardCard(shape = RoundedCornerShape(26.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                ContactItem(name = "妈妈", color = SafeWalkPalette.Lavender, note = "已开启位置接收")
                ContactItem(name = "爸爸", color = SafeWalkPalette.Sky, note = "紧急电话优先")
                ContactItem(name = "室友", color = SafeWalkPalette.Mint, note = "到家提醒")
            }
        }
    }
}

@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    name: String,
    color: Color,
    note: String = "紧急联系人",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name.take(1), color = color, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(text = note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuardDashboard(
    onThemeToggle: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NightMapBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 236.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DashboardHeader(
                    title = "实时守护",
                    subtitle = "地图同步中 · 预计 18 分钟到达",
                    isDarkTheme = true,
                    onThemeToggle = onThemeToggle,
                    onSettings = onSettings
                )
            }
            item {
                DashboardCard(
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconBadge(icon = Icons.Filled.GpsFixed, tint = SafeWalkPalette.Mint, modifier = Modifier.size(52.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("位置共享状态", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("3 位联系人正在接收实时轨迹", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        StatusChip(text = "守护中", color = SafeWalkPalette.Mint)
                    }
                }
            }
        }

        GuardActionPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            onStop = {}
        )
    }
}

@Composable
private fun GuardActionPanel(
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("当前目标", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("家 · 南门入口", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                StatusChip(text = "ETA 18 分钟", color = SafeWalkPalette.Sky)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("结束守护", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("重选", color = MaterialTheme.colorScheme.onSurface)
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
private fun CompanionChat(
    onThemeToggle: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 148.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardHeader(
                title = "AI 安全陪伴",
                subtitle = "安静在线 · 可持续对话",
                isDarkTheme = true,
                onThemeToggle = onThemeToggle,
                onSettings = onSettings
            )
        }
        item {
            AssistantIdentityBar()
        }
        item {
            ChatBubble(
                text = "我会陪你走完这段路。当前位置已记录，若你停止回应，我会提醒联系人。",
                fromUser = false
            )
        }
        item {
            ChatBubble(
                text = "我还有 15 分钟到家。",
                fromUser = true
            )
        }
        item {
            ChatBubble(
                text = "收到。我会在 10 分钟后确认一次，到家后帮你发送安全抵达提醒。",
                fromUser = false
            )
        }
    }
}

@Composable
private fun AssistantIdentityBar() {
    DashboardCard(shape = RoundedCornerShape(26.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = Icons.Filled.Shield, tint = SafeWalkPalette.Mint)
            Column(modifier = Modifier.weight(1f)) {
                Text("SafeWalk AI", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("位置异常、长时间沉默和紧急口令监测中", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(text = "在线", color = SafeWalkPalette.Mint)
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 292.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (fromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        SafeWalkPalette.Mint.copy(alpha = 0.14f)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = if (fromUser) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsScreen(onClose: () -> Unit) {
    FullScreenOverlay(
        background = Brush.verticalGradient(
            listOf(SafeWalkPalette.Night, SafeWalkPalette.NightSurface, SafeWalkPalette.NightPanel)
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverlayTopBar(title = "设置", onClose = onClose)
            }
            item {
                DashboardCard(shape = RoundedCornerShape(30.dp), containerColor = SafeWalkPalette.NightSurface) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconBadge(icon = Icons.Filled.Person, tint = SafeWalkPalette.Lavender, modifier = Modifier.size(60.dp))
                        Column {
                            Text("Celestine", color = SafeWalkPalette.DarkText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("安全身份已同步", color = SafeWalkPalette.DarkTextMuted)
                        }
                    }
                }
            }
            item {
                DashboardCard(shape = RoundedCornerShape(26.dp), containerColor = SafeWalkPalette.NightSurface) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsRow(Icons.Filled.Person, stringResource(id = R.string.profile), "昵称、头像与安全口令")
                        SettingsRow(Icons.Filled.Favorite, stringResource(id = R.string.emergency_contacts), "联系人、优先级与通知方式")
                        SettingsRow(Icons.Filled.Notifications, stringResource(id = R.string.notification_settings), "守护提醒、警报音与震动")
                        SettingsRow(Icons.Filled.Security, stringResource(id = R.string.about), "版本与隐私说明")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBadge(icon = icon, tint = SafeWalkPalette.Sky)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SafeWalkPalette.DarkText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SafeWalkPalette.DarkTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SafeWalkPalette.DarkTextMuted)
    }
}

@Composable
private fun SosEmergencyScreen(onCancel: () -> Unit) {
    FullScreenOverlay(
        background = Brush.verticalGradient(
            listOf(SafeWalkPalette.Danger, SafeWalkPalette.DangerDeep, Color(0xFF5B0717))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SOS 已启动", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("05", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("秒后呼叫", color = Color.White.copy(alpha = 0.82f))
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("取消", color = SafeWalkPalette.DangerDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FakeCallScreen(onClose: () -> Unit) {
    FullScreenOverlay(
        background = Brush.verticalGradient(
            listOf(Color(0xFF252B35), Color(0xFF0B0F16), Color(0xFF05070B))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OverlayTopBar(title = "模拟来电", onClose = onClose)
            Spacer(Modifier.height(82.dp))
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(SafeWalkPalette.Lavender.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("妈妈", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("正在呼入…", color = SafeWalkPalette.DarkTextMuted)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CallAction(icon = Icons.Filled.Close, label = "拒绝", color = SafeWalkPalette.Danger, onClick = onClose)
                CallAction(icon = Icons.Filled.Call, label = "接听", color = SafeWalkPalette.Mint, onClick = onClose)
            }
        }
    }
}

@Composable
private fun AlarmScreen(onClose: () -> Unit) {
    FullScreenOverlay(
        background = Brush.verticalGradient(
            listOf(Color.White, SafeWalkPalette.Danger, Color.White, SafeWalkPalette.DangerDeep)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("警报", color = SafeWalkPalette.DangerDeep, fontSize = 52.sp, fontWeight = FontWeight.Black)
            Text("ALARM", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("正在播放高可见警报", color = SafeWalkPalette.DangerDeep, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeWalkPalette.Night)
            ) {
                Text("解除", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OverlayTopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
        }
    }
}

@Composable
private fun CallAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White)
    }
}

@Composable
fun SosButton(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Button(
        onClick = { if (isActivated) onDeactivate() else onActivate() },
        modifier = modifier
            .padding(end = 20.dp, bottom = 94.dp)
            .testTag("sos_button")
            .semantics {
                stateDescription = if (isActivated) "activated" else "idle"
            }
            .height(58.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SafeWalkPalette.Danger,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.sos_button_text),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = border
    ) {
        content()
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier.size(44.dp),
    backgroundAlpha: Float = 0.14f,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun Modifier.nightDashboardBackground(isDarkTheme: Boolean): Modifier {
    val colors = if (isDarkTheme) {
        listOf(
            SafeWalkPalette.Night,
            SafeWalkPalette.NightSurface,
            SafeWalkPalette.NightPanel.copy(alpha = 0.96f)
        )
    } else {
        listOf(
            SafeWalkPalette.LightBackground,
            SafeWalkPalette.LightSurfaceSoft,
            SafeWalkPalette.LightBackground
        )
    }
    return background(Brush.verticalGradient(colors))
}

@Composable
private fun FullScreenOverlay(
    background: Brush,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        content()
    }
}

@Composable
fun NightMapBackdrop(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SafeWalkPalette.Night,
                        SafeWalkPalette.NightSurfaceVariant,
                        SafeWalkPalette.NightPanel
                    )
                )
            )
    ) {
        val gridColor = SafeWalkPalette.Sky.copy(alpha = 0.08f)
        val routeColor = SafeWalkPalette.Lavender.copy(alpha = 0.9f)
        val step = size.width / 5f
        var x = 0f
        while (x <= size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x + size.width * 0.2f, size.height), strokeWidth = 2f)
            x += step
        }
        var y = size.height * 0.12f
        while (y <= size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y - size.height * 0.1f), strokeWidth = 2f)
            y += step
        }
        drawLine(
            routeColor,
            Offset(size.width * 0.18f, size.height * 0.78f),
            Offset(size.width * 0.46f, size.height * 0.56f),
            strokeWidth = 10f
        )
        drawLine(
            routeColor,
            Offset(size.width * 0.46f, size.height * 0.56f),
            Offset(size.width * 0.72f, size.height * 0.34f),
            strokeWidth = 10f
        )
        drawCircle(SafeWalkPalette.Mint, radius = 15f, center = Offset(size.width * 0.18f, size.height * 0.78f))
        drawCircle(SafeWalkPalette.Danger, radius = 17f, center = Offset(size.width * 0.72f, size.height * 0.34f))
    }
}

@Preview(showBackground = true)
@Composable
fun SafeWalkScreenPreview() {
    SafeWalkApp {
        SafeWalkScreen()
    }
}
