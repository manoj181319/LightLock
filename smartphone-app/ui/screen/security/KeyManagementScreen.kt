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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
 * Key Management Screen - View and manage cryptographic keys
 */
@Composable
fun KeyManagementScreen(
    onClose: () -> Unit
) {
    var showKeyMaterial by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var autoRotateEnabled by remember { mutableStateOf(false) }

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
                            text = "KEY MANAGEMENT",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Master key & session key configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Master Key Section
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
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                                ) {
                                    Text(text = "MASTER KEY", style = monoMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "AES-256 • Hardware-backed • Android Keystore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            LightLockStatusDot(state = StatusState.Active, size = 10.dp)
                        }

                        // Key details
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            KeyDetailRow("Key ID", "LK-2024-001-A7F3", showKeyMaterial)
                            KeyDetailRow("Algorithm", "AES-256-GCM", false)
                            KeyDetailRow("Key Size", "256 bits", false)
                            KeyDetailRow("Created", "2024-01-15 10:30:00 UTC", false)
                            KeyDetailRow("Key Origin", "Hardware-backed (StrongBox)", false)
                            KeyDetailRow("Attestation", "Verified • StrongBox certified", false)
                        }

                        // Key material (hidden by default)
                        if (showKeyMaterial) {
                            LightLockDataBlock(
                                text = "Encrypted Key Blob: A1B2C3D4E5F6... (encrypted by Keystore)",
                                onCopy = { /* Copy */ }
                            )
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            LightLockSecondaryButton(
                                onClick = { showKeyMaterial = !showKeyMaterial },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (showKeyMaterial) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                        Text(if (showKeyMaterial) "Hide Key Info" else "Show Key Info", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            )
                            LightLockSecondaryButton(
                                onClick = { /* Export public key */ },
                                modifier = Modifier.weight(1f),
                                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp)) },
                                label = { Text("Export Public", style = MaterialTheme.typography.labelLarge) }
                            )
                            LightLockDangerButton(
                                onClick = { /* Rotate key with confirmation */ },
                                modifier = Modifier.weight(1f),
                                icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                label = { Text("Rotate Key", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }

                // Security Settings
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Text(text = "SECURITY SETTINGS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        SettingRow(
                            title = "Biometric Authentication",
                            subtitle = "Require fingerprint/face for key access",
                            icon = Icons.Default.Fingerprint,
                            value = biometricEnabled,
                            onValueChange = { biometricEnabled = it }
                        )

                        SettingRow(
                            title = "Auto Key Rotation",
                            subtitle = "Rotate master key every 90 days",
                            icon = Icons.Default.Refresh,
                            value = autoRotateEnabled,
                            onValueChange = { autoRotateEnabled = it }
                        )

                        SettingRow(
                            title = "Secure Hardware Required",
                            subtitle = "Only use StrongBox/TEE backed keys",
                            icon = Icons.Default.Security,
                            value = true,
                            onValueChange = { /* Always true */ },
                            enabled = false
                        )
                    }
                }

                // Session Key Derivation
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Text(text = "SESSION KEY DERIVATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            DerivationStep("1", "Master Key (AES-256)", "Stored in Android Keystore", true)
                            DerivationStep("2", "HKDF-SHA256", "Extract + Expand with salt", true)
                            DerivationStep("3", "Session Key (AES-256-GCM)", "Per-transmission unique key", true)
                            DerivationStep("4", "IV/Nonce", "12-byte random per encryption", true)
                        }

                        LightLockDataBlock(
                            text = "Salt: LIGHTLOCK_SESSION_v1\nInfo: challenge_nonce || device_id",
                            onCopy = { /* Copy */ }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Danger zone
                LightLockPanel(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                            ) {
                                Text(text = "DANGER ZONE", style = monoMedium, color = MaterialTheme.colorScheme.error)
                                Text(text = "These actions cannot be undone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            LightLockDangerButton(
                                onClick = { /* Wipe all keys with confirmation */ },
                                modifier = Modifier.weight(1f),
                                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                label = { Text("Wipe All Keys", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyDetailRow(label: String, value: String, isSecret: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = value, style = if (isSecret) monoTabular else monoMedium, color = MaterialTheme.colorScheme.onSurface)
            if (isSecret) {
                IconButton(onClick = { /* Copy */ }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.material.icons.filled.Icon,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = value,
            onCheckedChange = if (enabled) onValueChange else null,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                disabledTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
fun DerivationStep(number: String, title: String, description: String, completed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = monoMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.background(
                modifier = Modifier.size(28.dp),
                color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = androidx.compose.foundation.shape.CircleShape
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (completed) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}