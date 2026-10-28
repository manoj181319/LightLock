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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.lightlock.app.ui.theme.ProvideMonoStyles
import com.lightlock.app.ui.theme.monoLarge

/**
 * Receive Screen - For future optical reception (placeholder)
 */
@Composable
fun ReceiveScreen(
    onNavigateToDetail: () -> Unit
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
                            text = "RECEIVE",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Optical reception - coming soon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status card
                LightLockPanel(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "OPTICAL RECEIVER",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "The optical receiver will decode OOK signals at 20 bps from a transmitting device. This feature is planned for a future release.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(LightLockSpacing.space4)
                        )

                        // Specs preview
                        LightLockPanel(
                            modifier = Modifier.fillMaxWidth(),
                            elevated = true
                        ) {
                            Column(
                                modifier = Modifier.padding(LightLockSpacing.space4),
                                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                            ) {
                                LightLockSectionTitle("Planned Specifications")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SpecItem("Modulation", "OOK (On-Off Keying)")
                                    SpecItem("Rate", "20 bps")
                                    SpecItem("Bit Duration", "50 ms")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SpecItem("Frame Sync", "0xAAAAAAAA Preamble")
                                    SpecItem("Boundary", "0x55555555 End Marker")
                                    SpecItem("Checksum", "CRC-8 (poly 0x07)")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SpecItem("Camera", "CameraX + ML Kit")
                                    SpecItem("Resolution", "720p @ 30 fps")
                                    SpecItem("ROI", "Center 60%")
                                }
                            }
                        }

                        // Action button (disabled for now)
                        LightLockSecondaryButton(
                            onClick = { /* Future: start listening */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = LightLockSpacing.space4),
                            enabled = false,
                            label = {
                                Text("Start Listening (Coming Soon)", style = MaterialTheme.typography.labelLarge)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Info
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LightLockSpacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "This app currently only transmits credentials. The receive functionality requires a companion device or browser extension to send optical signals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
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