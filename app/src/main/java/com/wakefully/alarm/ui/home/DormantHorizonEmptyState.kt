package com.wakefully.alarm.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist, non-generic empty state depicting a dormant celestial chronometer.
 * Features an animated orbital beacon dial, real-time live clock readout,
 * horological calibration ticks, and archetype cadence presets.
 */
@Composable
fun DormantHorizonEmptyState(
    onCreateAlarm: () -> Unit,
    onSelectPreset: (Alarm) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf(getLiveTimeFormatted()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = getLiveTimeFormatted()
            delay(1000L)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WakefullySpacing.Space20, vertical = WakefullySpacing.Space24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== 1. LIVE CHRONOMETER HOROLOGY DIAL ====================
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clickable { onCreateAlarm() },
                contentAlignment = Alignment.Center
            ) {
                // Continuous kinetic orbital animation
                val transition = rememberInfiniteTransition(label = "chronometer_orbit")
                val rotationDegrees by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 12000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                val pulseAlpha by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

                Canvas(modifier = Modifier.size(176.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.width / 2f) - 12.dp.toPx()

                    // Outer celestial perimeter ring
                    drawCircle(
                        color = outlineColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Inner hairline orbit guide
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.2f),
                        radius = radius * 0.76f,
                        center = center,
                        style = Stroke(width = 0.75.dp.toPx())
                    )

                    // 12 Horological precision graduation ticks
                    for (i in 0 until 12) {
                        val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
                        val isQuarter = (i % 3 == 0)
                        val tickLength = if (isQuarter) 9.dp.toPx() else 5.dp.toPx()
                        val tickStroke = if (isQuarter) 1.5.dp.toPx() else 1.dp.toPx()

                        val startX = center.x + ((radius - tickLength) * cos(angleRad)).toFloat()
                        val startY = center.y + ((radius - tickLength) * sin(angleRad)).toFloat()
                        val endX = center.x + (radius * cos(angleRad)).toFloat()
                        val endY = center.y + (radius * sin(angleRad)).toFloat()

                        drawLine(
                            color = if (isQuarter) Terracotta.copy(alpha = 0.75f) else tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = tickStroke,
                            cap = StrokeCap.Round
                        )
                    }

                    // Rotating glowing arc along perimeter
                    val sweepAngle = 55f
                    val startAngle = rotationDegrees - sweepAngle
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Terracotta.copy(alpha = 0.02f),
                                Terracotta.copy(alpha = pulseAlpha * 0.75f),
                                TerracottaBright.copy(alpha = pulseAlpha)
                            )
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Orbiting celestial beacon node
                    val beaconAngleRad = (rotationDegrees - 90.0) * (PI / 180.0)
                    val beaconX = center.x + (radius * cos(beaconAngleRad)).toFloat()
                    val beaconY = center.y + (radius * sin(beaconAngleRad)).toFloat()

                    // Outer ambient flare
                    drawCircle(
                        color = Terracotta.copy(alpha = 0.25f * pulseAlpha),
                        radius = 7.dp.toPx(),
                        center = Offset(beaconX, beaconY)
                    )
                    // Inner dense star core
                    drawCircle(
                        color = TerracottaBright,
                        radius = 3.dp.toPx(),
                        center = Offset(beaconX, beaconY)
                    )
                }

                // Inner core digital chronometer readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTimeString,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(WakefullySpacing.RadiusChip))
                            .background(Terracotta.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Terracotta)
                        )
                        Text(
                            text = "DORMANT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Terracotta
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space20))

            // ==================== 2. EDITORIAL STATEMENT ====================
            Text(
                text = "HORIZON // UNWRITTEN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Terracotta
            )

            Spacer(modifier = Modifier.height(WakefullySpacing.Space6))

            Text(
                text = "Silence is unbroken.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

            Text(
                text = "No awakening cadences active. Set an intention for tomorrow before the night settles in.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = WakefullySpacing.Space12)
            )

            Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

            // ==================== 3. PRIMARY ACTION BUTTON ====================
            Button(
                onClick = onCreateAlarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FORGE FIRST CADENCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

            // ==================== 4. ARCHETYPE CADENCE PRESETS ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                Text(
                    text = "QUICK START ARCHETYPES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = WakefullySpacing.Space8)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                ArchetypePresetRow(
                    time = "06:00",
                    period = "AM",
                    title = "Dawn Focus",
                    missionLabel = "MATH MISSION",
                    icon = Icons.Default.Calculate,
                    onClick = {
                        onSelectPreset(
                            Alarm(
                                id = 0L,
                                hourOfDay = 6,
                                minute = 0,
                                missionType = MissionType.MATH,
                                label = "Dawn Focus",
                                toneName = "Radiant Dawn",
                                repeatDays = setOf(1, 2, 3, 4, 5)
                            )
                        )
                    }
                )

                ArchetypePresetRow(
                    time = "06:45",
                    period = "AM",
                    title = "Out-of-Bed Anchor",
                    missionLabel = "BARCODE MISSION",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = {
                        onSelectPreset(
                            Alarm(
                                id = 0L,
                                hourOfDay = 6,
                                minute = 45,
                                missionType = MissionType.BARCODE,
                                label = "Out-of-Bed Anchor",
                                toneName = "Zenith Pulse",
                                repeatDays = setOf(1, 2, 3, 4, 5)
                            )
                        )
                    }
                )

                ArchetypePresetRow(
                    time = "07:30",
                    period = "AM",
                    title = "Solar Ascent",
                    missionLabel = "AMBIENT RISE",
                    icon = Icons.Default.WbSunny,
                    onClick = {
                        onSelectPreset(
                            Alarm(
                                id = 0L,
                                hourOfDay = 7,
                                minute = 30,
                                missionType = MissionType.MATH,
                                label = "Solar Ascent",
                                toneName = "Nordic Mist",
                                repeatDays = setOf(1, 2, 3, 4, 5)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ArchetypePresetRow(
    time: String,
    period: String,
    title: String,
    missionLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(WakefullySpacing.RadiusSmall)
            )
            .clickable { onClick() }
            .padding(horizontal = WakefullySpacing.Space12, vertical = WakefullySpacing.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Terracotta,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = missionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                    color = Terracotta
                )
            }
        }

        Text(
            text = "+ APPLY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = Terracotta
        )
    }
}

private fun getLiveTimeFormatted(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}
