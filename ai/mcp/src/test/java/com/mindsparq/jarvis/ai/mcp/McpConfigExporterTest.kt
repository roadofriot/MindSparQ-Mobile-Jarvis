package com.mindsparq.jarvis.ai.mcp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class McpConfigExporterTest {

    @Test
    fun testMcpJsonGeneration() {
        val servers = listOf(
            McpServerConfig(
                id = "s1",
                name = "filesystem",
                transport = McpTransport.STDIO,
                command = "npx",
                arguments = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp/ws")
            ),
            McpServerConfig(
                id = "s2",
                name = "supabase",
                transport = McpTransport.STREAMABLE_HTTP,
                url = "https://api.supabase.com/mcp"
            )
        )

        val json = McpConfigExporter.generateMcpJsonString(servers)

        assertTrue(json.contains("\"filesystem\""))
        assertTrue(json.contains("\"command\": \"npx\""))
        assertTrue(json.contains("\"supabase\""))
        assertTrue(json.contains("\"url\": \"https://api.supabase.com/mcp\""))
    }

    @Test
    fun testSyncToWorkspace() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "jarvis_mcp_sync_test").apply { mkdirs() }
        val servers = listOf(
            McpServerConfig(
                id = "git-server",
                name = "git",
                transport = McpTransport.STDIO,
                command = "uvx",
                arguments = listOf("mcp-server-git")
            )
        )

        val syncedFile = McpConfigExporter.syncToProjectWorkspace(tempDir, servers)
        assertTrue(syncedFile.exists())
        assertTrue(syncedFile.readText().contains("mcp-server-git"))

        tempDir.deleteRecursively()
    }
}

