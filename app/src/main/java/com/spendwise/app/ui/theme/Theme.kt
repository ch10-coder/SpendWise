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
            primaryLight = Color(0xFF006C4C),
            primaryContainerLight = Color(0xFF8EF7C7),
            onPrimaryContainerLight = Color(0xFF002114),
            secondaryLight = Color(0xFF4C6357),
            primaryDark = Color(0xFF71DAAC),
            primaryContainerDark = Color(0xFF005138),
            onPrimaryContainerDark = Color(0xFF8EF7C7),
            secondaryDark = Color(0xFFB3CCBE)
        )
        ThemePalette.OCEAN -> PaletteColors(
            primaryLight = Color(0xFF0B57D0),
            primaryContainerLight = Color(0xFFD3E3FD),
            onPrimaryContainerLight = Color(0xFF041E49),
            secondaryLight = Color(0xFF00639B),
            primaryDark = Color(0xFFA8C7FA),
            primaryContainerDark = Color(0xFF0842A0),
            onPrimaryContainerDark = Color(0xFFD3E3FD),
            secondaryDark = Color(0xFF91CDFF)
        )
        ThemePalette.AMETHYST -> PaletteColors(
            primaryLight = Color(0xFF6750A4),
            primaryContainerLight = Color(0xFFEADDFF),
            onPrimaryContainerLight = Color(0xFF21005D),
            secondaryLight = Color(0xFF625B71),
            primaryDark = Color(0xFFD0BCFF),
            primaryContainerDark = Color(0xFF4F378B),
            onPrimaryContainerDark = Color(0xFFEADDFF),
            secondaryDark = Color(0xFFCCC2DC)
        )
        ThemePalette.SUNSET -> PaletteColors(
            primaryLight = Color(0xFF7D5700),
            primaryContainerLight = Color(0xFFFFDEA3),
            onPrimaryContainerLight = Color(0xFF281900),
            secondaryLight = Color(0xFF6C5C3F),
            primaryDark = Color(0xFFFFBA28),
            primaryContainerDark = Color(0xFF5F4100),
            onPrimaryContainerDark = Color(0xFFFFDEA3),
            secondaryDark = Color(0xFFD7C4A0)
        )
        ThemePalette.ROSE -> PaletteColors(
            primaryLight = Color(0xFF984061),
            primaryContainerLight = Color(0xFFFFD9E2),
            onPrimaryContainerLight = Color(0xFF3E001D),
            secondaryLight = Color(0xFF74565F),
            primaryDark = Color(0xFFFFB0C8),
            primaryContainerDark = Color(0xFF7B2949),
            onPrimaryContainerDark = Color(0xFFFFD9E2),
            secondaryDark = Color(0xFFE2BDC6)
        )
        ThemePalette.NORDIC -> PaletteColors(
            primaryLight = Color(0xFF006A6A),
            primaryContainerLight = Color(0xFF70F7F7),
            onPrimaryContainerLight = Color(0xFF002020),
            secondaryLight = Color(0xFF4A6363),
            primaryDark = Color(0xFF4DDADA),
            primaryContainerDark = Color(0xFF004F4F),
            onPrimaryContainerDark = Color(0xFF70F7F7),
            secondaryDark = Color(0xFFB1CCCC)
        )
        ThemePalette.CYBERPUNK -> PaletteColors(
            primaryLight = Color(0xFF00677F),
            primaryContainerLight = Color(0xFFB6EAFF),
            onPrimaryContainerLight = Color(0xFF001F28),
            secondaryLight = Color(0xFF4C626B),
            primaryDark = Color(0xFF5BD5FB),
            primaryContainerDark = Color(0xFF004D60),
            onPrimaryContainerDark = Color(0xFFB6EAFF),
            secondaryDark = Color(0xFFB3CAD4)
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
        val surface = if (isAmoled) AmoledSurfaceCard else DarkSurfaceCard
        val surfaceElevated = if (isAmoled) AmoledSurfaceElevated else DarkSurfaceElevated
        val outline = if (isAmoled) AmoledBorder else DarkBorderOutline
        val outlineVariant = if (isAmoled) AmoledBorder else DarkBorder

        darkColorScheme(
            primary = colors.primaryDark,
            onPrimary = Color(0xFF003322),
            primaryContainer = colors.primaryContainerDark,
            onPrimaryContainer = colors.onPrimaryContainerDark,
            secondary = colors.secondaryDark,
            onSecondary = Color(0xFF1E3328),
            secondaryContainer = colors.secondaryDark.copy(alpha = 0.2f),
            onSecondaryContainer = colors.secondaryDark,
            error = GoogleRedDark,
            onError = Color(0xFF601410),
            errorContainer = GoogleRedContainerDark,
            onErrorContainer = GoogleOnRedContainerDark,
            background = bg,
            onBackground = DarkTextPrimary,
            surface = surface,
            onSurface = DarkTextPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        lightColorScheme(
            primary = colors.primaryLight,
            onPrimary = Color.White,
            primaryContainer = colors.primaryContainerLight,
            onPrimaryContainer = colors.onPrimaryContainerLight,
            secondary = colors.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = colors.primaryContainerLight.copy(alpha = 0.6f),
            onSecondaryContainer = colors.primaryLight,
            error = GoogleRed,
            onError = Color.White,
            errorContainer = GoogleRedContainer,
            onErrorContainer = GoogleOnRedContainer,
            background = LightSurfaceBackground,
            onBackground = LightTextPrimary,
            surface = LightSurfaceCard,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceElevated,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorderOutline,
            outlineVariant = LightBorder
        )
    }
}

@Composable
fun SpendWiseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.OCEAN,
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
