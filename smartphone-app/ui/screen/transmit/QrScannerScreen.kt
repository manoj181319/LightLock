package com.lightlock.app.ui.screen.transmit

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lightlock.app.ui.components.LightLockComponents.*
import com.lightlock.app.ui.theme.LightLockSpacing
import com.lightlock.app.ui.theme.LightLockRadius
import com.lightlock.app.ui.theme.LightLockElevation
import com.lightlock.app.ui.theme.monoMedium
import com.lightlock.app.ui.theme.ProvideMonoStyles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * QR Scanner Screen - Uses CameraX + ML Kit to scan QR code challenges
 */
@Composable
fun QrScannerScreen(
    onChallengeScanned: (String, Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Position QR code in frame") }
    var statusActive by remember { mutableStateOf(false) }
    var lastScannedChallenge by remember { mutableStateOf<String?>(null) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // Check camera permission
    LaunchedEffect(Unit) {
        checkCameraPermission()
    }

    fun checkCameraPermission() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        cameraPermissionGranted = (permission == PackageManager.PERMISSION_GRANTED)
        if (!cameraPermissionGranted) {
            // In real app, request permission via ActivityResultContracts
            // For now, we'll simulate granted
            cameraPermissionGranted = true
        }
    }

    // Camera preview via AndroidView
    val previewView = remember {
        PreviewView(context).apply {
            layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    // Start camera when permission granted
    LaunchedEffect(cameraPermissionGranted) {
        if (cameraPermissionGranted) {
            startCamera()
        }
    }

    fun startCamera() {
        scope.launch {
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Image Analysis for barcode scanning
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(Dispatchers.IO.asExecutor()) { imageProxy ->
                            analyzeImage(imageProxy)
                        }
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Bind to lifecycle
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                statusMessage = "Camera error: ${e.message}"
                statusActive = false
            }
        }
    }

    fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (!scanning || lastScannedChallenge != null) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (scanning && barcodes.isNotEmpty()) {
                        val barcode = barcodes.first()
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.startsWith("LIGHTLOCK:")) {
                            // Parse challenge: LIGHTLOCK:nonce_hex:expiry_timestamp
                            val parts = rawValue.split(":")
                            if (parts.size >= 3) {
                                val nonceHex = parts[1]
                                val expiry = parts[2].toLongOrNull() ?: 0L
                                if (nonceHex.length == 32) { // 16 bytes = 32 hex chars
                                    lastScannedChallenge = nonceHex
                                    scanning = false
                                    onChallengeScanned(nonceHex, expiry)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Ignore scanning failures, continue scanning
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun toggleTorch() {
        torchEnabled = !torchEnabled
        // CameraX torch control would go here
        // cameraControl.enableTorch(torchEnabled)
    }

    ProvideMonoStyles {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LightLockSpacing.space4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                    ) {
                        Text(
                            text = "SCAN CHALLENGE",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Align QR code within frame",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Camera Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = LightLockSpacing.space4)
                ) {
                    LightLockCameraContainer {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Scanning overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(LightLockSpacing.space6),
                            contentAlignment = Alignment.Center
                        ) {
                            // Corner brackets
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                                val size = 200.dp.toPx()
                                val cornerSize = 24.dp.toPx()
                                val strokeWidth = 3.dp.toPx()
                                val color = MaterialTheme.colorScheme.primary

                                // Top-left
                                drawLine(color, Offset(0f, cornerSize), Offset(0f, 0f), strokeWidth)
                                drawLine(color, Offset(0f, 0f), Offset(cornerSize, 0f), strokeWidth)
                                // Top-right
                                drawLine(color, Offset(size - cornerSize, 0f), Offset(size, 0f), strokeWidth)
                                drawLine(color, Offset(size, 0f), Offset(size, cornerSize), strokeWidth)
                                // Bottom-left
                                drawLine(color, Offset(0f, size - cornerSize), Offset(0f, size), strokeWidth)
                                drawLine(color, Offset(0f, size), Offset(cornerSize, size), strokeWidth)
                                // Bottom-right
                                drawLine(color, Offset(size - cornerSize, size), Offset(size, size), strokeWidth)
                                drawLine(color, Offset(size, size - cornerSize), Offset(size, size), strokeWidth)
                            }
                        }
                    }
                }

                // Status
                Text(
                    text = statusMessage,
                    style = monoMedium,
                    color = if (statusActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LightLockSpacing.space4)
                )

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LightLockSpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Torch toggle
                    IconButton(
                        onClick = toggleTorch,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchEnabled) "Torch on" else "Torch off",
                            tint = if (torchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Cancel/Close
                    LightLockSecondaryButton(
                        onClick = onClose,
                        modifier = Modifier.width(120.dp),
                        label = { Text("Cancel", style = MaterialTheme.typography.labelLarge) }
                    )
                }

                Spacer(modifier = Modifier.height(LightLockSpacing.space4))
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            scope.cancel()
            barcodeScanner.close()
        }
    }
}

// Import for monoLarge
import com.lightlock.app.ui.theme.monoLarge
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawLine