package com.mindsparq.jarvis.core.policy

import com.mindsparq.jarvis.core.model.ExecutionMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PolicyEngineTest {

    @Test
    fun testHardDenyDestructiveCommands() {
        val decision = PolicyEngine.evaluateCommand("rm -rf /", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, decision.action)

        val mkfsDecision = PolicyEngine.evaluateCommand("mkfs.ext4 /dev/sda1", ExecutionMode.WORK)
        assertEquals(PolicyAction.DENY, mkfsDecision.action)
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
    fun testPathJailEnforcement() {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val workspace = File(tmpDir, "jarvis_test_ws").apply { mkdirs() }
        val inside = File(workspace, "src/Main.kt").apply { parentFile.mkdirs(); createNewFile() }
        val outside = File(tmpDir, "outside_file.txt").apply { createNewFile() }

        val allowDecision = PolicyEngine.evaluatePathAccess(workspace.absolutePath, inside.absolutePath)
        assertEquals(PolicyAction.ALLOW, allowDecision.action)

        val denyDecision = PolicyEngine.evaluatePathAccess(workspace.absolutePath, outside.absolutePath)
        assertEquals(PolicyAction.DENY, denyDecision.action)

        workspace.deleteRecursively()
        outside.delete()
    }
}

