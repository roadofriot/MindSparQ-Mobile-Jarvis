package com.mindsparq.jarvis.ai.gateway

import java.util.concurrent.ConcurrentHashMap

enum class KeyState {
    HEALTHY,
    COOLING,
    EXHAUSTED,
    INVALID
}

data class ManagedKey(
    val id: String,
    val providerId: String,
    val secretKey: String,
    val label: String,
    var state: KeyState = KeyState.HEALTHY,
    var cooldownUntil: Long = 0L,
    var priority: Int = 10,
    var requestsToday: Int = 0,
    var lastUsedAt: Long = 0L
)

class KeyPoolManager {

    private val pool = ConcurrentHashMap<String, MutableList<ManagedKey>>()

    fun registerKeys(providerId: String, keys: List<ManagedKey>) {
        pool.computeIfAbsent(providerId) { mutableListOf() }.apply {
            clear()
            addAll(keys)
        }
    }

    fun addKey(key: ManagedKey) {
        pool.computeIfAbsent(key.providerId) { mutableListOf() }.add(key)
    }

    fun acquireCandidateKey(providerId: String): ManagedKey? {
        val keys = pool[providerId] ?: return null
        val now = System.currentTimeMillis()

        synchronized(keys) {
            // Restore cooled keys
            keys.forEach { key ->
                if (key.state == KeyState.COOLING && now >= key.cooldownUntil) {
                    key.state = KeyState.HEALTHY
                    key.cooldownUntil = 0L
                }
            }

            // Pick healthy key: lowest priority first, then least recently used (LRU)
            val candidate = keys
                .filter { it.state == KeyState.HEALTHY }
                .minWithOrNull(compareBy({ it.priority }, { it.lastUsedAt }))

            candidate?.let {
                it.lastUsedAt = now
                it.requestsToday++
            }
            return candidate
        }
    }

    fun reportRateLimit(keyId: String, cooldownDurationMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    found.state = KeyState.COOLING
                    found.cooldownUntil = now + cooldownDurationMs
                    return
                }
            }
        }
    }

    fun reportQuotaExhausted(keyId: String, resetDurationMs: Long = 86_400_000L) {
        val now = System.currentTimeMillis()
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    found.state = KeyState.EXHAUSTED
                    found.cooldownUntil = now + resetDurationMs
                    return
                }
            }
        }
    }

    fun getHealthyKeyCount(providerId: String): Int {
        val keys = pool[providerId] ?: return 0
        val now = System.currentTimeMillis()
        return keys.count { it.state == KeyState.HEALTHY || (it.state == KeyState.COOLING && now >= it.cooldownUntil) }
    }
}

