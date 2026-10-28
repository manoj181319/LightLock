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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
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
import com.lightlock.app.ui.theme.monoLarge
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Nonce History Screen - Replay protection nonce tracking
 */
@Composable
fun NonceHistoryScreen(
    onClose: () -> Unit
) {
    // Mock nonce entries
    val nonceEntries = remember {
        mutableStateOf<List<NonceEntry>>(generateMockNonces())
    }

    var filter by remember { mutableStateOf<NonceFilter>(NonceFilter.All) }

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
                            text = "NONCE HISTORY",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Replay protection • 5-min sliding window",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Summary stats
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCard("Active Nonces", "${nonceEntries.value.count { it.status == NonceStatus.Valid }}", "In 5-min window", Icons.Default.History)
                            StatCard("Expired", "${nonceEntries.value.count { it.status == NonceStatus.Expired }}", "Auto-purged", Icons.Default.Info)
                            StatCard("Replay Blocked", "${nonceEntries.value.count { it.status == NonceStatus.Replay }}", "Attack prevented", Icons.Default.Warning)
                        }

                        // Filter chips
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = LightLockSpacing.space2),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)
                        ) {
                            NonceFilterChip(NonceFilter.All, filter) { filter = it }
                            NonceFilterChip(NonceFilter.Valid, filter) { filter = it }
                            NonceFilterChip(NonceFilter.Expired, filter) { filter = it }
                            NonceFilterChip(NonceFilter.Replay, filter) { filter = it }
                        }
                    }
                }

                // Nonce list
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = LightLockSpacing.space2)
                                .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline)),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "#", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                            Text(text = "NONCE (16 bytes)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(3f))
                            Text(text = "TIMESTAMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                            Text(text = "AGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(text = "STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }

                        // List
                        val filteredEntries = nonceEntries.value.filter { entry ->
                            when (filter) {
                                NonceFilter.All -> true
                                NonceFilter.Valid -> entry.status == NonceStatus.Valid
                                NonceFilter.Expired -> entry.status == NonceStatus.Expired
                                NonceFilter.Replay -> entry.status == NonceStatus.Replay
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                        ) {
                            items(filteredEntries) { entry ->
                                NonceRow(entry)
                            }
                        }

                        if (filteredEntries.isEmpty()) {
                            Text(
                                text = "No nonces match filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(LightLockSpacing.space8)
                            )
                        }
                    }
                }

                // Info panel
                LightLockPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(LightLockSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                    ) {
                        LightLockSectionTitle("Replay Protection Info")
                        InfoRow("Window", "5 minutes (300 seconds)")
                        InfoRow("Max Tracked", "1000 nonces (FIFO eviction)")
                        InfoRow("Algorithm", "Sliding window with timestamp validation")
                        InfoRow("Replay Action", "Immediate rejection + security log entry")
                        InfoRow("Storage", "EncryptedSharedPreferences (in-memory cache)")

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = LightLockSpacing.space2),
                            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
                        ) {
                            LightLockSecondaryButton(
                                onClick = { /* Clear history */ },
                                modifier = Modifier.weight(1f),
                                label = { Text("Clear History", style = MaterialTheme.typography.labelLarge) }
                            )
                            LightLockDangerButton(
                                onClick = { /* Test replay detection */ },
                                modifier = Modifier.weight(1f),
                                label = { Text("Test Replay", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

enum class NonceFilter { All, Valid, Expired, Replay }
enum class NonceStatus { Valid, Expired, Replay }

data class NonceEntry(
    val index: Int,
    val nonceHex: String,
    val timestamp: Long, // epoch millis
    val status: NonceStatus
)

fun generateMockNonces(): List<NonceEntry> {
    val now = System.currentTimeMillis()
    val entries = mutableListOf<NonceEntry>()

    // Valid nonces (within 5 min)
    for (i in 0..20) {
        val age = (i * 12000L) // 12 seconds apart
        entries.add(NonceEntry(
            index = 1000 - i,
            nonceHex = String.format("%032x", (System.nanoTime() + i).toLong()),
            timestamp = now - age,
            status = if (age < 300000) NonceStatus.Valid else NonceStatus.Expired
        ))
    }

    // Expired nonces
    for (i in 0..10) {
        entries.add(NonceEntry(
            index = 950 - i,
            nonceHex = String.format("%032x", (System.nanoTime() + 1000 + i).toLong()),
            timestamp = now - 300000 - (i * 60000L),
            status = NonceStatus.Expired
        ))
    }

    // Replay attempts
    for (i in 0..2) {
        entries.add(NonceEntry(
            index = 940 - i,
            nonceHex = entries[i % 20].nonceHex, // Duplicate!
            timestamp = now - (i * 5000L),
            status = NonceStatus.Replay
        ))
    }

    return entries.sortedByDescending { it.timestamp }
}

@Composable
fun NonceRow(entry: NonceEntry) {
    val age = System.currentTimeMillis() - entry.timestamp
    val ageText = when {
        age < 60000 -> "${age / 1000}s ago"
        age < 3600000 -> "${age / 60000}m ago"
        else -> "${age / 3600000}h ago"
    }

    val (statusColor, statusIcon, statusText) = when (entry.status) {
        NonceStatus.Valid -> MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle to "VALID"
        NonceStatus.Expired -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) to Icons.Default.Info to "EXPIRED"
        NonceStatus.Replay -> MaterialTheme.colorScheme.error to Icons.Default.Error to "REPLAY"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space3)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))),
        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${entry.index}", style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
        Text(text = entry.nonceHex, style = monoTabular, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(3f))
        Text(text = Instant.ofEpochMilli(entry.timestamp).toString().substring(11, 19), style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
        Text(text = ageText, style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(statusIcon, contentDescription = statusText, tint = statusColor, modifier = Modifier.size(16.dp))
            Text(text = statusText, style = monoMedium, color = statusColor)
        }
    }
}

@Composable
fun NonceFilterChip(filter: NonceFilter, selected: NonceFilter, onClick: (NonceFilter) -> Unit) {
    val isSelected = (selected == filter)
    val label = when (filter) {
        NonceFilter.All -> "All"
        NonceFilter.Valid -> "Valid"
        NonceFilter.Expired -> "Expired"
        NonceFilter.Replay -> "Replay"
    }

    androidx.compose.material3.FilterChip(
        selected = isSelected,
        onClick = { onClick(filter) },
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
        shape = RoundedCornerShape(LightLockRadius.full)
    )
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
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = monoMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}