package com.first_project.chronoai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.first_project.chronoai.ui1.viewmodel.ThemeMode

// Ultra-Expressive Palette - Adaptive & Pure
val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),     // Default Lavender
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFB4F0AD),   
    onSecondary = Color(0xFF00390A),
    background = Color(0xFF000000),   // Pure Black for OLED
    surface = Color(0xFF0D0D0D),    // Deepest Grey
    surfaceVariant = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    outline = Color(0xFF938F99),
    surfaceTint = Color(0xFFD0BCFF),
)

val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF326B24),   // Deep Green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F0AD),
    onSecondaryContainer = Color(0xFF002203),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    outline = Color(0xFF79747E),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun VyntaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VyntaTypography,
        shapes = VyntaShapes,
        content = content
    )
}
