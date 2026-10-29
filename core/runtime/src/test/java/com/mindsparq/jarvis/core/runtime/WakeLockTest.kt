package com.mindsparq.jarvis.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeLockTest {

    @Test
    fun testAcquireAndRelease() {
        val controller = TestWakeLockController()
        assertFalse(controller.isHeld())

        controller.acquire("BackgroundCompilation", 60000L)
        assertTrue(controller.isHeld())
        assertEquals("BackgroundCompilation", controller.lastTag)

        controller.release()
        assertFalse(controller.isHeld())
    }
}

