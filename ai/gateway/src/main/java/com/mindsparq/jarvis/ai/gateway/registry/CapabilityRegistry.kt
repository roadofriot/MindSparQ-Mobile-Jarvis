package com.mindsparq.jarvis.ai.gateway.registry

import kotlinx.serialization.Serializable

@Serializable
enum class ModelCapability {
    CODE_GENERATION,
    COMPLEX_REASONING,
    TOOLS_FUNCTION_CALLING,
    VISION_MULTIMODAL,
    AUDIO_PROCESSING,
    OFFLINE_LOCAL,
    STREAMING
}

@Serializable
data class ModelCapabilityProfile(
    val modelId: String,
    val providerId: String,
    val contextWindow: Int = 32000,
    val capabilities: Set<ModelCapability> = emptySet(),
    val isFree: Boolean = false,
    val openWeights: Boolean = false,
    val maxOutputTokens: Int = 4096
) {
    fun hasCapability(capability: ModelCapability): Boolean = capabilities.contains(capability)
    fun supportsTools(): Boolean = hasCapability(ModelCapability.TOOLS_FUNCTION_CALLING)
    fun supportsVision(): Boolean = hasCapability(ModelCapability.VISION_MULTIMODAL)
    fun supportsReasoning(): Boolean = hasCapability(ModelCapability.COMPLEX_REASONING)
    fun supportsCoding(): Boolean = hasCapability(ModelCapability.CODE_GENERATION)
}

