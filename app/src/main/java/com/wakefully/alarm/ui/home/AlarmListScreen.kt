package com.wakefully.alarm.ui.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.R
import com.wakefully.alarm.data.StreakStats
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.formatTimeRemainingToast
import com.wakefully.alarm.service.getNextAlarmTime
import com.wakefully.alarm.theme.LocalOnCycleThemeMode
import com.wakefully.alarm.theme.LocalThemeMode
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.ThemeMode
import com.wakefully.alarm.theme.WakefullySpacing

@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    barcodes: List<RegisteredBarcode> = emptyList(),
    streakStats: StreakStats = StreakStats(),
    onToggleAlarm: (id: Long, enabled: Boolean) -> Unit,
    onDeleteAlarm: (id: Long) -> Unit,
    onEditAlarm: (alarm: Alarm) -> Unit,
    onCreateAlarm: () -> Unit,
    onTriggerTestAlarm: (alarm: Alarm) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val currentTheme = LocalThemeMode.current
    val onCycleTheme = LocalOnCycleThemeMode.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = WakefullySpacing.ScreenHorizontalPadding,
                end = WakefullySpacing.ScreenHorizontalPadding,
                top = topInset + WakefullySpacing.Space12,
                bottom = bottomInset + WakefullySpacing.Space64 + WakefullySpacing.Space40
            ),
            verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space12)
        ) {
            // Calm, Decluttered Header
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = WakefullySpacing.Space4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.verve_logo),
                                contentDescription = "Verve Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "VERVE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Active count chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusChip))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = WakefullySpacing.Space8, vertical = WakefullySpacing.Space4)
                            ) {
                                Text(
                                    text = "${alarms.count { it.enabled }} ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Theme Mode Toggle Button
                            IconButton(
                                onClick = onCycleTheme,
                                modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                            ) {
                                val (icon, label) = when (currentTheme) {
                                    ThemeMode.LIGHT -> Icons.Default.Brightness7 to "Light Mode"
                                    ThemeMode.DARK -> Icons.Default.Brightness4 to "Dark Mode"
                                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness to "System Auto"
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                    Text(
                        text = "Alarms",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Full-Screen System Alarm Permission Banner (Shows only if overlay / full-screen access needs enabling)
            val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true

            val hasFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.canUseFullScreenIntent() ?: true
            } else true

            if (!hasOverlayPermission || !hasFullScreenIntent) {
                item(key = "permission_banner") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    } else {
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val fallback = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(fallback)
                                    } catch (_: Exception) {}
                                }
                            },
                        shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(WakefullySpacing.Space16),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Terracotta.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = Terracotta,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FULL-SCREEN ALARM SETUP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Terracotta
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enable 'Display over other apps' so alarms take over the full screen like an incoming call when your phone is locked or closed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Enable Full-Screen Alarm Access",
                                tint = Terracotta,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Decluttered, Minimalist Discipline Horizon Strip
            item(key = "streak_strip") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WakefullySpacing.Space16, vertical = WakefullySpacing.Space12),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Streak
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${streakStats.currentStreak}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Terracotta
                            )
                        }

                        // Best Record
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${streakStats.longestStreak}d",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "RECORD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Total Awakenings
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${streakStats.totalWakeups}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AWAKENINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Alarms List or Dormant Horizon Empty State
            if (alarms.isEmpty()) {
                item(key = "empty_state_dormant_horizon") {
                    DormantHorizonEmptyState(
                        onCreateAlarm = onCreateAlarm,
                        onSelectPreset = { preset -> onEditAlarm(preset) }
                    )
                }
            } else {
                items(items = alarms, key = { it.id }) { alarm ->
                    val linkedBarcode = barcodes.firstOrNull { it.id == alarm.barcodeId }
                    AlarmCard(
                        alarm = alarm,
                        barcodeName = linkedBarcode?.name,
                        onToggle = { enabled ->
                            onToggleAlarm(alarm.id, enabled)
                            if (enabled) {
                                val nextCal = getNextAlarmTime(alarm.hourOfDay, alarm.minute, alarm.repeatDays)
                                Toast.makeText(context, formatTimeRemainingToast(nextCal), Toast.LENGTH_LONG).show()
                            }
                        },
                        onDelete = { onDeleteAlarm(alarm.id) },
                        onEdit = { onEditAlarm(alarm) },
                        onTestTrigger = { onTriggerTestAlarm(alarm) }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateAlarm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = WakefullySpacing.ScreenHorizontalPadding,
                    bottom = bottomInset + WakefullySpacing.Space20
                )
                .size(56.dp),
            shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alarm",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    barcodeName: String?,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTestTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "card_press"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onEdit() },
        shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
            // Top Row: Badges on left (constrained with weight(1f)) + Toggle Switch on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges Row strictly constrained to weight(1f) to prevent switch overlap
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = WakefullySpacing.Space8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space6)
                ) {
                    MissionBadge(
                        missionType = alarm.missionType,
                        barcodeName = barcodeName,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WakefullySpacing.RadiusChip))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = WakefullySpacing.Space8, vertical = WakefullySpacing.Space4)
                    ) {
                        Text(
                            text = "♪ ${alarm.toneName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dedicated, non-overlapping Switch area
                Box(
                    modifier = Modifier.size(WakefullySpacing.MinTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = Terracotta,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

            // Time & Label Row
            val hourStr = alarm.hourOfDay.toString().padStart(2, '0')
            val minStr = alarm.minute.toString().padStart(2, '0')
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                Text(
                    text = "$hourStr:$minStr",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 46.sp
                    ),
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = WakefullySpacing.Space8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

            // Bottom Row: Repeat Days Chips + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days row (M T W T F S S)
                Row(horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4)) {
                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    dayNames.forEachIndexed { index, day ->
                        val dayNum = index + 1
                        val isSelected = alarm.repeatDays.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected && alarm.enabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected && alarm.enabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Row action buttons: Test Ring + Delete
                Row(horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4)) {
                    // Quick Test Ring button
                    Box(
                        modifier = Modifier
                            .size(WakefullySpacing.MinTouchTarget)
                            .clickable { onTestTrigger() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Ring Alarm",
                            tint = Terracotta,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete button
                    Box(
                        modifier = Modifier
                            .size(WakefullySpacing.MinTouchTarget)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alarm",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionBadge(
    missionType: MissionType,
    barcodeName: String?,
    modifier: Modifier = Modifier
) {
    val (label, icon) = when (missionType) {
        MissionType.BARCODE -> {
            val name = barcodeName ?: "BARCODE"
            "OPTICAL // $name" to Icons.Default.QrCodeScanner
        }
        MissionType.MATH -> "MATH // 3 PROBLEMS" to Icons.Default.Calculate
        MissionType.SHAKE -> "KINETIC // 30 SHAKES" to Icons.Default.Vibration
        MissionType.NONE -> "STANDARD // NO CHALLENGE" to Icons.Default.Alarm
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4),
        modifier = modifier
            .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = WakefullySpacing.Space8, vertical = WakefullySpacing.Space4)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Terracotta,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
