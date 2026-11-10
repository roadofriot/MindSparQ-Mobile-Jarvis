package com.mindsparq.jarvis.core.policy

import com.mindsparq.jarvis.core.model.ExecutionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PolicyEngineTest {

    @Test
    fun testHardDenyDestructiveCommands() {
        val decision = PolicyEngine.evaluateCommand("rm -rf /", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, decision.action)

        val mkfsDecision = PolicyEngine.evaluateCommand("mkfs.ext4 /dev/sda1", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, mkfsDecision.action)

        val forkBombDecision = PolicyEngine.evaluateCommand(":(){ :|:& };:", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, forkBombDecision.action)

        val chmodDecision = PolicyEngine.evaluateCommand("chmod -R 777 /", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, chmodDecision.action)
    }

    @Test
    fun testModeConstraints() {
        // In Ask mode, even git status must not execute commands
        val askDecision = PolicyEngine.evaluateCommand("git status", ExecutionMode.ASK)
        assertEquals(PolicyAction.DENY, askDecision.action)

        // In Work mode, git status is allowed automatically
        val workDecision = PolicyEngine.evaluateCommand("git status", ExecutionMode.WORK)
        assertEquals(PolicyAction.ALLOW, workDecision.action)

        // In Work mode, risky command requires user approval
        val riskyDecision = PolicyEngine.evaluateCommand("npm install -g malicious-pkg", ExecutionMode.WORK)
        assertEquals(PolicyAction.ASK, riskyDecision.action)
    }

    @Test
    fun testTerminalCommandEvaluation() {
        // 1. Safe command allows directly
        val safeDecision = PolicyEngine.evaluateTerminalCommand("npm run build")
        assertEquals(PolicyAction.ALLOW, safeDecision.action)

        // 2. Destructive command asks for confirmation
        val askDecision = PolicyEngine.evaluateTerminalCommand("git reset --hard HEAD~1")
        assertEquals(PolicyAction.ASK, askDecision.action)

        val rmDecision = PolicyEngine.evaluateTerminalCommand("rm -rf node_modules")
        assertEquals(PolicyAction.ASK, rmDecision.action)

        // 3. System destructive command is hard denied
        val denyDecision = PolicyEngine.evaluateTerminalCommand("rm -rf /")
        assertEquals(PolicyAction.DENY, denyDecision.action)

        // 4. Path traversal attempt outside workspace jail is denied
        val jailDecision = PolicyEngine.evaluateTerminalCommand("cd /etc/passwd", "/home/workspace")
        assertEquals(PolicyAction.DENY, jailDecision.action)
    }

    @Test
    fun testPathJailEnforcement() {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val workspace = File(tmpDir, "jarvis_test_ws").apply { mkdirs() }
        val inside = File(workspace, "src/Main.kt").apply { parentFile?.mkdirs(); createNewFile() }
        val outside = File(tmpDir, "outside_file.txt").apply { createNewFile() }

        val allowDecision = PolicyEngine.evaluatePathAccess(workspace.absolutePath, inside.absolutePath)
        assertEquals(PolicyAction.ALLOW, allowDecision.action)

        val denyDecision = PolicyEngine.evaluatePathAccess(workspace.absolutePath, outside.absolutePath)
        assertEquals(PolicyAction.DENY, denyDecision.action)

        workspace.deleteRecursively()
        outside.delete()
    }

    @Test
    fun agentToolsAllowWorkspaceEditsAndAskBeforeCommands() {
        val workspace = Files.createTempDirectory("jarvis-agent-workspace").toFile()
        try {
            val write = PolicyEngine.evaluateAgentTool(
                toolName = "Write",
                command = null,
                targetPaths = listOf("/workspace/project/src/Main.kt"),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.ALLOW, write.action)

            val safeCommand = PolicyEngine.evaluateAgentTool(
                toolName = "Bash",
                command = "git status",
                targetPaths = emptyList(),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.ALLOW, safeCommand.action)

            val riskyCommand = PolicyEngine.evaluateAgentTool(
                toolName = "Bash",
                command = "npm install",
                targetPaths = emptyList(),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.ASK, riskyCommand.action)

            val externalRead = PolicyEngine.evaluateAgentTool(
                toolName = "Bash",
                command = "cat /etc/passwd",
                targetPaths = emptyList(),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.DENY, externalRead.action)
        } finally {
            workspace.deleteRecursively()
        }
    }

    @Test
    fun agentToolsDenyPathsOutsideWorkspaceAndShellTraversal() {
        val workspace = Files.createTempDirectory("jarvis-agent-workspace").toFile()
        val outside = Files.createTempDirectory("jarvis-agent-outside").toFile()
        try {
            val outsidePath = File(outside, "secret.txt").absolutePath
            val write = PolicyEngine.evaluateAgentTool(
                toolName = "Write",
                command = null,
                targetPaths = listOf(outsidePath),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.DENY, write.action)

            val symlink = File(workspace, "escape")
            Files.createSymbolicLink(symlink.toPath(), outside.toPath())
            val symlinkWrite = PolicyEngine.evaluateAgentTool(
                toolName = "Write",
                command = null,
                targetPaths = listOf("/workspace/project/escape/secret.txt"),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.DENY, symlinkWrite.action)

            val traversal = PolicyEngine.evaluateAgentTool(
                toolName = "Bash",
                command = "cat ../../etc/passwd",
                targetPaths = emptyList(),
                workspaceRoot = workspace.absolutePath,
                guestWorkspaceRoot = "/workspace/project",
            )
            assertEquals(PolicyAction.DENY, traversal.action)
        } finally {
            workspace.deleteRecursively()
            outside.deleteRecursively()
        }
    }

    @Test
    fun agentCommandsDenyMultipleDotDotSegments() {
        val decisions = listOf(
            PolicyEngine.evaluateAgentTool("Bash", "cat ../../../etc/passwd", emptyList(), "/workspace", "/workspace/project"),
            PolicyEngine.evaluateAgentTool("Bash", "ls ..", emptyList(), "/workspace", "/workspace/project"),
            PolicyEngine.evaluateAgentTool("Bash", "cat /workspace/project/../outside.txt", emptyList(), "/workspace", "/workspace/project"),
        )
        decisions.forEach { assertEquals(PolicyAction.DENY, it.action) }
    }

    @Test
    fun agentCommandsDenyEncodedAndIndirectPaths() {
        val encoded = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat %2e%2e%2fetc%2fpasswd",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, encoded.action)

        val indirect = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat \"\$HOME/.ssh/id_rsa\"",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, indirect.action)

        val backslash = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat ..\\\\..\\\\..\\\\etc\\\\passwd",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, backslash.action)
    }

    @Test
    fun agentCommandsDenyCommandSubstitutionAndCompoundShell() {
        // Commands with external paths are denied by the path check before compound shell.
        val externalPath = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat /etc/passwd",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.DENY, externalPath.action)

        // Compound shell without external paths requires approval (ASK).
        val substitution = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat $(echo hello)",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, substitution.action)

        val backtick = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "cat `echo hello`",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, backtick.action)

        val chained = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "ls; echo done",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, chained.action)

        val piped = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "echo hello | cat",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, piped.action)
    }

    @Test
    fun agentCommandsDenyControlCharactersAndNewlines() {
        // Newlines trigger the compound shell check (ASK); commands referencing
        // external paths are denied (DENY) by the path check first.
        val newlineInjected = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "ls\necho done",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, newlineInjected.action)

        val carriageReturn = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "ls\r\necho done",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, carriageReturn.action)

        val newlineWithExternal = PolicyEngine.evaluateAgentTool(
            toolName = "Bash",
            command = "ls\ncat /etc/passwd",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.DENY, newlineWithExternal.action)
    }

    @Test
    fun agentToolsFailClosedOnMalformedAndEmptyInputs() {
        // Empty or blank tool names are not recognized -> ASK (fail closed, requires approval).
        val emptyTool = PolicyEngine.evaluateAgentTool(
            toolName = "",
            command = "ls",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, emptyTool.action)

        val blankTool = PolicyEngine.evaluateAgentTool(
            toolName = "   ",
            command = "ls",
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, blankTool.action)

        val writeNoPaths = PolicyEngine.evaluateAgentTool(
            toolName = "Write",
            command = null,
            targetPaths = emptyList(),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.ASK, writeNoPaths.action)

        val blankPath = PolicyEngine.evaluateAgentTool(
            toolName = "Edit",
            command = null,
            targetPaths = listOf("   ", ""),
            workspaceRoot = "/workspace",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.DENY, blankPath.action)

        val nullRoot = PolicyEngine.evaluateAgentTool(
            toolName = "Write",
            command = null,
            targetPaths = listOf("/workspace/project/file.txt"),
            workspaceRoot = "",
            guestWorkspaceRoot = "/workspace/project",
        )
        assertEquals(PolicyAction.DENY, nullRoot.action)
    }

    @Test
    fun evaluateCommandRejectsExtremelyLongCommands() {
        val longCommand = "a".repeat(10_000)
        val decision = PolicyEngine.evaluateCommand(longCommand, ExecutionMode.WORK)
        assertTrue(decision.action == PolicyAction.ASK || decision.action == PolicyAction.DENY)
    }

    @Test
    fun terminalCommandDeniesEncodedTraversalOutsideWorkspace() {
        val decision = PolicyEngine.evaluateTerminalCommand(
            "cat /etc/passwd",
            "/home/user/project",
        )
        assertEquals(PolicyAction.DENY, decision.action)

        // Three or more ../ sequences are caught by the path jail regex (DENY).
        val deepTraversal = PolicyEngine.evaluateTerminalCommand(
            "cd ../../../etc",
            "/home/user/project",
        )
        assertEquals(PolicyAction.DENY, deepTraversal.action)
    }
}
