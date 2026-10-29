package com.mindsparq.jarvis.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Obsidian Kinetic Dark Palette
val ObsidianBgTier0 = Color(0xFF07090E)
val ObsidianSurfaceTier1 = Color(0xFF0D1117)
val ObsidianContainerTier2 = Color(0xFF161B22)
val ObsidianContainerHigh = Color(0xFF262A31)
val ObsidianElectricCyan = Color(0xFF00F0FF)
val ObsidianMutedIndigo = Color(0xFF6366F1)
val ObsidianSyntheticEmerald = Color(0xFF00E5A3)
val ObsidianWarningAmber = Color(0xFFF5C451)
val ObsidianErrorRed = Color(0xFFFFB4AB)
val ObsidianTextOnSurface = Color(0xFFF0F6FC)
val ObsidianTextMuted = Color(0xFF8B949E)
val ObsidianTelemetry = Color(0xFF6E7681)
val ObsidianOutlineHairline = Color(0x14FFFFFF)

// Light Palette (Derived with WCAG AA compliance)
val LightBgTier0 = Color(0xFFF6F8FA)
val LightSurfaceTier1 = Color(0xFFFFFFFF)
val LightContainerTier2 = Color(0xFFF0F3F6)
val LightElectricCyanDarkened = Color(0xFF00838F)
val LightMutedIndigoDarkened = Color(0xFF4F46E5)
val LightSyntheticEmeraldDarkened = Color(0xFF00875F)
val LightTextOnSurface = Color(0xFF1F2328)
val LightTextMuted = Color(0xFF59636E)

val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianElectricCyan,
    onPrimary = Color(0xFF07090E),
    primaryContainer = ObsidianContainerTier2,
    onPrimaryContainer = ObsidianElectricCyan,
    secondary = ObsidianMutedIndigo,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = ObsidianSyntheticEmerald,
    onTertiary = Color(0xFF003825),
    background = ObsidianBgTier0,
    onBackground = ObsidianTextOnSurface,
    surface = ObsidianSurfaceTier1,
    onSurface = ObsidianTextOnSurface,
    surfaceVariant = ObsidianContainerTier2,
    onSurfaceVariant = ObsidianTextMuted,
    outline = ObsidianOutlineHairline,
    error = ObsidianErrorRed,
    onError = Color(0xFF690005)
)

val ObsidianLightColorScheme = lightColorScheme(
    primary = LightElectricCyanDarkened,
    onPrimary = Color(0xFFFFFFFF),
    secondary = LightMutedIndigoDarkened,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = LightSyntheticEmeraldDarkened,
    onTertiary = Color(0xFFFFFFFF),
    background = LightBgTier0,
    onBackground = LightTextOnSurface,
    surface = LightSurfaceTier1,
    onSurface = LightTextOnSurface,
    surfaceVariant = LightContainerTier2,
    onSurfaceVariant = LightTextMuted,
    outline = Color(0x1A1F2328),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun ObsidianKineticTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkColorScheme else ObsidianLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

