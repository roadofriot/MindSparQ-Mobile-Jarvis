package com.mindsparq.jarvis.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastVerificationTest {

    private fun calculateContrast(c1: Color, c2: Color): Double {
        val lum1 = c1.luminance()
        val lum2 = c2.luminance()
        val brightest = maxOf(lum1, lum2)
        val darkest = minOf(lum1, lum2)
        return (brightest + 0.05) / (darkest + 0.05)
    }

    @Test
    fun testDarkThemeTextContrast() {
        // WCAG AA requires minimum 4.5:1 for normal text
        val textOnBg = calculateContrast(ObsidianTextOnSurface, ObsidianBgTier0)
        assertTrue("Dark theme text contrast must be >= 4.5:1, got $textOnBg", textOnBg >= 4.5)

        val textOnSurface = calculateContrast(ObsidianTextOnSurface, ObsidianSurfaceTier1)
        assertTrue("Dark theme text on surface must be >= 4.5:1, got $textOnSurface", textOnSurface >= 4.5)

        // Electric cyan on primary button (obsidian text)
        val primaryButtonContrast = calculateContrast(Color(0xFF07090E), ObsidianElectricCyan)
        assertTrue("Primary button text contrast must be >= 4.5:1, got $primaryButtonContrast", primaryButtonContrast >= 4.5)
    }

    @Test
    fun testLightThemeTextContrast() {
        val textOnBg = calculateContrast(LightTextOnSurface, LightBgTier0)
        assertTrue("Light theme text contrast must be >= 4.5:1, got $textOnBg", textOnBg >= 4.5)

        val primaryButtonContrast = calculateContrast(Color(0xFFFFFFFF), LightElectricCyanDarkened)
        assertTrue("Light primary button contrast must be >= 4.5:1, got $primaryButtonContrast", primaryButtonContrast >= 4.5)
    }
}

