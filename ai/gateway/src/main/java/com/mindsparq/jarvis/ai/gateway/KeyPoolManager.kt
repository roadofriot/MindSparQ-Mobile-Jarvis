package com.mindsparq.jarvis.ai.gateway

import com.mindsparq.jarvis.core.model.BillingTier
import com.mindsparq.jarvis.core.model.QuotaState
import java.util.concurrent.ConcurrentHashMap

enum class KeyState {
    HEALTHY,
    COOLING,
    EXHAUSTED,
    INVALID
}

data class QuotaScope(
    val id: String,
    val providerId: String,
    val accountLabel: String,
    var state: QuotaState = QuotaState.AVAILABLE,
    var exhaustedUntil: Long = 0L,
    var lastError: String? = null
)

data class ManagedKey(
    val id: String,
    val providerId: String,
    val secretKey: String,
    val label: String,
    val accountLabel: String = "Default Account",
    val quotaScopeId: String = "default-project",
    val billingTier: BillingTier = BillingTier.FREE,
    var state: KeyState = KeyState.HEALTHY,
    var cooldownUntil: Long = 0L,
    var priority: Int = 10,
    var requestsToday: Int = 0,
    var tokensToday: Int = 0,
    var lastUsedAt: Long = 0L,
    var lastError: String? = null,
    var lastValidatedAt: Long = 0L
)

class KeyPoolManager {

    private val pool = ConcurrentHashMap<String, MutableList<ManagedKey>>()
    private val quotaScopes = ConcurrentHashMap<String, QuotaScope>()

    fun registerQuotaScope(scope: QuotaScope) {
        quotaScopes[scope.id] = scope
    }

    fun getQuotaScope(scopeId: String): QuotaScope? = quotaScopes[scopeId]

    fun registerKeys(providerId: String, keys: List<ManagedKey>) {
        pool.computeIfAbsent(providerId) { mutableListOf() }.apply {
            clear()
            addAll(keys)
        }
        // Auto-register any undeclared quota scopes for backward compatibility
        keys.forEach { key ->
            quotaScopes.computeIfAbsent(key.quotaScopeId) {
                QuotaScope(
                    id = key.quotaScopeId,
                    providerId = key.providerId,
                    accountLabel = key.accountLabel
                )
            }
        }
    }

    fun addKey(key: ManagedKey) {
        pool.computeIfAbsent(key.providerId) { mutableListOf() }.add(key)
        quotaScopes.computeIfAbsent(key.quotaScopeId) {
            QuotaScope(
                id = key.quotaScopeId,
                providerId = key.providerId,
                accountLabel = key.accountLabel
            )
        }
    }

    /**
     * Acquires the best eligible key for a provider.
     * Guarantees:
     * 1. Restores expired key cooldowns.
     * 2. Restores expired quota scopes.
     * 3. Ignores keys belonging to an EXHAUSTED or BILLING_BLOCKED quota scope (e.g. Google Cloud project quota).
     * 4. Orders candidates by: priority, then least recently used (LRU).
     */
    fun acquireCandidateKey(providerId: String, requiredTier: BillingTier? = null): ManagedKey? {
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

            // Restore exhausted quota scopes if reset duration elapsed
            quotaScopes.values.forEach { scope ->
                if (scope.state == QuotaState.QUOTA_EXHAUSTED && now >= scope.exhaustedUntil) {
                    scope.state = QuotaState.AVAILABLE
                    scope.exhaustedUntil = 0L
                    scope.lastError = null
                    // Restore keys in this scope that were only exhausted by scope exhaustion
                    keys.filter { it.quotaScopeId == scope.id && it.state == KeyState.EXHAUSTED }.forEach {
                        it.state = KeyState.HEALTHY
                        it.cooldownUntil = 0L
                    }
                }
            }

            // Filter keys that are healthy AND whose quota scope is available
            val eligibleKeys = keys.filter { key ->
                if (key.state != KeyState.HEALTHY) return@filter false
                if (requiredTier != null && key.billingTier != requiredTier) return@filter false

                val scope = quotaScopes[key.quotaScopeId]
                if (scope != null) {
                    if (scope.state == QuotaState.QUOTA_EXHAUSTED || scope.state == QuotaState.BILLING_BLOCKED) {
                        return@filter false
                    }
                }
                true
            }

            val candidate = eligibleKeys.minWithOrNull(compareBy({ it.priority }, { it.lastUsedAt }))

            candidate?.let {
                it.lastUsedAt = now
                it.requestsToday++
            }
            return candidate
        }
    }

    /**
     * Reports rate-limit (RPM) on a specific key.
     * Leaves other keys in the same project or provider available.
     */
    fun reportRateLimit(keyId: String, cooldownDurationMs: Long = 60_000L) {
        val now = System.currentTimeMillis()
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    found.state = KeyState.COOLING
                    found.cooldownUntil = now + cooldownDurationMs
                    found.lastError = "Rate limited (cooldown until ${now + cooldownDurationMs})"
                    return
                }
            }
        }
    }

    /**
     * Reports single credential authentication failure (HTTP 401 / 403).
     * Marks ONLY this key invalid; does NOT exhaust the project-level quota.
     */
    fun reportKeyAuthFailure(keyId: String, error: String = "Authentication failed (401)") {
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    found.state = KeyState.INVALID
                    found.lastError = error
                    return
                }
            }
        }
    }

    /**
     * Reports that a whole project or account quota scope is exhausted (HTTP 429 daily/project limit).
     * CRITICAL: Marks the ENTIRE quota scope exhausted and updates all associated keys,
     * so subsequent requests WILL NOT waste attempts rotating to other keys of the same project!
     */
    fun reportProjectQuotaExhausted(quotaScopeId: String, resetDurationMs: Long = 86_400_000L, error: String = "Project quota exhausted") {
        val now = System.currentTimeMillis()
        val scope = quotaScopes[quotaScopeId]
        if (scope != null) {
            scope.state = QuotaState.QUOTA_EXHAUSTED
            scope.exhaustedUntil = now + resetDurationMs
            scope.lastError = error
        }

        // Mark all keys sharing this quota scope as EXHAUSTED
        for (list in pool.values) {
            synchronized(list) {
                list.filter { it.quotaScopeId == quotaScopeId }.forEach { key ->
                    key.state = KeyState.EXHAUSTED
                    key.cooldownUntil = now + resetDurationMs
                    key.lastError = error
                }
            }
        }
    }

    /**
     * Reports billing blocked error (HTTP 402) on a quota scope.
     */
    fun reportBillingBlocked(quotaScopeId: String, error: String = "Billing account suspended or quota exceeded") {
        val scope = quotaScopes[quotaScopeId]
        if (scope != null) {
            scope.state = QuotaState.BILLING_BLOCKED
            scope.lastError = error
        }
        for (list in pool.values) {
            synchronized(list) {
                list.filter { it.quotaScopeId == quotaScopeId }.forEach { key ->
                    key.state = KeyState.INVALID
                    key.lastError = error
                }
            }
        }
    }

    /**
     * Reports successful inference execution.
     */
    fun reportSuccess(keyId: String, quotaScopeId: String, tokensIn: Int, tokensOut: Int) {
        val now = System.currentTimeMillis()
        val scope = quotaScopes[quotaScopeId]
        if (scope != null && scope.state != QuotaState.BILLING_BLOCKED) {
            scope.state = QuotaState.AVAILABLE
            scope.lastError = null
        }
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    found.state = KeyState.HEALTHY
                    found.lastError = null
                    found.tokensToday += (tokensIn + tokensOut)
                    found.lastUsedAt = now
                    return
                }
            }
        }
    }

    /**
     * Backward-compatible reportQuotaExhausted for a single key ID.
     */
    fun reportQuotaExhausted(keyId: String, resetDurationMs: Long = 86_400_000L) {
        var scopeIdToExhaust: String? = null
        for (list in pool.values) {
            synchronized(list) {
                val found = list.find { it.id == keyId }
                if (found != null) {
                    scopeIdToExhaust = found.quotaScopeId
                }
            }
            if (scopeIdToExhaust != null) break
        }
        if (scopeIdToExhaust != null) {
            reportProjectQuotaExhausted(scopeIdToExhaust!!, resetDurationMs)
        } else {
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
    }

    fun getHealthyKeyCount(providerId: String): Int {
        val keys = pool[providerId] ?: return 0
        val now = System.currentTimeMillis()
        return keys.count { key ->
            val scope = quotaScopes[key.quotaScopeId]
            val scopeAvailable = scope == null || (scope.state != QuotaState.QUOTA_EXHAUSTED && scope.state != QuotaState.BILLING_BLOCKED)
            scopeAvailable && (key.state == KeyState.HEALTHY || (key.state == KeyState.COOLING && now >= key.cooldownUntil))
        }
    }

    fun getAllManagedKeys(providerId: String): List<ManagedKey> = pool[providerId]?.toList() ?: emptyList()

    fun getAllQuotaScopes(): List<QuotaScope> = quotaScopes.values.toList()
}

