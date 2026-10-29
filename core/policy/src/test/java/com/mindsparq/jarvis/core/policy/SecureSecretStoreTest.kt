package com.mindsparq.jarvis.core.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSecretStoreTest {

    @Test
    fun testStoreAndRetrieveSecret() {
        val store: SecretStore = InMemorySecretStore()
        store.storeSecret("gemini_key_1", "AIzaSyD-dummy-key-value-1234567890")
        
        assertTrue(store.hasSecret("gemini_key_1"))
        assertEquals("AIzaSyD-dummy-key-value-1234567890", store.getSecret("gemini_key_1"))
    }

    @Test
    fun testDeleteSecret() {
        val store: SecretStore = InMemorySecretStore()
        store.storeSecret("nvidia_key_1", "nvapi-dummy-key-value")
        
        store.deleteSecret("nvidia_key_1")
        assertFalse(store.hasSecret("nvidia_key_1"))
        assertNull(store.getSecret("nvidia_key_1"))
    }

    @Test
    fun testMultipleKeysIsolation() {
        val store: SecretStore = InMemorySecretStore()
        store.storeSecret("openrouter_1", "sk-or-v1-aaa")
        store.storeSecret("openrouter_2", "sk-or-v1-bbb")

        assertEquals(2, store.getAllAliases().size)
        assertTrue(store.getAllAliases().contains("openrouter_1"))
        assertTrue(store.getAllAliases().contains("openrouter_2"))
    }
}

