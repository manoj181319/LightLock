package com.lightlock.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FontFamily
import androidx.compose.ui.graphics.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.setDecorFitsSystemWindows

/**
 * LightLock Design System Theme
 * Based on DESIGN_SYSTEM.md specifications
 */

// Color System from DESIGN_SYSTEM.md
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D4AA),        // signal - the one accent color
    onPrimary = Color(0xFF0A0F14),      // ink
    primaryContainer = Color(0xFF004336),
    onPrimaryContainer = Color(0xFF00D4AA),
    secondary = Color(0xFF484F58),      // noise-strong
    onSecondary = Color(0xFFE8ECF1),    // ink (dark)
    surface = Color(0xFF161B22),        // bg-elevated
    onSurface = Color(0xFFE8ECF1),      // ink (dark)
    surfaceContainerHighest = Color(0xFF2D333B), // noise
    onSurfaceVariant = Color(0xFFCED4DA), // noise-strong
    error = Color(0xFFE03E3E),          // danger
    onError = Color(0xFFF8F9FA),        // paper
    outline = Color(0xFF484F58),        // noise-strong
    background = Color(0xFF0D1117),     // paper (dark)
    onBackground = Color(0xFFE8ECF1),   // ink (dark)
    surfaceTint = Color(0xFF00D4AA),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00D4AA),        // signal
    onPrimary = Color(0xFF0A0F14),      // ink
    primaryContainer = Color(0xFFE8FDF6),
    onPrimaryContainer = Color(0xFF004336),
    secondary = Color(0xFFCED4DA),      // noise-strong
    onSecondary = Color(0xFF0A0F14),    // ink
    surface = Color(0xFFF8F9FA),        // paper
    onSurface = Color(0xFF0A0F14),      // ink
    surfaceContainerHighest = Color(0xFFE8ECEF), // noise
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFE03E3E),          // danger
    onError = Color(0xFFF8F9FA),
    outline = Color(0xFFCED4DA),        // noise-strong
    background = Color(0xFFF8F9FA),     // paper
    onBackground = Color(0xFF0A0F14),   // ink
    surfaceTint = Color(0xFF00D4AA),
)

// Typography from DESIGN_SYSTEM.md
// Using JetBrains Mono for data/mono, Inter for UI labels
// Fallback to system fonts since custom font resources are not bundled
private val fontFamilyMono = FontFamily.Monospace
private val fontFamilyUI = FontFamily.SansSerif

private val LightLockTypography = androidx.compose.material3.Typography(
    // Display - for hero/primary status
    displayLarge = TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = -0.64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = -0.56.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = -0.48.sp,
    ),

    // Headline - for section headers
    headlineLarge = TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),

    // Title - for panel titles
    titleLarge = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body - for primary content
    bodyLarge = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),

    // Label - for UI labels (uppercase with tracking)
    labelLarge = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamilyUI,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp,
    ),

    // Custom mono styles for data values (nonces, keys, timing)
    // These are additional custom styles we'll use via LocalTextStyle
) { }

// Custom typography extensions for mono data display
@Composable
fun ProvideMonoStyles(content: @Composable () -> Unit) {
    androidx.compose.ui.text.style.TextStyle(
        fontFamily = fontFamilyMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ).let { monoLarge ->
        androidx.compose.ui.text.style.TextStyle(
            fontFamily = fontFamilyMono,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        ).let { monoMedium ->
            androidx.compose.ui.text.style.TextStyle(
                fontFamily = fontFamilyMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                fontFeatureSettings = "tnum" // tabular numbers
            ).let { monoTabular ->
                CompositionLocalProvider(
                    LocalMonoLarge provides monoLarge,
                    LocalMonoMedium provides monoMedium,
                    LocalMonoTabular provides monoTabular,
                    content = content
                )
            }
        }
    }
}

// CompositionLocal for custom mono styles
private val LocalMonoLarge = staticCompositionLocalOf { TextStyle.Default }
private val LocalMonoMedium = staticCompositionLocalOf { TextStyle.Default }
private val LocalMonoTabular = staticCompositionLocalOf { TextStyle.Default }

val monoLarge: TextStyle
    @Composable
    get() = LocalMonoLarge.current

val monoMedium: TextStyle
    @Composable
    get() = LocalMonoMedium.current

val monoTabular: TextStyle
    @Composable
    get() = LocalMonoTabular.current

// Spacing system from DESIGN_SYSTEM.md (base unit: 4px)
object LightLockSpacing {
    val space1 = 4.dp   // 4px - tight internal padding
    val space2 = 8.dp   // 8px - default gap, internal padding
    val space3 = 12.dp  // 12px - medium gap
    val space4 = 16.dp  // 16px - standard padding, card gaps
    val space5 = 20.dp  // 20px - section spacing
    val space6 = 24.dp  // 24px - major section spacing
    val space8 = 32.dp  // 32px - page-level spacing
    val space10 = 40.dp // 40px - hero spacing
}

// Border radius from DESIGN_SYSTEM.md
object LightLockRadius {
    val none = 0.dp
    val sm = 4.dp     // buttons, small chips
    val md = 8.dp     // cards, panels, inputs
    val lg = 12.dp    // major containers, camera preview
    val full = 9999.dp // pills, status dots
}

// Shadows/Elevation from DESIGN_SYSTEM.md
object LightLockElevation {
    val shadow1 = "0 1px 2px rgba(10,15,20,0.06)"      // subtle card lift
    val shadow2 = "0 4px 8px rgba(10,15,20,0.08)"      // standard card
    val shadow3 = "0 8px 24px rgba(10,15,20,0.1)"      // floating panels, modals
    val shadowSignal = "0 0 0 1px #00D4AA, 0 0 16px #00D4AA44" // active signal glow
}

// Motion from DESIGN_SYSTEM.md
object LightLockMotion {
    val durationFast = 120  // ms - micro-interactions, hover
    val durationBase = 200  // ms - standard transitions
    val durationSlow = 350  // ms - panel expand, mode switch
    val easeStandard = "cubic-bezier(0.2, 0, 0.38, 0.9)" // default easing
    val easeOut = "cubic-bezier(0, 0, 0.38, 0.9)"         // exiting, dismissing
    val easeSpring = "cubic-bezier(0.34, 1.56, 0.64, 1)"  // playful, signal pulse
}

// Main Theme Composable
@Composable
fun LightLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Set up edge-to-edge and status bar handling
    SideEffect {
        val activity = context as Activity
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
        activity.window.navigationBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LightLockTypography,
        content = content
    )
}

// Preview
@Preview(showBackground = true)
@Composable
fun LightLockThemePreview() {
    LightLockTheme {
        androidx.compose.material3.Text("LightLock Theme Preview")
    }
}