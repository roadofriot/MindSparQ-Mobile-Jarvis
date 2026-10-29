package com.mindsparq.jarvis.ai.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

@Serializable
enum class McpTransport {
    STDIO,
    STREAMABLE_HTTP
}

@Serializable
data class McpServerConfig(
    val id: String,
    val name: String,
    val transport: McpTransport,
    val command: String? = null,
    val arguments: List<String> = emptyList(),
    val url: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

object McpConfigExporter {

    private val json = Json { prettyPrint = true }

    fun generateMcpJsonString(servers: List<McpServerConfig>): String {
        val root = buildJsonObject {
            putJsonObject("mcpServers") {
                servers.filter { it.enabled }.forEach { server ->
                    putJsonObject(server.name) {
                        when (server.transport) {
                            McpTransport.STDIO -> {
                                server.command?.let { put("command", it) }
                                putJsonArray("args") {
                                    server.arguments.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                                }
                                if (server.environment.isNotEmpty()) {
                                    putJsonObject("env") {
                                        server.environment.forEach { (k, v) -> put(k, v) }
                                    }
                                }
                            }
                            McpTransport.STREAMABLE_HTTP -> {
                                server.url?.let { put("url", it) }
                            }
                        }
                    }
                }
            }
        }
        return json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), root)
    }

    fun syncToProjectWorkspace(workspaceDir: File, servers: List<McpServerConfig>): File {
        val mcpJsonFile = File(workspaceDir, ".mcp.json")
        val content = generateMcpJsonString(servers)
        mcpJsonFile.writeText(content)
        return mcpJsonFile
    }
}

