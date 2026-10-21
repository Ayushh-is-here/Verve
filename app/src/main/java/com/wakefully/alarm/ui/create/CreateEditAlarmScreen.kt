package com.wakefully.alarm.ui.create

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.audio.VerveAudioPlayer
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.formatTimeRemainingToast
import com.wakefully.alarm.service.getNextAlarmTime
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.WakefullySpacing
import java.util.Calendar

@Composable
fun CreateEditAlarmScreen(
    initialAlarm: Alarm? = null,
    barcodes: List<RegisteredBarcode> = emptyList(),
    onSaveAlarm: (Alarm) -> Unit,
    onNavigateBack: () -> Unit,
    onRegisterBarcode: (name: String, code: String) -> Long = { _, _ -> 0L },
    onDeleteBarcode: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    var hour by remember { mutableIntStateOf(initialAlarm?.hourOfDay ?: 7) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Morning Awakening") }
    var repeatDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: setOf(1, 2, 3, 4, 5)) }
    var missionType by remember { mutableStateOf(initialAlarm?.missionType ?: MissionType.MATH) }
    var selectedBarcodeId by remember { mutableStateOf(initialAlarm?.barcodeId ?: barcodes.firstOrNull()?.id) }
    var toneName by remember { mutableStateOf(initialAlarm?.toneName ?: "Radiant Dawn") }
    var toneUri by remember { mutableStateOf(initialAlarm?.toneUri) }

    // Register barcode scanner dialog state
    var showRegisterBarcodeScanner by remember { mutableStateOf(false) }

    // Ensure a valid barcode is selected if any are available
    LaunchedEffect(barcodes) {
        if (selectedBarcodeId == null || barcodes.none { it.id == selectedBarcodeId }) {
            selectedBarcodeId = barcodes.firstOrNull()?.id
        }
    }

    // Audio preview state
    val isPlayingPreview by VerveAudioPlayer.isPlayingPreview.collectAsState()
    val currentPlayingTone by VerveAudioPlayer.currentPlayingTone.collectAsState()

    // Stop audio preview when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            VerveAudioPlayer.stopAll()
        }
    }

    // Custom soundtrack audio picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getAudioFileName(context, uri)
            val savedLocalUri = VerveAudioPlayer.saveCustomToneLocally(context, uri, fileName)
            val finalToneUri = (savedLocalUri ?: uri).toString()
            toneUri = finalToneUri
            toneName = fileName
            // Automatically preview the newly selected track
            VerveAudioPlayer.previewTone(context, fileName, finalToneUri, durationSeconds = 6)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topInset + WakefullySpacing.Space12,
                    bottom = maxOf(bottomInset, imeInset) + WakefullySpacing.Space16,
                    start = WakefullySpacing.ScreenHorizontalPadding,
                    end = WakefullySpacing.ScreenHorizontalPadding
                )
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        VerveAudioPlayer.stopAll()
                        onNavigateBack()
                    },
                    modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (initialAlarm == null) "NEW CADENCE" else "EDIT CADENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Terracotta
                )

                // Placeholder to balance back button
                Spacer(modifier = Modifier.size(WakefullySpacing.MinTouchTarget))
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space16)
            ) {
                // ==================== 1. TIME PICKER & PRESETS ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WakefullySpacing.CardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCHEDULE TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                        // Large Digital Time Display with Quick Steppers & Direct Typing
                        val isPm = hour >= 12
                        val displayHour = when (val h = hour % 12) {
                            0 -> 12
                            else -> h
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Hour Box (Supports direct typing & +/- step)
                            EditableTimeDigitBox(
                                value = displayHour,
                                range = 1..12,
                                onValueChange = { typedHour ->
                                    if (typedHour in 13..23) {
                                        hour = typedHour
                                    } else if (typedHour == 0) {
                                        hour = 0
                                    } else if (typedHour in 1..12) {
                                        hour = if (isPm) {
                                            if (typedHour == 12) 12 else typedHour + 12
                                        } else {
                                            if (typedHour == 12) 0 else typedHour
                                        }
                                    }
                                },
                                onIncrement = {
                                    hour = (hour + 1) % 24
                                },
                                onDecrement = {
                                    hour = if (hour - 1 < 0) 23 else hour - 1
                                },
                                label = "HOUR"
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = WakefullySpacing.Space8)
                            )

                            // Minute Box (Supports direct typing & +/- step)
                            EditableTimeDigitBox(
                                value = minute,
                                range = 0..59,
                                onValueChange = { typedMin ->
                                    minute = typedMin.coerceIn(0, 59)
                                },
                                onIncrement = {
                                    minute = (minute + 1) % 60
                                },
                                onDecrement = {
                                    minute = if (minute - 1 < 0) 59 else minute - 1
                                },
                                label = "MIN"
                            )

                            Spacer(modifier = Modifier.width(WakefullySpacing.Space16))

                            // AM / PM Selector Pill
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                        .background(if (!isPm) Terracotta else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (hour >= 12) hour -= 12
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "AM",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (!isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                        .background(if (isPm) Terracotta else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (hour < 12) hour += 12
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PM",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

                        // Quick Presets Row
                        Text(
                            text = "QUICK PRESETS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space6))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(
                                "06:00 AM" to (6 to 0),
                                "06:30 AM" to (6 to 30),
                                "07:00 AM" to (7 to 0),
                                "07:30 AM" to (7 to 30),
                                "08:00 AM" to (8 to 0),
                                "+15 min" to null,
                                "+30 min" to null
                            )
                            items(presets) { (title, pair) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WakefullySpacing.RadiusChip))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (pair != null) {
                                                hour = pair.first
                                                minute = pair.second
                                            } else if (title == "+15 min") {
                                                val total = hour * 60 + minute + 15
                                                hour = (total / 60) % 24
                                                minute = total % 60
                                            } else if (title == "+30 min") {
                                                val total = hour * 60 + minute + 30
                                                hour = (total / 60) % 24
                                                minute = total % 60
                                            }
                                        }
                                        .padding(horizontal = WakefullySpacing.Space10, vertical = WakefullySpacing.Space6)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== 2. ALARM NAME / LABEL ====================
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label / Routine Name") },
                    placeholder = { Text("e.g. Morning Routine, Deep Work") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Terracotta,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = Terracotta,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = Terracotta
                    )
                )

                // ==================== 3. REPEAT SCHEDULE ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                        Text(
                            text = "REPEAT SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                            days.forEach { (name, dayNum) ->
                                val isSelected = repeatDays.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(WakefullySpacing.MinTouchTarget)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            repeatDays = if (isSelected) repeatDays - dayNum else repeatDays + dayNum
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== 4. MISSION CHALLENGE ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                        Text(
                            text = "WAKEFULNESS MISSION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                        val missions = listOf(
                            MissionType.BARCODE to ("Optical Scan (Barcode/QR)" to Icons.Default.QrCodeScanner),
                            MissionType.MATH to ("Mental Math (Easy Arithmetic)" to Icons.Default.Calculate),
                            MissionType.SHAKE to ("Kinetic Shake (30 Shakes)" to Icons.Default.Vibration),
                            MissionType.NONE to ("Standard (No Challenge)" to Icons.Default.Alarm)
                        )

                        missions.forEach { (type, meta) ->
                            val isSelected = missionType == type
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Terracotta else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(WakefullySpacing.RadiusButton)
                                    )
                                    .clickable { missionType = type }
                                    .padding(WakefullySpacing.Space12),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
                                ) {
                                    Icon(
                                        imageVector = meta.second,
                                        contentDescription = null,
                                        tint = if (isSelected) Terracotta else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = meta.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Terracotta)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                        }

                        // Barcode Target Selection & Registration
                        if (missionType == MissionType.BARCODE) {
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET REGISTERED BARCODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+ Scan New Barcode",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Terracotta,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showRegisterBarcodeScanner = true }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                            if (barcodes.isEmpty()) {
                                // Prominent callout card when no barcodes are registered yet
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(WakefullySpacing.RadiusSmall),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(WakefullySpacing.Space16),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Terracotta,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                                        Text(
                                            text = "No Barcode Registered Yet",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Scan any household item (e.g. toothpaste, coffee) that will force you out of bed to turn off this alarm.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))
                                        Button(
                                            onClick = { showRegisterBarcodeScanner = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                            shape = RoundedCornerShape(WakefullySpacing.RadiusButton)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("SCAN BARCODE WITH CAMERA")
                                        }
                                    }
                                }
                            } else {
                                // Saved barcodes: Tap any to select for this alarm without re-registering
                                barcodes.forEach { barcode ->
                                    val isChosen = selectedBarcodeId == barcode.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                            .background(
                                                if (isChosen) MaterialTheme.colorScheme.surfaceVariant
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isChosen) Terracotta else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(WakefullySpacing.RadiusSmall)
                                            )
                                            .clickable { selectedBarcodeId = barcode.id }
                                            .padding(WakefullySpacing.Space12),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                tint = if (isChosen) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = barcode.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Code: ${barcode.value}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isChosen) {
                                                Text(
                                                    text = "[TARGET]",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Terracotta
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    onDeleteBarcode(barcode.id)
                                                    if (selectedBarcodeId == barcode.id) {
                                                        selectedBarcodeId = barcodes.firstOrNull { it.id != barcode.id }?.id
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Barcode",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(WakefullySpacing.Space6))
                                }
                            }
                        }
                    }
                }

                // ==================== 5. ACOUSTIC SIGNATURE & PREVIEW ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACOUSTIC SIGNATURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Terracotta
                            )
                            if (isPlayingPreview) {
                                Text(
                                    text = "PLAYING PREVIEW...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Terracotta
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                        val tones = listOf(
                            "Radiant Dawn" to "Harmonic ambient chime crescendo",
                            "Zenith Pulse" to "Crisp metallic cadence for prompt focus",
                            "Nordic Mist" to "Gentle resonant woodwind harmonics",
                            "Verve Synthesis" to "Dynamic progressive high-energy tempo"
                        )

                        tones.forEach { (name, subtitle) ->
                            val isSelected = toneName == name && toneUri.isNullOrBlank()
                            val isThisPlaying = isPlayingPreview && currentPlayingTone == name

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Terracotta else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(WakefullySpacing.RadiusButton)
                                    )
                                    .clickable {
                                        toneName = name
                                        toneUri = null
                                    }
                                    .padding(horizontal = WakefullySpacing.Space12, vertical = WakefullySpacing.Space8),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Preview Tone Button (▶ Preview / ■ Stop)
                                IconButton(
                                    onClick = {
                                        if (isThisPlaying) {
                                            VerveAudioPlayer.stopAll()
                                        } else {
                                            toneName = name
                                            toneUri = null
                                            VerveAudioPlayer.previewTone(
                                                context = context,
                                                toneName = name,
                                                toneUri = null,
                                                durationSeconds = 5
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isThisPlaying) "Stop" else "Preview",
                                        tint = if (isThisPlaying) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                        }

                        // Custom User Audio Track Selection Row
                        val isCustomSelected = !toneUri.isNullOrBlank()
                        val isCustomPlaying = isPlayingPreview && currentPlayingTone == toneName && isCustomSelected

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCustomSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(WakefullySpacing.Space12)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            tint = if (isCustomSelected) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isCustomSelected) toneName else "Custom Audio Soundtrack",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isCustomSelected) "Selected from local storage" else "Assign your own audio file / ringtone",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isCustomSelected) {
                                        IconButton(
                                            onClick = {
                                                if (isCustomPlaying) {
                                                    VerveAudioPlayer.stopAll()
                                                } else {
                                                    VerveAudioPlayer.previewTone(
                                                        context = context,
                                                        toneName = toneName,
                                                        toneUri = toneUri,
                                                        durationSeconds = 6
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                                        ) {
                                            Icon(
                                                imageVector = if (isCustomPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (isCustomPlaying) "Stop" else "Preview",
                                                tint = if (isCustomPlaying) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                                OutlinedButton(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(WakefullySpacing.RadiusSmall)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCustomSelected) "Change Audio File" else "Choose Audio File...",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

            // ==================== 6. PINNED SAVE BUTTON ====================
            val isBarcodeMissing = missionType == MissionType.BARCODE && (selectedBarcodeId == null || barcodes.none { it.id == selectedBarcodeId })

            if (isBarcodeMissing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRegisterBarcodeScanner = true },
                    shape = RoundedCornerShape(WakefullySpacing.RadiusSmall),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WakefullySpacing.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TARGET BARCODE REQUIRED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "You must scan or select a target barcode above before saving this cadence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
            }

            Button(
                onClick = {
                    if (isBarcodeMissing) {
                        Toast.makeText(context, "Please scan or select a target barcode first", Toast.LENGTH_SHORT).show()
                        showRegisterBarcodeScanner = true
                        return@Button
                    }

                    VerveAudioPlayer.stopAll()
                    val toSave = Alarm(
                        id = initialAlarm?.id ?: 0L,
                        hourOfDay = hour,
                        minute = minute,
                        repeatDays = repeatDays,
                        missionType = missionType,
                        barcodeId = if (missionType == MissionType.BARCODE) selectedBarcodeId else null,
                        enabled = true,
                        label = label.ifBlank { "Morning Awakening" },
                        toneName = toneName,
                        toneUri = toneUri
                    )

                    // Compute upcoming trigger time and display Toast
                    val nextCal = getNextAlarmTime(toSave.hourOfDay, toSave.minute, toSave.repeatDays)
                    val toastMessage = formatTimeRemainingToast(nextCal)
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()

                    onSaveAlarm(toSave)
                },
                enabled = !isBarcodeMissing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WakefullySpacing.Space56),
                shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isBarcodeMissing) "BARCODE REQUIRED TO SAVE" else "SAVE CADENCE",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp
                )
            }
        }

        // Register Barcode Scanner Dialog
        if (showRegisterBarcodeScanner) {
            RegisterBarcodeScannerDialog(
                onDismiss = { showRegisterBarcodeScanner = false },
                onBarcodeRegistered = { name, code ->
                    val createdId = onRegisterBarcode(name, code)
                    selectedBarcodeId = createdId
                    showRegisterBarcodeScanner = false
                    Toast.makeText(context, "Registered '$name' as alarm target!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun EditableTimeDigitBox(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var textValue by remember(value, isFocused) {
        mutableStateOf(if (isFocused) value.toString() else value.toString().padStart(2, '0'))
    }
    val focusRequester = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        // Increment Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Editable Digit Container Box
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Terracotta else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton)
                )
                .clickable { focusRequester.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newText ->
                    val filtered = newText.filter { it.isDigit() }.take(2)
                    textValue = filtered
                    val parsed = filtered.toIntOrNull()
                    if (parsed != null) {
                        onValueChange(parsed)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            val parsed = textValue.toIntOrNull() ?: value
                            val clamped = parsed.coerceIn(range.first, range.last)
                            onValueChange(clamped)
                            textValue = clamped.toString().padStart(2, '0')
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(Terracotta),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (textValue.isEmpty()) {
                            Text(
                                text = "00",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Decrement Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isFocused) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getAudioFileName(context: Context, uri: Uri): String {
    var name = "Custom Soundtrack"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index) ?: name
                }
            }
        }
    } catch (_: Exception) {
        name = uri.lastPathSegment ?: "Custom Soundtrack"
    }
    return name
}
