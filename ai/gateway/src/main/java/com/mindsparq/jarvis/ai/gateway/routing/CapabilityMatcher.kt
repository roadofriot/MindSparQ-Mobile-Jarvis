package com.mindsparq.jarvis.ai.gateway.routing

import com.mindsparq.jarvis.core.model.ModelDescriptor
import kotlinx.serialization.Serializable

@Serializable
data class TaskRequirement(
    val requiresTools: Boolean = false,
    val requiresVision: Boolean = false,
    val requiresReasoning: Boolean = false,
    val requiresCoding: Boolean = false,
    val minContextTokens: Int = 8192,
    val allowPaid: Boolean = false,
    val preferLocalOffline: Boolean = false
)

object CapabilityMatcher {

    /**
     * Determines if a model meets the baseline non-negotiable criteria for a task.
     */
    fun satisfies(model: ModelDescriptor, requirement: TaskRequirement): Boolean {
        if (requirement.requiresTools && !model.supportsTools) return false
        if (requirement.requiresVision && !model.supportsVision) return false
        if (requirement.requiresReasoning && !model.supportsThinking) return false
        if (model.contextLength < requirement.minContextTokens) return false
        if (!requirement.allowPaid && !model.isFree) return false
        return true
    }

    /**
     * Scores a model for a given task requirement. Higher score is better.
     */
    fun scoreModel(model: ModelDescriptor, requirement: TaskRequirement): Int {
        if (!satisfies(model, requirement)) return -1

        var score = 100

        // Local preference
        if (requirement.preferLocalOffline && model.providerId.equals("ollama", ignoreCase = true)) {
            score += 200
        }

        // Free tier preference
        if (model.isFree) {
            score += 50
        }

        // Pinned / Recommended
        if (model.pinned) {
            score += 30
        }

        // Large context bonus
        if (model.contextLength >= 100_000) {
            score += 25
        } else if (model.contextLength >= 32_000) {
            score += 10
        }

        // Specific task bonuses
        if (requirement.requiresReasoning && model.supportsThinking) {
            score += 40
        }
        if (requirement.requiresCoding && (model.modelId.contains("coder") || model.displayName.contains("Coder") || model.providerId == "gemini")) {
            score += 30
        }

        return score
    }

    /**
     * Filters and sorts candidate models by score descending.
     */
    fun rankCandidates(candidates: List<ModelDescriptor>, requirement: TaskRequirement): List<ModelDescriptor> {
        return candidates
            .map { model -> model to scoreModel(model, requirement) }
            .filter { (_, score) -> score >= 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (model, _) -> model }
    }
}

