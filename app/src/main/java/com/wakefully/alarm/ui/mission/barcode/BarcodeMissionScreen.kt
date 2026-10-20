package com.wakefully.alarm.ui.mission.barcode

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakefully.alarm.data.InMemoryWakefullyRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.DefaultAlarmController
import com.wakefully.alarm.theme.BorderHairline
import com.wakefully.alarm.theme.CarbonInk
import com.wakefully.alarm.theme.ChalkWhite
import com.wakefully.alarm.theme.ErrorContainerRed
import com.wakefully.alarm.theme.ErrorRed
import com.wakefully.alarm.theme.MutedText
import com.wakefully.alarm.theme.PaperWhite
import com.wakefully.alarm.theme.RecessiveGray
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import com.wakefully.alarm.theme.WakefullyTheme
import kotlin.math.roundToInt

@Composable
fun BarcodeMissionScreen(
    viewModel: BarcodeMissionViewModel,
    onRequestCameraPermission: () -> Unit = {},
    onFallbackEmergency: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Back button is strictly disabled while alarm is ringing!
    BackHandler(enabled = true) {}

    val uiState by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Viewfinder laser beam motion
    val laserTransition = rememberInfiniteTransition(label = "laser")
    val laserY by laserTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Shake animation on wrong barcode
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.isError) {
        if (uiState.isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkWhite)
            .padding(
                top = topInset + WakefullySpacing.Space12,
                bottom = bottomInset + WakefullySpacing.Space16,
                start = WakefullySpacing.ScreenHorizontalPadding,
                end = WakefullySpacing.ScreenHorizontalPadding
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Telemetry Bar
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            text = "MISSION // OPTICAL SCAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WakefullySpacing.Space4)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "DECIBEL: 84 dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonInk
                        )
                    }
                }
            }

            // Central Viewfinder or Camera Permission Denied View
            if (!uiState.hasCameraPermission) {
                // Camera Permission Denied state: Explain and offer grant button.
                // NOTE: The alarm sound keeps playing continuously!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = ErrorContainerRed)
                ) {
                    Column(
                        modifier = Modifier.padding(WakefullySpacing.CardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                        Text(
                            text = "Optical verification cannot read barcodes without camera access. The alarm will continue sounding until camera access is granted or emergency hold is completed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CarbonInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space16))
                        Button(
                            onClick = onRequestCameraPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = PaperWhite)
                        ) {
                            Text("GRANT CAMERA PERMISSION")
                        }
                    }
                }
            } else {
                // Architectural Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(WakefullySpacing.RadiusCard))
                        .background(CarbonInk)
                        .border(1.dp, BorderHairline, RoundedCornerShape(WakefullySpacing.RadiusCard)),
                    contentAlignment = Alignment.Center
                ) {
                    // HUD Reticle and Targeting Frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .border(1.dp, PaperWhite.copy(alpha = 0.4f), RoundedCornerShape(WakefullySpacing.RadiusSmall))
                    ) {
                        // Scan Laser Beam
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (200 * laserY).dp)
                                .background(TerracottaBright)
                        )

                        // Center Crosshair
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .align(Alignment.Center)
                                    .background(PaperWhite.copy(alpha = 0.6f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .align(Alignment.Center)
                                    .background(PaperWhite.copy(alpha = 0.6f))
                            )
                        }
                    }

                    // Torch Toggle Button (>= 48x48dp touch target)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(WakefullySpacing.Space12)
                            .size(WakefullySpacing.MinTouchTarget)
                            .clip(CircleShape)
                            .background(if (uiState.isTorchOn) Terracotta else RecessiveGray.copy(alpha = 0.8f))
                            .clickable { viewModel.toggleTorch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = if (uiState.isTorchOn) PaperWhite else CarbonInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Viewfinder text overlay
                    Text(
                        text = "ALIGN OBJECT CODE PERPENDICULAR TO OPTICAL AXIS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = PaperWhite.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = WakefullySpacing.Space12)
                    )
                }
            }

            // Target Metadata & Error Feedback Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .border(
                        1.dp,
                        if (uiState.isError) ErrorRed else BorderHairline,
                        RoundedCornerShape(WakefullySpacing.RadiusCard)
                    ),
                shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError) ErrorContainerRed.copy(alpha = 0.5f) else PaperWhite
                )
            ) {
                Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REGISTERED TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isError) ErrorRed else MutedText
                        )
                        Text(
                            text = "FORMAT: EAN-13",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                    }

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                    Text(
                        text = uiState.targetBarcode?.name ?: "Ensuite C-09 (Ceramic Cleanser)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CarbonInk
                    )

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                    if (uiState.isError) {
                        Text(
                            text = uiState.errorMessage ?: "That's not the right one",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ErrorRed
                        )
                    } else {
                        Text(
                            text = "Expected signature: ${uiState.targetBarcode?.value ?: "4006381333931"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }

            // Interactive Actions: Optical Confirmation & Troubleshooting Fallback
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                // Confirm Optical Alignment Button (Simulates camera detecting the registered barcode)
                Button(
                    onClick = {
                        val target = uiState.targetBarcode
                        if (target != null) {
                            // Inject 2 consecutive frames as required by debounce engine
                            viewModel.onScanFrame(target.value, target.format)
                            viewModel.onScanFrame(target.value, target.format)
                        }
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
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(WakefullySpacing.Space8))
                    Text(
                        text = "CONFIRM OPTICAL SCAN (RIGHT ITEM)",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.5.sp
                    )
                }

                // Simulate Wrong Barcode Scan Button (For live QA testing)
                OutlinedButton(
                    onClick = {
                        // Injects 2 consecutive frames of a WRONG item
                        viewModel.onScanFrame("9999999999999", 32)
                        viewModel.onScanFrame("9999999999999", 32)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.MinTouchTarget),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text(
                        text = "TEST WRONG BARCODE SCAN",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Troubleshooting Fallback (Mental Math / Emergency Hold)
                OutlinedButton(
                    onClick = {
                        onFallbackEmergency()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.MinTouchTarget),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonInk)
                ) {
                    Text(
                        text = "TROUBLESHOOT SCAN (3 COMPLEX MATH PROBLEMS)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ==================== COMPOSE PREVIEWS ====================

@Preview(name = "Small Phone (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun BarcodeMissionScreenSmallPreview() {
    WakefullyTheme {
        SampleBarcodeMission()
    }
}

@Preview(name = "Standard Phone (411x891)", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun BarcodeMissionScreenStandardPreview() {
    WakefullyTheme {
        SampleBarcodeMission()
    }
}

@Composable
private fun SampleBarcodeMission() {
    val barcode = RegisteredBarcode(101, "Ensuite C-09 (Ceramic Cleanser)", "4006381333931", 32)
    val vm = BarcodeMissionViewModel(
        alarm = Alarm(id = 1, hourOfDay = 6, minute = 30, missionType = MissionType.BARCODE, barcodeId = 101),
        barcodeRepository = InMemoryWakefullyRepository(),
        alarmController = DefaultAlarmController(),
        initialTarget = barcode
    )
    BarcodeMissionScreen(viewModel = vm)
}
