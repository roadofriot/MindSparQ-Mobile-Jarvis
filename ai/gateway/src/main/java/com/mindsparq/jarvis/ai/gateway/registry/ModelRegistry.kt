package com.mindsparq.jarvis.ai.gateway.registry

import com.mindsparq.jarvis.core.model.ModelDescriptor
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry of all known AI models across supported providers.
 * Backs both in-memory fast lookups and caches dynamically discovered models.
 */
class ModelRegistry private constructor() {

    // Key is "$providerId:$modelId" (normalized lowercase)
    private val models = ConcurrentHashMap<String, ModelDescriptor>()

    init {
        registerBuiltInModels()
    }

    private fun registerBuiltInModels() {
        val builtIns = listOf(
            // Google Gemini
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-2.5-pro",
                displayName = "Gemini 2.5 Pro",
                contextLength = 2_097_152,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                pinned = false
            ),

            // OpenRouter (Free Tier)
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "meta-llama/llama-3.3-70b-instruct:free",
                displayName = "Llama 3.3 70B Instruct (Free)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "qwen/qwen-2.5-coder-32b-instruct:free",
                displayName = "Qwen 2.5 Coder 32B (Free)",
                contextLength = 32_768,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "deepseek/deepseek-r1:free",
                displayName = "DeepSeek R1 (Free)",
                contextLength = 65_536,
                supportsTools = false,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),

            // DeepSeek Official
            ModelDescriptor(
                providerId = "deepseek",
                modelId = "deepseek-chat",
                displayName = "DeepSeek V3 (Chat/Code)",
                contextLength = 64_000,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = false,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "deepseek",
                modelId = "deepseek-reasoner",
                displayName = "DeepSeek R1 (Reasoner)",
                contextLength = 64_000,
                supportsTools = false,
                supportsVision = false,
                supportsThinking = true,
                isFree = false,
                pinned = true
            ),

            // NVIDIA NIM
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "meta/llama-3.3-70b-instruct",
                displayName = "NVIDIA NIM Llama 3.3 70B",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "deepseek-ai/deepseek-r1",
                displayName = "NVIDIA NIM DeepSeek R1",
                contextLength = 65_536,
                supportsTools = false,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),

            // Ollama (Local / Offline)
            ModelDescriptor(
                providerId = "ollama",
                modelId = "qwen2.5-coder:1.5b",
                displayName = "Qwen 2.5 Coder 1.5B (Local)",
                contextLength = 32_768,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "ollama",
                modelId = "qwen2.5-coder:0.5b",
                displayName = "Qwen 2.5 Coder 0.5B (Ultralight Local)",
                contextLength = 16_384,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "ollama",
                modelId = "deepseek-r1:1.5b",
                displayName = "DeepSeek R1 1.5B (Reasoning Local)",
                contextLength = 32_768,
                supportsTools = false,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),

            // Anthropic
            ModelDescriptor(
                providerId = "anthropic",
                modelId = "claude-sonnet-4-6",
                displayName = "Claude Sonnet 4.6",
                contextLength = 200_000,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = false,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "anthropic",
                modelId = "claude-3-7-sonnet-latest",
                displayName = "Claude 3.7 Sonnet (Hybrid)",
                contextLength = 200_000,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = false,
                pinned = false
            ),

            // OpenCode
            ModelDescriptor(
                providerId = "opencode",
                modelId = "deepseek-v4-flash",
                displayName = "OpenCode Zen V4 Flash",
                contextLength = 64_000,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = false,
                pinned = true
            )
        )

        builtIns.forEach { registerModel(it) }
    }

    private fun key(providerId: String, modelId: String): String =
        "${providerId.lowercase().trim()}:${modelId.trim()}"

    fun registerModel(model: ModelDescriptor) {
        models[key(model.providerId, model.modelId)] = model
    }

    fun registerModels(modelsList: List<ModelDescriptor>) {
        modelsList.forEach { registerModel(it) }
    }

    fun getModel(providerId: String, modelId: String): ModelDescriptor? =
        models[key(providerId, modelId)]

    fun getAllModels(): List<ModelDescriptor> =
        models.values.toList().sortedWith(
            compareByDescending<ModelDescriptor> { it.pinned }
                .thenByDescending { it.isFree }
                .thenByDescending { it.contextLength }
        )

    fun getModelsForProvider(providerId: String): List<ModelDescriptor> =
        models.values
            .filter { it.providerId.equals(providerId, ignoreCase = true) }
            .sortedWith(
                compareByDescending<ModelDescriptor> { it.pinned }
                    .thenByDescending { it.isFree }
                    .thenByDescending { it.contextLength }
            )

    fun findModels(
        providerId: String? = null,
        onlyFree: Boolean = false,
        requiresTools: Boolean = false,
        requiresVision: Boolean = false,
        requiresThinking: Boolean = false,
        query: String? = null
    ): List<ModelDescriptor> {
        return models.values.filter { model ->
            if (providerId != null && !model.providerId.equals(providerId, ignoreCase = true)) return@filter false
            if (onlyFree && !model.isFree) return@filter false
            if (requiresTools && !model.supportsTools) return@filter false
            if (requiresVision && !model.supportsVision) return@filter false
            if (requiresThinking && !model.supportsThinking) return@filter false
            if (!query.isNullOrBlank()) {
                val q = query.trim().lowercase()
                val match = model.modelId.lowercase().contains(q) ||
                    model.displayName.lowercase().contains(q) ||
                    model.providerId.lowercase().contains(q)
                if (!match) return@filter false
            }
            true
        }.sortedWith(
            compareByDescending<ModelDescriptor> { it.pinned }
                .thenByDescending { it.isFree }
                .thenByDescending { it.contextLength }
        )
    }

    fun count(): Int = models.size

    fun clear() {
        models.clear()
        registerBuiltInModels()
    }

    companion object {
        val instance by lazy { ModelRegistry() }
    }
}

