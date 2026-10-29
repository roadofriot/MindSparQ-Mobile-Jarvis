package com.mindsparq.jarvis.engine.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMemoryTest {

    @Test
    fun testRecordMemoryAndSecretRedaction() {
        val memoryStore = ProjectMemoryStore()

        // Record architectural decision
        memoryStore.recordMemory(
            projectId = "proj_1",
            type = MemoryType.DECISION,
            content = "Use SQLite FTS5 for local search instead of vector DB",
            pinned = true
        )

        // Record a note containing accidentally pasted API key
        memoryStore.recordMemory(
            projectId = "proj_1",
            type = MemoryType.FACT,
            content = "Tested endpoint with key AIzaSyD12345678901234567890123456789012"
        )

        val items = memoryStore.getProjectMemory("proj_1")
        assertEquals(2, items.size)

        // Verify secret redaction
        val fact = items.find { it.type == MemoryType.FACT }!!
        assertFalse(fact.content.contains("AIzaSyD12345678901234567890123456789012"))
        assertTrue(fact.content.contains("[REDACTED_GEMINI_KEY]"))

        val context = memoryStore.buildMemoryPromptContext("proj_1")
        assertTrue(context.contains("### Persistent Project Memory & Decisions:"))
        assertTrue(context.contains("[DECISION] Use SQLite FTS5"))
    }
}

