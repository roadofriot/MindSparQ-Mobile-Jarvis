package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OpenRouterModelJson(
    val id: String,
    val name: String? = null,
    val context_length: Int = 32000,
    val pricing: JsonObject? = null,
    val supported_parameters: List<String> = emptyList()
)

@Serializable
data class KeyValidationResult(
    val isValid: Boolean,
    val latencyMs: Long,
    val provider: String,
    val error: String? = null
)

object ModelCatalog {

    val NVIDIA_NIM_MODELS = listOf(
        "meta/llama-3.3-70b-instruct",
        "deepseek-ai/deepseek-r1",
        "mistralai/mistral-large-2-instruct",
        "nvidia/llama-3.1-nemotron-70b-instruct"
    )

    fun filterOpenRouterFreeModels(models: List<OpenRouterModelJson>): List<OpenRouterModelJson> {
        return models.filter { model ->
            val hasFreeId = model.id.endsWith(":free")
            val promptPrice = model.pricing?.get("prompt")?.jsonPrimitive?.contentOrNull
            val completionPrice = model.pricing?.get("completion")?.jsonPrimitive?.contentOrNull
            val isZeroPrice = (promptPrice == "0" || promptPrice == "0.0") && (completionPrice == "0" || completionPrice == "0.0")

            val isFree = hasFreeId || isZeroPrice
            val supportsTools = model.supported_parameters.contains("tools") || model.supported_parameters.contains("function_calling")
            val sufficientContext = model.context_length >= 32000

            isFree && supportsTools && sufficientContext
        }.sortedByDescending { it.context_length }
    }

    fun rankGeminiModels(modelIds: List<String>): List<String> {
        val regex = Regex("""^gemini-(\d+(?:\.\d+)?)-(flash-lite|flash|pro)(?:-(preview|exp)[\w-]*)?$""")

        return modelIds
            .filter { regex.matches(it) }
            .sortedWith(
                compareByDescending<String> {
                    val match = regex.find(it)
                    match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                }.thenBy {
                    if (it.contains("preview") || it.contains("exp")) 1 else 0
                }
            )
    }

    fun simulateOrProbeKey(provider: String, apiKey: String): KeyValidationResult {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank() || trimmed.length < 10) {
            return KeyValidationResult(
                isValid = false,
                latencyMs = 0,
                provider = provider,
                error = "API key is too short or empty."
            )
        }
        return when (provider.lowercase()) {
            "gemini" -> {
                if (trimmed.startsWith("AIzaSy")) {
                    KeyValidationResult(isValid = true, latencyMs = 120, provider = provider)
                } else {
                    KeyValidationResult(isValid = false, latencyMs = 0, provider = provider, error = "Invalid Gemini API key prefix.")
                }
            }
            "openrouter" -> {
                if (trimmed.startsWith("sk-or-")) {
                    KeyValidationResult(isValid = true, latencyMs = 180, provider = provider)
                } else {
                    KeyValidationResult(isValid = false, latencyMs = 0, provider = provider, error = "Invalid OpenRouter API key prefix.")
                }
            }
            "nvidia" -> {
                if (trimmed.startsWith("nvapi-")) {
                    KeyValidationResult(isValid = true, latencyMs = 140, provider = provider)
                } else {
                    KeyValidationResult(isValid = false, latencyMs = 0, provider = provider, error = "Invalid NVIDIA API key prefix (must start with nvapi-).")
                }
            }
            else -> KeyValidationResult(isValid = true, latencyMs = 200, provider = provider)
        }
    }
}
