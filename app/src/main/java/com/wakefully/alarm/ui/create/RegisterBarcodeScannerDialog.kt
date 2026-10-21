package com.wakefully.alarm.ui.create

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/**
 * Full-screen modal scanner dialog for registering a new barcode target.
 * Prompts the user to scan any household item barcode with CameraX + ML Kit,
 * captures the digits automatically, asks for a friendly label, and saves it.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun RegisterBarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeRegistered: (name: String, code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Scanning state
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var scannedFormat by remember { mutableIntStateOf(32) }
    var itemName by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var isManualMode by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Torch control
    LaunchedEffect(isTorchOn, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(isTorchOn)
        } catch (_: Exception) {}
    }

    // Clean up torch on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraInstance?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
        }
    }

    // Viewfinder laser motion
    val laserTransition = rememberInfiniteTransition(label = "register_laser")
    val laserY by laserTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WakefullySpacing.ScreenHorizontalPadding)
            ) {
                Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "REGISTER BARCODE TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = Terracotta,
                        letterSpacing = 1.sp
                    )

                    if (!isManualMode && hasCameraPermission && scannedCode == null) {
                        IconButton(
                            onClick = { isTorchOn = !isTorchOn },
                            modifier = Modifier.size(WakefullySpacing.MinTouchTarget)
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                                contentDescription = "Toggle Torch",
                                tint = if (isTorchOn) Terracotta else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(WakefullySpacing.MinTouchTarget))
                    }
                }

                Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                // Mode 1: Code Detected -> Form to name and confirm item
                if (scannedCode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Barcode Detected",
                            tint = Terracotta,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

                        Text(
                            text = "Barcode Scanned Successfully!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space8))

                        // Code Container
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = scannedCode ?: "",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Terracotta
                            )
                        }

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                        // Friendly name input
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name / Location") },
                            placeholder = { Text("e.g. Bathroom Toothpaste, Kitchen Coffee") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Terracotta,
                                focusedLabelColor = Terracotta,
                                cursorColor = Terracotta
                            )
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space24))

                        // Action Buttons
                        Button(
                            onClick = {
                                val finalName = itemName.ifBlank { "Bathroom Item (${scannedCode?.takeLast(4)})" }
                                onBarcodeRegistered(finalName, scannedCode!!)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WakefullySpacing.Space56),
                            shape = RoundedCornerShape(WakefullySpacing.RadiusButton),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                        ) {
                            Text(
                                text = "SAVE BARCODE TARGET",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))

                        OutlinedButton(
                            onClick = {
                                scannedCode = null
                                itemName = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(WakefullySpacing.RadiusButton)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan a Different Item")
                        }
                    }
                } else if (isManualMode) {
                    // Mode 2: Manual digit entry fallback
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Enter Barcode Digits Manually",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name (e.g. Toothpaste, Coffee)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        var manualDigits by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = manualDigits,
                            onValueChange = { manualDigits = it },
                            label = { Text("Barcode Digits (e.g. 4006381333931)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (manualDigits.isNotBlank()) {
                                    val finalName = itemName.ifBlank { "Barcoded Item (${manualDigits.takeLast(4)})" }
                                    onBarcodeRegistered(finalName, manualDigits)
                                    onDismiss()
                                }
                            },
                            enabled = manualDigits.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WakefullySpacing.Space56),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                        ) {
                            Text("SAVE TARGET BARCODE")
                        }

                        OutlinedButton(
                            onClick = { isManualMode = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Back to Camera Scanner")
                        }
                    }
                } else {
                    // Mode 3: Live CameraX Scanner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!hasCameraPermission) {
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
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                                    Text(
                                        text = "Camera Permission Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ErrorRed
                                    )
                                    Spacer(modifier = Modifier.height(WakefullySpacing.Space8))
                                    Text(
                                        text = "Please allow camera access to scan physical barcodes on your bathroom or kitchen items.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1C1B1F),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(WakefullySpacing.Space16))
                                    Button(
                                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                    ) {
                                        Text("GRANT CAMERA PERMISSION")
                                    }
                                }
                            }
                        } else {
                            // Live CameraX Viewfinder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.05f)
                                    .clip(RoundedCornerShape(WakefullySpacing.RadiusCard))
                                    .background(Color.Black)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(WakefullySpacing.RadiusCard)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
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
                                                if (mediaImage != null && scannedCode == null) {
                                                    val image = InputImage.fromMediaImage(
                                                        mediaImage,
                                                        imageProxy.imageInfo.rotationDegrees
                                                    )
                                                    scanner.process(image)
                                                        .addOnSuccessListener { barcodes ->
                                                            for (barcode in barcodes) {
                                                                val raw = barcode.rawValue
                                                                if (!raw.isNullOrBlank() && scannedCode == null) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    scannedCode = raw
                                                                    scannedFormat = barcode.format
                                                                    itemName = "Scanned Item"
                                                                    break
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

                                // Reticle Framing Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.72f)
                                        .border(
                                            2.dp,
                                            Color.White.copy(alpha = 0.7f),
                                            RoundedCornerShape(WakefullySpacing.RadiusSmall)
                                        )
                                ) {
                                    // Animated Laser Sweep
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .align(Alignment.TopCenter)
                                            .offset(y = (200 * laserY).dp)
                                            .background(TerracottaBright)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(WakefullySpacing.Space16))

                            Text(
                                text = "Point camera at any barcode or QR code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Keep it steady in good lighting",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Switch to manual input fallback
                        Text(
                            text = "Can't scan? Enter digits manually",
                            style = MaterialTheme.typography.labelMedium,
                            color = Terracotta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(WakefullySpacing.RadiusSmall))
                                .clickable { isManualMode = true }
                                .padding(WakefullySpacing.Space8)
                        )

                        Spacer(modifier = Modifier.height(WakefullySpacing.Space12))
                    }
                }
            }
        }
    }
}
