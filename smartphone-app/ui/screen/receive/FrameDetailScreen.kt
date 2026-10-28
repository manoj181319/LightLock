package com.lightlock.app.ui.screen.receive

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightlock.app.ui.components.LightLockComponents.*
import com.lightlock.app.ui.theme.LightLockSpacing
import com.lightlock.app.ui.theme.LightLockRadius
import com.lightlock.app.ui.theme.LightLockElevation
import com.lightlock.app.ui.theme.monoMedium
import com.lightlock.app.ui.theme.monoTabular
import com.lightlock.app.ui.theme.ProvideMonoStyles

/**
 * Frame Detail Screen - Shows details of a received frame (placeholder)
 */
@Composable
fun FrameDetailScreen(
    onClose: () -> Unit
) {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                    ) {
                        Text(
                            text = "FRAME DETAIL",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Received frame analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Frame status
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "VALID FRAME",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Frame checksum verified • Payload decrypted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Frame structure breakdown
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        LightLockSectionTitle("Frame Structure")
                        FrameField("Preamble (32 bits)", "0xAAAAAAAA", "Sync pattern", true)
                        FrameField("Length (8 bits)", "0x40", "64 bytes payload", true)
                        FrameField("Payload", "64 bytes", "Encrypted credential + HMAC", true)
                        FrameField("Checksum (8 bits)", "0x73", "CRC-8 verified", true)
                        FrameField("End Marker (32 bits)", "0x55555555", "Boundary detected", true)
                    }
                }

                // Decoded payload
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        LightLockSectionTitle("Decoded Payload")
                        PayloadField("Challenge Nonce", "A1B2C3D4E5F6...", "16 bytes")
                        PayloadField("HMAC-SHA256", "F7E8D9C0...", "32 bytes")
                        PayloadField("Encrypted Credential", "8B9A...", "16+ bytes (AES-256-GCM)")
                    }
                }

                // Timing info
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        LightLockSectionTitle("Reception Timing")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("Duration", "3.2 s")
                            StatItem("Bits Received", "512")
                            StatItem("Bit Errors", "0")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("SNR", "24.5 dB")
                            StatItem("Threshold", "Adaptive")
                            StatItem("Sync Lock", "Preamble @ bit 0")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FrameField(label: String, value: String, description: String, valid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = monoTabular, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Icon(
            imageVector = if (valid) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (valid) "Valid" else "Invalid",
            tint = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PayloadField(label: String, value: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = monoTabular, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = size, style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
    ) {
        Text(text = value, style = monoMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Import for monoLarge
import com.lightlock.app.ui.theme.monoLarge