package com.mindsparq.jarvis.ai.gateway.registry

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
enum class ProviderProtocolKind {
    OPENAI_CHAT_COMPLETIONS,
    ANTHROPIC_MESSAGES,
    GEMINI_GENERATE_CONTENT,
    OLLAMA_NATIVE,
    CUSTOM_HTTP
}

@Serializable
data class ProviderDescriptor(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val protocol: ProviderProtocolKind,
    val defaultModelId: String,
    val supportsKeyPool: Boolean = true,
    val isLocalOffline: Boolean = false,
    val isFreeTierAvailable: Boolean = false,
    val websiteUrl: String = "",
    val authPlaceholder: String = ""
)

class ProviderRegistry private constructor() {

    private val providers = ConcurrentHashMap<String, ProviderDescriptor>()

    init {
        registerBuiltIns()
    }

    private fun registerBuiltIns() {
        val builtIns = listOf(
            ProviderDescriptor(
                id = "gemini",
                displayName = "Google Gemini",
                defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
                protocol = ProviderProtocolKind.GEMINI_GENERATE_CONTENT,
                defaultModelId = "gemini-2.5-flash",
                supportsKeyPool = true,
                isFreeTierAvailable = true,
                websiteUrl = "https://aistudio.google.com/app/apikey"
            ),
            ProviderDescriptor(
                id = "openrouter",
                displayName = "OpenRouter",
                defaultBaseUrl = "https://openrouter.ai/api/v1",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "meta-llama/llama-3.3-70b-instruct:free",
                supportsKeyPool = true,
                isFreeTierAvailable = true,
                websiteUrl = "https://openrouter.ai/keys"
            ),
            ProviderDescriptor(
                id = "deepseek",
                displayName = "DeepSeek",
                defaultBaseUrl = "https://api.deepseek.com",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "deepseek-chat",
                supportsKeyPool = true,
                isFreeTierAvailable = false,
                websiteUrl = "https://platform.deepseek.com/api_keys"
            ),
            ProviderDescriptor(
                id = "nvidia",
                displayName = "NVIDIA NIM",
                defaultBaseUrl = "https://integrate.api.nvidia.com/v1",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "meta/llama-3.3-70b-instruct",
                supportsKeyPool = true,
                isFreeTierAvailable = true,
                websiteUrl = "https://build.nvidia.com/"
            ),
            ProviderDescriptor(
                id = "ollama",
                displayName = "Ollama (Local / Offline)",
                defaultBaseUrl = "http://127.0.0.1:11434/v1",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "qwen2.5-coder:1.5b",
                supportsKeyPool = false,
                isLocalOffline = true,
                isFreeTierAvailable = true,
                authPlaceholder = "no-key-required"
            ),
            ProviderDescriptor(
                id = "anthropic",
                displayName = "Anthropic API",
                defaultBaseUrl = "https://api.anthropic.com",
                protocol = ProviderProtocolKind.ANTHROPIC_MESSAGES,
                defaultModelId = "claude-sonnet-4-6",
                supportsKeyPool = true,
                isFreeTierAvailable = false,
                websiteUrl = "https://console.anthropic.com/"
            ),
            ProviderDescriptor(
                id = "opencode",
                displayName = "OpenCode Zen",
                defaultBaseUrl = "https://opencode.ai/zen/v1",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "deepseek-v4-flash",
                supportsKeyPool = true,
                isFreeTierAvailable = false
            ),
            ProviderDescriptor(
                id = "custom",
                displayName = "Custom OpenAI Endpoint",
                defaultBaseUrl = "",
                protocol = ProviderProtocolKind.OPENAI_CHAT_COMPLETIONS,
                defaultModelId = "",
                supportsKeyPool = true,
                isFreeTierAvailable = false
            )
        )
        builtIns.forEach { providers[it.id] = it }
    }

    fun getProvider(id: String): ProviderDescriptor? = providers[id.lowercase()]

    fun getAllProviders(): List<ProviderDescriptor> = providers.values.toList()

    fun registerCustomProvider(descriptor: ProviderDescriptor) {
        providers[descriptor.id.lowercase()] = descriptor
    }

    companion object {
        val instance by lazy { ProviderRegistry() }
    }
}

