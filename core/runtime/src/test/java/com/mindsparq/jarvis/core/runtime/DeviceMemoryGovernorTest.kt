package com.mindsparq.jarvis.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMemoryGovernorTest {

    private val governor = DeviceMemoryGovernor()

    @Test
    fun testMemoryConstraintThreshold() {
        assertTrue(governor.isMemoryConstrained(availableMb = 350L))
        assertFalse(governor.isMemoryConstrained(availableMb = 800L))
    }

    @Test
    fun testRecommendedConcurrency() {
        assertEquals(4, governor.recommendedWorkerConcurrency(availableMb = 4096L))
        assertEquals(2, governor.recommendedWorkerConcurrency(availableMb = 2048L))
        assertEquals(1, governor.recommendedWorkerConcurrency(availableMb = 400L))
    }

    @Test
    fun testThermalThrottling() {
        assertTrue(governor.shouldThrottleThermal(4)) // SEVERE
        assertTrue(governor.shouldThrottleThermal(5)) // CRITICAL
        assertFalse(governor.shouldThrottleThermal(2)) // MODERATE
        assertFalse(governor.shouldThrottleThermal(0)) // NONE
    }
}

