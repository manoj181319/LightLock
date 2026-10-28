package com.lightlock.app.ui.screen.security

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.lightlock.app.ui.theme.monoTabular
import com.lightlock.app.ui.theme.ProvideMonoStyles
import com.lightlock.app.ui.theme.monoLarge

/**
 * Security Screen - Device key status, session keys, nonce history, pairing
 */
@Composable
fun SecurityScreen(
    onNavigateToKeyManagement: () -> Unit,
    onNavigateToNonceHistory: () -> Unit
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
                            text = "SECURITY",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Key management & security audit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Device Key Status
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                                ) {
                                    Text(text = "DEVICE KEY", style = monoMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "AES-256 master key in Android Keystore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            LightLockStatusDot(state = StatusState.Active, size = 12.dp)
                        }

                        // Key info
                        LightLockDataBlock(
                            text = "Key ID: LK-2024-001-A7F3\nAlgorithm: AES-256-GCM\nCreated: 2024-01-15\nStatus: Active • Hardware-backed",
                            onCopy = { /* Copy key ID */ }
                        )

                        // Key actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            LightLockSecondaryButton(
                                onClick = onNavigateToKeyManagement,
                                modifier = Modifier.weight(1f),
                                label = { Text("Manage Keys", style = MaterialTheme.typography.labelLarge) }
                            )
                            LightLockGhostButton(
                                onClick = { /* Rotate key */ },
                                label = { Text("Rotate Key", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }

                // Session Keys
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Text(text = "ACTIVE SESSION KEYS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        SessionKeyRow(
                            label = "Browser Extension #1",
                            created = "2024-01-20 14:32",
                            expires = "2024-01-20 19:32",
                            status = SessionStatus.Active,
                            onRevoke = { /* Revoke */ }
                        )
                        SessionKeyRow(
                            label = "Browser Extension #2",
                            created = "2024-01-19 09:15",
                            expires = "2024-01-19 14:15",
                            status = SessionStatus.Expired,
                            onRevoke = { /* Revoke */ }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = LightLockSpacing.space2),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            LightLockGhostButton(
                                onClick = { /* Pair new device */ },
                                label = { Text("Pair New Device", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }

                // Nonce History Summary
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "REPLAY PROTECTION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LightLockSecondaryButton(
                                onClick = onNavigateToNonceHistory,
                                modifier = Modifier.width(140.dp),
                                label = { Text("View History", style = MaterialTheme.typography.labelLarge) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCard("Tracked Nonces", "847 / 1000", "5-min window", Icons.Default.History)
                            StatCard("Replays Blocked", "3", "This session", Icons.Default.Warning)
                            StatCard("Oldest Valid", "4:32 ago", "Auto-expires", Icons.Default.Info)
                        }
                    }
                }

                // Security Info
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        LightLockSectionTitle("Security Configuration")
                        SecurityRow("Encryption", "AES-256-GCM", "Hardware-backed keystore", true)
                        SecurityRow("Authentication", "HMAC-SHA256", "Challenge-response", true)
                        SecurityRow("Replay Protection", "Sliding window", "5 min / 1000 nonces", true)
                        SecurityRow("Key Derivation", "HKDF-SHA256", "Per-session keys", true)
                        SecurityRow("Transport", "OOK Optical", "20 bps / Air-gapped", true)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

enum class SessionStatus { Active, Expired, Revoked }

@Composable
fun SessionKeyRow(
    label: String,
    created: String,
    expires: String,
    status: SessionStatus,
    onRevoke: () -> Unit
) {
    val (statusColor, statusText) = when (status) {
        SessionStatus.Active -> MaterialTheme.colorScheme.primary to "ACTIVE"
        SessionStatus.Expired -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) to "EXPIRED"
        SessionStatus.Revoked -> MaterialTheme.colorScheme.error to "REVOKED"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Created: $created", style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Expires: $expires", style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LightLockStatusDot(state = if (status == SessionStatus.Active) StatusState.Active else StatusState.Idle, size = 8.dp)
            Text(text = statusText, style = monoMedium, color = statusColor)
            if (status == SessionStatus.Active) {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Default.Warning, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, subtitle: String, icon: androidx.compose.material.icons.filled.Icon) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(LightLockSpacing.space3)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f), RoundedCornerShape(LightLockRadius.md)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(text = value, style = monoLarge.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SecurityRow(label: String, value: String, description: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = value, style = monoMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            LightLockStatusDot(state = if (enabled) StatusState.Active else StatusState.Idle, size = 8.dp)
        }
    }
}