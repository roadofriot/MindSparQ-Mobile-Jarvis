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
            // Google Gemini (2026 Production & Research Cut-off)
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-4-argon",
                displayName = "Gemini 4 Argon (Flagship Reasoning & Orchestration)",
                contextLength = 2_097_152,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash (Autonomous Coding Agent)",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3.6-flash",
                displayName = "Gemini 3.6 Flash (General Coding & Multimodal)",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3.5-flash-lite",
                displayName = "Gemini 3.5 Flash Lite (Fast Subtasks & Classification)",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3.1-flash-lite",
                displayName = "Gemini 3.1 Flash Lite",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                pinned = false
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3.1-pro-preview",
                displayName = "Gemini 3.1 Pro Preview",
                contextLength = 2_097_152,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = false
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-3-flash-preview",
                displayName = "Gemini 3 Flash Preview",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                pinned = false
            ),
            ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                contextLength = 1_048_576,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                pinned = false
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
                pinned = false
            ),

            // OpenRouter (Free Tier & 2026 Free Models)
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "cohere/north-mini-code:free",
                displayName = "Cohere North Mini Code (Free)",
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
                modelId = "recursive/flint-v1.0:free",
                displayName = "Flint v1.0 (Codebase Analysis & Tests)",
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
                modelId = "nvidia/nemotron-3-ultra-550b-a55b:free",
                displayName = "Nemotron 3 Ultra 550B (Deep Reasoning)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "nvidia/nemotron-3.5-super-vl:free",
                displayName = "Nemotron 3.5 Super VL (Multimodal GUI Agent)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "nvidia/nemotron-3.5-lightning:free",
                displayName = "Nemotron 3.5 Lightning (Fast Agentic)",
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
                modelId = "dots-studio/dots-3-note-preview:free",
                displayName = "Dots 3 Note Preview (Multimodal Workflows)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "thinkingmachines/inkling:free",
                displayName = "Inkling (Coding & Tool Use)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "thinkingmachines/inkling-small:free",
                displayName = "Inkling Small (Lightweight Subtasks)",
                contextLength = 32_768,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = false
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "xingchen-agi/xing4.0-29b-a4b:free",
                displayName = "Xing 4.0 29B (Engineering & Planning)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "google/gemma-4-26b-a4b-it:free",
                displayName = "Gemma 4 26B Instruct (Coding & Functions)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "openrouter",
                modelId = "openrouter/free",
                displayName = "OpenRouter Auto Free Router",
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
                modelId = "meta-llama/llama-3.3-70b-instruct:free",
                displayName = "Llama 3.3 70B Instruct (Free)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = false
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
                pinned = false
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
                pinned = false
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

            // NVIDIA NIM (Free Endpoints Catalogue)
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "nemotron-3.5-lightning-30b-a3b",
                displayName = "Nemotron 3.5 Lightning 30B (Fast Agentic)",
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
                modelId = "nemotron-3.5-super-vl-120b-a12b",
                displayName = "Nemotron 3.5 Super VL 120B (Multimodal)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "Nemotron 3.5 Bolt E4B",
                displayName = "Nemotron 3.5 Bolt E4B (Compact Reasoning)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "minimax-m3.1-flash",
                displayName = "MiniMax M3.1 Flash (Agentic & Computer Use)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "Step 5 Preview",
                displayName = "Step 5 Preview (Reasoning & Multimodal)",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "deepseek-v4.1-flash",
                displayName = "DeepSeek V4.1 Flash (Code & Reasoning)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "glm-5-3-flash",
                displayName = "GLM 5.3 Flash (Multimodal & Tool Calling)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "nvidia",
                modelId = "Mistral Large 4",
                displayName = "Mistral Large 4 (Function Calling)",
                contextLength = 128_000,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
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
                pinned = false
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
                pinned = false
            ),

            // Groq (Ultra-Fast Inference)
            ModelDescriptor(
                providerId = "groq",
                modelId = "openai/gpt-oss-120b",
                displayName = "GPT-OSS 120B on Groq",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "groq",
                modelId = "openai/gpt-oss-20b",
                displayName = "GPT-OSS 20B on Groq",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "groq",
                modelId = "qwen/qwen3.8-27b",
                displayName = "Qwen 3.8 27B on Groq",
                contextLength = 65_536,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),

            // Hugging Face Router
            ModelDescriptor(
                providerId = "huggingface",
                modelId = "openai/gpt-oss-120b:fastest",
                displayName = "GPT-OSS 120B (HuggingFace Fastest)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "huggingface",
                modelId = "openai/gpt-oss-120b:cheapest",
                displayName = "GPT-OSS 120B (HuggingFace Cheapest)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),

            // Cerebras (Wafer-Scale Inference)
            ModelDescriptor(
                providerId = "cerebras",
                modelId = "qwen/qwen-3-coder-480b",
                displayName = "Qwen 3 Coder 480B (Cerebras)",
                contextLength = 131_072,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
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
                modelId = "claude-3-7-sonnet-latest",
                displayName = "Claude 3.7 Sonnet (Hybrid)",
                contextLength = 200_000,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = true,
                isFree = false,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "anthropic",
                modelId = "claude-3-5-sonnet-latest",
                displayName = "Claude 3.5 Sonnet",
                contextLength = 200_000,
                supportsTools = true,
                supportsVision = true,
                supportsThinking = false,
                isFree = false,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "anthropic",
                modelId = "claude-3-5-haiku-latest",
                displayName = "Claude 3.5 Haiku",
                contextLength = 200_000,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = false,
                pinned = false
            ),

            // OpenCode Zen / Free Tier
            ModelDescriptor(
                providerId = "opencode",
                modelId = "qwen/qwen-2.5-coder-32b-instruct:free",
                displayName = "Qwen 2.5 Coder 32B (Zen Free)",
                contextLength = 32_768,
                supportsTools = true,
                supportsVision = false,
                supportsThinking = false,
                isFree = true,
                openWeights = true,
                pinned = true
            ),
            ModelDescriptor(
                providerId = "opencode",
                modelId = "deepseek/deepseek-r1:free",
                displayName = "DeepSeek R1 (Zen Free)",
                contextLength = 65_536,
                supportsTools = false,
                supportsVision = false,
                supportsThinking = true,
                isFree = true,
                openWeights = true,
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

    private fun normalizeProviderId(id: String): String = when (id.lowercase().trim()) {
        "llm_router", "openrouter" -> "openrouter"
        "nvidia_nim", "nvidia" -> "nvidia"
        "google_gemini", "gemini" -> "gemini"
        "huggingface", "hf" -> "huggingface"
        "cerebras" -> "cerebras"
        "groq" -> "groq"
        "ollama", "local" -> "ollama"
        else -> id.lowercase().trim()
    }

    fun getModelsForProvider(providerId: String): List<ModelDescriptor> {
        val target = normalizeProviderId(providerId)
        return models.values
            .filter { normalizeProviderId(it.providerId) == target }
            .sortedWith(
                compareByDescending<ModelDescriptor> { it.pinned }
                    .thenByDescending { it.isFree }
                    .thenByDescending { it.contextLength }
            )
    }

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

