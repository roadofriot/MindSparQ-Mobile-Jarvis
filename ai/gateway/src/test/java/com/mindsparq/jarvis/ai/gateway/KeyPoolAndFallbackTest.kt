package com.mindsparq.jarvis.ai.gateway

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun testGatewayAutomaticFallbackOn429() = runBlocking {
        val pool = KeyPoolManager()
        val key1 = ManagedKey(id = "k1", providerId = "gemini", secretKey = "key-1", label = "Key 1")
        val key2 = ManagedKey(id = "k2", providerId = "gemini", secretKey = "key-2", label = "Key 2")
        pool.registerKeys("gemini", listOf(key1, key2))

        var key1Calls = 0
        var key2Calls = 0

        val mockProvider = object : LlmProvider {
            override val providerId: String = "gemini"
            override suspend fun executeChat(request: ChatRequest, key: ManagedKey) = flow {
                if (key.id == "k1") {
                    key1Calls++
                    emit(ChatResponseEvent.Error(429, "Resource exhausted (quota)", isRateLimit = true))
                } else if (key.id == "k2") {
                    key2Calls++
                    emit(ChatResponseEvent.TextDelta("Hello from Key 2!"))
                    emit(ChatResponseEvent.Done)
                }
            }
        }

        val gateway = AiGateway(pool, mapOf("gemini" to mockProvider))
        val request = ChatRequest(model = "gemini-flash", messages = listOf(ChatMessage(ChatRole.USER, "Hi")))

        val events = gateway.executeWithFallback(request, "gemini").toList()

        // Verify fallback behavior
        assertEquals(1, key1Calls)
        assertEquals(1, key2Calls)

        // Verify RouteSwitched event was emitted
        val switchEvent = events.find { it is ChatResponseEvent.RouteSwitched } as? ChatResponseEvent.RouteSwitched
        assertTrue(switchEvent != null)
        assertEquals("k1", switchEvent?.fromKeyOrProvider)
        assertEquals("Key 2", switchEvent?.toKeyOrProvider)

        // Verify successful text delta from Key 2
        val textDelta = events.find { it is ChatResponseEvent.TextDelta } as? ChatResponseEvent.TextDelta
        assertEquals("Hello from Key 2!", textDelta?.text)
    }
}

