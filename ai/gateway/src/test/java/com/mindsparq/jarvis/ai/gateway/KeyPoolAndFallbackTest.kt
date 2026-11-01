package com.mindsparq.jarvis.ai.gateway

import com.mindsparq.jarvis.core.model.BillingTier
import com.mindsparq.jarvis.core.model.QuotaState
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyPoolAndFallbackTest {

    @Test
    fun testKeyPoolRotationAndCooldown() {
        val pool = KeyPoolManager()
        val key1 = ManagedKey(id = "k1", providerId = "gemini", secretKey = "key-1", label = "Gemini Key 1", priority = 1)
        val key2 = ManagedKey(id = "k2", providerId = "gemini", secretKey = "key-2", label = "Gemini Key 2", priority = 2)

        pool.registerKeys("gemini", listOf(key1, key2))

        // First key acquired should be key1 (lower priority number = higher priority)
        val acquired1 = pool.acquireCandidateKey("gemini")
        assertEquals("k1", acquired1?.id)

        // Mark key1 as rate limited
        pool.reportRateLimit("k1", cooldownDurationMs = 10_000L)
        assertEquals(KeyState.COOLING, key1.state)

        // Next acquire should automatically pick key2!
        val acquired2 = pool.acquireCandidateKey("gemini")
        assertEquals("k2", acquired2?.id)
    }

    @Test
    fun testSharedQuotaScopeDoesNotWasteRetriesOnSameProject() = runBlocking {
        val pool = KeyPoolManager()
        // Two keys sharing Google Cloud Project A
        val keyA1 = ManagedKey(
            id = "kA1",
            providerId = "gemini",
            secretKey = "key-a1",
            label = "Key A1",
            quotaScopeId = "google-project-a",
            billingTier = BillingTier.PAID,
            priority = 1
        )
        val keyA2 = ManagedKey(
            id = "kA2",
            providerId = "gemini",
            secretKey = "key-a2",
            label = "Key A2",
            quotaScopeId = "google-project-a",
            billingTier = BillingTier.PAID,
            priority = 2
        )
        // One key belonging to independent Google Cloud Project B
        val keyB1 = ManagedKey(
            id = "kB1",
            providerId = "gemini",
            secretKey = "key-b1",
            label = "Key B1",
            quotaScopeId = "google-project-b",
            billingTier = BillingTier.PAID,
            priority = 3
        )

        pool.registerKeys("gemini", listOf(keyA1, keyA2, keyB1))

        var keyA1Calls = 0
        var keyA2Calls = 0
        var keyB1Calls = 0

        val mockProvider = object : LlmProvider {
            override val providerId: String = "gemini"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                when (key.id) {
                    "kA1" -> {
                        keyA1Calls++
                        // Project-level daily quota exhaustion
                        emit(ChatResponseEvent.Error(429, "Resource exhausted (quota limit reached for project)", isRateLimit = true))
                    }
                    "kA2" -> {
                        keyA2Calls++
                        emit(ChatResponseEvent.TextDelta("Should not be called!"))
                    }
                    "kB1" -> {
                        keyB1Calls++
                        emit(ChatResponseEvent.TextDelta("Success from Project B!"))
                        emit(ChatResponseEvent.Done)
                    }
                }
            }
        }

        val gateway = AiGateway(pool, mapOf("gemini" to mockProvider))
        val request = ChatRequest(model = "gemini-4.0-flash", messages = listOf(ChatMessage(ChatRole.USER, "Hi")))

        val events = gateway.executeWithFallback(request, "gemini").toList()

        // Evidence: Key A1 was tried, but Key A2 was SKIPPED because Project A quota is exhausted!
        assertEquals(1, keyA1Calls)
        assertEquals(0, keyA2Calls)
        assertEquals(1, keyB1Calls)

        // Project A is marked EXHAUSTED
        assertEquals(QuotaState.QUOTA_EXHAUSTED, pool.getQuotaScope("google-project-a")?.state)

        val textDelta = events.find { it is ChatResponseEvent.TextDelta } as? ChatResponseEvent.TextDelta
        assertEquals("Success from Project B!", textDelta?.text)
    }

    @Test
    fun testAuthFailureInvalidatesSingleKeyWithoutExhaustingProjectScope() = runBlocking {
        val pool = KeyPoolManager()
        val keyA1 = ManagedKey(
            id = "kA1",
            providerId = "gemini",
            secretKey = "bad-key",
            label = "Key A1 (Revoked)",
            quotaScopeId = "google-project-a",
            billingTier = BillingTier.PAID,
            priority = 1
        )
        val keyA2 = ManagedKey(
            id = "kA2",
            providerId = "gemini",
            secretKey = "good-key",
            label = "Key A2 (Valid)",
            quotaScopeId = "google-project-a",
            billingTier = BillingTier.PAID,
            priority = 2
        )

        pool.registerKeys("gemini", listOf(keyA1, keyA2))

        var keyA1Calls = 0
        var keyA2Calls = 0

        val mockProvider = object : LlmProvider {
            override val providerId: String = "gemini"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                if (key.id == "kA1") {
                    keyA1Calls++
                    emit(ChatResponseEvent.Error(401, "API key not valid", isRateLimit = false))
                } else if (key.id == "kA2") {
                    keyA2Calls++
                    emit(ChatResponseEvent.TextDelta("Recovered via Key A2 in same project!"))
                    emit(ChatResponseEvent.Done)
                }
            }
        }

        val gateway = AiGateway(pool, mapOf("gemini" to mockProvider))
        val request = ChatRequest(model = "gemini-4.0-flash", messages = listOf(ChatMessage(ChatRole.USER, "Test")))

        val events = gateway.executeWithFallback(request, "gemini").toList()

        assertEquals(1, keyA1Calls)
        assertEquals(1, keyA2Calls)
        assertEquals(KeyState.INVALID, keyA1.state)
        assertEquals(KeyState.HEALTHY, keyA2.state)
        // Project A is still AVAILABLE because only the credential failed, not the project quota!
        assertEquals(QuotaState.AVAILABLE, pool.getQuotaScope("google-project-a")?.state)

        val textDelta = events.find { it is ChatResponseEvent.TextDelta } as? ChatResponseEvent.TextDelta
        assertEquals("Recovered via Key A2 in same project!", textDelta?.text)
    }

    @Test
    fun testCrossProviderFailoverToOpenRouterAndLocal() = runBlocking {
        val pool = KeyPoolManager()
        val geminiKey = ManagedKey(id = "gem1", providerId = "gemini", secretKey = "gkey", label = "Gemini", quotaScopeId = "gem-proj")
        val openRouterKey = ManagedKey(id = "or1", providerId = "openrouter", secretKey = "orkey", label = "OpenRouter", quotaScopeId = "or-acct")
        val ollamaKey = ManagedKey(id = "ol1", providerId = "ollama", secretKey = "no-key", label = "Ollama Local", quotaScopeId = "local")

        pool.registerKeys("gemini", listOf(geminiKey))
        pool.registerKeys("openrouter", listOf(openRouterKey))
        pool.registerKeys("ollama", listOf(ollamaKey))

        val mockGemini = object : LlmProvider {
            override val providerId: String = "gemini"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                emit(ChatResponseEvent.Error(429, "Quota exhausted", isRateLimit = true))
            }
        }
        val mockOpenRouter = object : LlmProvider {
            override val providerId: String = "openrouter"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                emit(ChatResponseEvent.Error(429, "Free tier rate limit", isRateLimit = true))
            }
        }
        val mockOllama = object : LlmProvider {
            override val providerId: String = "ollama"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                emit(ChatResponseEvent.TextDelta("Offline Inference Success!"))
                emit(ChatResponseEvent.Done)
            }
        }

        val gateway = AiGateway(
            pool,
            mapOf("gemini" to mockGemini, "openrouter" to mockOpenRouter, "ollama" to mockOllama)
        )
        val request = ChatRequest(model = "default", messages = listOf(ChatMessage(ChatRole.USER, "Coding task")))

        val events = gateway.executeWithFallback(
            request,
            primaryProviderId = "gemini",
            fallbackProviderIds = listOf("openrouter", "ollama")
        ).toList()

        val textDelta = events.find { it is ChatResponseEvent.TextDelta } as? ChatResponseEvent.TextDelta
        assertEquals("Offline Inference Success!", textDelta?.text)
    }

    @Test
    fun testPaidCostProtectionBlocksUnapprovedPaidModels() {
        val costPolicyEngine = CostPolicyEngine(
            CostPolicy(
                allowGeminiPaidApi = true,
                allowPaidModels = false, // All other paid models forbidden
                allowVerifiedFreeModels = true,
                allowLocalModels = true
            )
        )

        // Gemini Paid is allowed
        assertTrue(costPolicyEngine.isExecutionPermitted("gemini", "gemini-4.0-flash", BillingTier.PAID))

        // OpenRouter Free is allowed
        assertTrue(costPolicyEngine.isExecutionPermitted("openrouter", "qwen/qwen-2.5-coder-32b-instruct:free", BillingTier.FREE))

        // Local Ollama is allowed
        assertTrue(costPolicyEngine.isExecutionPermitted("ollama", "qwen2.5-coder:1.5b", BillingTier.FREE))

        // Paid model on other provider without permission is BLOCKED
        assertFalse(costPolicyEngine.isExecutionPermitted("openai", "gpt-4o", BillingTier.PAID))
        assertFalse(costPolicyEngine.isExecutionPermitted("anthropic", "claude-3-7-sonnet", BillingTier.PAID))
    }
}

