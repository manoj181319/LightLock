package com.lightlock.app.ui.screen.transmit

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.animateIntAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.lightlock.app.ui.components.LightLockComponents.*
import com.lightlock.app.ui.theme.LightLockSpacing
import com.lightlock.app.ui.theme.LightLockRadius
import com.lightlock.app.ui.theme.LightLockElevation
import com.lightlock.app.ui.theme.monoLarge
import com.lightlock.app.ui.theme.monoMedium
import com.lightlock.app.ui.theme.ProvideMonoStyles
// import com.lightlock.app.OokTransmitter // Not used in simulation mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Transmit Progress Screen - Shows 5-6 second countdown then OOK transmission progress
 */
@Composable
fun TransmitProgressScreen(
    challenge: String,
    challengeExpiry: Long,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var phase by remember { mutableStateOf<TransmitPhase>(TransmitPhase.Countdown) }
    var countdown by remember { mutableStateOf(6) } // 5-6 seconds
    var transmissionProgress by remember { mutableStateOf(0f) }
    var transmittedBits by remember { mutableStateOf(0) }
    var totalBits by remember { mutableStateOf(0) }
    var currentByte by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var waveformSamples by remember { mutableStateOf<List<Float>>(List(100) { 0.5f }) }

    // Animation states
    val countdownScale by animateFloatAsState(
        targetValue = if (phase == TransmitPhase.Countdown) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
    )
    val pulseAnimation by animateFloatAsState(
        targetValue = if (phase == TransmitPhase.Transmitting) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 0, easing = { it })
    )

    // Start countdown
    LaunchedEffect(Unit) {
        startCountdown()
    }

    fun startCountdown() {
        scope.launch {
            phase = TransmitPhase.Countdown
            countdown = 6
            while (countdown > 0 && phase == TransmitPhase.Countdown) {
                delay(1000)
                countdown--
            }
            if (phase == TransmitPhase.Countdown) {
                startTransmission()
            }
        }
    }

    fun startTransmission() {
        phase = TransmitPhase.Transmitting

        // Prepare payload: [challenge(16)][hmac(32)][encrypted_credential...]
        // For demo, we'll simulate a transmission
        val payload = preparePayload()
        totalBits = payload.size * 8 + 32 + 8 + 8 + 32 // payload + preamble + length + checksum + end

        scope.launch {
            try {
                // Simulate transmission with progress updates
                var bitsSent = 0
                val bitsPerUpdate = 20 // 20 bps = 1 bit per 50ms, update every 1 second = 20 bits

                while (bitsSent < totalBits && phase == TransmitPhase.Transmitting) {
                    delay(1000) // Update every second
                    bitsSent += bitsPerUpdate
                    transmittedBits = bitsSent.coerceAtMost(totalBits)
                    transmissionProgress = (transmittedBits.toFloat() / totalBits) * 100f

                    // Generate simulated waveform samples
                    waveformSamples = generateWaveformSamples(transmittedBits, totalBits)

                    // Update current byte being transmitted
                    currentByte = bitsSent / 8
                }

                if (phase == TransmitPhase.Transmitting) {
                    phase = TransmitPhase.Complete
                    // Small delay to show completion
                    delay(500)
                    onComplete()
                }
            } catch (e: Exception) {
                phase = TransmitPhase.Error
                errorMessage = e.message ?: "Transmission failed"
            }
        }
    }

    fun preparePayload(): ByteArray {
        // In real app: challenge(16) + hmac(32) + encrypted_credential
        // For demo, return dummy data
        return ByteArray(64) { (it % 256).toByte() }
    }

    fun generateWaveformSamples(sent: Int, total: Int): List<Float> {
        val samples = mutableListOf<Float>()
        val progress = sent.toFloat() / total
        val sampleCount = 100

        for (i in 0 until sampleCount) {
            val sampleProgress = i.toFloat() / sampleCount
            if (sampleProgress < progress) {
                // Transmitted region - show bit pattern
                samples.add(if ((i * 8) % 2 == 0) 0.8f else 0.2f)
            } else if (sampleProgress == progress) {
                // Current bit being transmitted - pulsing
                val pulse = (Math.sin((System.currentTimeMillis() / 100.0) % (2 * Math.PI)) + 1) / 2
                samples.add(0.5f + 0.3f * pulse)
            } else {
                // Not yet transmitted
                samples.add(0.5f)
            }
        }
        return samples
    }

    ProvideMonoStyles {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LightLockSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = LightLockSpacing.space2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                    ) {
                        Text(
                            text = when (phase) {
                                TransmitPhase.Countdown -> "PREPARING TRANSMISSION"
                                TransmitPhase.Transmitting -> "TRANSMITTING VIA OPTICAL"
                                TransmitPhase.Complete -> "TRANSMISSION COMPLETE"
                                TransmitPhase.Error -> "TRANSMISSION FAILED"
                            },
                            style = monoLarge,
                            color = when (phase) {
                                TransmitPhase.Countdown -> MaterialTheme.colorScheme.primary
                                TransmitPhase.Transmitting -> MaterialTheme.colorScheme.primary
                                TransmitPhase.Complete -> MaterialTheme.colorScheme.primary
                                TransmitPhase.Error -> MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = when (phase) {
                                TransmitPhase.Countdown -> "Flip phone to face laptop camera"
                                TransmitPhase.Transmitting -> "OOK @ 20 bps • 50ms/bit"
                                TransmitPhase.Complete -> "Credential sent successfully"
                                TransmitPhase.Error -> errorMessage ?: "Unknown error"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (phase == TransmitPhase.Countdown || phase == TransmitPhase.Transmitting) {
                        IconButton(onClick = {
                            scope.cancel()
                            phase = TransmitPhase.Error
                            errorMessage = "Cancelled by user"
                            onCancel()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Countdown or Progress
                when (phase) {
                    TransmitPhase.Countdown -> {
                        // Large countdown display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(LightLockSpacing.space4),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = countdown.toString(),
                                style = monoLarge.copy(
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 80.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Progress indicator for countdown
                        LightLockProgressBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LightLockSpacing.space4),
                            progress = ((6 - countdown) / 6f) * 100f,
                            label = "Preparing optical link..."
                        )
                    }

                    TransmitPhase.Transmitting -> {
                        // Transmission progress
                        LightLockProgressBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LightLockSpacing.space4),
                            progress = transmissionProgress,
                            label = "$transmittedBits / $totalBits bits • Byte $currentByte"
                        )

                        // Waveform visualization
                        LightLockWaveform(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LightLockSpacing.space4),
                            samples = waveformSamples,
                            height = 80.dp,
                            state = WaveformState.Receiving
                        )

                        // Transmission stats
                        LightLockPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LightLockSpacing.space4)
                        ) {
                            Column(
                                modifier = Modifier.padding(LightLockSpacing.space4),
                                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                            ) {
                                LightLockSectionTitle("Transmission Stats")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatItem("Rate", "20 bps")
                                    StatItem("Bit Duration", "50 ms")
                                    StatItem("Frame", "PREAMBLE|LEN|PAYLOAD|CRC|END")
                                    StatItem("Checksum", "CRC-8 (0x07)")
                                }
                            }
                        }
                    }

                    TransmitPhase.Complete -> {
                        // Success state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LightLockSpacing.space8),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "COMPLETE",
                                style = monoLarge.copy(fontSize = 32.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Credential transmitted and verified",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    TransmitPhase.Error -> {
                        // Error state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LightLockSpacing.space8),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "FAILED",
                                style = monoLarge.copy(fontSize = 32.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMessage ?: "Transmission failed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(LightLockSpacing.space4)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                            ) {
                                LightLockSecondaryButton(
                                    onClick = { startCountdown() },
                                    modifier = Modifier.width(140.dp),
                                    label = { Text("Retry", style = MaterialTheme.typography.labelLarge) }
                                )
                                LightLockDangerButton(
                                    onClick = onCancel,
                                    modifier = Modifier.width(140.dp),
                                    label = { Text("Cancel", style = MaterialTheme.typography.labelLarge) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            scope.cancel()
        }
    }
}

enum class TransmitPhase {
    Countdown, Transmitting, Complete, Error
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
    ) {
        Text(
            text = value,
            style = monoMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Import for monoLarge (already imported above)