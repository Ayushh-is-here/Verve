package com.wakefully.alarm.ui.create

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.wakefully.alarm.theme.ChalkWhite
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme

@Composable
fun CreateEditAlarmScreen(
    initialAlarm: Alarm? = null,
    barcodes: List<RegisteredBarcode> = emptyList(),
    onSaveAlarm: (Alarm) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    var hour by remember { mutableIntStateOf(initialAlarm?.hourOfDay ?: 7) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Morning Awakening") }
    var repeatDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: setOf(1, 2, 3, 4, 5)) }
    var missionType by remember { mutableStateOf(initialAlarm?.missionType ?: MissionType.MATH) }
    var selectedBarcodeId by remember { mutableStateOf(initialAlarm?.barcodeId ?: barcodes.firstOrNull()?.id) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkWhite)
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
                    onClick = onNavigateBack,
                    modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CarbonInk
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

            Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space20)
            ) {
                // Time Picker Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = PaperWhite),
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
                            color = MutedText
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Hour selector
                            TimeStepper(
                                value = hour,
                                range = 0..23,
                                onValueChange = { hour = it }
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                                color = CarbonInk,
                                modifier = Modifier.padding(horizontal = WakefullySpacing.Space8)
                            )

                            // Minute selector
                            TimeStepper(
                                value = minute,
                                range = 0..59,
                                step = 5,
                                onValueChange = { minute = it }
                            )
                        }
                    }
                }

                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Cadence Name / Label") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    singleLine = true
                )

                // Repeat Days Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = PaperWhite),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                        Text(
                            text = "REPEAT SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
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
                                        .background(if (isSelected) CarbonInk else RecessiveGray)
                                        .clickable {
                                            repeatDays = if (isSelected) repeatDays - dayNum else repeatDays + dayNum
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) PaperWhite else CarbonInk
                                    )
                                }
                            }
                        }
                    }
                }

                // Mission Challenge Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = PaperWhite),
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
                            MissionType.BARCODE to ("Optical Scan" to Icons.Default.QrCodeScanner),
                            MissionType.MATH to ("Mental Math" to Icons.Default.Calculate),
                            MissionType.SHAKE to ("Kinetic Shake" to Icons.Default.Vibration),
                            MissionType.NONE to ("Standard (No Challenge)" to Icons.Default.Alarm)
                        )

                        missions.forEach { (type, meta) ->
                            val isSelected = missionType == type
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                                    .background(if (isSelected) RecessiveGray else ChalkWhite)
                                    .border(
                                        1.dp,
                                        if (isSelected) CarbonInk else BorderHairline,
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
                                        tint = if (isSelected) Terracotta else CarbonInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = meta.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CarbonInk
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

                        // If Barcode mission is chosen, show registered barcode dropdown / selection
                        if (missionType == MissionType.BARCODE) {
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                            Text(
                                text = "SELECT REGISTERED BARCODE TARGET",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                            barcodes.forEach { barcode ->
                                val isChosen = selectedBarcodeId == barcode.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                        .background(if (isChosen) PaperWhite else RecessiveGray)
                                        .border(
                                            1.dp,
                                            if (isChosen) Terracotta else BorderHairline,
                                            RoundedCornerShape(WakefullySpacing.RadiusSmall)
                                        )
                                        .clickable { selectedBarcodeId = barcode.id }
                                        .padding(WakefullySpacing.Space12),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = barcode.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = CarbonInk
                                        )
                                        Text(
                                            text = "Code: ${barcode.value}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedText
                                        )
                                    }
                                    if (isChosen) {
                                        Text(
                                            text = "[TARGET]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Terracotta
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(WakefullySpacing.Space4))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

            // Pinned Bottom Save Button
            Button(
                onClick = {
                    val toSave = Alarm(
                        id = initialAlarm?.id ?: 0L,
                        hourOfDay = hour,
                        minute = minute,
                        repeatDays = repeatDays,
                        missionType = missionType,
                        barcodeId = if (missionType == MissionType.BARCODE) selectedBarcodeId else null,
                        enabled = true,
                        label = label
                    )
                    onSaveAlarm(toSave)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WakefullySpacing.Space56),
                shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CarbonInk,
                    contentColor = PaperWhite
                )
            ) {
                Text(
                    text = "SAVE CADENCE",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun TimeStepper(
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(RecessiveGray)
                .clickable {
                    val next = if (value + step > range.last) range.first else value + step
                    onValueChange(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Text("▲", color = CarbonInk, fontSize = 12.sp)
        }

        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
            color = CarbonInk
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(RecessiveGray)
                .clickable {
                    val prev = if (value - step < range.first) range.last else value - step
                    onValueChange(prev)
                },
            contentAlignment = Alignment.Center
        ) {
            Text("▼", color = CarbonInk, fontSize = 12.sp)
        }
    }
}

@Preview(name = "Create Alarm Screen", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun CreateEditAlarmScreenPreview() {
    WakefullyTheme {
        CreateEditAlarmScreen(
            barcodes = listOf(RegisteredBarcode(101, "Ensuite C-09 (Ceramic Cleanser)", "4006381333931")),
            onSaveAlarm = {},
            onNavigateBack = {}
        )
    }
}
