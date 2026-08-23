package com.spendwise.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.spendwise.app.model.ThemeMode
import com.spendwise.app.model.ThemePalette

data class PaletteColors(
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val primaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryDark: Color
)

fun getColorsForPalette(palette: ThemePalette): PaletteColors {
    return when (palette) {
        ThemePalette.EMERALD -> PaletteColors(
            primaryLight = Color(0xFF059669),
            primaryContainerLight = Color(0xFFD1FAE5),
            onPrimaryContainerLight = Color(0xFF047857),
            secondaryLight = Color(0xFF4F46E5),
            primaryDark = Color(0xFF10B981),
            primaryContainerDark = Color(0xFF047857),
            onPrimaryContainerDark = Color(0xFFECFDF5),
            secondaryDark = Color(0xFF6366F1)
        )
        ThemePalette.OCEAN -> PaletteColors(
            primaryLight = Color(0xFF2563EB),
            primaryContainerLight = Color(0xFFDBEAFE),
            onPrimaryContainerLight = Color(0xFF1E40AF),
            secondaryLight = Color(0xFF0284C7),
            primaryDark = Color(0xFF3B82F6),
            primaryContainerDark = Color(0xFF1D4ED8),
            onPrimaryContainerDark = Color(0xFFEFF6FF),
            secondaryDark = Color(0xFF38BDF8)
        )
        ThemePalette.AMETHYST -> PaletteColors(
            primaryLight = Color(0xFF7C3AED),
            primaryContainerLight = Color(0xFFEDE9FE),
            onPrimaryContainerLight = Color(0xFF5B21B6),
            secondaryLight = Color(0xFFDB2777),
            primaryDark = Color(0xFF8B5CF6),
            primaryContainerDark = Color(0xFF6D28D9),
            onPrimaryContainerDark = Color(0xFFF5F3FF),
            secondaryDark = Color(0xFFF472B6)
        )
        ThemePalette.SUNSET -> PaletteColors(
            primaryLight = Color(0xFFD97706),
            primaryContainerLight = Color(0xFFFEF3C7),
            onPrimaryContainerLight = Color(0xFF92400E),
            secondaryLight = Color(0xFFEA580C),
            primaryDark = Color(0xFFF59E0B),
            primaryContainerDark = Color(0xFFB45309),
            onPrimaryContainerDark = Color(0xFFFFFBEB),
            secondaryDark = Color(0xFFFB923C)
        )
        ThemePalette.ROSE -> PaletteColors(
            primaryLight = Color(0xFFE11D48),
            primaryContainerLight = Color(0xFFFFE4E6),
            onPrimaryContainerLight = Color(0xFF9F1239),
            secondaryLight = Color(0xFF9333EA),
            primaryDark = Color(0xFFF43F5E),
            primaryContainerDark = Color(0xFFBE123C),
            onPrimaryContainerDark = Color(0xFFFFF1F2),
            secondaryDark = Color(0xFFA855F7)
        )
        ThemePalette.NORDIC -> PaletteColors(
            primaryLight = Color(0xFF0D9488),
            primaryContainerLight = Color(0xFFCCFBF1),
            onPrimaryContainerLight = Color(0xFF115E59),
            secondaryLight = Color(0xFF0284C7),
            primaryDark = Color(0xFF14B8A6),
            primaryContainerDark = Color(0xFF0F766E),
            onPrimaryContainerDark = Color(0xFFF0FDFA),
            secondaryDark = Color(0xFF38BDF8)
        )
        ThemePalette.CYBERPUNK -> PaletteColors(
            primaryLight = Color(0xFF0891B2),
            primaryContainerLight = Color(0xFFCFFAFE),
            onPrimaryContainerLight = Color(0xFF155E75),
            secondaryLight = Color(0xFFA855F7),
            primaryDark = Color(0xFF06B6D4),
            primaryContainerDark = Color(0xFF0E7490),
            onPrimaryContainerDark = Color(0xFFECFEFF),
            secondaryDark = Color(0xFFC084FC)
        )
    }
}

fun buildColorScheme(
    darkTheme: Boolean,
    isAmoled: Boolean,
    palette: ThemePalette
): ColorScheme {
    val colors = getColorsForPalette(palette)
    return if (darkTheme) {
        val bg = if (isAmoled) AmoledBlack else DarkSurfaceBackground
        val surface = if (isAmoled) Color(0xFF0A0A0A) else DarkSurfaceCard
        val surfaceElevated = if (isAmoled) Color(0xFF161616) else DarkSurfaceElevated
        val outline = if (isAmoled) Color(0xFF262626) else DarkBorder

        darkColorScheme(
            primary = colors.primaryDark,
            onPrimary = Color.White,
            primaryContainer = colors.primaryContainerDark,
            onPrimaryContainer = colors.onPrimaryContainerDark,
            secondary = colors.secondaryDark,
            onSecondary = Color.White,
            error = Rose500,
            onError = Color.White,
            background = bg,
            onBackground = DarkTextPrimary,
            surface = surface,
            onSurface = DarkTextPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = outline
        )
    } else {
        lightColorScheme(
            primary = colors.primaryLight,
            onPrimary = Color.White,
            primaryContainer = colors.primaryContainerLight,
            onPrimaryContainer = colors.onPrimaryContainerLight,
            secondary = colors.secondaryLight,
            onSecondary = Color.White,
            error = Rose600,
            onError = Color.White,
            background = LightSurfaceBackground,
            onBackground = LightTextPrimary,
            surface = LightSurfaceCard,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceElevated,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder
        )
    }
}

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

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildColorScheme(darkTheme = darkTheme, isAmoled = isAmoledBlack, palette = themePalette)
    }

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
