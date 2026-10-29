package com.mindsparq.jarvis.engine.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsIndexTest {

    @Test
    fun testChunkingAndRetrievalWithProvenance() {
        val indexManager = FtsIndexManager()

        val sampleAuthCode = """
            package com.example.auth
            
            class AuthService {
                fun authenticateUser(username: String, token: String): Boolean {
                    if (token.isEmpty()) return false
                    return true
                }
                
                fun logoutUser(userId: String) {
                    println("User logged out")
                }
            }
        """.trimIndent()

        indexManager.indexFile("src/main/AuthService.kt", sampleAuthCode)

        val results = indexManager.search("authenticateUser token", topK = 3)
        assertTrue(results.isNotEmpty())

        val topResult = results[0]
        assertEquals("src/main/AuthService.kt", topResult.filePath)
        assertEquals(1, topResult.startLine)
        assertTrue(topResult.snippet.contains("fun authenticateUser"))

        val promptContext = indexManager.formatPromptContext(results)
        assertTrue(promptContext.contains("--- src/main/AuthService.kt:1-"))
    }
}

