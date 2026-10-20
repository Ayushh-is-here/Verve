package com.wakefully.alarm.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.theme.BorderHairline
import com.wakefully.alarm.theme.CarbonInk
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme

@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    barcodes: List<RegisteredBarcode> = emptyList(),
    onToggleAlarm: (id: Long, enabled: Boolean) -> Unit,
    onDeleteAlarm: (id: Long) -> Unit,
    onEditAlarm: (alarm: Alarm) -> Unit,
    onCreateAlarm: () -> Unit,
    onTriggerTestAlarm: (alarm: Alarm) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                top = topInset + WakefullySpacing.Space16,
                // Generous bottom content padding so FAB never overlaps the last item!
                bottom = bottomInset + WakefullySpacing.Space64 + WakefullySpacing.Space40
            ),
            verticalArrangement = Arrangement.spacedBy(WakefullySpacing.ListGap)
        ) {
            // Header Section
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = WakefullySpacing.Space8)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WAKEFULLY // PROTOCOL 4.2",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WakefullySpacing.RadiusChip))
                                .background(RecessiveGray)
                                .padding(horizontal = WakefullySpacing.Space8, vertical = WakefullySpacing.Space4)
                        ) {
                            Text(
                                text = "${alarms.count { it.enabled }} ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                    Text(
                        text = "Scheduled Alarms",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                    Text(
                        text = "Disciplined wakefulness. Strict verification guarantees zero accidental shutoffs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Next Alarm Banner
            val nextAlarm = alarms.firstOrNull { it.enabled }
            if (nextAlarm != null) {
                item(key = "next_alarm_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                        colors = CardDefaults.cardColors(containerColor = CarbonInk)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(WakefullySpacing.CardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NEXT SCHEDULED WAKEFULNESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TerracottaBright
                                )
                                Spacer(modifier = Modifier.height(WakefullySpacing.Space4))
                                val hourStr = nextAlarm.hourOfDay.toString().padStart(2, '0')
                                val minStr = nextAlarm.minute.toString().padStart(2, '0')
                                Text(
                                    text = "$hourStr:$minStr",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = PaperWhite
                                )
                                if (nextAlarm.label.isNotBlank()) {
                                    Text(
                                        text = nextAlarm.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC7C6CB)
                                    )
                                }
                            }

                            // Quick trigger button for manual testing / demo
                            Box(
                                modifier = Modifier
                                    .size(WakefullySpacing.MinTouchTarget)
                                    .clip(CircleShape)
                                    .background(Terracotta)
                                    .clickable { onTriggerTestAlarm(nextAlarm) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Test Trigger Alarm",
                                    tint = PaperWhite
                                )
                            }
                        }
                    }
                }
            }

            // Alarms List
            items(items = alarms, key = { it.id }) { alarm ->
                val linkedBarcode = barcodes.firstOrNull { it.id == alarm.barcodeId }
                AlarmCard(
                    alarm = alarm,
                    barcodeName = linkedBarcode?.name,
                    onToggle = { enabled -> onToggleAlarm(alarm.id, enabled) },
                    onDelete = { onDeleteAlarm(alarm.id) },
                    onEdit = { onEditAlarm(alarm) },
                    onTestTrigger = { onTriggerTestAlarm(alarm) }
                )
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
            containerColor = CarbonInk,
            contentColor = PaperWhite
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
            .border(1.dp, BorderHairline, RoundedCornerShape(WakefullySpacing.RadiusCard))
            .clickable(interactionSource = interactionSource, indication = null) { onEdit() },
        shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled) PaperWhite else RecessiveGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
            // Top row: Mission tag + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mission Tag
                MissionBadge(missionType = alarm.missionType, barcodeName = barcodeName)

                // Toggle Switch (guaranteed >= 48x48dp touch target)
                Box(
                    modifier = Modifier.size(WakefullySpacing.MinTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PaperWhite,
                            checkedTrackColor = Terracotta,
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = RecessiveGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

            // Time Row
            val hourStr = alarm.hourOfDay.toString().padStart(2, '0')
            val minStr = alarm.minute.toString().padStart(2, '0')
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                Text(
                    text = "$hourStr:$minStr",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Light,
                        fontSize = 48.sp
                    ),
                    color = if (alarm.enabled) CarbonInk else MutedText
                )
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alarm.enabled) CarbonInk else MutedText,
                        modifier = Modifier.padding(bottom = WakefullySpacing.Space8)
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
                                .background(if (isSelected && alarm.enabled) CarbonInk else RecessiveGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected && alarm.enabled) PaperWhite else MutedText
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
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionBadge(missionType: MissionType, barcodeName: String?) {
    val (label, icon) = when (missionType) {
        MissionType.BARCODE -> {
            val name = barcodeName ?: "REGISTERED BARCODE"
            "OPTICAL // $name" to Icons.Default.QrCodeScanner
        }
        MissionType.MATH -> "MATH // 3 PROBLEMS" to Icons.Default.Calculate
        MissionType.SHAKE -> "KINETIC // 30 SHAKES" to Icons.Default.Vibration
        MissionType.NONE -> "STANDARD // NO CHALLENGE" to Icons.Default.Alarm
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4),
        modifier = Modifier
            .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
            .background(RecessiveGray)
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
            color = CarbonInk
        )
    }
}

// ==================== COMPOSE PREVIEWS ====================

@Preview(name = "Small Phone (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun AlarmListScreenSmallPreview() {
    WakefullyTheme {
        SampleAlarmList()
    }
}

@Preview(name = "Standard Phone (411x891)", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun AlarmListScreenStandardPreview() {
    WakefullyTheme {
        SampleAlarmList()
    }
}

@Preview(name = "Large Phone (480x1080)", widthDp = 480, heightDp = 1080, showBackground = true)
@Composable
fun AlarmListScreenLargePreview() {
    WakefullyTheme {
        SampleAlarmList()
    }
}

@Composable
private fun SampleAlarmList() {
    val sampleAlarms = listOf(
        Alarm(
            id = 1,
            hourOfDay = 6,
            minute = 30,
            repeatDays = setOf(1, 2, 3, 4, 5),
            missionType = MissionType.BARCODE,
            barcodeId = 101,
            enabled = true,
            label = "Morning Shower"
        ),
        Alarm(
            id = 2,
            hourOfDay = 7,
            minute = 15,
            repeatDays = setOf(6, 7),
            missionType = MissionType.MATH,
            barcodeId = null,
            enabled = false,
            label = "Weekend Awakening"
        )
    )
    AlarmListScreen(
        alarms = sampleAlarms,
        barcodes = listOf(RegisteredBarcode(101, "Ensuite C-09", "4006381333931")),
        onToggleAlarm = { _, _ -> },
        onDeleteAlarm = {},
        onEditAlarm = {},
        onCreateAlarm = {},
        onTriggerTestAlarm = {}
    )
}
