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
enum class McpTaskProfile {
    ALL,
    CODING,
    TESTING,
    DESIGN,
    RESEARCH,
    DATABASE
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
    val enabled: Boolean = true,
    val profiles: Set<McpTaskProfile> = setOf(McpTaskProfile.ALL)
)

object McpConfigExporter {

    private val json = Json { prettyPrint = true }

    fun filterServersForProfile(
        servers: List<McpServerConfig>,
        profile: McpTaskProfile
    ): List<McpServerConfig> {
        if (profile == McpTaskProfile.ALL) return servers.filter { it.enabled }
        return servers.filter { server ->
            server.enabled && (server.profiles.contains(McpTaskProfile.ALL) || server.profiles.contains(profile))
        }
    }

    fun generateMcpJsonString(
        servers: List<McpServerConfig>,
        profile: McpTaskProfile = McpTaskProfile.ALL
    ): String {
        val filtered = filterServersForProfile(servers, profile)
        val root = buildJsonObject {
            putJsonObject("mcpServers") {
                filtered.forEach { server ->
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

    fun syncToProjectWorkspace(
        workspaceDir: File,
        servers: List<McpServerConfig>,
        profile: McpTaskProfile = McpTaskProfile.ALL
    ): File {
        val mcpJsonFile = File(workspaceDir, ".mcp.json")
        val content = generateMcpJsonString(servers, profile)
        mcpJsonFile.writeText(content)
        return mcpJsonFile
    }
}

