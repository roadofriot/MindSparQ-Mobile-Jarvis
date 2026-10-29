package com.mindsparq.jarvis.core.runtime

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DesktopProcessRunnerTest {

    @Test
    fun testHostPlatformDetection() {
        val platform = HostPlatform.detectCurrentHost()
        assertNotNull(platform)
        // On Linux host, should detect LINUX_DESKTOP or ANDROID
        assertTrue(platform == HostPlatform.LINUX_DESKTOP || platform == HostPlatform.ANDROID_PROOT)
    }

    @Test
    fun testExecuteEchoCommand() = runBlocking {
        val runner = DesktopProcessRunner()
        val tmpDir = System.getProperty("java.io.tmpdir")

        val spec = ExecSpec(
            command = listOf("echo", "MindSparQ Jarvis Desktop Process"),
            workingDir = tmpDir
        )

        val result = runner.execute(spec)
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("MindSparQ Jarvis Desktop Process"))
        assertTrue(result.durationMs >= 0)
    }
}

