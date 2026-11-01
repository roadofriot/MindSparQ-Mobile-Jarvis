package com.mindsparq.jarvis.ai.gateway

import com.mindsparq.jarvis.core.model.BillingTier

data class CostPolicy(
    val allowGeminiPaidApi: Boolean = true,
    val allowPaidModels: Boolean = false,
    val allowPaidFallback: Boolean = false,
    val allowUnverifiedPricing: Boolean = false,
    val allowVerifiedFreeModels: Boolean = true,
    val allowLocalModels: Boolean = true,
    val requireExplicitApprovalForNewPaidProvider: Boolean = true,
    val enforceProviderTerms: Boolean = true,
    val respectAccountQuotas: Boolean = true
)

class CostPolicyEngine(
    var policy: CostPolicy = CostPolicy()
) {
    /**
     * Verifies if a given provider and credential tier are allowed under current policy.
     */
    fun isExecutionPermitted(providerId: String, modelId: String, tier: BillingTier): Boolean {
        val pId = providerId.lowercase()

        // 1. Local offline models always permitted if allowLocalModels is true
        if (pId in setOf("ollama", "llama.cpp", "local")) {
            return policy.allowLocalModels
        }

        // 2. Gemini Paid API is explicitly authorized if allowGeminiPaidApi is true
        if (pId == "gemini" && tier == BillingTier.PAID) {
            return policy.allowGeminiPaidApi
        }

        // 3. Free verified models permitted
        if (tier == BillingTier.FREE || modelId.endsWith(":free") || modelId.contains("free")) {
            return policy.allowVerifiedFreeModels
        }

        // 4. Any other paid models require explicit permission
        if (tier == BillingTier.PAID) {
            return policy.allowPaidModels
        }

        // 5. Unverified pricing
        return policy.allowUnverifiedPricing
    }
}

