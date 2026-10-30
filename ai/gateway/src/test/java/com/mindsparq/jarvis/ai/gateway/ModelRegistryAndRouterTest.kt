package com.mindsparq.jarvis.ai.gateway

import com.mindsparq.jarvis.ai.gateway.discovery.ModelDiscoveryService
import com.mindsparq.jarvis.ai.gateway.registry.ModelRegistry
import com.mindsparq.jarvis.ai.gateway.registry.ProviderRegistry
import com.mindsparq.jarvis.ai.gateway.routing.CapabilityMatcher
import com.mindsparq.jarvis.ai.gateway.routing.ModelRouter
import com.mindsparq.jarvis.ai.gateway.routing.TaskRequirement
import com.mindsparq.jarvis.core.model.ModelDescriptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModelRegistryAndRouterTest {

    private lateinit var registry: ModelRegistry
    private lateinit var router: ModelRouter

    @Before
    fun setup() {
        registry = ModelRegistry.instance
        registry.clear()
        router = ModelRouter(registry, ProviderRegistry.instance)
    }

    @Test
    fun testBuiltInRegistryModels() {
        val gemini = registry.getModel("gemini", "gemini-2.5-flash")
        assertNotNull(gemini)
        assertTrue(gemini!!.isFree)
        assertTrue(gemini.supportsTools)
        assertTrue(gemini.supportsVision)

        val openRouterFree = registry.getModelsForProvider("openrouter").filter { it.isFree }
        assertTrue(openRouterFree.isNotEmpty())

        val ollama = registry.getModelsForProvider("ollama")
        assertTrue(ollama.isNotEmpty())
    }

    @Test
    fun testCapabilityFilter() {
        val visionModels = registry.findModels(requiresVision = true)
        assertTrue(visionModels.all { it.supportsVision })

        val freeToolsModels = registry.findModels(onlyFree = true, requiresTools = true)
        assertTrue(freeToolsModels.all { it.isFree && it.supportsTools })

        val reasoningModels = registry.findModels(requiresThinking = true)
        assertTrue(reasoningModels.all { it.supportsThinking })
    }

    @Test
    fun testModelRouterSafetyAndFallbacks() {
        // Enforce: User wants free tier only with tools support
        val req = TaskRequirement(requiresTools = true, allowPaid = false)
        val plan = router.resolvePlan(requestedProviderId = null, requestedModelId = null, requirement = req)

        assertTrue(plan.primary.isFree)
        assertTrue(plan.primary.supportsTools)

        // All fallbacks must strictly be free
        for (fallback in plan.fallbacks) {
            assertTrue("Fallback ${fallback.modelId} must be free", fallback.isFree)
            assertTrue("Fallback ${fallback.modelId} must support tools", fallback.supportsTools)
        }

        // Fallbacks should be from diverse providers
        val providers = plan.fallbacks.map { it.providerId }.toSet()
        assertTrue(providers.isNotEmpty())
    }

    @Test
    fun testModelRouterOfflinePreference() {
        val req = TaskRequirement(preferLocalOffline = true, allowPaid = false)
        val plan = router.resolvePlan(requestedProviderId = null, requestedModelId = null, requirement = req)

        assertEquals("ollama", plan.primary.providerId)
    }

    @Test
    fun testModelDiscoveryGeminiWithMockEngine() = runBlocking {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("models")) {
                respond(
                    content = """
                        {
                          "models": [
                            {
                              "name": "models/gemini-2.5-flash",
                              "displayName": "Gemini 2.5 Flash",
                              "inputTokenLimit": 1048576,
                              "supportedGenerationMethods": ["generateContent", "countTokens"]
                            },
                            {
                              "name": "models/gemini-3.0-pro-preview",
                              "displayName": "Gemini 3.0 Pro Preview",
                              "inputTokenLimit": 2097152,
                              "supportedGenerationMethods": ["generateContent"]
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("Not Found", HttpStatusCode.NotFound)
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val discoveryService = ModelDiscoveryService(client = client, registry = registry)
        val result = discoveryService.discoverGeminiModels("test-key")

        assertTrue(result.success)
        assertEquals(2, result.modelsDiscovered.size)
        assertEquals("gemini-2.5-flash", result.modelsDiscovered[0].modelId)
        assertEquals("gemini-3.0-pro-preview", result.modelsDiscovered[1].modelId)

        // Verify registered in registry
        assertNotNull(registry.getModel("gemini", "gemini-3.0-pro-preview"))
    }

    @Test
    fun testModelDiscoveryOllamaWithMockEngine() = runBlocking {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/api/tags")) {
                respond(
                    content = """
                        {
                          "models": [
                            {
                              "name": "qwen2.5-coder:1.5b",
                              "size": 986000000
                            },
                            {
                              "name": "deepseek-r1:1.5b",
                              "size": 1100000000
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("Not Found", HttpStatusCode.NotFound)
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val discoveryService = ModelDiscoveryService(client = client, registry = registry)
        val result = discoveryService.discoverOllamaModels("http://127.0.0.1:11434")

        assertTrue(result.success)
        assertEquals(2, result.modelsDiscovered.size)
        val qwen = result.modelsDiscovered.find { it.modelId == "qwen2.5-coder:1.5b" }
        assertNotNull(qwen)
        assertTrue(qwen!!.supportsTools)
        val deepseek = result.modelsDiscovered.find { it.modelId == "deepseek-r1:1.5b" }
        assertNotNull(deepseek)
        assertTrue(deepseek!!.supportsThinking)
    }
}

