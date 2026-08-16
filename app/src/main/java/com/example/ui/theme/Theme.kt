package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RealityEngineColorScheme = darkColorScheme(
    primary = RealityEngineAmber,
    onPrimary = RealityEngineDarkBg,
    primaryContainer = RealityEngineAmberMuted,
    onPrimaryContainer = RealityEngineAmber,
    secondary = RealityEngineCyan,
    onSecondary = RealityEngineDarkBg,
    secondaryContainer = RealityEngineCyanMuted,
    onSecondaryContainer = RealityEngineCyan,
    tertiary = RealityEngineCrimson,
    onTertiary = RealityEngineTextPrimary,
    background = RealityEngineDarkBg,
    onBackground = RealityEngineTextPrimary,
    surface = RealityEngineSurface,
    onSurface = RealityEngineTextPrimary,
    surfaceVariant = RealityEngineSurfaceElevated,
    onSurfaceVariant = RealityEngineTextSecondary,
    outline = RealityEngineBorder,
    outlineVariant = RealityEngineBorderSubtle,
    error = RealityEngineCrimson,
    onError = RealityEngineDarkBg
)

@Composable
fun RealityEngineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Reality Engine is intentionally an authentic precision dark interface
    val colorScheme = RealityEngineColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = RealityEngineDarkBg.toArgb()
                window.navigationBarColor = RealityEngineDarkBg.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RealityEngineTheme(darkTheme = darkTheme, content = content)
}
