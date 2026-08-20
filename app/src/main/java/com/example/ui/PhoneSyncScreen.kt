package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.ActivityEventType
import com.example.models.SyncActivityEvent
import com.example.models.SyncFilterType
import com.example.sync.SyncStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.BorderSubtleLight
import com.example.ui.theme.CallCyan
import com.example.ui.theme.CallCyanBg
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallGreenBg
import com.example.ui.theme.CanvasBackgroundLight
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.DeletionSlate
import com.example.ui.theme.DeletionSlateBg
import com.example.ui.theme.DeviceBadgePhoneA
import com.example.ui.theme.DeviceBadgePhoneB
import com.example.ui.theme.SmsBlue
import com.example.ui.theme.SmsBlueBg
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.VibrantBlue
import com.example.ui.theme.VibrantBlueHero
import com.example.ui.theme.VibrantBlueHeroSub
import com.example.ui.theme.VibrantBlueHeroText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavTab { FEED, DEVICES, ALERTS, SETUP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSyncScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val deviceConfig by viewModel.deviceConfig.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val selectedDeviceFilter by viewModel.selectedDeviceFilter.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(NavTab.FEED) }
    var showSimulateDialog by remember { mutableStateOf(false) }
    var showGuideSheet by remember { mutableStateOf(false) }
    var showDeviceRenameDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Check permissions
    var hasSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPerm = perms[Manifest.permission.READ_SMS] == true
        hasCallPerm = perms[Manifest.permission.READ_CALL_LOG] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifPerm = perms[Manifest.permission.POST_NOTIFICATIONS] == true
        }
        if (hasSmsPerm || hasCallPerm) {
            viewModel.triggerDiff()
        }
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isBatteryOptimized = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false

    val isSyncing = syncStatus is SyncStatus.Syncing
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_spin"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackgroundLight,
        bottomBar = {
            VibrantBottomNavBar(
                activeTab = activeTab,
                onTabSelect = { tab ->
                    activeTab = tab
                    when (tab) {
                        NavTab.SETUP -> showGuideSheet = true
                        NavTab.DEVICES -> showDeviceRenameDialog = true
                        NavTab.ALERTS -> showSimulateDialog = true
                        else -> {}
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Vibrant Top Header
            item {
                VibrantHeader(
                    isServiceRunning = isServiceRunning,
                    onAvatarClick = { showOptionsMenu = true },
                    onSimulateClick = { showSimulateDialog = true }
                )
            }

            // 2. Vibrant Hero Card (Periwinkle Blue Container)
            item {
                VibrantHeroSyncCard(
                    deviceName = deviceConfig.deviceName,
                    pairedDeviceName = availableDevices.firstOrNull { it.first != deviceConfig.deviceId }?.second ?: "Galaxy S23",
                    deviceCount = if (availableDevices.size > 1) availableDevices.size else 2,
                    isSyncing = isSyncing,
                    syncAngle = spinAngle,
                    syncStatus = syncStatus,
                    onRefresh = { viewModel.triggerDiff() }
                )
            }

            // 3. Permission Notice if missing
            if (!hasSmsPerm || !hasCallPerm) {
                item {
                    PermissionRequestCard(
                        hasSms = hasSmsPerm,
                        hasCall = hasCallPerm,
                        onRequest = {
                            val perms = mutableListOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.READ_CONTACTS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    )
                }
            }

            // 4. Battery Optimization Notice
            if (isBatteryOptimized) {
                item {
                    BatteryOptimizationCard(
                        onOpenSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            // 5. Activity Log Section Header & Filter Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVITY LOG",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        ),
                        color = TextMutedLight
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { showSimulateDialog = true }
                    ) {
                        Text(
                            text = "+ Simulate",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = VibrantBlue
                        )
                    }
                }
            }

            // 6. Category Filter Chips (All, SMS, Calls, Deletions)
            item {
                VibrantCategoryFilterBar(
                    selectedType = selectedTypeFilter,
                    counts = counts,
                    onSelect = { viewModel.setTypeFilter(it) }
                )
            }

            // 7. Device Filter Row (if multiple)
            if (availableDevices.size > 1) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDeviceFilter == null,
                                onClick = { viewModel.setDeviceFilter(null) },
                                label = { Text("All Phones (${counts[SyncFilterType.ALL] ?: 0})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VibrantBlueHero,
                                    selectedLabelColor = VibrantBlueHeroSub
                                )
                            )
                        }
                        items(availableDevices) { (id, name) ->
                            val isSelected = selectedDeviceFilter == id
                            val isThisDevice = id == deviceConfig.deviceId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDeviceFilter(id) },
                                label = { Text(if (isThisDevice) "📱 $name" else "📲 $name") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isThisDevice) VibrantBlueHero else Color(0xFFEDE9FE),
                                    selectedLabelColor = if (isThisDevice) VibrantBlueHeroSub else Color(0xFF5B21B6)
                                )
                            )
                        }
                    }
                }
            }

            // 8. Search Input Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search contact, number, or SMS...",
                            color = TextMutedLight,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMutedLight,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedLight)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurfaceLight,
                        unfocusedContainerColor = CardSurfaceLight,
                        focusedBorderColor = VibrantBlue,
                        unfocusedBorderColor = BorderSubtleLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_events_input")
                )
            }

            // 9. Activity Feed Items
            if (events.isEmpty()) {
                item {
                    EmptyEventsView(
                        hasPermissions = hasSmsPerm || hasCallPerm,
                        onSimulateClick = { showSimulateDialog = true },
                        onRequestPermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.READ_CONTACTS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    )
                }
            } else {
                items(
                    items = events,
                    key = { it.id }
                ) { event ->
                    VibrantEventCard(
                        event = event,
                        isCurrentDevice = event.deviceId == deviceConfig.deviceId,
                        onDeleteClick = { viewModel.markEventDeleted(event) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dropdown / Options Menu
    if (showOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            title = { Text("Feed & Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Device ID: ${deviceConfig.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                    TextButton(
                        onClick = {
                            showOptionsMenu = false
                            showDeviceRenameDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rename Device")
                    }
                    TextButton(
                        onClick = {
                            showOptionsMenu = false
                            showClearConfirmDialog = true
                        }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Local Feed", color = AlertRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsMenu = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialogs & Sheets
    if (showSimulateDialog) {
        SimulateEventDialog(
            currentDeviceName = deviceConfig.deviceName,
            onDismiss = { showSimulateDialog = false },
            onInject = { isSecondary, type, address, name, body, duration ->
                viewModel.injectSimulatedEvent(
                    isSecondaryPhone = isSecondary,
                    type = type,
                    address = address,
                    contactName = name,
                    bodySnippet = body,
                    callDurationSeconds = duration
                )
                showSimulateDialog = false
            }
        )
    }

    if (showGuideSheet) {
        FirebaseGuideBottomSheet(
            isFirebaseActive = isFirebaseConnected,
            onDismiss = { showGuideSheet = false }
        )
    }

    if (showDeviceRenameDialog) {
        DeviceRenameDialog(
            initialName = deviceConfig.deviceName,
            onDismiss = { showDeviceRenameDialog = false },
            onSave = {
                viewModel.updateDeviceName(it)
                showDeviceRenameDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Local Feed?") },
            text = { Text("This will clear the cached activity list on this phone. Active sync will continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllEvents()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = AlertRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VibrantHeader(
    isServiceRunning: Boolean,
    onAvatarClick: () -> Unit,
    onSimulateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Phone Sync",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Pill: Cloud Relay Active
            Surface(
                color = SmsBlueBg,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) Color(0xFF22C55E) else Color(0xFFF59E0B))
                    )
                    Text(
                        text = if (isServiceRunning) "Cloud Relay Active" else "Relay Paused",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SmsBlue
                    )
                }
            }
        }

        // Avatar icon card
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onAvatarClick() },
            color = CardSurfaceLight,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderSubtleLight),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "👤", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun VibrantHeroSyncCard(
    deviceName: String,
    pairedDeviceName: String,
    deviceCount: Int,
    isSyncing: Boolean,
    syncAngle: Float,
    syncStatus: SyncStatus,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantBlueHero),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_sync_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Background decorative circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(VibrantBlue.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "Syncing $deviceCount Devices",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = VibrantBlueHeroSub
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Device Names
                Text(
                    text = "$deviceName\n& $pairedDeviceName",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    ),
                    color = VibrantBlueHeroText
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom row with timestamp & Refresh button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (syncStatus) {
                        is SyncStatus.Syncing -> "Syncing snapshots..."
                        is SyncStatus.Success -> "Live snapshot updated"
                        is SyncStatus.Error -> "Offline / Retrying"
                        else -> "Last snapshot: 2m ago"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = VibrantBlueHeroSub.copy(alpha = 0.85f)
                    )

                    // Crisp White Refresh Button
                    Surface(
                        color = CardSurfaceLight,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onRefresh() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = VibrantBlue,
                                modifier = if (isSyncing) Modifier
                                    .size(14.dp)
                                    .rotate(syncAngle) else Modifier.size(14.dp)
                            )
                            Text(
                                text = "Refresh",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = VibrantBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VibrantCategoryFilterBar(
    selectedType: SyncFilterType,
    counts: Map<SyncFilterType, Int>,
    onSelect: (SyncFilterType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(SyncFilterType.entries.toTypedArray()) { type ->
            val isSelected = selectedType == type
            val count = counts[type] ?: 0
            val label = when (type) {
                SyncFilterType.ALL -> "All ($count)"
                SyncFilterType.SMS_ONLY -> "💬 SMS ($count)"
                SyncFilterType.CALLS_ONLY -> "📞 Calls ($count)"
                SyncFilterType.DELETIONS_ONLY -> "🗑️ Deletions ($count)"
            }

            Surface(
                color = if (isSelected) VibrantBlue else CardSurfaceLight,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isSelected) VibrantBlue else BorderSubtleLight),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(type) }
                    .testTag("filter_chip_${type.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else TextSecondaryLight,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun VibrantEventCard(
    event: SyncActivityEvent,
    isCurrentDevice: Boolean,
    onDeleteClick: () -> Unit
) {
    val typedType = event.getTypedEventType()
    val isDeletion = typedType.isDeletion || event.isDeleted
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(event.timestamp) { timeFormatter.format(Date(event.timestamp)) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurfaceLight
        ),
        border = BorderStroke(1.dp, BorderSubtleLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded 48x48 icon square
            VibrantIconSquare(type = typedType, isDeletion = isDeletion)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header line: Contact/Type + Device Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = when {
                        isDeletion -> "Log Deleted: ${event.contactName ?: event.address}"
                        typedType == ActivityEventType.CALL_MISSED -> "Missed Call: ${event.contactName ?: event.address}"
                        typedType == ActivityEventType.SMS_INCOMING -> "SMS: ${event.contactName ?: event.address}"
                        typedType == ActivityEventType.SMS_OUTGOING -> "Sent SMS: ${event.contactName ?: event.address}"
                        typedType == ActivityEventType.CALL_INCOMING -> "Call: ${event.contactName ?: event.address}"
                        else -> "Call: ${event.contactName ?: event.address}"
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Micro device pill tag: e.g. [PIXEL 7]
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = event.deviceName.uppercase().take(8),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle / message snippet / duration
                val subtitleText = when {
                    isDeletion -> "SMS/Call log removed on device"
                    typedType == ActivityEventType.CALL_MISSED -> "No voicemail left"
                    !event.bodySnippet.isNullOrBlank() -> "\"${event.bodySnippet}\""
                    event.callDurationSeconds > 0 -> "Duration: ${formatDuration(event.callDurationSeconds)}"
                    else -> event.address
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (typedType == ActivityEventType.CALL_MISSED) AlertRed else TextSecondaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Timestamp on far right
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMutedLight
            )
        }
    }
}

@Composable
fun VibrantIconSquare(type: ActivityEventType, isDeletion: Boolean) {
    val (iconEmoji, bgTint, iconTint) = when {
        isDeletion -> Triple("🗑️", DeletionSlateBg, DeletionSlate)
        type == ActivityEventType.CALL_MISSED -> Triple("📞", AlertRedBg, AlertRed)
        type == ActivityEventType.CALL_INCOMING -> Triple("📞", CallGreenBg, CallGreen)
        type == ActivityEventType.CALL_OUTGOING -> Triple("↗️", CallCyanBg, CallCyan)
        type == ActivityEventType.SMS_OUTGOING -> Triple("↗️", SmsBlueBg, SmsBlue)
        else -> Triple("💬", SmsBlueBg, SmsBlue)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgTint),
        contentAlignment = Alignment.Center
    ) {
        Text(text = iconEmoji, fontSize = 20.sp)
    }
}

@Composable
fun VibrantBottomNavBar(
    activeTab: NavTab,
    onTabSelect: (NavTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurfaceLight,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, BorderSubtleLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibrantNavItem(
                icon = "🏠",
                label = "FEED",
                isActive = activeTab == NavTab.FEED,
                onClick = { onTabSelect(NavTab.FEED) }
            )
            VibrantNavItem(
                icon = "📱",
                label = "DEVICES",
                isActive = activeTab == NavTab.DEVICES,
                onClick = { onTabSelect(NavTab.DEVICES) }
            )
            VibrantNavItem(
                icon = "🔔",
                label = "ALERTS",
                isActive = activeTab == NavTab.ALERTS,
                onClick = { onTabSelect(NavTab.ALERTS) }
            )
            VibrantNavItem(
                icon = "⚙️",
                label = "SETUP",
                isActive = activeTab == NavTab.SETUP,
                onClick = { onTabSelect(NavTab.SETUP) }
            )
        }
    }
}

@Composable
fun VibrantNavItem(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 18.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            ),
            color = if (isActive) VibrantBlue else TextMutedLight
        )
    }
}

@Composable
fun PermissionRequestCard(
    hasSms: Boolean,
    hasCall: Boolean,
    onRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AlertRedBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMS & Call Log Permissions Needed",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AlertRed
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phone Activity Sync requires READ_SMS and READ_CALL_LOG to log calls, messages, and deletion events.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_permissions_button")
            ) {
                Text("Grant Permissions", color = Color.White)
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard(
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderSubtleLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Battery Optimization Active",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryLight
                )
                Text(
                    text = "Set battery to 'Unrestricted' for continuous background syncing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Exempt", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val mins = seconds / 60
    val remSecs = seconds % 60
    return if (mins > 0) "${mins}m ${remSecs}s" else "${remSecs}s"
}

@Composable
fun EmptyEventsView(
    hasPermissions: Boolean,
    onSimulateClick: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),
        border = BorderStroke(1.dp, BorderSubtleLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(VibrantBlueHero),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📲", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Activity Events Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When SMS or call activity occurs on this phone or paired devices, it will appear here in real time.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!hasPermissions) {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue)
                    ) {
                        Text("Grant Permissions")
                    }
                }
                OutlinedButton(
                    onClick = onSimulateClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("simulate_sample_event_button")
                ) {
                    Text("+ Simulate Event", color = VibrantBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulateEventDialog(
    currentDeviceName: String,
    onDismiss: () -> Unit,
    onInject: (isSecondary: Boolean, type: ActivityEventType, address: String, name: String?, body: String?, duration: Long) -> Unit
) {
    var selectedDeviceIndex by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf(ActivityEventType.SMS_INCOMING) }
    var contactName by remember { mutableStateOf("Jordan") }
    var phoneNumber by remember { mutableStateOf("+1 (555) 0124") }
    var messageBody by remember { mutableStateOf("Hey, are we still on for lunch?") }
    var callDuration by remember { mutableStateOf("95") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Phone Activity", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select originating phone and event type to inject into feed:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDeviceIndex == 0,
                        onClick = { selectedDeviceIndex = 0 },
                        label = { Text("📱 Pixel 7 (This)") }
                    )
                    FilterChip(
                        selected = selectedDeviceIndex == 1,
                        onClick = { selectedDeviceIndex = 1 },
                        label = { Text("📲 Galaxy S23") }
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        ActivityEventType.SMS_INCOMING,
                        ActivityEventType.CALL_MISSED,
                        ActivityEventType.SMS_DELETED,
                        ActivityEventType.CALL_INCOMING,
                        ActivityEventType.SMS_OUTGOING
                    )
                    items(types) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.title) }
                        )
                    }
                }

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType.isSms || selectedType == ActivityEventType.SMS_DELETED) {
                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("Message Text") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = callDuration,
                        onValueChange = { callDuration = it },
                        label = { Text("Call Duration (seconds)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = callDuration.toLongOrNull() ?: 0L
                    onInject(
                        selectedDeviceIndex == 1,
                        selectedType,
                        phoneNumber,
                        contactName,
                        messageBody,
                        dur
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_simulate_button")
            ) {
                Text("Inject Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeviceRenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Device", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Specify a label for this phone (e.g. Pixel 7 Pro):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseGuideBottomSheet(
    isFirebaseActive: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Setup & Dual-Phone Pairing",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(10.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("1. Firebase") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("2. Battery") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("3. Deletion Diff") })
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Cross-Device Setup Steps:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "1. Add app to console.firebase.google.com\n" +
                                    "2. Download google-services.json to app/\n" +
                                    "3. Install identical APK on both phones!\n" +
                                    "4. Both phones sync via 'activity_events' collection.",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryLight
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Firestore Security Rules:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val rulesSnippet = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /activity_events/{document=**} {
      allow read, write: if true;
    }
  }
}"""
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtleLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = rulesSnippet,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { clipboardManager.setText(AnnotatedString(rulesSnippet)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Copy Rules", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Battery Optimization:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "• Set app battery usage to 'Unrestricted' in Android settings.\n• Ensures ContentObservers are triggered when the screen is off.\n• Built-in 15-minute background loop acts as fallback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Deletion Detection:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "• Compares current table with snapshot table.\n• When an ID disappears, generates a DELETED log entry.\n• Instantly relays the deletion to both devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
