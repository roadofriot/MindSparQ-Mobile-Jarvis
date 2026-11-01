package com.jarves.mh.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyVault(context: Context) {
    private val preferences = context.getSharedPreferences("pocket_secrets", Context.MODE_PRIVATE)
    private val alias = "pocket-provider-key"

    @Synchronized
    fun put(providerId: String, secret: String) {
        if (secret.isBlank()) return
        val entries = ensurePool(providerId)
        val active = entries.firstOrNull { it.id == activeId(providerId) } ?: entries.firstOrNull()
        if (active == null) {
            add(providerId, "Primary", secret)
        } else {
            putEncrypted(secretKey(providerId, active.id), secret)
        }
    }

    @Synchronized
    fun add(
        providerId: String,
        name: String,
        secret: String,
        accountLabel: String = "Default Account",
        quotaScopeId: String = "default-project",
        billingTier: String = "FREE",
        priority: Int = 10,
    ): ApiKeyInfo {
        require(secret.isNotBlank()) { "API key cannot be empty" }
        val entries = ensurePool(providerId).toMutableList()
        val entry = ApiKeyInfo(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "API key ${entries.size + 1}" }.take(60),
            isActive = entries.isEmpty(),
            accountLabel = accountLabel.trim().ifBlank { "Default Account" },
            quotaScopeId = quotaScopeId.trim().ifBlank { "default-project" },
            billingTier = billingTier,
            priority = priority,
            state = "HEALTHY",
            lastValidatedAt = System.currentTimeMillis(),
        )
        putEncrypted(secretKey(providerId, entry.id), secret)
        entries += entry.copy(isActive = false)
        savePool(providerId, entries)
        if (entries.size == 1) setActiveId(providerId, entry.id)
        return entry.copy(isActive = entries.size == 1)
    }

    @Synchronized
    fun list(providerId: String): List<ApiKeyInfo> {
        val entries = ensurePool(providerId)
        val active = activeId(providerId) ?: entries.firstOrNull()?.id
        return entries.map { it.copy(isActive = it.id == active) }
    }

    @Synchronized
    fun credentials(providerId: String): List<ApiKeyCredential> = list(providerId).mapNotNull { info ->
        getEncrypted(secretKey(providerId, info.id))?.let { secret ->
            ApiKeyCredential(
                id = info.id,
                name = info.name,
                secret = secret,
                isActive = info.isActive,
                accountLabel = info.accountLabel,
                quotaScopeId = info.quotaScopeId,
                billingTier = info.billingTier,
                priority = info.priority,
                state = info.state,
            )
        }
    }.sortedByDescending(ApiKeyCredential::isActive)

    @Synchronized
    fun updateKeyMetadata(
        providerId: String,
        keyId: String,
        state: String? = null,
        lastError: String? = null,
        cooldownUntil: Long? = null,
        lastValidatedAt: Long? = null,
    ) {
        val entries = ensurePool(providerId).map { key ->
            if (key.id == keyId) {
                key.copy(
                    state = state ?: key.state,
                    lastError = lastError ?: key.lastError,
                    cooldownUntil = cooldownUntil ?: key.cooldownUntil,
                    lastValidatedAt = lastValidatedAt ?: key.lastValidatedAt,
                )
            } else key
        }
        savePool(providerId, entries)
    }

    @Synchronized
    fun activate(providerId: String, keyId: String): Boolean {
        if (ensurePool(providerId).none { it.id == keyId }) return false
        setActiveId(providerId, keyId)
        return true
    }

    @Synchronized
    fun remove(providerId: String, keyId: String) {
        val remaining = ensurePool(providerId).filterNot { it.id == keyId }
        removeEncrypted(secretKey(providerId, keyId))
        savePool(providerId, remaining)
        if (activeId(providerId) == keyId) {
            preferences.edit().remove(activeKey(providerId)).apply()
            remaining.firstOrNull()?.let { setActiveId(providerId, it.id) }
        }
    }

    private fun putEncrypted(storageId: String, secret: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(cleanSecret(secret).toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("$storageId.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$storageId.value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun contains(providerId: String): Boolean = get(providerId) != null

    @Synchronized
    fun remove(providerId: String) {
        ensurePool(providerId).forEach { removeEncrypted(secretKey(providerId, it.id)) }
        preferences.edit()
            .remove("$providerId.iv")
            .remove("$providerId.value")
            .remove(poolKey(providerId))
            .remove(activeKey(providerId))
            .apply()
    }

    @Synchronized
    fun get(providerId: String): String? {
        val active = list(providerId).firstOrNull { it.isActive } ?: return null
        return getEncrypted(secretKey(providerId, active.id))
    }

    private fun getEncrypted(storageId: String): String? = runCatching {
        val iv = Base64.decode(preferences.getString("$storageId.iv", null), Base64.NO_WRAP)
        val encrypted = Base64.decode(preferences.getString("$storageId.value", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        // Also clean on read so keys saved before this fix (e.g. a token wrapped across lines) still work.
        cleanSecret(cipher.doFinal(encrypted).toString(Charsets.UTF_8))
    }.getOrNull()

    /**
     * API keys and OAuth tokens never contain whitespace. Terminals often wrap long tokens such as
     * `claude setup-token` output, so pasted secrets can include line breaks or a "Bearer " prefix.
     */
    private fun cleanSecret(secret: String): String =
        secret.trim().removePrefix("Bearer ").removePrefix("bearer ").filterNot(Char::isWhitespace)

    private fun removeEncrypted(storageId: String) {
        preferences.edit().remove("$storageId.iv").remove("$storageId.value").apply()
    }

    private fun ensurePool(providerId: String): List<ApiKeyInfo> {
        readPool(providerId).takeIf(List<ApiKeyInfo>::isNotEmpty)?.let { return it }
        if (!preferences.contains("$providerId.value")) return emptyList()
        val legacySecret = getEncrypted(providerId) ?: return emptyList()
        val legacy = ApiKeyInfo("legacy", "Primary", true)
        putEncrypted(secretKey(providerId, legacy.id), legacySecret)
        savePool(providerId, listOf(legacy))
        setActiveId(providerId, legacy.id)
        return listOf(legacy)
    }

    private fun readPool(providerId: String): List<ApiKeyInfo> = runCatching {
        val array = JSONArray(preferences.getString(poolKey(providerId), "[]"))
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { item ->
                val id = item.optString("id")
                if (id.isBlank()) null else ApiKeyInfo(
                    id = id,
                    name = item.optString("name").ifBlank { "API key" },
                    isActive = false,
                    accountLabel = item.optString("accountLabel").ifBlank { "Default Account" },
                    quotaScopeId = item.optString("quotaScopeId").ifBlank { "default-project" },
                    billingTier = item.optString("billingTier").ifBlank { "FREE" },
                    priority = item.optInt("priority", 10),
                    state = item.optString("state").ifBlank { "HEALTHY" },
                    lastError = item.optString("lastError").takeIf { it.isNotBlank() },
                    lastValidatedAt = item.optLong("lastValidatedAt", 0L),
                    cooldownUntil = item.optLong("cooldownUntil", 0L),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun savePool(providerId: String, entries: List<ApiKeyInfo>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
                .put("id", entry.id)
                .put("name", entry.name)
                .put("accountLabel", entry.accountLabel)
                .put("quotaScopeId", entry.quotaScopeId)
                .put("billingTier", entry.billingTier)
                .put("priority", entry.priority)
                .put("state", entry.state)
                .put("lastError", entry.lastError ?: "")
                .put("lastValidatedAt", entry.lastValidatedAt)
                .put("cooldownUntil", entry.cooldownUntil)
            array.put(obj)
        }
        preferences.edit().putString(poolKey(providerId), array.toString()).apply()
    }

    private fun activeId(providerId: String): String? = preferences.getString(activeKey(providerId), null)
    private fun setActiveId(providerId: String, id: String) = preferences.edit().putString(activeKey(providerId), id).apply()
    private fun poolKey(providerId: String) = "$providerId.pool"
    private fun activeKey(providerId: String) = "$providerId.active"
    private fun secretKey(providerId: String, id: String) = "$providerId.pool.$id"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }
}

data class ApiKeyInfo(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val accountLabel: String = "Default Account",
    val quotaScopeId: String = "default-project",
    val billingTier: String = "FREE",
    val priority: Int = 10,
    val state: String = "HEALTHY",
    val lastError: String? = null,
    val lastValidatedAt: Long = 0L,
    val cooldownUntil: Long = 0L,
)

data class ApiKeyCredential(
    val id: String,
    val name: String,
    val secret: String,
    val isActive: Boolean,
    val accountLabel: String = "Default Account",
    val quotaScopeId: String = "default-project",
    val billingTier: String = "FREE",
    val priority: Int = 10,
    val state: String = "HEALTHY",
)
