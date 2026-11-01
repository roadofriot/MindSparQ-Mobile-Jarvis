package com.jarves.mh.ui.delegate

import android.app.Application
import android.app.ActivityManager
import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.os.SystemClock
import android.os.Build
import android.os.StatFs
import android.os.Debug
import android.system.Os
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jarves.mh.BuildConfig
import com.jarves.mh.R
import com.jarves.mh.data.ApiKeyVault
import com.jarves.mh.data.ApiKeyInfo
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.data.ProjectStorageManager
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectType
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.RuntimeEvent
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.projectSlug
import com.jarves.mh.model.generateQuickChatIdentity
import com.jarves.mh.model.providerProtocolForAgent
import com.jarves.mh.model.resolveFailoverChain
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.network.ProviderApiClient
import com.jarves.mh.network.GitHubClient
import com.jarves.mh.network.GitHubTokenPoll
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.runtime.ClaudeRuntimeBridge
import com.jarves.mh.runtime.DshRuntimeBridge
import com.jarves.mh.runtime.AgentRegistry
import com.jarves.mh.runtime.AgentUpdateInfo
import com.jarves.mh.runtime.AntigravityAuthController
import com.jarves.mh.runtime.AntigravityAuthState
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.runtime.AntigravityRuntimeBridge
import com.jarves.mh.runtime.NativeSpawnProcess
import com.jarves.mh.runtime.RuntimeInstallProgress
import com.jarves.mh.runtime.localServerPlaceholderKey
import com.jarves.mh.runtime.RuntimeInstaller
import com.jarves.mh.runtime.RuntimeSetupController
import com.jarves.mh.runtime.RuntimeSetupService
import com.jarves.mh.runtime.RuntimeSetupSnapshot
import com.jarves.mh.runtime.RuntimeSetupStatus
import com.jarves.mh.runtime.readHeadBytes
import com.jarves.mh.runtime.readTailText
import com.jarves.mh.runtime.supportsArm64Runtime
import com.jarves.mh.runtime.AndroidAppInstaller
import com.jarves.mh.runtime.AndroidBuildPhase
import com.jarves.mh.runtime.IntegrityScheduler
import com.jarves.mh.runtime.WorkspaceCheckpoints
import com.mindsparq.jarvis.ai.gateway.KeyPoolManager
import com.mindsparq.jarvis.ai.gateway.ManagedKey
import com.mindsparq.jarvis.ai.gateway.KeyState
import com.jarves.mh.runtime.AndroidBuildRecord
import com.jarves.mh.runtime.AndroidAction
import com.jarves.mh.runtime.AndroidApkInfo
import com.jarves.mh.runtime.AndroidBuildIssue
import com.jarves.mh.runtime.AndroidBuildStage
import com.jarves.mh.runtime.AndroidHealthCheck
import com.jarves.mh.runtime.AndroidHealthFix
import com.jarves.mh.runtime.AndroidHealthStatus
import com.jarves.mh.runtime.AndroidLogLevel
import com.jarves.mh.runtime.AndroidLogLine
import com.jarves.mh.runtime.AndroidLogcatState
import com.jarves.mh.runtime.AndroidProjectTemplateGenerator
import com.jarves.mh.runtime.MultiPlatformProjectTemplateGenerator
import com.jarves.mh.runtime.SupportedProjectTemplate
import com.jarves.mh.runtime.RuntimeExecutionService
import com.jarves.mh.runtime.RuntimeTaskController
import com.jarves.mh.runtime.androidGradleCommand
import com.jarves.mh.runtime.diagnoseAndroidBuildFailure
import com.jarves.mh.runtime.findAndroidProjectRoot
import com.jarves.mh.runtime.findDebugApk
import com.jarves.mh.runtime.findReusableDebugApk
import com.jarves.mh.runtime.inferAndroidBuildStage
import com.jarves.mh.runtime.isDebugApkStale
import com.jarves.mh.runtime.parseAndroidBuildIssues
import com.jarves.mh.update.AppUpdateInfo
import com.jarves.mh.update.AppUpdater
import com.jarves.mh.ui.*
import com.jarves.mh.ui.MainViewModel.Companion.MAX_VISIBLE_WORK_ITEMS
import com.jarves.mh.ui.MainViewModel.Companion.DEFERRED_FOLDER_METADATA_NAMES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_ACTIVITY_DETAIL_CHARS
import com.jarves.mh.ui.MainViewModel.Companion.NOISY_ITEM_SCAN_CHARS
import com.jarves.mh.ui.MainViewModel.Companion.TRANSCRIPT_WRITE_DEBOUNCE_MS
import com.jarves.mh.ui.MainViewModel.Companion.DIAGNOSTICS_REFRESH_COOLDOWN_MS
import com.jarves.mh.ui.MainViewModel.Companion.RECENT_EXIT_CACHE_MS
import com.jarves.mh.ui.MainViewModel.Companion.MINIMUM_INITIALIZATION_SCREEN_MS
import com.jarves.mh.ui.MainViewModel.Companion.MAX_EDITABLE_FILE_BYTES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_PROJECT_TERMINAL_HISTORY
import com.jarves.mh.ui.MainViewModel.Companion.MAX_PROJECT_TERMINAL_OUTPUT
import com.jarves.mh.ui.MainViewModel.Companion.MAX_ANDROID_BUILD_LOG
import com.jarves.mh.ui.MainViewModel.Companion.MAX_LOGCAT_LINES
import com.jarves.mh.ui.MainViewModel.Companion.MEMORY_PRESSURE_TERMINAL_LINES
import com.jarves.mh.ui.MainViewModel.Companion.MEMORY_PRESSURE_LOGCAT_LINES
import com.jarves.mh.ui.MainViewModel.Companion.MEMORY_PRESSURE_TEXT_CHARS
import com.jarves.mh.ui.MainViewModel.Companion.LOGCAT_GRANT_COMMAND
import com.jarves.mh.ui.MainViewModel.Companion.MAX_ATTACHMENTS_PER_MESSAGE
import com.jarves.mh.ui.MainViewModel.Companion.MAX_PROCESS_OUTPUT_BYTES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_ATTACHMENT_BYTES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_UPLOAD_BYTES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_IMPORTED_PROJECT_BYTES
import com.jarves.mh.ui.MainViewModel.Companion.MAX_IMPORTED_ZIP_ENTRIES
import com.jarves.mh.ui.MainViewModel.Companion.LEGACY_GITHUB_TOKEN_KEY
import com.jarves.mh.ui.MainViewModel.Companion.GITHUB_DEVICE_URL
import com.jarves.mh.ui.MainViewModel.Companion.GITHUB_DEVICE_CODE
import com.jarves.mh.ui.MainViewModel.Companion.TEST_PROVIDER_DEFAULTS_VERSION
import com.jarves.mh.ui.MainViewModel.Companion.TEST_OPENROUTER_BASE_URL
import com.jarves.mh.ui.MainViewModel.Companion.TEST_OPENROUTER_MODEL
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.net.UnknownHostException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayDeque
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TerminalDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    internal val _terminalLines = MutableStateFlow<List<TerminalOutputLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalOutputLine>> = _terminalLines.asStateFlow()

    internal val _isTerminalRunning = MutableStateFlow(false)
    val isTerminalRunning: StateFlow<Boolean> = _isTerminalRunning.asStateFlow()

    internal val _terminalLiveOutput = MutableStateFlow("")
    val terminalLiveOutput: StateFlow<String> = _terminalLiveOutput.asStateFlow()

    internal val _terminalCurrentCommand = MutableStateFlow<String?>(null)
    val terminalCurrentCommand: StateFlow<String?> = _terminalCurrentCommand.asStateFlow()

    @Volatile internal var projectTerminalProcess: Process? = null
    @Volatile internal var terminalProcess: Process? = null
    @Volatile internal var projectTerminalProjectId: String? = null
    @Volatile internal var projectTerminalStopRequested: Boolean = false
    private var diagnosticsJob: Job? = null
    private var lastDiagnosticsRefreshAtElapsedMillis = 0L
    private var cachedRecentExitSummary: String? = null
    private var recentExitLoadedAtElapsedMillis = 0L
    fun runTerminalCommand(cmd: String) {
        val command = cmd.trim()
        if (command.isBlank() || _isTerminalRunning.value) return
        if (command == "clear") {
            _terminalLines.value = emptyList()
            return
        }
        _isTerminalRunning.value = true
        _terminalCurrentCommand.value = command
        _terminalLiveOutput.value = ""
        viewModelScope.launch {
            val (output, exitCode) = withContext(Dispatchers.IO) {
                runCatching {
                    if (!installer.isInstalled()) {
                        return@runCatching str(R.string.vm_linux_not_ready) to 1
                    }
                    val runtime = installer.installedRuntime()
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/terminal").apply { mkdirs() }
                    val preparedCommand = prepareInteractiveShellCommand(command)
                    val proc = installer.process(
                        proot = runtime.proot,
                        rootfs = runtime.rootfs,
                        workspace = workspace,
                        environment = emptyMap(),
                        guestCommand = listOf("/usr/bin/bash", "-c", preparedCommand),
                    )
                    terminalProcess = proc
                    val native = proc as? NativeSpawnProcess
                    var offset = 0L
                    val streamed = StringBuilder()
                    var autoConfirmed = false
                    while (proc.isAlive || (native?.outputFile?.length() ?: 0L) > offset) {
                        val file = native?.outputFile
                        val available = (file?.length() ?: 0L) - offset
                        if (file == null || available <= 0) {
                            Thread.sleep(50)
                            continue
                        }
                        val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
                        val count = RandomAccessFile(file, "r").use { input ->
                            input.seek(offset)
                            input.read(bytes)
                        }
                        if (count > 0) {
                            offset += count
                            streamed.append(bytes.decodeToString(0, count))
                            _terminalLiveOutput.value = sanitizeTerminalOutput(streamed.toString())
                                .trimEnd()
                                .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
                            if (!autoConfirmed && shouldAutoConfirmPackageCommand(command, streamed.toString())) {
                                proc.outputStream.write("y\n".toByteArray())
                                proc.outputStream.flush()
                                autoConfirmed = true
                            }
                        }
                    }
                    val exit = proc.waitFor()
                    runCatching { proc.outputStream.close() }
                    val out = sanitizeTerminalOutput(streamed.toString()).trim()
                    val finalOut = if (out.isNotEmpty() || exit == 0) out else str(R.string.vm_process_exited_code, exit)
                    finalOut to exit
                }.getOrElse { str(R.string.vm_error_with_message, it.message.orEmpty()) to 1 }
            }
            _terminalLines.update { it + TerminalOutputLine(command = command, output = output, exitCode = exitCode) }
            _terminalLiveOutput.value = ""
            _terminalCurrentCommand.value = null
            _isTerminalRunning.value = false
            terminalProcess = null
        }
    }

    fun sendTerminalInput(text: String) {
        sendProcessInput(terminalProcess, text)
    }

    fun interruptTerminalCommand() {
        interruptProcess(terminalProcess)
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun requestProjectTerminalCommand(command: String) {
        val normalized = command.trim()
        if (normalized.isBlank() || _state.value.projectTerminalRunning || _state.value.androidBuildRunning || projectTerminalProcess?.isAlive == true) return
        if (androidDelegate.requiresAndroidToolchain(normalized) && !installer.isStackInstalled(DevStack.ANDROID)) {
            _state.update {
                it.copy(toastMessage = str(R.string.vm_android_tools_missing))
            }
            return
        }
        if (isDestructiveTerminalCommand(normalized)) {
            _state.update { it.copy(pendingTerminalCommand = normalized) }
        } else {
            runProjectTerminalCommand(normalized)
        }
    }

    fun prepareProjectTerminalCommand(command: String) {
        val project = _state.value.activeProject ?: return
        if (command.isBlank() || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        _state.update {
            it.copy(
                projectTerminalCwd = projectGuestRoot(project),
                projectTerminalDraft = command.trim(),
            )
        }
    }

    fun consumeProjectTerminalDraft() {
        _state.update { it.copy(projectTerminalDraft = null) }
    }

    fun openProjectTerminal() {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        _state.update { it.copy(projectTerminalCwd = projectGuestRoot(project)) }
    }

    fun confirmProjectTerminalCommand() {
        val command = _state.value.pendingTerminalCommand ?: return
        _state.update { it.copy(pendingTerminalCommand = null) }
        runProjectTerminalCommand(command)
    }

    fun cancelProjectTerminalCommand() {
        _state.update { it.copy(pendingTerminalCommand = null) }
    }

    private fun runProjectTerminalCommand(command: String) {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        val startingCwd = _state.value.projectTerminalCwd
        val existingLines = _state.value.projectTerminalLines
        projectTerminalStopRequested = false
        val requestedPreviewUrl = detectServerUrl(command)
        _state.update {
            it.copy(
                projectTerminalRunning = true,
                projectTerminalLiveOutput = "",
                projectTerminalCommand = command,
                pendingTerminalCommand = null,
                previewReady = it.previewReady || requestedPreviewUrl != null,
                previewUrl = requestedPreviewUrl ?: it.previewUrl,
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { runProjectTerminalProcess(project.id, command, startingCwd) }
                    .getOrElse { error ->
                        ProjectTerminalResult(
                            output = str(R.string.vm_terminal_error, error.message ?: error::class.java.simpleName),
                            exitCode = 1,
                            cwd = startingCwd,
                        )
                    }
            }
            val completedLine = TerminalOutputLine(
                command = command,
                output = result.output.ifBlank {
                    if (result.exitCode == 0) "" else str(R.string.vm_process_exited_code, result.exitCode)
                },
                exitCode = result.exitCode,
            )
            val updatedLines = (existingLines + completedLine).takeLast(MAX_PROJECT_TERMINAL_HISTORY)
            saveProjectTerminal(project.id, result.cwd, updatedLines)
            if (_state.value.activeProject?.id == project.id) {
                _state.update {
                    it.copy(
                        projectTerminalLines = updatedLines,
                        projectTerminalLiveOutput = "",
                        projectTerminalRunning = false,
                        projectTerminalCwd = result.cwd,
                        projectTerminalCommand = null,
                    )
                }
                refreshProjectFiles()
            }
            projectTerminalProcess = null
            projectTerminalProjectId = null
            projectTerminalStopRequested = false
        }
    }

    fun stopProjectTerminalCommand() {
        if (!_state.value.projectTerminalRunning) return
        projectTerminalStopRequested = true
        viewModelScope.launch(Dispatchers.IO) {
            projectTerminalProcess?.destroy()
            delay(400)
            if (projectTerminalProcess?.isAlive == true) projectTerminalProcess?.destroyForcibly()
        }
    }

    fun sendProjectTerminalInput(text: String) {
        sendProcessInput(projectTerminalProcess, text)
    }

    fun interruptProjectTerminalCommand() {
        interruptProcess(projectTerminalProcess)
    }

    fun clearProjectTerminal() {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning) return
        _state.update { it.copy(projectTerminalLines = emptyList(), projectTerminalLiveOutput = "") }
        saveProjectTerminal(project.id, _state.value.projectTerminalCwd, emptyList())
    }

    fun refreshDeveloperDiagnostics() {
        if (diagnosticsJob?.isActive == true) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastDiagnosticsRefreshAtElapsedMillis < DIAGNOSTICS_REFRESH_COOLDOWN_MS) return
        _state.update { it.copy(diagnostics = it.diagnostics.copy(loading = true)) }
        diagnosticsJob = viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { collectDeveloperDiagnostics() }
                lastDiagnosticsRefreshAtElapsedMillis = SystemClock.elapsedRealtime()
                _state.update { it.copy(diagnostics = snapshot) }
            } finally {
                diagnosticsJob = null
            }
        }
    }

    fun stopInactiveDeveloperProcesses() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _state.value
            if (!current.isRunning) {
                runCatching { claudeRuntime.stopActiveSession() }
                runCatching { dshRuntime.stopActiveSession() }
                runCatching { antigravityRuntime.stopActiveSession() }
            }
            if (!current.projectTerminalRunning) {
                projectTerminalProcess?.takeIf(Process::isAlive)?.destroyForcibly()
                projectTerminalProcess = null
            }
            if (!current.androidBuildRunning) {
                androidDelegate.androidBuildProcess?.takeIf(Process::isAlive)?.destroyForcibly()
                androidDelegate.androidBuildProcess = null
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(toastMessage = str(R.string.diagnostics_inactive_stopped)) }
                lastDiagnosticsRefreshAtElapsedMillis = 0L
                refreshDeveloperDiagnostics()
            }
        }
    }

    fun clearDeveloperRuntimeCache() {
        val current = _state.value
        if (current.isRunning || current.projectTerminalRunning || current.androidBuildRunning) {
            _state.update { it.copy(toastMessage = str(R.string.diagnostics_cache_busy)) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.jarves.mh.runtime.OfflineModelManager.clearAppCache(getApplication())
            releaseMemoryCaches()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(toastMessage = "✨ Cache Cleared! Freed ${result.formattedFreed} · System optimized.") }
                lastDiagnosticsRefreshAtElapsedMillis = 0L
                refreshDeveloperDiagnostics()
            }
        }
    }

    /** Drops only rebuildable or already-persisted data when Android reports memory pressure. */
    fun releaseMemoryCaches() {
        workspaceDelegate.folderMetadataCache.clear()
        cachedRecentExitSummary = null
        _state.update { current ->
            current.copy(
                projectTerminalLines = current.projectTerminalLines.takeLast(MEMORY_PRESSURE_TERMINAL_LINES),
                projectTerminalLiveOutput = current.projectTerminalLiveOutput.takeLast(MEMORY_PRESSURE_TEXT_CHARS),
                androidBuildLog = current.androidBuildLog.takeLast(MEMORY_PRESSURE_TEXT_CHARS),
                androidLogcat = current.androidLogcat.copy(
                    lines = current.androidLogcat.lines.takeLast(MEMORY_PRESSURE_LOGCAT_LINES),
                ),
            )
        }
    }

    private fun collectDeveloperDiagnostics(): DeveloperDiagnostics {
        val current = _state.value
        val memory = Debug.MemoryInfo().also(Debug::getMemoryInfo)
        val runtime = Runtime.getRuntime()
        val active = buildList {
            if (current.isRunning) add(current.agentKind.title)
            if (current.projectTerminalRunning || projectTerminalProcess?.isAlive == true) add(str(R.string.an_process_project_terminal))
            if (terminalProcess?.isAlive == true) add(str(R.string.an_process_quick_terminal))
            if (current.androidBuildRunning || androidDelegate.androidBuildProcess?.isAlive == true) add(str(R.string.an_android_build))
            if (current.androidLogcat.running || androidDelegate.androidLogcatProcess?.isAlive == true) add(str(R.string.android_logcat_tab))
        }
        val runtimeCache = getApplication<Application>().cacheDir.listFiles().orEmpty()
            .asSequence()
            .filter { it.isFile && it.name.startsWith("runtime-output-") }
            .sumOf(File::length)
        return DeveloperDiagnostics(
            appPssBytes = memory.totalPss.toLong() * 1_024L,
            javaHeapBytes = runtime.totalMemory() - runtime.freeMemory(),
            javaHeapMaxBytes = runtime.maxMemory(),
            activeProcesses = active,
            messageCount = current.messages.size,
            liveActivityCount = current.liveProcess.size,
            terminalBufferBytes = utf8SizeInBytes(current.projectTerminalLiveOutput) +
                current.projectTerminalLines.sumOf { utf8SizeInBytes(it.output) },
            buildLogBytes = utf8SizeInBytes(current.androidBuildLog),
            logcatLines = current.androidLogcat.lines.size,
            fileEntries = current.workspaceFiles.size,
            runtimeCacheBytes = runtimeCache,
            recentExit = recentAppExitSummary(),
            refreshedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun recentAppExitSummary(): String? {
        val now = SystemClock.elapsedRealtime()
        if (now - recentExitLoadedAtElapsedMillis < RECENT_EXIT_CACHE_MS) return cachedRecentExitSummary
        recentExitLoadedAtElapsedMillis = now
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val manager = getApplication<Application>().getSystemService(ActivityManager::class.java)
        val exit = manager.getHistoricalProcessExitReasons(getApplication<Application>().packageName, 0, 8)
            .firstOrNull { info ->
                info.reason == android.app.ApplicationExitInfo.REASON_ANR ||
                    info.reason == android.app.ApplicationExitInfo.REASON_CRASH ||
                    info.reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    info.reason == android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ||
                    info.reason == android.app.ApplicationExitInfo.REASON_LOW_MEMORY
            } ?: run {
                cachedRecentExitSummary = null
                return null
            }
        val reason = when (exit.reason) {
            android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
            android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> str(R.string.an_exit_low_memory)
            android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> str(R.string.an_exit_excessive_resources)
            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> str(R.string.an_exit_native_crash)
            else -> str(R.string.an_exit_crash)
        }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(exit.timestamp))
        return ("$reason · $time" + exit.description?.takeIf(String::isNotBlank)?.let { " · ${it.take(160)}" }.orEmpty())
            .also { cachedRecentExitSummary = it }
    }

    private fun runProjectTerminalProcess(projectId: String, command: String, cwd: String): ProjectTerminalResult {
        if (!installer.isInstalled()) return ProjectTerminalResult(str(R.string.vm_linux_not_ready), 1, cwd)
        val installed = installer.installedRuntime()
        val project = _state.value.projects.firstOrNull { it.id == projectId }
            ?: _state.value.activeProject?.takeIf { it.id == projectId }
            ?: return ProjectTerminalResult(str(R.string.vm_project_unavailable), 1, cwd)
        val workspace = projectWorkspaceRoot(project)
        val guestWorkspacePath = projectGuestRoot(project)
        val marker = "__POCKETDEV_CWD_${UUID.randomUUID()}__"
        val preparedCommand = prepareInteractiveShellCommand(command)
        val script = """
            cd -- ${shellQuote(cwd)} || exit 1
            $preparedCommand
            pocket_status=${'$'}?
            printf '\n$marker%s\n' "${'$'}PWD"
            exit ${'$'}pocket_status
        """.trimIndent()
        val process = installer.process(
            proot = installed.proot,
            rootfs = installed.rootfs,
            workspace = workspace,
            environment = emptyMap(),
            guestCommand = listOf("/usr/bin/bash", "-lc", script),
            guestWorkspacePath = guestWorkspacePath,
        )
        projectTerminalProcess = process
        projectTerminalProjectId = projectId
        if (projectTerminalStopRequested) process.destroy()
        val native = process as? NativeSpawnProcess
            ?: return ProjectTerminalResult(str(R.string.vm_unsupported_terminal_process), 1, cwd)
        var offset = 0L
        val output = StringBuilder()
        var autoConfirmed = false
        while (process.isAlive || native.outputFile.length() > offset) {
            val available = native.outputFile.length() - offset
            if (available <= 0) {
                Thread.sleep(50)
                continue
            }
            val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
            val count = RandomAccessFile(native.outputFile, "r").use { file ->
                file.seek(offset)
                file.read(bytes)
            }
            if (count > 0) {
                offset += count
                output.append(bytes.decodeToString(0, count))
                val visible = sanitizeTerminalOutput(output.toString().substringBefore(marker))
                    .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
                if (!autoConfirmed && shouldAutoConfirmPackageCommand(command, visible)) {
                    process.outputStream.write("y\n".toByteArray())
                    process.outputStream.flush()
                    autoConfirmed = true
                }
                val detectedPreviewUrl = detectPreviewUrl(visible)
                _state.update { current ->
                    if (current.activeProject?.id == projectId) {
                        current.copy(
                            projectTerminalLiveOutput = visible,
                            previewReady = current.previewReady || detectedPreviewUrl != null,
                            previewUrl = detectedPreviewUrl ?: current.previewUrl,
                        )
                    } else current
                }
            }
        }
        val exitCode = process.waitFor()
        runCatching { process.outputStream.close() }
        val raw = output.toString()
        val cwdAfter = raw.substringAfter(marker, "")
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.takeIf { it == guestWorkspacePath || it.startsWith("$guestWorkspacePath/") }
            ?: cwd
        val cleanOutput = sanitizeTerminalOutput(raw.substringBefore(marker))
            .trim()
            .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
        return ProjectTerminalResult(cleanOutput, exitCode, cwdAfter)
    }

    private fun sendProcessInput(process: Process?, text: String) {
        if (process?.isAlive != true || text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                process.outputStream.write((text + "\n").toByteArray())
                process.outputStream.flush()
            }.onFailure {
                _state.update { current -> current.copy(toastMessage = str(R.string.vm_process_not_accepting_input)) }
            }
        }
    }

    private fun interruptProcess(process: Process?) {
        if (process?.isAlive != true) return
        viewModelScope.launch(Dispatchers.IO) {
            (process as? NativeSpawnProcess)?.interrupt() ?: process.destroy()
        }
    }

    /**
     * Package management must never block on Y/N, locale, timezone, service-restart,
     * or config-file dialogs in the phone UI. Other commands remain interactive and
     * can receive input through [sendProcessInput].
     */
    private fun prepareInteractiveShellCommand(command: String): String {
        val normalizedApt = command
            .replace(Regex("(?<![\\w-])sudo\\s+apt(?:-get)?\\s+"), "apt-get ")
            .replace(Regex("(?<![\\w-])apt\\s+"), "apt-get ")
            .replace(
                Regex("(?<![\\w-])apt-get\\s+(install|upgrade|full-upgrade|dist-upgrade|remove|autoremove|fix-broken)\\b"),
                "apt-get -y -o Dpkg::Options::=--force-confold $1",
            )
        return "export DEBIAN_FRONTEND=noninteractive APT_LISTCHANGES_FRONTEND=none UCF_FORCE_CONFFOLD=1 NEEDRESTART_MODE=a TZ=Etc/UTC LC_ALL=C.UTF-8; $normalizedApt"
    }

    private fun shouldAutoConfirmPackageCommand(command: String, output: String): Boolean {
        val packageCommand = Regex("(?i)(^|[;&|]\\s*)(sudo\\s+)?(apt|apt-get|dpkg)\\b").containsMatchIn(command)
        if (!packageCommand) return false
        val tail = output.takeLast(500)
        return Regex("(?i)(do you want to continue|continue\\?)\\s*\\[[Yy]/[Nn]\\]").containsMatchIn(tail)
    }

    private fun isDestructiveTerminalCommand(command: String): Boolean {
        val normalized = command.lowercase().replace(Regex("\\s+"), " ")
        return listOf(
            "rm -rf", "rm -fr", "git reset --hard", "git clean -f", "git push --force",
            "mkfs", "dd if=", "chmod -r 777", "shutdown", "reboot", ":(){", "kill \$(pgrep", "pkill -f",
        ).any(normalized::contains) || Regex("(curl|wget).*(\\||>)\\s*(sh|bash)").containsMatchIn(normalized)
    }

    private fun detectPreviewUrl(output: String): String? {
        val match = Regex("https?://(?:localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0):(\\d{2,5})(?:/[^\\s]*)?")
            .findAll(output)
            .lastOrNull()
            ?: return null
        val port = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return "http://127.0.0.1:$port/"
    }

    private fun detectServerUrl(command: String): String? {
        val match = Regex("""python(?:3)?\s+-m\s+http\.server(?:\s+(\d{2,5}))?""")
            .find(command)
            ?: return null
        val port = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 8000
        return port.takeIf { it in 1..65535 }?.let { "http://127.0.0.1:$it/" }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    internal fun terminalHistoryFile(projectId: String): File =
        File(getApplication<Application>().filesDir, "terminal-history/$projectId.json")

    internal fun loadProjectTerminal(project: Project): ProjectTerminalSnapshot {
        val file = terminalHistoryFile(project.id)
        val guestRoot = projectGuestRoot(project)
        if (!file.isFile) return ProjectTerminalSnapshot(cwd = guestRoot)
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("lines") ?: JSONArray()
            val lines = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                TerminalOutputLine(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    command = item.optString("command"),
                    output = item.optString("output"),
                    exitCode = item.optInt("exitCode"),
                )
            }
            ProjectTerminalSnapshot(
                lines = lines.takeLast(MAX_PROJECT_TERMINAL_HISTORY),
                cwd = root.optString("cwd", guestRoot).takeIf {
                    it == guestRoot || it.startsWith("$guestRoot/")
                } ?: guestRoot,
            )
        }.getOrDefault(ProjectTerminalSnapshot(cwd = guestRoot))
    }

    internal fun saveProjectTerminal(projectId: String, cwd: String, lines: List<TerminalOutputLine>) {
        runCatching {
            val file = terminalHistoryFile(projectId)
            file.parentFile?.mkdirs()
            val array = JSONArray()
            lines.takeLast(MAX_PROJECT_TERMINAL_HISTORY).forEach { line ->
                array.put(
                    JSONObject()
                        .put("id", line.id)
                        .put("command", line.command)
                        .put("output", line.output.takeLast(MAX_PROJECT_TERMINAL_OUTPUT))
                        .put("exitCode", line.exitCode),
                )
            }
            file.writeText(JSONObject().put("cwd", cwd).put("lines", array).toString())
        }
    }


}
