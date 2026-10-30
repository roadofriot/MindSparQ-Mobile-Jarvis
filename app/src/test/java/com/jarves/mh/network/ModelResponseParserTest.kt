package com.jarves.mh.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelResponseParserTest {
    @Test
    fun parsesOpenAiStyleDataList() {
        val models = ModelResponseParser.parse(
            """{"data":[{"id":"model-b"},{"id":"model-a","display_name":"Model A"}]}""",
        )

        assertEquals(listOf("model-a", "model-b"), models.map { it.id })
        assertEquals("Model A", models.first().displayName)
    }

    @Test
    fun parsesModelsAndStringArrays() {
        assertEquals(
            listOf("alpha"),
            ModelResponseParser.parse("""{"models":[{"name":"alpha"}]}""").map { it.id },
        )
        assertEquals(
            listOf("alpha", "beta"),
            ModelResponseParser.parse("""["beta","alpha"]""").map { it.id },
        )
    }

    @Test
    fun parsesGeminiModelsAndStripsPrefix() {
        val json = """
            {
              "models": [
                {
                  "name": "models/gemini-2.5-flash",
                  "displayName": "Gemini 2.5 Flash",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/text-embedding-004",
                  "displayName": "Embedding 004",
                  "supportedGenerationMethods": ["embedContent"]
                }
              ]
            }
        """.trimIndent()
        val models = ModelResponseParser.parse(json)
        assertEquals(1, models.size)
        assertEquals("gemini-2.5-flash", models.first().id)
        assertEquals("Gemini 2.5 Flash", models.first().displayName)
        assertEquals(true, models.first().isFree)
    }

    @Test
    fun parsesOllamaTagsResponse() {
        val json = """
            {
              "models": [
                {
                  "name": "qwen2.5-coder:1.5b",
                  "model": "qwen2.5-coder:1.5b",
                  "size": 986000000
                }
              ]
            }
        """.trimIndent()
        val models = ModelResponseParser.parse(json)
        assertEquals(1, models.size)
        assertEquals("qwen2.5-coder:1.5b", models.first().id)
        assertEquals(true, models.first().isFree)
    }

    @Test
    fun malformedResponseReturnsEmptyList() {
        assertEquals(emptyList<DiscoveredModel>(), ModelResponseParser.parse("not json"))
    }
}
