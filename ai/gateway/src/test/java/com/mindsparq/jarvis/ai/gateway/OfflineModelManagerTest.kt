package com.mindsparq.jarvis.ai.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelManagerTest {

    private val manager = OfflineModelManager()
    private val hybridRouter = HybridRouteManager(manager)

    @Test
    fun testDeviceRecommendationFor12GbRam() {
        val recommended = manager.getRecommendedModel(deviceRamGb = 12)
        assertEquals("llama-3.2-3b-q4", recommended.id)
    }

    @Test
    fun testDeviceRecommendationFor4GbRam() {
        val recommended = manager.getRecommendedModel(deviceRamGb = 4)
        assertEquals("qwen2.5-coder-0.5b-q4", recommended.id)
    }

    @Test
    fun testDownloadAndMarking() {
        assertFalse(manager.isDownloaded("qwen2.5-coder-1.5b-q4"))
        manager.markDownloaded("qwen2.5-coder-1.5b-q4")
        assertTrue(manager.isDownloaded("qwen2.5-coder-1.5b-q4"))

        manager.deleteModel("qwen2.5-coder-1.5b-q4")
        assertFalse(manager.isDownloaded("qwen2.5-coder-1.5b-q4"))
    }

    @Test
    fun testHybridRouteResolution() {
        // Online mode
        assertEquals(
            "cloud:multi-key-pool",
            hybridRouter.resolveRoute(isNetworkAvailable = true, forceOffline = false, deviceRamGb = 12)
        )

        // Offline mode (Network down)
        assertEquals(
            "offline:llama-3.2-3b-q4",
            hybridRouter.resolveRoute(isNetworkAvailable = false, forceOffline = false, deviceRamGb = 12)
        )

        // Force offline mode (User setting)
        assertEquals(
            "offline:qwen2.5-coder-0.5b-q4",
            hybridRouter.resolveRoute(isNetworkAvailable = true, forceOffline = true, deviceRamGb = 4)
        )
    }
}

