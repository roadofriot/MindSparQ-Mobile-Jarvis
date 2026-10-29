package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

@Serializable
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCall> = emptyList()
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition> = emptyList(),
    val temperature: Float = 0.2f,
    val maxTokens: Int = 4096,
    val thinkingLevel: String? = null
)

sealed interface ChatResponseEvent {
    data class TextDelta(val text: String) : ChatResponseEvent
    data class ToolCallDelta(val toolCall: ToolCall) : ChatResponseEvent
    data class Usage(val tokensIn: Int, val tokensOut: Int) : ChatResponseEvent
    data class RouteSwitched(val fromKeyOrProvider: String, val toKeyOrProvider: String, val reason: String) : ChatResponseEvent
    data class Error(val statusCode: Int, val message: String, val isRateLimit: Boolean) : ChatResponseEvent
    data object Done : ChatResponseEvent
}

