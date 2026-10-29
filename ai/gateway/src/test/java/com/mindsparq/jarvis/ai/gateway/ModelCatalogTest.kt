package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun testOpenRouterFreeModelFiltering() {
        val freeWithSuffix = OpenRouterModelJson(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Llama 3.3 Free",
            context_length = 64000,
            supported_parameters = listOf("tools", "temperature")
        )

        val freeZeroPrice = OpenRouterModelJson(
            id = "qwen/qwen-2.5-coder-32b",
            name = "Qwen Coder",
            context_length = 32768,
            pricing = buildJsonObject {
                put("prompt", "0")
                put("completion", "0")
            },
            supported_parameters = listOf("tools")
        )

        val paidModel = OpenRouterModelJson(
            id = "anthropic/claude-3.5-sonnet",
            context_length = 200000,
            pricing = buildJsonObject {
                put("prompt", "0.000003")
                put("completion", "0.000015")
            },
            supported_parameters = listOf("tools")
        )

        val filtered = ModelCatalog.filterOpenRouterFreeModels(listOf(freeWithSuffix, freeZeroPrice, paidModel))

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id.contains("llama-3.3") })
        assertTrue(filtered.any { it.id.contains("qwen") })
    }

    @Test
    fun testGeminiModelRanking() {
        val candidates = listOf(
            "gemini-2.0-flash",
            "gemini-3.8-flash",
            "gemini-2.5-pro",
            "gemini-3.8-flash-exp",
            "text-embedding-004",
            "gemini-3.5-flash-lite"
        )

        val ranked = ModelCatalog.rankGeminiModels(candidates)

        // 3.8 Flash should be first (highest version stable)
        assertEquals("gemini-3.8-flash", ranked[0])
        // Embeddings should be omitted
        assertTrue(ranked.none { it.contains("embedding") })
    }
}

