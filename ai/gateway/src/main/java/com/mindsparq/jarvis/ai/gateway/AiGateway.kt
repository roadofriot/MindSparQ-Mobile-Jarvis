package com.mindsparq.jarvis.ai.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface LlmProvider {
    val providerId: String
    suspend fun executeChat(request: ChatRequest, key: ManagedKey): Flow<ChatResponseEvent>
}

class AiGateway(
    private val keyPool: KeyPoolManager,
    private val providers: Map<String, LlmProvider>,
    private val costPolicy: CostPolicyEngine = CostPolicyEngine()
) {

    fun executeWithFallback(
        request: ChatRequest,
        primaryProviderId: String,
        fallbackProviderIds: List<String> = emptyList()
    ): Flow<ChatResponseEvent> = flow {
        val candidateProviders = listOf(primaryProviderId) + fallbackProviderIds
        var lastRouteDescription: String? = null

        for (providerId in candidateProviders) {
            val provider = providers[providerId] ?: continue

            while (true) {
                val candidateKey = keyPool.acquireCandidateKey(providerId)
                if (candidateKey == null) {
                    // No healthy keys or eligible quota scopes left for this provider, advance to fallback provider
                    break
                }

                // Verify with Cost Policy Engine
                if (!costPolicy.isExecutionPermitted(providerId, request.model, candidateKey.billingTier)) {
                    continue
                }

                val currentRoute = "${candidateKey.label} [${candidateKey.quotaScopeId}]"
                if (lastRouteDescription != null && lastRouteDescription != currentRoute) {
                    emit(
                        ChatResponseEvent.RouteSwitched(
                            fromKeyOrProvider = lastRouteDescription!!,
                            toKeyOrProvider = currentRoute,
                            reason = "Failover to next eligible credential/quota-scope"
                        )
                    )
                }
                lastRouteDescription = currentRoute

                var shouldRetryNextKeyOrScope = false
                var totalIn = 0
                var totalOut = 0

                try {
                    provider.executeChat(request, candidateKey).collect { event ->
                        when (event) {
                            is ChatResponseEvent.Error -> {
                                if (event.statusCode == 401 || event.statusCode == 403) {
                                    // Key-level authentication error: invalidate key, don't kill whole project
                                    keyPool.reportKeyAuthFailure(candidateKey.id, event.message)
                                    shouldRetryNextKeyOrScope = true
                                } else if (event.statusCode == 402) {
                                    // Billing blocked on project
                                    keyPool.reportBillingBlocked(candidateKey.quotaScopeId, event.message)
                                    shouldRetryNextKeyOrScope = true
                                } else if (event.statusCode == 429 || event.isRateLimit) {
                                    val isProjectQuota = event.message.contains("quota", ignoreCase = true) ||
                                        event.message.contains("resource_exhausted", ignoreCase = true) ||
                                        event.message.contains("daily", ignoreCase = true)

                                    if (isProjectQuota) {
                                        // Project/Account quota exhausted: marks entire quota scope so other keys in it are skipped!
                                        keyPool.reportProjectQuotaExhausted(candidateKey.quotaScopeId, error = event.message)
                                    } else {
                                        // Temporary RPM rate limit on key
                                        keyPool.reportRateLimit(candidateKey.id, 60_000L)
                                    }
                                    shouldRetryNextKeyOrScope = true
                                } else {
                                    emit(event)
                                }
                            }
                            is ChatResponseEvent.Usage -> {
                                totalIn += event.tokensIn
                                totalOut += event.tokensOut
                                emit(event)
                            }
                            else -> emit(event)
                        }
                    }
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("quota", ignoreCase = true) || msg.contains("resource_exhausted", ignoreCase = true)) {
                        keyPool.reportProjectQuotaExhausted(candidateKey.quotaScopeId, error = msg)
                    } else {
                        keyPool.reportRateLimit(candidateKey.id, 60_000L)
                    }
                    shouldRetryNextKeyOrScope = true
                }

                if (!shouldRetryNextKeyOrScope) {
                    keyPool.reportSuccess(candidateKey.id, candidateKey.quotaScopeId, totalIn, totalOut)
                    return@flow
                }
            }
        }

        // All configured providers and quota scopes exhausted!
        emit(
            ChatResponseEvent.Error(
                statusCode = 429,
                message = "All configured API keys, quota scopes, and fallback providers exhausted",
                isRateLimit = true
            )
        )
    }
}

