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

object ModelCatalog {

    fun filterOpenRouterFreeModels(models: List<OpenRouterModelJson>): List<OpenRouterModelJson> {
        return models.filter { model ->
            // Condition 1: ID ends with ":free" OR prompt & completion pricing is exactly "0"
            val hasFreeId = model.id.endsWith(":free")
            val promptPrice = model.pricing?.get("prompt")?.jsonPrimitive?.contentOrNull
            val completionPrice = model.pricing?.get("completion")?.jsonPrimitive?.contentOrNull
            val isZeroPrice = (promptPrice == "0" || promptPrice == "0.0") && (completionPrice == "0" || completionPrice == "0.0")

            val isFree = hasFreeId || isZeroPrice

            // Condition 2: Must support tools for agentic coding workflows
            val supportsTools = model.supported_parameters.contains("tools") || model.supported_parameters.contains("function_calling")

            // Condition 3: Minimum context length 32k
            val sufficientContext = model.context_length >= 32000

            isFree && supportsTools && sufficientContext
        }.sortedByDescending { it.context_length }
    }

    fun rankGeminiModels(modelIds: List<String>): List<String> {
        // Ranks Flash and Pro models, filtering out embeddings, tts, and vision-only
        val regex = Regex("""^gemini-(\d+(?:\.\d+)?)-(flash-lite|flash|pro)(?:-(preview|exp)[\w-]*)?$""")

        return modelIds
            .filter { regex.matches(it) }
            .sortedWith(
                compareByDescending<String> {
                    val match = regex.find(it)
                    match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                }.thenBy {
                    // Stable models before preview
                    if (it.contains("preview") || it.contains("exp")) 1 else 0
                }
            )
    }
}

