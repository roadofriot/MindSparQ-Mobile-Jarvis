package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelManagerTest {

    @Test
    fun availableModelsCatalogContainsLegitimateHuggingFaceEndpoints() {
        val models = OfflineModelManager.AVAILABLE_MODELS
        assertTrue("Model catalog should not be empty", models.isNotEmpty())

        for (model in models) {
            assertTrue("Direct URL must be HTTPS", model.directUrl.startsWith("https://"))
            assertTrue("Direct URL must point to HuggingFace GGUF", model.directUrl.contains("huggingface.co") && model.directUrl.endsWith(".gguf"))
            assertTrue("File name must end with .gguf", model.fileName.endsWith(".gguf"))
            assertTrue("File size must be positive", model.fileSizeBytes > 100_000_000L)
            assertTrue("Min RAM must be positive", model.minRamGb > 0f)
            assertTrue("Recommended RAM must be >= min RAM", model.recommendedRamGb >= model.minRamGb)
        }
    }

    @Test
    fun modelsCoverDifferentHardwareTiers() {
        val models = OfflineModelManager.AVAILABLE_MODELS
        val hasUltralight = models.any { it.minRamGb <= 2.0f }
        val hasMidTier = models.any { it.minRamGb in 3.0f..6.0f }
        val hasHighTier = models.any { it.minRamGb >= 8.0f }

        assertTrue("Must have an ultralight model for low-spec phones", hasUltralight)
        assertTrue("Must have mid-tier models (1.5B)", hasMidTier)
        assertTrue("Must have high-tier models (3B) for 10GB+ devices", hasHighTier)
    }

    @Test
    fun highRamDevicesGetLlama3Recommended() {
        val models = OfflineModelManager.AVAILABLE_MODELS
        val llama3 = models.find { it.id == "llama-3.2-3b" }
        assertNotNull(llama3)
        assertEquals("Q4_K_M", llama3?.quantization)
    }
}

