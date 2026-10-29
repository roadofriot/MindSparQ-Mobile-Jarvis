package com.mindsparq.jarvis.core.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class HostPlatform {
    ANDROID_PROOT,
    LINUX_DESKTOP,
    WINDOWS_DESKTOP,
    MACOS_DESKTOP;

    companion object {
        fun detectCurrentHost(): HostPlatform {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            return when {
                os.contains("win") -> WINDOWS_DESKTOP
                os.contains("mac") -> MACOS_DESKTOP
                System.getProperty("java.vendor")?.contains("Android") == true -> ANDROID_PROOT
                else -> LINUX_DESKTOP
            }
        }
    }
}

data class ExecSpec(
    val command: List<String>,
    val workingDir: String,
    val environment: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 30_000L
)

data class ExecResult(
    val exitCode: Int,
    val durationMs: Long,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false
)

class DesktopProcessRunner {

    suspend fun execute(spec: ExecSpec): ExecResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val processBuilder = ProcessBuilder(spec.command).apply {
            directory(File(spec.workingDir))
            environment().putAll(spec.environment)
        }

        var process: Process? = null
        try {
            process = processBuilder.start()
            val completedInTime = process.waitFor(spec.timeoutMs, TimeUnit.MILLISECONDS)
            val duration = System.currentTimeMillis() - startTime

            if (!completedInTime) {
                process.destroyForcibly()
                return@withContext ExecResult(
                    exitCode = -1,
                    durationMs = duration,
                    stdout = "",
                    stderr = "Process timed out after ${spec.timeoutMs}ms",
                    timedOut = true
                )
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            ExecResult(
                exitCode = process.exitValue(),
                durationMs = duration,
                stdout = stdout,
                stderr = stderr,
                timedOut = false
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            ExecResult(
                exitCode = -1,
                durationMs = duration,
                stdout = "",
                stderr = e.message ?: "Failed to spawn process",
                timedOut = false
            )
        } finally {
            process?.destroy()
        }
    }
}

