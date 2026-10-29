package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun testFilterOpenRouterFreeModels() {
        val candidateModels = listOf(
            OpenRouterModelJson(
                id = "meta-llama/llama-3.3-70b-instruct:free",
                context_length = 131072,
                supported_parameters = listOf("tools", "temperature")
            ),
            OpenRouterModelJson(
                id = "openai/gpt-4o",
                context_length = 128000,
                pricing = buildJsonObject {
                    put("prompt", "0.000005")
                    put("completion", "0.000015")
                },
                supported_parameters = listOf("tools")
            ),
            OpenRouterModelJson(
                id = "qwen/qwen-2.5-coder-32b-instruct:free",
                context_length = 32768,
                supported_parameters = listOf("tools")
            ),
            OpenRouterModelJson(
                id = "google/gemma-2-9b-it:free",
                context_length = 8192,
                supported_parameters = listOf("tools")
            )
        )

        val freeModels = ModelCatalog.filterOpenRouterFreeModels(candidateModels)
        assertEquals(2, freeModels.size)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", freeModels[0].id)
        assertEquals("qwen/qwen-2.5-coder-32b-instruct:free", freeModels[1].id)
    }

    @Test
    fun testRankGeminiModels() {
        val geminiCandidates = listOf(
            "gemini-1.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-1.5-pro",
            "text-embedding-004",
            "gemini-2.5-flash-preview-0520"
        )

        val ranked = ModelCatalog.rankGeminiModels(geminiCandidates)
        assertEquals(5, ranked.size)
        assertEquals("gemini-2.5-flash", ranked[0])
        assertEquals("gemini-2.5-pro", ranked[1])
        assertEquals("gemini-2.5-flash-preview-0520", ranked[2])
        assertEquals("gemini-1.5-flash", ranked[3])
        assertEquals("gemini-1.5-pro", ranked[4])
    }

    @Test
    fun testNvidiaNimModelsAndKeyValidation() {
        assertTrue(ModelCatalog.NVIDIA_NIM_MODELS.contains("deepseek-ai/deepseek-r1"))
        
        val validNvidia = ModelCatalog.simulateOrProbeKey("nvidia", "nvapi-abcdef1234567890")
        assertTrue(validNvidia.isValid)

        val invalidNvidia = ModelCatalog.simulateOrProbeKey("nvidia", "sk-invalid")
        assertFalse(invalidNvidia.isValid)

        val validGemini = ModelCatalog.simulateOrProbeKey("gemini", "AIzaSyD1234567890abcdef")
        assertTrue(validGemini.isValid)
    }
}
