package com.mindsparq.jarvis.core.designsystem.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveLayoutTest {

    @Test
    fun testWindowWidthClassBreakpoints() {
        // Phone portrait (e.g. 390 dp)
        assertEquals(WindowWidthClass.COMPACT, ResponsiveLayoutHelper.resolveWidthClass(390))

        // Foldable / Tablet (e.g. 820 dp)
        assertEquals(WindowWidthClass.MEDIUM, ResponsiveLayoutHelper.resolveWidthClass(820))

        // PC Desktop / Ultra-wide (e.g. 1440 dp)
        assertEquals(WindowWidthClass.EXPANDED, ResponsiveLayoutHelper.resolveWidthClass(1440))
    }
}

