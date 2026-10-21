package com.wakefully.alarm.ui.mission.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.wakefully.alarm.theme.ErrorContainerRed
import com.wakefully.alarm.theme.ErrorRed
import com.wakefully.alarm.theme.Terracotta
import com.wakefully.alarm.theme.TerracottaBright
import com.wakefully.alarm.theme.WakefullySpacing
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeMissionScreen(
    viewModel: BarcodeMissionViewModel,
    onFallbackEmergency: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Back button is strictly disabled while alarm is ringing!
    BackHandler(enabled = true) {}

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // Auto-check and request camera permission on screen entry
    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.onCameraPermissionResult(true)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Torch control
    LaunchedEffect(uiState.isTorchOn, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(uiState.isTorchOn)
        } catch (_: Exception) {}
    }

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
            .background(MaterialTheme.colorScheme.background)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ALARM ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Central Viewfinder (Live CameraX feed + ML Kit scanning)
            if (!uiState.hasCameraPermission) {
                // Permission Denied View
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
                            color = Color(0xFF1C1B1F),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(WakefullySpacing.Space16))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                        ) {
                            Text("GRANT CAMERA PERMISSION")
                        }
                    }
                }
            } else {
                // Live CameraX Scanner Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(WakefullySpacing.RadiusCard))
                        .background(Color.Black)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(WakefullySpacing.RadiusCard)),
                    contentAlignment = Alignment.Center
                ) {
                    // Camera Preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val cameraExecutor = Executors.newSingleThreadExecutor()
                            val scanner = BarcodeScanning.getClient()

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = CameraPreview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val raw = barcode.rawValue
                                                    if (!raw.isNullOrBlank()) {
                                                        viewModel.onScanFrame(raw, barcode.format)
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraInstance = cam
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // HUD Reticle Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(WakefullySpacing.RadiusSmall))
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
                                    .background(Color.White.copy(alpha = 0.6f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .align(Alignment.Center)
                                    .background(Color.White.copy(alpha = 0.6f))
                            )
                        }
                    }

                    // Torch Toggle Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(WakefullySpacing.Space12)
                            .size(WakefullySpacing.MinTouchTarget)
                            .clip(CircleShape)
                            .background(if (uiState.isTorchOn) Terracotta else Color.Black.copy(alpha = 0.6f))
                            .clickable { viewModel.toggleTorch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Viewfinder Instruction
                    Text(
                        text = "POINT CAMERA AT TARGET BARCODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.85f),
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
                        if (uiState.isError) ErrorRed else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(WakefullySpacing.RadiusCard)
                    ),
                shape = RoundedCornerShape(WakefullySpacing.RadiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError) ErrorContainerRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(WakefullySpacing.CardPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REGISTERED TARGET ITEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isError) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CODE VERIFICATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta
                        )
                    }

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                    Text(
                        text = uiState.targetBarcode?.name ?: "Bathroom / Ensuite Cleanser",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(WakefullySpacing.Space4))

                    if (uiState.isError) {
                        Text(
                            text = uiState.errorMessage ?: "That's not the right barcode!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ErrorRed
                        )
                    } else {
                        Text(
                            text = "Expected barcode: ${uiState.targetBarcode?.value ?: "Any registered code"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Buttons: Quick test confirmation & Fallback
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WakefullySpacing.Space8)
            ) {
                // Confirm Optical Alignment Button (Allows quick verification if in dark room)
                Button(
                    onClick = {
                        val target = uiState.targetBarcode
                        if (target != null) {
                            viewModel.onScanFrame(target.value, target.format)
                            viewModel.onScanFrame(target.value, target.format)
                        } else {
                            viewModel.onEmergencyHoldComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.Space56),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(WakefullySpacing.Space8))
                    Text(
                        text = "MANUAL VERIFY (MATCH TARGET)",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.5.sp
                    )
                }

                // Troubleshooting Fallback (Mental Math fallback)
                OutlinedButton(
                    onClick = onFallbackEmergency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WakefullySpacing.MinTouchTarget),
                    shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(
                        text = "CAN'T SCAN? SOLVE 3 MATH PROBLEMS INSTEAD",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
