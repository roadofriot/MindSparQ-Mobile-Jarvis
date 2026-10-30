package com.mindsparq.jarvis.ai.gateway.routing

import com.mindsparq.jarvis.ai.gateway.registry.ModelRegistry
import com.mindsparq.jarvis.ai.gateway.registry.ProviderRegistry
import com.mindsparq.jarvis.core.model.ModelDescriptor

data class RoutingPlan(
    val primary: ModelDescriptor,
    val fallbacks: List<ModelDescriptor>,
    val reason: String
)

class ModelRouter(
    private val modelRegistry: ModelRegistry = ModelRegistry.instance,
    private val providerRegistry: ProviderRegistry = ProviderRegistry.instance
) {

    /**
     * Resolves an execution plan consisting of a primary model and an ordered chain of fallback models.
     */
    fun resolvePlan(
        requestedProviderId: String?,
        requestedModelId: String?,
        requirement: TaskRequirement = TaskRequirement()
    ): RoutingPlan {
        val allModels = modelRegistry.getAllModels()

        // 1. Try to honor requested model if specified and compliant
        val explicitModel = if (!requestedProviderId.isNullOrBlank() && !requestedModelId.isNullOrBlank()) {
            modelRegistry.getModel(requestedProviderId, requestedModelId)
        } else null

        val primary: ModelDescriptor = if (explicitModel != null && CapabilityMatcher.satisfies(explicitModel, requirement)) {
            explicitModel
        } else {
            // Find top ranked candidate across registered models
            val candidates = if (!requestedProviderId.isNullOrBlank()) {
                modelRegistry.getModelsForProvider(requestedProviderId)
            } else {
                allModels
            }
            val ranked = CapabilityMatcher.rankCandidates(candidates, requirement)
            ranked.firstOrNull() ?: allModels.firstOrNull() ?: ModelDescriptor(
                providerId = "gemini",
                modelId = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                contextLength = 1_048_576,
                isFree = true
            )
        }

        // 2. Build intelligent fallback chain across different providers
        val fallbackCandidates = allModels.filter { candidate ->
            // Different provider or significantly different model
            (candidate.providerId != primary.providerId || candidate.modelId != primary.modelId) &&
                CapabilityMatcher.satisfies(candidate, requirement)
        }

        // Group by provider to avoid 5 models from the same failed provider
        val rankedFallbacks = CapabilityMatcher.rankCandidates(fallbackCandidates, requirement)
        val selectedFallbacks = mutableListOf<ModelDescriptor>()
        val seenProviders = mutableSetOf(primary.providerId.lowercase())

        // First pass: one diverse model per healthy fallback provider
        for (candidate in rankedFallbacks) {
            val pId = candidate.providerId.lowercase()
            if (!seenProviders.contains(pId)) {
                selectedFallbacks.add(candidate)
                seenProviders.add(pId)
            }
            if (selectedFallbacks.size >= 3) break
        }

        // Always guarantee Ollama local is in fallback chain if offline execution is possible
        if (!seenProviders.contains("ollama")) {
            val ollamaFallback = modelRegistry.getModelsForProvider("ollama")
                .firstOrNull { CapabilityMatcher.satisfies(it, requirement.copy(allowPaid = false)) }
            if (ollamaFallback != null) {
                selectedFallbacks.add(ollamaFallback)
            }
        }

        return RoutingPlan(
            primary = primary,
            fallbacks = selectedFallbacks,
            reason = "Selected primary ${primary.displayName} [${primary.providerId}] with ${selectedFallbacks.size} cross-provider fallbacks."
        )
    }
}

