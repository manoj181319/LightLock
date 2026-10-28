package com.lightlock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.drawContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.detectTapGestures
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import com.lightlock.app.ui.theme.LightLockSpacing
import com.lightlock.app.ui.theme.LightLockRadius
import com.lightlock.app.ui.theme.LightLockElevation
import com.lightlock.app.ui.theme.monoLarge
import com.lightlock.app.ui.theme.monoMedium
import com.lightlock.app.ui.theme.monoTabular
import com.lightlock.app.ui.theme.ProvideMonoStyles

/**
 * LightLock Design System Components
 * Based on DESIGN_SYSTEM.md specifications
 */

// ============================================================================
// BUTTONS
// ============================================================================

/**
 * Primary Button - one per view, signal color background
 */
@Composable
fun LightLockPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f),
    )
    val shape = RoundedCornerShape(LightLockRadius.sm)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { mergeDescendants = true },
        enabled = enabled,
        colors = colors,
        shape = shape,
        elevation = ButtonDefaults.elevatedButtonElevation(
            pressedElevation = 4.dp,
            hoveredElevation = 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LightLockSpacing.space4),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(LightLockSpacing.space2))
            label()
        }
    }
}

/**
 * Secondary Button - transparent with border
 */
@Composable
fun LightLockSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    )
    val shape = RoundedCornerShape(LightLockRadius.sm)
    val border = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { mergeDescendants = true },
        enabled = enabled,
        colors = colors,
        shape = shape,
        border = border,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LightLockSpacing.space4),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(LightLockSpacing.space2))
            label()
        }
    }
}

/**
 * Danger Button - transparent with danger border/text
 */
@Composable
fun LightLockDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    val dangerColor = Color(0xFFE03E3E)
    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = dangerColor,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        disabledContentColor = dangerColor.copy(alpha = 0.38f),
    )
    val shape = RoundedCornerShape(LightLockRadius.sm)
    val border = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, dangerColor)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics { mergeDescendants = true },
        enabled = enabled,
        colors = colors,
        shape = shape,
        border = border,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LightLockSpacing.space4),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(LightLockSpacing.space2))
            label()
        }
    }
}

/**
 * Ghost Button - for secondary/destructive actions
 */
@Composable
fun LightLockGhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )
    val shape = RoundedCornerShape(LightLockRadius.sm)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .semantics { mergeDescendants = true },
        enabled = enabled,
        colors = colors,
        shape = shape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        label()
    }
}

// ============================================================================
// STATUS INDICATOR
// ============================================================================

@Composable
fun LightLockStatusDot(
    state: StatusState = StatusState.Idle,
    size: Dp = 8.dp
) {
    val (color, shadow) = when (state) {
        StatusState.Idle -> MaterialTheme.colorScheme.outline to null
        StatusState.Active -> MaterialTheme.colorScheme.primary to "0 0 8dp ${MaterialTheme.colorScheme.primary}"
        StatusState.Warning -> Color(0xFFF5A623) to null
        StatusState.Error -> Color(0xFFE03E3E) to null
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                if (shadow != null) {
                    // Simple shadow via graphics layer - in real app use Box with shadow modifier
                }
            }
            .background(color, CircleShape)
    )
}

enum class StatusState {
    Idle, Active, Warning, Error
}

// ============================================================================
// DATA DISPLAY (Monospace Block)
// ============================================================================

@Composable
fun LightLockDataBlock(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = monoTabular,
    selectable: Boolean = true,
    onCopy: (() -> Unit)? = null,
    copyLabel: String = "Copied!",
) {
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val effectiveStyle = style.copy(
        fontFeatureSettings = if (style.fontFeatureSettings == null) "tnum" else style.fontFeatureSettings
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space3, LightLockSpacing.space4)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f), RoundedCornerShape(LightLockRadius.md))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(LightLockRadius.md))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = effectiveStyle,
                maxLines = 3,
                overflow = androidx.compose.ui.text.overflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
            if (selectable && onCopy != null) {
                IconButton(
                    onClick = {
                        clipboard.setText(text)
                        onCopy()
                        copied = true
                        // Reset copied state after 2 seconds
                        // Note: In real app, use LaunchedEffect with delay
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(LightLockSpacing.space1)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) copyLabel else "Copy to clipboard",
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// PANEL / CARD
// ============================================================================

@Composable
fun LightLockPanel(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
    )
    val elevation = if (elevated) {
        CardDefaults.cardElevation(defaultElevation = 4.dp)
    } else {
        CardDefaults.cardElevation(defaultElevation = 1.dp)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = colors,
        elevation = elevation,
        shape = RoundedCornerShape(LightLockRadius.lg),
        border = androidx.compose.ui.graphics.drawscope.Stroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        content = content
    )
}

@Composable
fun LightLockSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ============================================================================
// CAMERA PREVIEW CONTAINER
// ============================================================================

@Composable
fun LightLockCameraContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(LightLockRadius.lg))
            .clip(RoundedCornerShape(LightLockRadius.lg))
    ) {
        content()
    }
}

// ============================================================================
// WAVEFORM VISUALIZATION
// ============================================================================

@Composable
fun LightLockWaveform(
    modifier: Modifier = Modifier,
    samples: List<Float>,
    height: Dp = 60.dp,
    threshold: Float = 0.5f,
    state: WaveformState = WaveformState.Idle
) {
    ProvideMonoStyles {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f), RoundedCornerShape(0.dp))
                .border(top = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline))
        ) {
            // Canvas for waveform drawing
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val heightPx = size.height
                val sampleCount = samples.size
                if (sampleCount < 2) return@Canvas

                // Grid lines
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val strokeWidth = 0.5.dp.toPx()

                // Vertical grid every ~1.67s at 30fps (50px)
                var x = 0f
                while (x <= width) {
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, heightPx),
                        strokeWidth = strokeWidth
                    )
                    x += 50.dp.toPx()
                }

                // Horizontal grid at 25%, 50%, 75%
                for (yRatio in listOf(0.25f, 0.5f, 0.75f)) {
                    val y = heightPx * yRatio
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = strokeWidth
                    )
                }

                // Threshold line at 50%
                val thresholdY = heightPx * 0.5f
                val warningColor = Color(0xFFF5A623).copy(alpha = 0.4f)
                drawLine(
                    color = warningColor,
                    start = androidx.compose.ui.geometry.Offset(0f, thresholdY),
                    end = androidx.compose.ui.geometry.Offset(width, thresholdY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = android.graphics.PathEffect.makeDashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )

                // Waveform
                val signalColor = MaterialTheme.colorScheme.primary
                val signalWidth = 2.dp.toPx()

                val path = android.graphics.Path()
                var first = true
                for (i in samples.indices) {
                    val xPos = (i / (sampleCount - 1).toFloat()) * width
                    val yPos = heightPx - (samples[i] * heightPx * 0.9f)
                    if (first) {
                        path.moveTo(xPos, yPos)
                        first = false
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                }
                drawPath(
                    path = path,
                    color = signalColor,
                    style = androidx.compose.ui.graphics.DrawingStyle.Stroke(strokeWidth = signalWidth),
                    cap = androidx.compose.ui.graphics.Cap.Round,
                    join = androidx.compose.ui.graphics.Join.Round,
                )

                // Current time indicator (right edge)
                val timeIndicatorWidth = 1.dp.toPx()
                drawLine(
                    color = signalColor,
                    start = androidx.compose.ui.geometry.Offset(width, 0f),
                    end = androidx.compose.ui.geometry.Offset(width, heightPx),
                    strokeWidth = timeIndicatorWidth,
                    pathEffect = android.graphics.PathEffect.makeDashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )
            }

            // State-specific overlays
            when (state) {
                WaveformState.Detecting -> {
                    // Subtle pulse animation would go here
                }
                WaveformState.Preamble -> {
                    // Highlight preamble pattern
                }
                WaveformState.Receiving -> {
                    // Highlight payload region
                }
                WaveformState.ValidFrame -> {
                    // Glow pulse
                }
                WaveformState.InvalidFrame -> {
                    // Danger flash
                }
            }
        }
    }
}

enum class WaveformState {
    Idle, Detecting, Preamble, Receiving, ValidFrame, InvalidFrame
}

// ============================================================================
// QR CODE DISPLAY
// ============================================================================

@Composable
fun LightLockQrCodeDisplay(
    modifier: Modifier = Modifier,
    qrBitmap: android.graphics.Bitmap?,
    size: Dp = 150.dp,
    status: String = "",
    statusActive: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(LightLockRadius.md))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(LightLockRadius.md))
            .padding(LightLockSpacing.space2)
    ) {
        if (qrBitmap != null) {
            androidx.compose.ui.graphics.ImageBitmap.imageResource(
                // We'll need to convert bitmap to ImageBitmap
            )?.let { imageBitmap ->
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap,
                    contentDescription = "QR Code Challenge",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Status text below QR code
    if (status.isNotBlank()) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = if (statusActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LightLockSpacing.space3)
        )
    }
}

// ============================================================================
// CHALLENGE PANEL
// ============================================================================

@Composable
fun LightLockChallengePanel(
    modifier: Modifier = Modifier,
    challengeHex: String,
    expirySeconds: Int,
    onCopy: () -> Unit
) {
    LightLockPanel(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LightLockSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
        ) {
            LightLockSectionTitle("Challenge")
            LightLockDataBlock(
                text = challengeHex,
                onCopy = onCopy
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
            ) {
                Text(
                    text = "Bits: 128",
                    style = monoMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expires: ${expirySeconds / 60}m ${expirySeconds % 60}s",
                    style = monoMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// LOG ENTRY
// ============================================================================

@Composable
fun LightLockLogEntry(
    modifier: Modifier = Modifier,
    time: String,
    message: String,
    type: LogType = LogType.Info
) {
    val color = when (type) {
        LogType.Success -> MaterialTheme.colorScheme.primary
        LogType.Warning -> Color(0xFFF5A623)
        LogType.Error -> Color(0xFFE03E3E)
        LogType.Info -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space2, LightLockSpacing.space4)
            .border(bottom = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, MaterialTheme.colorScheme.outline)),
        horizontalArrangement = Arrangement.spacedBy(LightLockSpacing.space3)
    ) {
        Text(
            text = time,
            style = monoMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = message,
            style = monoMedium,
            color = color,
            maxLines = 2,
            overflow = androidx.compose.ui.text.overflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class LogType {
    Success, Warning, Error, Info
}

// ============================================================================
// PROGRESS BAR (for transmission)
// ============================================================================

@Composable
fun LightLockProgressBar(
    modifier: Modifier = Modifier,
    progress: Float, // 0-100
    label: String
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space2)
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            indicatorColor = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = monoMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
fun LightLockEmptyState(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LightLockSpacing.space8)
            .wrapContentSize(Alignment.CenterHorizontally),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LightLockSpacing.space4)
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        actionLabel?.let { label ->
            LightLockSecondaryButton(
                onClick = onAction!!,
                modifier = Modifier.width(200.dp)
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}