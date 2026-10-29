package com.mindsparq.jarvis.core.policy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SecretStore {
    fun storeSecret(alias: String, secret: String)
    fun getSecret(alias: String): String?
    fun deleteSecret(alias: String)
    fun hasSecret(alias: String): Boolean
    fun getAllAliases(): Set<String>
}

class AndroidKeystoreSecretStore(context: Context) : SecretStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "mindsparq_secure_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun storeSecret(alias: String, secret: String) {
        sharedPreferences.edit().putString(alias, secret).apply()
    }

    override fun getSecret(alias: String): String? {
        return sharedPreferences.getString(alias, null)
    }

    override fun deleteSecret(alias: String) {
        sharedPreferences.edit().remove(alias).apply()
    }

    override fun hasSecret(alias: String): Boolean {
        return sharedPreferences.contains(alias)
    }

    override fun getAllAliases(): Set<String> {
        return sharedPreferences.all.keys
    }
}

class InMemorySecretStore : SecretStore {
    private val memory = mutableMapOf<String, String>()

    override fun storeSecret(alias: String, secret: String) {
        memory[alias] = secret
    }

    override fun getSecret(alias: String): String? {
        return memory[alias]
    }

    override fun deleteSecret(alias: String) {
        memory.remove(alias)
    }

    override fun hasSecret(alias: String): Boolean {
        return memory.containsKey(alias)
    }

    override fun getAllAliases(): Set<String> {
        return memory.keys.toSet()
    }
}

