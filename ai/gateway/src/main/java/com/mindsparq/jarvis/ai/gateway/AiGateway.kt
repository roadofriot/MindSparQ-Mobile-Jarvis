package com.mindsparq.jarvis.ai.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface LlmProvider {
    val providerId: String
    suspend fun executeChat(request: ChatRequest, key: ManagedKey): Flow<ChatResponseEvent>
}

class AiGateway(
    private val keyPool: KeyPoolManager,
    private val providers: Map<String, LlmProvider>
) {

    fun executeWithFallback(
        request: ChatRequest,
        primaryProviderId: String,
        fallbackProviderIds: List<String> = emptyList()
    ): Flow<ChatResponseEvent> = flow {
        val candidateProviders = listOf(primaryProviderId) + fallbackProviderIds
        var lastKeyId: String? = null

        for (providerId in candidateProviders) {
            val provider = providers[providerId] ?: continue

            while (true) {
                val candidateKey = keyPool.acquireCandidateKey(providerId)
                if (candidateKey == null) {
                    // No healthy keys left for this provider, advance to fallback provider
                    break
                }

                if (lastKeyId != null && lastKeyId != candidateKey.id) {
                    emit(
                        ChatResponseEvent.RouteSwitched(
                            fromKeyOrProvider = lastKeyId!!,
                            toKeyOrProvider = candidateKey.label,
                            reason = "Rate limit or quota exhaustion on previous route"
                        )
                    )
                }
                lastKeyId = candidateKey.id

                var rateLimited = false
                try {
                    provider.executeChat(request, candidateKey).collect { event ->
                        if (event is ChatResponseEvent.Error && event.isRateLimit) {
                            rateLimited = true
                            keyPool.reportRateLimit(candidateKey.id, 60_000L)
                        } else {
                            emit(event)
                        }
                    }
                } catch (e: Exception) {
                    rateLimited = true
                    keyPool.reportRateLimit(candidateKey.id, 60_000L)
                }

                if (!rateLimited) {
                    // Request finished without rate-limit error
                    return@flow
                }
                // If rate limited, loop continues to pick next key in pool
            }
        }

        // All providers and keys exhausted!
        emit(
            ChatResponseEvent.Error(
                statusCode = 429,
                message = "All configured API keys and fallback providers exhausted",
                isRateLimit = true
            )
        )
    }
}

