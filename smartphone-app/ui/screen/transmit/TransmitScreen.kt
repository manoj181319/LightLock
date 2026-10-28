package com.lightlock.app.ui.screen.transmit

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.lightlock.app.ui.components.LightLockComponents.*
import com.lightlock.app.ui.theme.LightLockSpacing
import com.lightlock.app.ui.theme.LightLockRadius
import com.lightlock.app.ui.theme.LightLockElevation
import com.lightlock.app.ui.theme.monoMedium
import com.lightlock.app.ui.theme.ProvideMonoStyles
import com.lightlock.app.CryptoManager
import com.lightlock.app.CryptoUtils

/**
 * Transmit Screen - Main screen for password transmission
 * Shows list of stored passwords, allows adding new ones, and initiates QR scanning
 */
@Composable
fun TransmitScreen(
    onNavigateToQrScanner: () -> Unit
) {
    val context = LocalContext.current
    val cryptoManager = remember { CryptoManager(context) }

    // Password entries: (label, username, password)
    var passwordEntries by remember { mutableStateOf<List<PasswordEntry>>(emptyList()) }
    var showingAddDialog by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var generatePassword by remember { mutableStateOf(false) }

    // Load passwords on first composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadPasswords()
    }

    fun loadPasswords() {
        // In real app, this would load from EncryptedSharedPreferences
        // For now, use mock data
        passwordEntries = listOf(
            PasswordEntry("GitHub", "user@github.com", "ghp_abc123xyz"),
            PasswordEntry("AWS Console", "admin@company.com", "AwsSecretKey123!"),
            PasswordEntry("Database", "db_admin", "DbPass#2024"),
        )
    }

    fun addPassword() {
        val password = if (generatePassword) {
            CryptoUtils.generateSecurePassword(16)
        } else {
            newPassword
        }
        if (newLabel.isNotBlank() && password.isNotBlank()) {
            // In real app, save to EncryptedSharedPreferences
            passwordEntries = passwordEntries + PasswordEntry(newLabel, newUsername, password)
            showingAddDialog = false
            newLabel = ""
            newUsername = ""
            newPassword = ""
            generatePassword = false
        }
    }

    fun deletePassword(index: Int) {
        passwordEntries = passwordEntries.toMutableList().apply { removeAt(index) }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
                    ) {
                        Text(
                            text = "TRANSMIT",
                            style = monoLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select a credential to transmit via optical link",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LightLockPrimaryButton(
                        onClick = { showingAddDialog = true },
                        modifier = Modifier.width(140.dp),
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                Text("Add", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    )
                }

                // Password list
                if (passwordEntries.isEmpty()) {
                    LightLockEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                        },
                        title = "No Credentials Stored",
                        subtitle = "Add your first credential to begin secure optical transmission",
                        actionLabel = "Add Credential",
                        onAction = { showingAddDialog = true }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)
                    ) {
                        passwordEntries.forEachIndexed { index, entry ->
                            PasswordEntryCard(
                                entry = entry,
                                index = index,
                                onTransmit = {
                                    // Navigate to QR scanner with selected password
                                    onNavigateToQrScanner()
                                },
                                onDelete = { deletePassword(index) }
                            )
                        }
                    }
                }

                // Spacer for FAB area
                Spacer(modifier = Modifier.height(LightLockSpacing.space8))
            }
        }
    }

    // Add Credential Dialog
    if (showingAddDialog) {
        AddCredentialDialog(
            onDismiss = { showingAddDialog = false },
            onAdd = addPassword,
            label = newLabel,
            onLabelChange = { newLabel = it },
            username = newUsername,
            onUsernameChange = { newUsername = it },
            password = newPassword,
            onPasswordChange = { newPassword = it },
            generatePassword = generatePassword,
            onGenerateChange = { generatePassword = it }
        )
    }
}

data class PasswordEntry(
    val label: String,
    val username: String,
    val password: String
)

@Composable
fun PasswordEntryCard(
    entry: PasswordEntry,
    index: Int,
    onTransmit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(LightLockSpacing.space2)

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(LightLockRadius.md),
        border = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onTransmit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LightLockSpacing.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.overflow.Ellipsis
                )
                Text(
                    text = entry.username,
                    style = monoMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.overflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy password button
                IconButton(
                    onClick = {
                        // Copy to clipboard
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Transmit indicator
                Box(
                    modifier = Modifier
                        .padding(LightLockSpacing.space2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Transmit via optical",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete credential",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddCredentialDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    label: String,
    onLabelChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    generatePassword: Boolean,
    onGenerateChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LightLockSpacing.space4)
                .align(Alignment.Center),
            shape = RoundedCornerShape(LightLockRadius.lg),
            elevation = CardDefaults.cardElevation(defaultElevation = LightLockElevation.shadow3)
        ) {
            Column(
                modifier = Modifier.padding(LightLockSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
            ) {
                Text(
                    text = "ADD CREDENTIAL",
                    style = monoLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Label field
                Column(verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)) {
                    Text(
                        text = "LABEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.TextField(
                        value = label,
                        onValueChange = onLabelChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., GitHub, AWS, Database") },
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        )
                    )
                }

                // Username field
                Column(verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space1)) {
                    Text(
                        text = "USERNAME (OPTIONAL)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.TextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("user@example.com") },
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        )
                    )
                }

                // Password field + Generate toggle
                Column(verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PASSWORD",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.material3.Switch(
                            checked = generatePassword,
                            onCheckedChange = onGenerateChange,
                            modifier = Modifier.padding(start = LightLockSpacing.space4),
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    Text(
                        text = if (generatePassword) "Auto-generate secure password" else "Enter password manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (!generatePassword) {
                        androidx.compose.material3.TextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.material3.PasswordVisualTransformation(),
                            placeholder = { Text("Enter password") },
                            colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            )
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LightLockSecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        label = { Text("Cancel", style = MaterialTheme.typography.labelLarge) }
                    )
                    LightLockPrimaryButton(
                        onClick = onAdd,
                        modifier = Modifier.weight(1f),
                        enabled = label.isNotBlank() && (generatePassword || password.isNotBlank()),
                        label = { Text("Save", style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }
        }
    }
}

// Import for monoLarge
import com.lightlock.app.ui.theme.monoLarge