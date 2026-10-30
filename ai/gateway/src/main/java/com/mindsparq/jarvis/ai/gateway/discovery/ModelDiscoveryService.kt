package com.mindsparq.jarvis.ai.gateway.discovery

import com.mindsparq.jarvis.ai.gateway.registry.ModelRegistry
import com.mindsparq.jarvis.core.model.ModelDescriptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class DiscoveryResult(
    val providerId: String,
    val success: Boolean,
    val modelsDiscovered: List<ModelDescriptor> = emptyList(),
    val errorMessage: String? = null,
    val latencyMs: Long = 0L
)

/**
 * Service to dynamically query provider endpoints and discover currently available models,
 * validating capabilities (tools, vision, reasoning, free-tier) and registering them in [ModelRegistry].
 */
class ModelDiscoveryService(
    private val client: HttpClient = createDefaultHttpClient(),
    private val registry: ModelRegistry = ModelRegistry.instance
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun discoverGeminiModels(apiKey: String): DiscoveryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext DiscoveryResult(
                providerId = "gemini",
                success = false,
                errorMessage = "Gemini API key is missing or blank"
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val response = client.get(url)
            val duration = System.currentTimeMillis() - start

            if (response.status != HttpStatusCode.OK) {
                return@withContext DiscoveryResult(
                    providerId = "gemini",
                    success = false,
                    errorMessage = "Gemini API returned status ${response.status.value}: ${response.bodyAsText().take(200)}",
                    latencyMs = duration
                )
            }

            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val modelsArray = root["models"]?.jsonArray ?: return@withContext DiscoveryResult(
                providerId = "gemini",
                success = true,
                latencyMs = duration
            )

            val descriptors = mutableListOf<ModelDescriptor>()
            for (element in modelsArray) {
                val obj = element.jsonObject
                val rawName = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val modelId = rawName.removePrefix("models/")
                val displayName = obj["displayName"]?.jsonPrimitive?.contentOrNull ?: modelId
                val inputLimit = obj["inputTokenLimit"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1_048_576

                val methods = obj["supportedGenerationMethods"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                if (!methods.contains("generateContent")) continue

                val lower = modelId.lowercase()
                val isFlash = lower.contains("flash")
                val isPro = lower.contains("pro")
                val isThinking = lower.contains("2.5") || lower.contains("thinking")
                val isFree = true // Gemini Flash / Pro developer tier has free quota

                val desc = ModelDescriptor(
                    providerId = "gemini",
                    modelId = modelId,
                    displayName = displayName,
                    contextLength = inputLimit,
                    supportsTools = true,
                    supportsVision = true,
                    supportsThinking = isThinking,
                    isFree = isFree,
                    pinned = isFlash || isPro
                )
                descriptors.add(desc)
            }

            registry.registerModels(descriptors)
            DiscoveryResult(
                providerId = "gemini",
                success = true,
                modelsDiscovered = descriptors,
                latencyMs = duration
            )
        } catch (e: Exception) {
            DiscoveryResult(
                providerId = "gemini",
                success = false,
                errorMessage = e.message ?: "Failed to connect to Gemini API",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun discoverOpenRouterModels(apiKey: String? = null): DiscoveryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val response = client.get("https://openrouter.ai/api/v1/models") {
                if (!apiKey.isNullOrBlank()) {
                    header("Authorization", "Bearer $apiKey")
                }
            }
            val duration = System.currentTimeMillis() - start

            if (response.status != HttpStatusCode.OK) {
                return@withContext DiscoveryResult(
                    providerId = "openrouter",
                    success = false,
                    errorMessage = "OpenRouter returned status ${response.status.value}",
                    latencyMs = duration
                )
            }

            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val dataArray = root["data"]?.jsonArray ?: return@withContext DiscoveryResult(
                providerId = "openrouter",
                success = true,
                latencyMs = duration
            )

            val descriptors = mutableListOf<ModelDescriptor>()
            for (element in dataArray) {
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
                val contextLen = obj["context_length"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 32_000

                val pricing = obj["pricing"]?.jsonObject
                val promptPrice = pricing?.get("prompt")?.jsonPrimitive?.contentOrNull
                val completionPrice = pricing?.get("completion")?.jsonPrimitive?.contentOrNull
                val isZeroPrice = (promptPrice == "0" || promptPrice == "0.0") && (completionPrice == "0" || completionPrice == "0.0")
                val isFree = id.endsWith(":free") || isZeroPrice

                val supportedParams = obj["supported_parameters"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val supportsTools = supportedParams.contains("tools") || supportedParams.contains("function_calling")
                val idLower = id.lowercase()
                val supportsVision = idLower.contains("vision") || idLower.contains("multimodal") || idLower.contains("4o")
                val supportsThinking = idLower.contains("r1") || idLower.contains("reason") || idLower.contains("o1") || idLower.contains("o3")

                descriptors.add(
                    ModelDescriptor(
                        providerId = "openrouter",
                        modelId = id,
                        displayName = name,
                        contextLength = contextLen,
                        supportsTools = supportsTools,
                        supportsVision = supportsVision,
                        supportsThinking = supportsThinking,
                        isFree = isFree,
                        openWeights = !idLower.contains("openai") && !idLower.contains("anthropic"),
                        pinned = isFree && supportsTools
                    )
                )
            }

            registry.registerModels(descriptors)
            DiscoveryResult(
                providerId = "openrouter",
                success = true,
                modelsDiscovered = descriptors,
                latencyMs = duration
            )
        } catch (e: Exception) {
            DiscoveryResult(
                providerId = "openrouter",
                success = false,
                errorMessage = e.message ?: "Failed to connect to OpenRouter API",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun discoverOllamaModels(baseUrl: String = "http://127.0.0.1:11434"): DiscoveryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val endpoint = "${baseUrl.trimEnd('/')}/api/tags"
            val response = client.get(endpoint)
            val duration = System.currentTimeMillis() - start

            if (response.status != HttpStatusCode.OK) {
                return@withContext DiscoveryResult(
                    providerId = "ollama",
                    success = false,
                    errorMessage = "Ollama returned status ${response.status.value}",
                    latencyMs = duration
                )
            }

            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val modelsArray = root["models"]?.jsonArray ?: return@withContext DiscoveryResult(
                providerId = "ollama",
                success = true,
                latencyMs = duration
            )

            val descriptors = mutableListOf<ModelDescriptor>()
            for (element in modelsArray) {
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val nameLower = name.lowercase()
                val supportsTools = nameLower.contains("qwen2.5") || nameLower.contains("llama3") || nameLower.contains("command")
                val supportsThinking = nameLower.contains("r1") || nameLower.contains("deepseek-r1")

                descriptors.add(
                    ModelDescriptor(
                        providerId = "ollama",
                        modelId = name,
                        displayName = "$name (Local)",
                        contextLength = 32_768,
                        supportsTools = supportsTools,
                        supportsVision = nameLower.contains("llava") || nameLower.contains("vision"),
                        supportsThinking = supportsThinking,
                        isFree = true,
                        openWeights = true,
                        pinned = true
                    )
                )
            }

            registry.registerModels(descriptors)
            DiscoveryResult(
                providerId = "ollama",
                success = true,
                modelsDiscovered = descriptors,
                latencyMs = duration
            )
        } catch (e: Exception) {
            DiscoveryResult(
                providerId = "ollama",
                success = false,
                errorMessage = e.message ?: "Ollama server is not responding at $baseUrl",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun discoverNvidiaModels(apiKey: String): DiscoveryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext DiscoveryResult(
                providerId = "nvidia",
                success = false,
                errorMessage = "NVIDIA NIM API key is missing or blank"
            )
        }

        try {
            val response = client.get("https://integrate.api.nvidia.com/v1/models") {
                header("Authorization", "Bearer $apiKey")
            }
            val duration = System.currentTimeMillis() - start

            if (response.status != HttpStatusCode.OK) {
                return@withContext DiscoveryResult(
                    providerId = "nvidia",
                    success = false,
                    errorMessage = "NVIDIA NIM returned status ${response.status.value}",
                    latencyMs = duration
                )
            }

            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val dataArray = root["data"]?.jsonArray ?: return@withContext DiscoveryResult(
                providerId = "nvidia",
                success = true,
                latencyMs = duration
            )

            val descriptors = mutableListOf<ModelDescriptor>()
            for (element in dataArray) {
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val idLower = id.lowercase()

                descriptors.add(
                    ModelDescriptor(
                        providerId = "nvidia",
                        modelId = id,
                        displayName = "NVIDIA $id",
                        contextLength = if (idLower.contains("128k") || idLower.contains("llama-3.3")) 131_072 else 32_768,
                        supportsTools = !idLower.contains("r1"),
                        supportsVision = idLower.contains("vision") || idLower.contains("multimodal"),
                        supportsThinking = idLower.contains("r1") || idLower.contains("reason"),
                        isFree = true, // NVIDIA NIM free developer credits
                        openWeights = true,
                        pinned = true
                    )
                )
            }

            registry.registerModels(descriptors)
            DiscoveryResult(
                providerId = "nvidia",
                success = true,
                modelsDiscovered = descriptors,
                latencyMs = duration
            )
        } catch (e: Exception) {
            DiscoveryResult(
                providerId = "nvidia",
                success = false,
                errorMessage = e.message ?: "Failed to connect to NVIDIA NIM",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    })
                }
            }
        }
    }
}

