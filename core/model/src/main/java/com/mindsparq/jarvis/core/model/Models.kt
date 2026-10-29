package com.mindsparq.jarvis.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AgentKind(val stableId: String, val displayName: String) {
    JARVIS("jarvis", "Jarvis Native"),
    CLAUDE_CODE("claude-code", "Claude Code"),
    DEEPSEEK_HARNESS("deepseek-harness", "DeepSeek Harness"),
    ANTIGRAVITY("antigravity", "Antigravity CLI"),
    GEMINI_CLI("gemini-cli", "Gemini CLI"),
    CODEX("codex", "OpenAI Codex"),
    OPENCODE("opencode", "OpenCode");

    companion object {
        fun fromId(id: String): AgentKind =
            entries.find { it.stableId == id } ?: JARVIS
    }
}

@Serializable
enum class ExecutionMode {
    ASK,
    PLAN,
    WORK
}

@Serializable
enum class TaskState {
    QUEUED,
    INSPECTING,
    PLANNING,
    AWAITING_APPROVAL,
    IMPLEMENTING,
    TESTING,
    DEBUGGING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    BLOCKED
}

@Serializable
enum class EvidenceKind {
    COMMAND,
    DIFF,
    TEST_RUN,
    BUILD,
    FILE_WRITE,
    MCP_CALL,
    ROUTE_SWITCH,
    APPROVAL
}

@Serializable
data class EvidenceRecord(
    val id: String,
    val taskId: String,
    val kind: EvidenceKind,
    val summary: String,
    val exitCode: Int? = null,
    val payloadPath: String? = null,
    val createdAt: Long = 0L
)

@Serializable
data class KeyPoolEntry(
    val id: String,
    val providerId: String,
    val label: String,
    val secretRef: String,
    val state: String = "HEALTHY",
    val cooldownUntil: Long? = null,
    val priority: Int = 10,
    val requestsToday: Int = 0,
    val tokensToday: Int = 0,
    val lastUsedAt: Long = 0L
)

@Serializable
data class ModelDescriptor(
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val contextLength: Int = 32000,
    val supportsTools: Boolean = true,
    val supportsVision: Boolean = false,
    val supportsThinking: Boolean = false,
    val isFree: Boolean = false,
    val openWeights: Boolean = false,
    val pinned: Boolean = false
)

