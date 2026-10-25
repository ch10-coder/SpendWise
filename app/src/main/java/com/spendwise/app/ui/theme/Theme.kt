package com.spendwise.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.spendwise.app.model.ThemeMode
import com.spendwise.app.model.ThemePalette

/**
 * Bharat Modern Ledger Design System
 * Inspired by modern Indian fintech applications (CRED, Jupiter, Fi Money).
 * Clean, high-contrast, lightning-fast Light & Dark themes.
 */

// Core Brand Colors
val BharatBlue = Color(0xFF0052FF)           // Electric Action Blue
val BharatBlueDark = Color(0xFF3B82F6)       // Vibrant Blue for Dark Mode
val SaffronPulse = Color(0xFFFF3D00)         // Saffron Accent for "Made in India"
val SaffronPulseDark = Color(0xFFFF5722)     // Warm Saffron for Dark Mode

// Functional Cashflow Accents
val InflowGreen = Color(0xFF00C853)          // Crisp Inflow Green
val InflowGreenDark = Color(0xFF10B981)      // Emerald Inflow
val OutflowRed = Color(0xFFDC2626)           // Crisp Outflow Red
val OutflowRedDark = Color(0xFFEF4444)       // Coral Outflow Red

// Hero card gradient (balance card, lock screen)
val HeroGradientLight = listOf(Color(0xFF1E1B4B), Color(0xFF0052FF))
val HeroGradientDark = listOf(Color(0xFF111A3A), Color(0xFF1D4ED8))
val HeroInflowTint = Color(0xFF86EFAC)
val HeroOutflowTint = Color(0xFFFCA5A5)

// Dark Canvas (Stitch Midnight Indigo)
val MidnightDarkBackground = Color(0xFF0A0F1D)
val MidnightDarkSurface = Color(0xFF161E2E)
val MidnightDarkSurfaceElevated = Color(0xFF1E293B)
val MidnightDarkBorder = Color(0xFF283044)
val MidnightDarkTextPrimary = Color(0xFFF8FAFC)
val MidnightDarkTextSecondary = Color(0xFF94A3B8)

// Light Canvas (Optical Paper White)
val PaperLightBackground = Color(0xFFF8FAFC)
val PaperLightSurface = Color(0xFFFFFFFF)
val PaperLightSurfaceElevated = Color(0xFFF1F5F9)
val PaperLightBorder = Color(0xFFE2E8F0)
val PaperLightTextPrimary = Color(0xFF0F172A)
val PaperLightTextSecondary = Color(0xFF64748B)

val BharatDarkColorScheme: ColorScheme = darkColorScheme(
    primary = BharatBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SaffronPulseDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF431407),
    onSecondaryContainer = Color(0xFFFFCCBC),
    tertiary = InflowGreenDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = OutflowRedDark,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = MidnightDarkBackground,
    onBackground = MidnightDarkTextPrimary,
    surface = MidnightDarkSurface,
    onSurface = MidnightDarkTextPrimary,
    surfaceVariant = MidnightDarkSurfaceElevated,
    onSurfaceVariant = MidnightDarkTextSecondary,
    outline = MidnightDarkBorder,
    outlineVariant = Color(0xFF1E293B),
    // Material 3 surface roles (dialogs, menus, pickers, sheets, snackbars). Without these the
    // components fall back to the stock lavender baseline and look off-brand.
    surfaceTint = BharatBlueDark,
    surfaceBright = MidnightDarkSurfaceElevated,
    surfaceDim = MidnightDarkBackground,
    surfaceContainerLowest = MidnightDarkBackground,
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = MidnightDarkSurface,
    surfaceContainerHigh = MidnightDarkSurfaceElevated,
    surfaceContainerHighest = MidnightDarkBorder,
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = BharatBlue
)

val BharatLightColorScheme: ColorScheme = lightColorScheme(
    primary = BharatBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = SaffronPulse,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBE5),
    onSecondaryContainer = Color(0xFF992400),
    tertiary = InflowGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    error = OutflowRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = PaperLightBackground,
    onBackground = PaperLightTextPrimary,
    surface = PaperLightSurface,
    onSurface = PaperLightTextPrimary,
    surfaceVariant = PaperLightSurfaceElevated,
    onSurfaceVariant = PaperLightTextSecondary,
    outline = PaperLightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    // Material 3 surface roles (dialogs, menus, pickers, sheets, snackbars) kept on the clean
    // white / slate palette instead of the stock lavender baseline.
    surfaceTint = BharatBlue,
    surfaceBright = PaperLightSurface,
    surfaceDim = Color(0xFFE2E8F0),
    surfaceContainerLowest = PaperLightSurface,
    surfaceContainerLow = PaperLightSurface,
    surfaceContainer = PaperLightBackground,
    surfaceContainerHigh = PaperLightSurface,
    surfaceContainerHighest = PaperLightBorder,
    inverseSurface = PaperLightTextPrimary,
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Color(0xFF93C5FD)
)

@Composable
fun SpendWiseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.EMERALD,
    isAmoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) BharatDarkColorScheme else BharatLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
