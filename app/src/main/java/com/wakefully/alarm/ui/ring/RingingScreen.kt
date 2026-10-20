package com.wakefully.alarm.ui.ring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.theme.BorderHairline
import com.wakefully.alarm.theme.CarbonInk
import com.wakefully.alarm.theme.ChalkWhite
import com.wakefully.alarm.theme.ErrorRed
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RingingScreen(
    alarm: Alarm,
    onStartMission: () -> Unit,
    onEmergencyDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // CRITICAL: Block the Android system back button while alarm is ringing!
    BackHandler(enabled = true) {
        // Back button is intentionally blocked. Alarm cannot be bypassed by pressing back!
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Subtle audio pulse animation (250ms)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Emergency Hold State Machine (5-second continuous press requirement)
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (isActive && isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / 5000f).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    onEmergencyDismiss()
                    break
                }
                delay(30)
            }
        } else {
            // Decay progress on premature release
            while (isActive && !isHolding && holdProgress > 0f) {
                holdProgress = (holdProgress - 0.05f).coerceAtLeast(0f)
                delay(30)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkWhite)
            .padding(
                top = topInset + WakefullySpacing.Space16,
                bottom = bottomInset + WakefullySpacing.Space24,
                start = WakefullySpacing.ScreenHorizontalPadding,
                end = WakefullySpacing.ScreenHorizontalPadding
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top telemetry bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                    .background(RecessiveGray)
                    .padding(horizontal = WakefullySpacing.Space12, vertical = WakefullySpacing.Space8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Terracotta)
                    )
                    Text(
                        text = "ALARM ACTIVE // WAKEFULLY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Terracotta
                    )
                }
                Text(
                    text = "DECIBEL: 84 dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }

            // Central time display with pulse indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(RecessiveGray)
                        .border(1.dp, BorderHairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(PaperWhite)
                            .border(2.dp, Terracotta, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space32))

                // Time Numerals
                val hourStr = alarm.hourOfDay.toString().padStart(2, '0')
                val minStr = alarm.minute.toString().padStart(2, '0')
                Text(
                    text = "$hourStr:$minStr",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Light,
                        fontSize = 64.sp
                    ),
                    color = CarbonInk
                )

                if (alarm.label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CarbonInk
                    )
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                Text(
                    text = when (alarm.missionType) {
                        MissionType.BARCODE -> "Optical Scan Verification Required"
                        MissionType.MATH -> "3 Mental Math Problems Required"
                        MissionType.SHAKE -> "30 Kinetic Cycles Required"
                        MissionType.NONE -> "Tap to dismiss"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }

            // Bottom Actions: Start Mission + Emergency Hold
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space12)
            ) {
                // Primary Action Button: Start Mission
                Button(
                    onClick = onStartMission,
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
                        text = when (alarm.missionType) {
                            MissionType.BARCODE -> "START BARCODE SCANNER"
                            MissionType.MATH -> "START MENTAL MATH CHALLENGE"
                            MissionType.SHAKE -> "START SHAKE CHALLENGE"
                            MissionType.NONE -> "DISMISS ALARM"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                }

                // Emergency 5-Second Hold Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.MinTouchTarget)
                        .clip(RoundedCornerShape(WakefullySpacing.RadiusButton))
                        .background(RecessiveGray)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    tryAwaitRelease()
                                    isHolding = false
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Hold progress fill bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(holdProgress)
                            .height(WakefullySpacing.MinTouchTarget)
                            .background(if (holdProgress > 0.8f) ErrorRed else TerracottaBright.copy(alpha = 0.5f))
                    )

                    // Button text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CarbonInk,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(WakefullySpacing.Space8))
                        Text(
                            text = if (isHolding) {
                                "HOLDING... ${(5 - (holdProgress * 5)).toInt() + 1}s"
                            } else {
                                "HOLD 5s FOR EMERGENCY SHUTOFF"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CarbonInk
                        )
                    }
                }

                Text(
                    text = "SNOOZE DISABLED // VERIFICATION PROTOCOL ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== COMPOSE PREVIEWS ====================

@Preview(name = "Small Phone (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun RingingScreenSmallPreview() {
    WakefullyTheme {
        RingingScreen(
            alarm = Alarm(hourOfDay = 6, minute = 30, missionType = MissionType.BARCODE, label = "Morning Shower"),
            onStartMission = {},
            onEmergencyDismiss = {}
        )
    }
}

@Preview(name = "Standard Phone (411x891)", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun RingingScreenStandardPreview() {
    WakefullyTheme {
        RingingScreen(
            alarm = Alarm(hourOfDay = 7, minute = 15, missionType = MissionType.MATH, label = "Wake Up Call"),
            onStartMission = {},
            onEmergencyDismiss = {}
        )
    }
}
