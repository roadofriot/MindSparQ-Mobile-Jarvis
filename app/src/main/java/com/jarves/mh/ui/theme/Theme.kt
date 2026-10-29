package com.jarves.mh.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PocketOrange = Color(0xFF00F0FF) // Modern MindSparQ Electric Cyan
val PocketBlue = Color(0xFF8B5CF6)   // Electric Violet
val PocketGreen = Color(0xFF10B981)  // Emerald Matrix
val PocketBackground = Color(0xFF08090B) // Obsidian Void
val PocketSurface = Color(0xFF101216)    // Tier 1 Surface
val PocketSurfaceVariant = Color(0xFF161920) // Tier 2 Container
val PocketOutline = Color(0xFF262C36)   // Sleek Border

private val DarkColors = darkColorScheme(
    primary = Color(0xFF00F0FF),
    onPrimary = Color(0xFF050B14),
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = Color(0xFF10B981),
    onTertiary = Color(0xFF003825),
    background = Color(0xFF08090B),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF101216),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF161920),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF262C36),
    outlineVariant = Color(0xFF334155),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
)

enum class AppThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun PocketTheme(themeMode: AppThemeMode = AppThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
