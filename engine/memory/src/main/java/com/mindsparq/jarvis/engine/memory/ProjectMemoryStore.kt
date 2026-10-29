package com.mindsparq.jarvis.engine.memory

import java.util.UUID

enum class MemoryType {
    FACT,
    DECISION,
    TASK_STATE,
    KNOWN_ISSUE,
    PREFERENCE
}

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val type: MemoryType,
    val content: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

class ProjectMemoryStore {

    private val items = mutableListOf<MemoryItem>()

    fun recordMemory(projectId: String, type: MemoryType, content: String, pinned: Boolean = false): MemoryItem {
        val sanitized = sanitizeSecretContent(content)
        val item = MemoryItem(
            projectId = projectId,
            type = type,
            content = sanitized,
            pinned = pinned
        )
        items.add(item)
        return item
    }

    fun getProjectMemory(projectId: String): List<MemoryItem> {
        return items.filter { it.projectId == projectId }.sortedByDescending { it.pinned }
    }

    fun removeMemory(id: String) {
        items.removeAll { it.id == id }
    }

    fun buildMemoryPromptContext(projectId: String, maxTokensBudget: Int = 1500): String {
        val projectItems = getProjectMemory(projectId)
        if (projectItems.isEmpty()) return ""

        return buildString {
            appendLine("### Persistent Project Memory & Decisions:")
            for (item in projectItems) {
                val line = "- [${item.type.name}] ${item.content}"
                if (length + line.length > maxTokensBudget * 4) break
                appendLine(line)
            }
        }
    }

    private fun sanitizeSecretContent(raw: String): String {
        // Redact standard secret patterns
        return raw
            .replace(Regex("""AIza[0-9A-Za-z-_]{35}"""), "[REDACTED_GEMINI_KEY]")
            .replace(Regex("""sk-[0-9A-Za-z-_]{32,}"""), "[REDACTED_API_KEY]")
            .replace(Regex("""ghp_[0-9A-Za-z]{36}"""), "[REDACTED_GH_TOKEN]")
    }
}

