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

class AndroidBuildDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    @Volatile internal var androidBuildProcess: Process? = null
    @Volatile internal var androidBuildStopRequested: Boolean = false
    @Volatile internal var androidLogcatProcess: Process? = null
    internal var androidLogcatJob: Job? = null
    internal var pendingAndroidInstallId: String? = null
    internal var pendingAndroidInstallLaunch = false
    internal var androidLaunchAtMillis: Long? = null
    fun buildAndRunAndroidApp() {
        performAndroidAction(AndroidAction.BUILD_AND_RUN)
    }

    fun performAndroidAction(action: AndroidAction) {
        when (action) {
            AndroidAction.BUILD -> {
                if (!completeFromReusableApk(action, launch = false)) startAndroidGradleTask("assembleDebug", action)
            }
            AndroidAction.FORCE_BUILD -> startAndroidGradleTask("assembleDebug", action)
            AndroidAction.INSTALL -> installLatestAndroidApk(launch = false, action = action)
            AndroidAction.RUN -> runLatestAndroidApk()
            AndroidAction.BUILD_AND_RUN -> {
                if (!completeFromReusableApk(action, launch = true)) startAndroidGradleTask("assembleDebug", action)
            }
            AndroidAction.CLEAN -> startAndroidGradleTask("clean", action)
            AndroidAction.NONE -> Unit
        }
    }

    private fun completeFromReusableApk(action: AndroidAction, launch: Boolean): Boolean {
        val current = _state.value
        val project = current.activeProject ?: return false
        if (current.isRunning || current.projectTerminalRunning || current.androidBuildRunning) return false
        val workspace = findAndroidProjectRoot(projectWorkspaceRoot(project)) ?: return false
        val apk = findReusableDebugApk(workspace) ?: return false
        if (launch) {
            if (AndroidAppInstaller.openIfAlreadyInstalled(getApplication(), apk)) {
                androidLaunchAtMillis = System.currentTimeMillis()
            } else {
                beginAndroidInstall(apk, launch = true, action = action)
                return true
            }
        }
        val now = System.currentTimeMillis()
        val message = if (launch) str(R.string.an_no_changes_opened_app) else str(R.string.an_apk_already_current)
        _state.update {
            it.copy(
                androidBuildRunning = false,
                androidBuildPhase = AndroidBuildPhase.SUCCEEDED,
                androidBuildAction = action,
                androidBuildStage = AndroidBuildStage.COMPLETE,
                androidBuildIssues = emptyList(),
                androidBuildMessage = message,
                androidBuildLog = "Gradle skipped: project files have not changed since the last successful build.\n",
                androidBuildStartedAtMillis = now,
                androidBuildFinishedAtMillis = now,
                androidBuildApkPath = apk.absolutePath,
                androidBuildApkSizeBytes = apk.length(),
                toastMessage = message,
            )
        }
        saveAndroidBuildRecord(project.id)
        refreshAndroidApkInfo()
        return true
    }

    fun cleanAndroidProject() {
        performAndroidAction(AndroidAction.CLEAN)
    }

    private fun runLatestAndroidApk() {
        val project = _state.value.activeProject ?: return
        val root = findAndroidProjectRoot(projectWorkspaceRoot(project))
        val apk = root?.let(::findDebugApk)
        if (apk == null) {
            _state.update { it.copy(toastMessage = str(R.string.an_build_apk_before_run)) }
            return
        }
        if (AndroidAppInstaller.openIfAlreadyInstalled(getApplication(), apk)) {
            androidLaunchAtMillis = System.currentTimeMillis()
            val now = System.currentTimeMillis()
            _state.update {
                it.copy(
                    androidBuildAction = AndroidAction.RUN,
                    androidBuildStage = AndroidBuildStage.COMPLETE,
                    androidBuildPhase = AndroidBuildPhase.SUCCEEDED,
                    androidBuildMessage = str(R.string.an_app_launched_without_rebuild),
                    androidBuildStartedAtMillis = now,
                    androidBuildFinishedAtMillis = now,
                    toastMessage = str(R.string.an_app_launched),
                )
            }
        } else {
            beginAndroidInstall(apk, launch = true, action = AndroidAction.RUN)
        }
    }

    private fun installLatestAndroidApk(launch: Boolean, action: AndroidAction) {
        val project = _state.value.activeProject ?: return
        val root = findAndroidProjectRoot(projectWorkspaceRoot(project))
        val apk = root?.let(::findDebugApk)
        if (apk == null) {
            _state.update { it.copy(toastMessage = str(R.string.an_build_apk_before_install)) }
            return
        }
        beginAndroidInstall(apk, launch, action)
    }

    private fun beginAndroidInstall(apk: File, launch: Boolean, action: AndroidAction) {
        if (_state.value.androidBuildRunning && pendingAndroidInstallId != null) return
        val operationId = UUID.randomUUID().toString()
        pendingAndroidInstallId = operationId
        pendingAndroidInstallLaunch = launch
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                androidBuildRunning = true,
                androidBuildAction = action,
                androidBuildStage = AndroidBuildStage.INSTALLING,
                androidBuildPhase = AndroidBuildPhase.BUILDING,
                androidBuildMessage = if (launch) str(R.string.an_installing_apk_before_launch) else str(R.string.an_installing_apk),
                androidBuildIssues = emptyList(),
                androidBuildStartedAtMillis = it.androidBuildStartedAtMillis ?: now,
                androidBuildFinishedAtMillis = null,
                androidBuildApkPath = apk.absolutePath,
                androidBuildApkSizeBytes = apk.length(),
            )
        }
        runCatching {
            AndroidAppInstaller.install(getApplication(), apk, launchAfterInstall = launch, operationId = operationId)
        }.onFailure { error ->
            pendingAndroidInstallId = null
            pendingAndroidInstallLaunch = false
            _state.update {
                it.copy(
                    androidBuildRunning = false,
                    androidBuildPhase = AndroidBuildPhase.FAILED,
                    androidBuildStage = AndroidBuildStage.FAILED,
                    androidBuildMessage = error.message ?: str(R.string.an_could_not_start_install),
                    androidBuildIssues = listOf(AndroidBuildIssue(title = str(R.string.rt_apk_install_failed), detail = error.message ?: str(R.string.an_could_not_start_installer))),
                )
            }
        }
    }

    fun cancelAndroidBuild() {
        if (!_state.value.androidBuildRunning) return
        androidBuildStopRequested = true
        _state.update {
            it.copy(
                androidBuildPhase = AndroidBuildPhase.CANCELLING,
                androidBuildStage = AndroidBuildStage.CANCELLING,
                androidBuildMessage = str(R.string.an_stopping_gradle),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            androidBuildProcess?.destroy()
            delay(500)
            if (androidBuildProcess?.isAlive == true) androidBuildProcess?.destroyForcibly()
        }
    }

    private fun startAndroidGradleTask(task: String, action: AndroidAction) {
        val project = _state.value.activeProject ?: return
        if (_state.value.androidBuildRunning) return
        if (!installer.isStackInstalled(DevStack.ANDROID)) {
            _state.update {
                it.copy(toastMessage = str(R.string.vm_android_tools_missing))
            }
            return
        }
        if (_state.value.isRunning) {
            _state.update { it.copy(toastMessage = str(R.string.vm_wait_agent_before_build)) }
            return
        }
        if (_state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = str(R.string.vm_wait_terminal_before_build)) }
            return
        }
        val startedAt = System.currentTimeMillis()
        androidBuildStopRequested = false
        _state.update {
            it.copy(
                androidBuildRunning = true,
                androidBuildPhase = AndroidBuildPhase.PREPARING,
                androidBuildAction = action,
                androidBuildStage = AndroidBuildStage.PREPARING,
                androidBuildIssues = emptyList(),
                androidBuildMessage = if (task == "clean") str(R.string.an_preparing_clean) else str(R.string.an_preparing_build),
                androidBuildLog = "",
                androidBuildStartedAtMillis = startedAt,
                androidBuildFinishedAtMillis = null,
                androidBuildApkPath = null,
                androidBuildApkSizeBytes = null,
                toastMessage = null,
            )
        }
        saveAndroidBuildRecord(project.id)
        startAndroidBuildForegroundService(project, task)
        viewModelScope.launch(Dispatchers.IO) {
            var buildWorkspace: File? = null
            var finalOutput = ""
            var finalExitCode = -1
            runCatching {
                val installed = installer.installedRuntime()
                val workspace = findAndroidProjectRoot(projectWorkspaceRoot(project))
                    ?: error(str(R.string.vm_no_android_project))
                buildWorkspace = workspace
                val command = androidGradleCommand(workspace, task)
                _state.update {
                    it.copy(
                        androidBuildPhase = AndroidBuildPhase.BUILDING,
                        androidBuildStage = AndroidBuildStage.RESOLVING,
                        androidBuildMessage = if (task == "clean") str(R.string.an_cleaning_project) else str(R.string.an_running_assemble),
                        androidBuildLog = "$ $command\n",
                    )
                }
                val process = installer.process(
                    installed.proot, installed.rootfs, workspace, emptyMap(),
                    listOf("/usr/bin/bash", "-lc", command),
                    projectGuestRoot(project),
                )
                androidBuildProcess = process
                RuntimeTaskController.stopAction = ::cancelAndroidBuild
                val native = process as? NativeSpawnProcess ?: error(str(R.string.an_unsupported_build_process))
                var offset = 0L
                while (process.isAlive || native.outputFile.length() > offset) {
                    val available = native.outputFile.length() - offset
                    if (available <= 0L) {
                        Thread.sleep(75)
                        continue
                    }
                    val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
                    val count = RandomAccessFile(native.outputFile, "r").use { file ->
                        file.seek(offset)
                        file.read(bytes)
                    }
                    if (count > 0) {
                        offset += count
                        val chunk = sanitizeTerminalOutput(bytes.decodeToString(0, count))
                        _state.update { current ->
                            val nextStage = inferAndroidBuildStage(chunk, current.androidBuildStage)
                            current.copy(
                                androidBuildLog = (current.androidBuildLog + chunk).takeLast(MAX_ANDROID_BUILD_LOG),
                                androidBuildStage = nextStage,
                                androidBuildMessage = androidStageMessage(nextStage),
                            )
                        }
                    }
                }
                val exitCode = process.waitFor()
                val buildOutput = sanitizeTerminalOutput(native.outputFile.readTailText(MAX_PROCESS_OUTPUT_BYTES))
                finalExitCode = exitCode
                finalOutput = buildOutput
                if (androidBuildStopRequested) throw java.util.concurrent.CancellationException("Build cancelled")
                check(exitCode == 0) {
                    diagnoseAndroidBuildFailure(buildOutput, exitCode)
                }
                if (task == "clean") null else {
                    _state.update { it.copy(androidBuildStage = AndroidBuildStage.FINDING_APK, androidBuildMessage = str(R.string.an_finding_apk)) }
                    findDebugApk(workspace) ?: error(str(R.string.an_no_debug_apk_found))
                }
            }.onSuccess { apk ->
                val shouldInstallAndRun = apk != null && action == AndroidAction.BUILD_AND_RUN
                val message = if (apk == null) str(R.string.an_project_cleaned) else str(R.string.an_debug_apk_built)
                val buildCompletedAt = System.currentTimeMillis()
                withContext(Dispatchers.Main) {
                    _state.update { current ->
                        val duration = (buildCompletedAt - startedAt).coerceAtLeast(1L)
                        val recentDurations = if (apk != null) {
                            (current.androidBuildRecentDurationsMillis + duration).takeLast(5)
                        } else current.androidBuildRecentDurationsMillis
                        current.copy(
                            androidBuildRunning = shouldInstallAndRun,
                            androidBuildPhase = AndroidBuildPhase.SUCCEEDED,
                            androidBuildStage = if (shouldInstallAndRun) AndroidBuildStage.INSTALLING else AndroidBuildStage.COMPLETE,
                            androidBuildMessage = message,
                            androidBuildIssues = emptyList(),
                            androidBuildFinishedAtMillis = buildCompletedAt,
                            androidBuildApkPath = apk?.absolutePath,
                            androidBuildApkSizeBytes = apk?.length(),
                            androidBuildRecentDurationsMillis = recentDurations,
                            toastMessage = message,
                        )
                    }
                    saveAndroidBuildRecord(project.id)
                    refreshAndroidApkInfo()
                }
                finishAndroidBuildForegroundService(project, message, failed = false)
                if (shouldInstallAndRun) beginAndroidInstall(apk!!, launch = true, action = action)
            }.onFailure { error ->
                val cancelled = error is java.util.concurrent.CancellationException || androidBuildStopRequested
                val message = if (cancelled) str(R.string.an_build_cancelled) else error.message?.let { localizedAndroidBuildText(AppStrings.context(getApplication()), it) } ?: str(R.string.an_could_not_build_apk)
                val issues = if (cancelled) emptyList() else buildWorkspace?.let {
                    parseAndroidBuildIssues(finalOutput.ifBlank { message }, it, finalExitCode)
                }.orEmpty().ifEmpty { listOf(AndroidBuildIssue(title = str(R.string.android_build_failed_title), detail = message)) }
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            androidBuildRunning = false,
                            androidBuildPhase = if (cancelled) AndroidBuildPhase.CANCELLED else AndroidBuildPhase.FAILED,
                            androidBuildStage = if (cancelled) AndroidBuildStage.CANCELLED else AndroidBuildStage.FAILED,
                            androidBuildMessage = message,
                            androidBuildIssues = issues,
                            androidBuildFinishedAtMillis = System.currentTimeMillis(),
                            toastMessage = message,
                        )
                    }
                    saveAndroidBuildRecord(project.id)
                }
                finishAndroidBuildForegroundService(project, message, failed = !cancelled, cancelled = cancelled)
            }.also {
                androidBuildProcess = null
                androidBuildStopRequested = false
                RuntimeTaskController.stopAction = null
                chatDelegate.steeringToFollowUp = false
                if (chatDelegate.pendingFollowUps.isNotEmpty()) {
                    viewModelScope.launch {
                        delay(150)
                        if (!_state.value.isRunning && !_state.value.androidBuildRunning && !_state.value.projectTerminalRunning) {
                            chatDelegate.startNextFollowUp()
                        }
                    }
                }
            }
        }
    }

    internal fun requiresAndroidToolchain(command: String): Boolean =
        Regex("(?m)(^|[;&|]\\s*)(?:\\./)?gradle(?:w)?(?:\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(command)

    private fun androidStageMessage(stage: AndroidBuildStage): String = when (stage) {
        AndroidBuildStage.PREPARING -> str(R.string.an_preparing_build)
        AndroidBuildStage.RESOLVING -> str(R.string.an_resolving_dependencies)
        AndroidBuildStage.COMPILING -> str(R.string.an_compiling_source)
        AndroidBuildStage.RESOURCES -> str(R.string.an_processing_resources)
        AndroidBuildStage.PACKAGING -> str(R.string.an_packaging_apk)
        AndroidBuildStage.FINDING_APK -> str(R.string.an_finding_apk)
        AndroidBuildStage.INSTALLING -> str(R.string.an_installing_apk)
        AndroidBuildStage.LAUNCHING -> str(R.string.an_launching_app)
        AndroidBuildStage.COMPLETE -> str(R.string.android_stage_complete)
        AndroidBuildStage.CANCELLING -> str(R.string.an_stopping_gradle)
        AndroidBuildStage.CANCELLED -> str(R.string.an_build_cancelled)
        AndroidBuildStage.FAILED -> str(R.string.an_operation_failed)
        AndroidBuildStage.IDLE -> str(R.string.android_stage_idle)
    }

    private fun startAndroidBuildForegroundService(project: Project, task: String) {
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), RuntimeExecutionService::class.java)
                .setAction(RuntimeExecutionService.ACTION_START)
                .putExtra(RuntimeExecutionService.EXTRA_PROJECT_NAME, project.name)
                .putExtra(RuntimeExecutionService.EXTRA_TITLE, if (task == "clean") str(R.string.an_notif_cleaning_title) else str(R.string.an_notif_building_title))
                .putExtra(RuntimeExecutionService.EXTRA_DETAIL, if (task == "clean") str(R.string.an_notif_cleaning_detail) else str(R.string.an_notif_building_detail))
                .putExtra(RuntimeExecutionService.EXTRA_CAN_STOP, true),
        )
    }

    private fun finishAndroidBuildForegroundService(
        project: Project,
        detail: String,
        failed: Boolean,
        cancelled: Boolean = false,
    ) {
        runCatching {
            getApplication<Application>().startService(
                Intent(getApplication(), RuntimeExecutionService::class.java)
                    .setAction(
                        when {
                            cancelled -> RuntimeExecutionService.ACTION_CANCELLED
                            failed -> RuntimeExecutionService.ACTION_FAILED
                            else -> RuntimeExecutionService.ACTION_COMPLETE
                        },
                    )
                    .putExtra(RuntimeExecutionService.EXTRA_PROJECT_NAME, project.name)
                    .putExtra(RuntimeExecutionService.EXTRA_DETAIL, detail),
            )
        }
    }

    internal fun androidBuildRecordFile(projectId: String): File =
        File(getApplication<Application>().filesDir, "android-builds/$projectId.json")

    internal fun saveAndroidBuildRecord(projectId: String) {
        val state = _state.value
        runCatching {
            androidBuildRecordFile(projectId).apply {
                parentFile?.mkdirs()
                writeText(
                    JSONObject()
                        .put("phase", state.androidBuildPhase.name)
                        .put("message", state.androidBuildMessage)
                        .put("log", state.androidBuildLog.takeLast(MAX_ANDROID_BUILD_LOG))
                        .put("startedAtMillis", state.androidBuildStartedAtMillis)
                        .put("finishedAtMillis", state.androidBuildFinishedAtMillis)
                        .put("apkPath", state.androidBuildApkPath)
                        .put("apkSizeBytes", state.androidBuildApkSizeBytes)
                        .put("action", state.androidBuildAction.name)
                        .put("stage", state.androidBuildStage.name)
                        .put("recentDurationsMillis", JSONArray(state.androidBuildRecentDurationsMillis))
                        .put("issues", JSONArray().apply {
                            state.androidBuildIssues.forEach { issue ->
                                put(JSONObject()
                                    .put("severity", issue.severity.name)
                                    .put("title", issue.title)
                                    .put("detail", issue.detail)
                                    .put("suggestion", issue.suggestion)
                                    .put("filePath", issue.filePath)
                                    .put("line", issue.line)
                                    .put("column", issue.column))
                            }
                        })
                        .toString(),
                )
            }
        }
    }

    internal fun loadAndroidBuildRecord(projectId: String): AndroidBuildRecord {
        val file = androidBuildRecordFile(projectId)
        if (!file.isFile) return AndroidBuildRecord()
        return runCatching {
            val json = JSONObject(file.readText())
            val phase = runCatching { AndroidBuildPhase.valueOf(json.optString("phase")) }
                .getOrDefault(AndroidBuildPhase.IDLE)
                .let { if (it in setOf(AndroidBuildPhase.PREPARING, AndroidBuildPhase.BUILDING, AndroidBuildPhase.CANCELLING)) AndroidBuildPhase.CANCELLED else it }
            AndroidBuildRecord(
                phase = phase,
                message = if (phase == AndroidBuildPhase.CANCELLED) str(R.string.an_previous_build_interrupted) else json.optString("message").takeIf(String::isNotBlank),
                log = json.optString("log"),
                startedAtMillis = json.optLong("startedAtMillis").takeIf { it > 0L },
                finishedAtMillis = json.optLong("finishedAtMillis").takeIf { it > 0L },
                apkPath = json.optString("apkPath").takeIf { it.isNotBlank() && File(it).isFile },
                apkSizeBytes = json.optLong("apkSizeBytes").takeIf { it > 0L },
                action = runCatching { AndroidAction.valueOf(json.optString("action")) }.getOrDefault(AndroidAction.NONE),
                recentDurationsMillis = json.optJSONArray("recentDurationsMillis")?.let { array ->
                    (0 until array.length()).mapNotNull { index -> array.optLong(index).takeIf { it > 0L } }
                }.orEmpty(),
                stage = runCatching { AndroidBuildStage.valueOf(json.optString("stage")) }.getOrDefault(
                    when (phase) {
                        AndroidBuildPhase.SUCCEEDED -> AndroidBuildStage.COMPLETE
                        AndroidBuildPhase.FAILED -> AndroidBuildStage.FAILED
                        AndroidBuildPhase.CANCELLED -> AndroidBuildStage.CANCELLED
                        else -> AndroidBuildStage.IDLE
                    },
                ),
                issues = json.optJSONArray("issues")?.let { array ->
                    (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.let { item ->
                            AndroidBuildIssue(
                                severity = runCatching { com.jarves.mh.runtime.AndroidIssueSeverity.valueOf(item.optString("severity")) }
                                    .getOrDefault(com.jarves.mh.runtime.AndroidIssueSeverity.ERROR),
                                title = item.optString("title"),
                                detail = item.optString("detail"),
                                suggestion = item.optString("suggestion").takeIf(String::isNotBlank),
                                filePath = item.optString("filePath").takeIf(String::isNotBlank),
                                line = item.optInt("line").takeIf { it > 0 },
                                column = item.optInt("column").takeIf { it > 0 },
                            )
                        }
                    }
                }.orEmpty(),
            )
        }.getOrDefault(AndroidBuildRecord())
    }

    fun refreshAndroidDashboard() {
        refreshAndroidApkInfo()
        refreshAndroidHealth()
    }

    fun refreshAndroidApkInfo() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val info = runCatching {
                val projectRoot = findAndroidProjectRoot(projectWorkspaceRoot(project)) ?: return@runCatching null
                val apk = findDebugApk(projectRoot) ?: return@runCatching null
                val app = getApplication<Application>()
                @Suppress("DEPRECATION")
                val archive = app.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                val packageName = archive?.packageName ?: AndroidAppInstaller.packageName(app, apk)
                AndroidApkInfo(
                    path = apk.absolutePath,
                    fileName = apk.name,
                    sizeBytes = apk.length(),
                    builtAtMillis = apk.lastModified(),
                    packageName = packageName,
                    versionName = archive?.versionName,
                    versionCode = archive?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
                    },
                    minSdk = archive?.applicationInfo?.minSdkVersion,
                    targetSdk = archive?.applicationInfo?.targetSdkVersion,
                    stale = isDebugApkStale(projectRoot, apk),
                    installed = packageName?.let { AndroidAppInstaller.isInstalled(app, it) } == true,
                    installedMatches = AndroidAppInstaller.installedMatches(app, apk),
                )
            }.getOrNull()
            _state.update { it.copy(androidApkInfo = info) }
        }
    }

    fun shareAndroidApk() {
        val info = _state.value.androidApkInfo ?: return
        val file = File(info.path).takeIf(File::isFile) ?: return
        runCatching {
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            app.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }, str(R.string.an_share_apk)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { error -> _state.update { it.copy(toastMessage = error.message ?: str(R.string.an_could_not_share_apk)) } }
    }

    fun saveAndroidApk(uri: Uri) {
        val info = _state.value.androidApkInfo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val source = File(info.path).takeIf(File::isFile) ?: error(str(R.string.an_apk_unavailable))
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().buffered().use { it.copyTo(output) }
                } ?: error(str(R.string.an_location_unavailable))
            }
            _state.update { it.copy(toastMessage = if (result.isSuccess) str(R.string.an_apk_saved) else result.exceptionOrNull()?.message ?: str(R.string.an_could_not_save_apk)) }
        }
    }

    fun openInstalledAndroidAppDetails() {
        val packageName = _state.value.androidApkInfo?.packageName ?: return
        getApplication<Application>().startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun uninstallAndroidApp() {
        val packageName = _state.value.androidApkInfo?.packageName ?: return
        getApplication<Application>().startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openAndroidIssue(issue: AndroidBuildIssue) {
        val project = _state.value.activeProject ?: return
        val path = issue.filePath ?: return
        val root = projectWorkspaceRoot(project)
        val candidate = File(root, path)
        val resolved = when {
            candidate.isFile -> candidate
            else -> root.walkTopDown().firstOrNull { it.isFile && it.name == File(path).name }
        } ?: return
        val relative = resolved.relativeTo(root).invariantSeparatorsPath
        _state.update { it.copy(openedFile = OpenedFileState(path = relative, targetLine = issue.line, targetColumn = issue.column)) }
        workspaceDelegate.loadOpenedFile(project, relative, issue.line, issue.column)
    }

    fun refreshAndroidHealth() {
        val project = _state.value.activeProject ?: return
        if (_state.value.androidHealthRunning) return
        _state.update { it.copy(androidHealthRunning = true, androidHealthChecks = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val checks = mutableListOf<AndroidHealthCheck>()
            val toolsInstalled = installer.isStackInstalled(DevStack.ANDROID)
            checks += AndroidHealthCheck(
                "tools", str(R.string.an_health_tools_title),
                if (toolsInstalled) str(R.string.an_health_tools_ok) else str(R.string.an_health_tools_missing),
                if (toolsInstalled) AndroidHealthStatus.PASSED else AndroidHealthStatus.FAILED,
                if (toolsInstalled) AndroidHealthFix.NONE else AndroidHealthFix.INSTALL_TOOLS,
            )
            val root = findAndroidProjectRoot(projectWorkspaceRoot(project))
            checks += AndroidHealthCheck(
                "project", str(R.string.an_health_project_title),
                root?.let { str(R.string.an_health_project_ok, it.name) } ?: str(R.string.an_health_project_missing),
                if (root != null) AndroidHealthStatus.PASSED else AndroidHealthStatus.FAILED,
            )
            val launcher = root?.let { File(it, "gradlew").isFile || File(it, "build.gradle").isFile || File(it, "build.gradle.kts").isFile } == true
            checks += AndroidHealthCheck(
                "gradle", str(R.string.an_health_gradle_title),
                if (launcher) str(R.string.an_health_gradle_ok) else str(R.string.an_health_gradle_missing),
                if (launcher) AndroidHealthStatus.PASSED else AndroidHealthStatus.FAILED,
            )
            if (toolsInstalled && root != null) {
                val (toolchainExit, output) = runAndroidGuestCommand(
                    project,
                    "java -version 2>&1 | grep -E '\"17\\.|version 17' >/dev/null && " +
                        "gradle --version >/dev/null && " +
                        "/root/android-sdk/build-tools/35.0.0/aapt2 version >/dev/null && " +
                        "printf POCKETDEV_TOOLCHAIN_OK",
                )
                val ok = toolchainExit == 0 && "POCKETDEV_TOOLCHAIN_OK" in output
                checks += AndroidHealthCheck(
                    "toolchain", str(R.string.an_health_toolchain_title),
                    if (ok) str(R.string.an_health_toolchain_ok) else str(R.string.an_health_toolchain_failed),
                    if (ok) AndroidHealthStatus.PASSED else AndroidHealthStatus.FAILED,
                    if (ok) AndroidHealthFix.NONE else AndroidHealthFix.INSTALL_TOOLS,
                )
                val compileSdk = root.walkTopDown().maxDepth(3).filter { it.isFile && it.name in setOf("build.gradle", "build.gradle.kts") }
                    .mapNotNull { Regex("compileSdk(?:Version)?\\s*(?:=|\\s)\\s*(\\d+)").find(it.readText())?.groupValues?.get(1)?.toIntOrNull() }
                    .maxOrNull()
                val platformAvailable = compileSdk == null || File(installer.installedRuntime().rootfs, "root/android-sdk/platforms/android-$compileSdk/android.jar").isFile
                checks += AndroidHealthCheck(
                    "sdk", str(R.string.an_health_sdk_title),
                    when { compileSdk == null -> str(R.string.an_health_sdk_unknown); platformAvailable -> str(R.string.an_health_sdk_ok, compileSdk); else -> str(R.string.an_health_sdk_missing, compileSdk) },
                    when { compileSdk == null -> AndroidHealthStatus.WARNING; platformAvailable -> AndroidHealthStatus.PASSED; else -> AndroidHealthStatus.FAILED },
                    if (!platformAvailable) AndroidHealthFix.INSTALL_TOOLS else AndroidHealthFix.NONE,
                )
            }
            val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
            val online = network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            checks += AndroidHealthCheck(
                "network", str(R.string.an_health_network_title),
                if (online) str(R.string.an_health_network_ok) else str(R.string.an_health_network_offline),
                if (online) AndroidHealthStatus.PASSED else AndroidHealthStatus.WARNING,
                if (online) AndroidHealthFix.NONE else AndroidHealthFix.OPEN_NETWORK_SETTINGS,
            )
            val freeBytes = StatFs(app.filesDir.absolutePath).availableBytes
            val enoughStorage = freeBytes >= 1_500L * 1024L * 1024L
            checks += AndroidHealthCheck(
                "storage", str(R.string.an_health_storage_title),
                str(R.string.an_health_storage_detail, freeBytes / 1024 / 1024),
                if (enoughStorage) AndroidHealthStatus.PASSED else AndroidHealthStatus.WARNING,
                if (enoughStorage) AndroidHealthFix.NONE else AndroidHealthFix.OPEN_STORAGE_SETTINGS,
            )
            val installAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || app.packageManager.canRequestPackageInstalls()
            checks += AndroidHealthCheck(
                "install", str(R.string.an_health_install_title),
                if (installAllowed) str(R.string.an_health_install_ok) else str(R.string.an_health_install_missing),
                if (installAllowed) AndroidHealthStatus.PASSED else AndroidHealthStatus.FAILED,
                if (installAllowed) AndroidHealthFix.NONE else AndroidHealthFix.OPEN_INSTALL_SETTINGS,
            )
            val apk = root?.let(::findDebugApk)
            checks += AndroidHealthCheck(
                "apk", str(R.string.an_health_apk_title),
                when { apk == null -> str(R.string.an_health_apk_missing); isDebugApkStale(root, apk) -> str(R.string.an_health_apk_stale); else -> str(R.string.an_health_apk_ok) },
                when { apk == null -> AndroidHealthStatus.WARNING; isDebugApkStale(root, apk) -> AndroidHealthStatus.WARNING; else -> AndroidHealthStatus.PASSED },
            )
            _state.update { it.copy(androidHealthRunning = false, androidHealthChecks = checks) }
        }
    }

    fun applyAndroidHealthFix(fix: AndroidHealthFix) = performAndroidHealthFix(fix)
    fun performAndroidHealthFix(fix: AndroidHealthFix) {
        val app = getApplication<Application>()
        when (fix) {
            AndroidHealthFix.INSTALL_TOOLS -> {
                _state.update { current ->
                    current.copy(
                        androidHealthRunning = true,
                        androidHealthChecks = current.androidHealthChecks.map { check ->
                            if (check.fix == AndroidHealthFix.INSTALL_TOOLS) {
                                check.copy(
                                    detail = str(R.string.an_repairing_tools),
                                    status = AndroidHealthStatus.RUNNING,
                                    fix = AndroidHealthFix.NONE,
                                )
                            } else check
                        },
                    )
                }
                agentDelegate.installDevStack(DevStack.ANDROID, refreshAndroidHealthAfter = true)
            }
            AndroidHealthFix.OPEN_INSTALL_SETTINGS -> app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            AndroidHealthFix.OPEN_NETWORK_SETTINGS -> app.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            AndroidHealthFix.OPEN_STORAGE_SETTINGS -> app.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            AndroidHealthFix.CLEAN -> cleanAndroidProject()
            AndroidHealthFix.REFRESH -> refreshAndroidHealth()
            AndroidHealthFix.COPY_LOGCAT_COMMAND, AndroidHealthFix.NONE -> Unit
        }
    }

    private fun runAndroidGuestCommand(project: Project, command: String): Pair<Int, String> {
        return runCatching {
            val runtime = installer.installedRuntime()
            val workspace = projectWorkspaceRoot(project)
            val outputFile = File(getApplication<Application>().cacheDir, "android-health-${System.nanoTime()}.log")
            try {
                val process = installer.process(
                    runtime.proot, runtime.rootfs, workspace, emptyMap(),
                    listOf("/usr/bin/bash", "-lc", command), projectGuestRoot(project), outputFile = outputFile,
                )
                val exit = process.waitFor()
                exit to sanitizeTerminalOutput(outputFile.readTailText(64 * 1024)).trim()
            } finally { outputFile.delete() }
        }.getOrElse { -1 to (it.message ?: "Command failed") }
    }

    private fun hasLogcatAccess(): Boolean {
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) return true
        return runCatching {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val finished = process.waitFor(800, TimeUnit.MILLISECONDS)
            if (!finished) process.destroyForcibly()
            finished && process.exitValue() == 0
        }.getOrDefault(false)
    }

    fun startAndroidLogcat() {
        if (androidLogcatJob?.isActive == true) return
        val info = _state.value.androidApkInfo
        val packageName = info?.packageName
        if (packageName.isNullOrBlank() || info.installed.not()) {
            _state.update { it.copy(androidLogcat = it.androidLogcat.copy(available = false, message = str(R.string.an_install_apk_before_logcat))) }
            return
        }
        if (!hasLogcatAccess()) {
            _state.update { it.copy(androidLogcat = it.androidLogcat.copy(available = false, message = LOGCAT_GRANT_COMMAND, packageName = packageName)) }
            return
        }
        val app = getApplication<Application>()
        val uid = runCatching { app.packageManager.getApplicationInfo(packageName, 0).uid }.getOrNull()
        if (uid == null) {
            _state.update { it.copy(androidLogcat = it.androidLogcat.copy(available = false, message = str(R.string.an_installed_app_not_found))) }
            return
        }
        androidLogcatJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val root = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED
                val since = androidLaunchAtMillis?.let {
                    SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(it))
                } ?: "1"
                val logCommand = "/system/bin/logcat --uid=$uid -v threadtime -T '$since'"
                val process = if (root) ProcessBuilder("su", "-c", logCommand).redirectErrorStream(true).start()
                    else ProcessBuilder("/system/bin/logcat", "--uid=$uid", "-v", "threadtime", "-T", since).redirectErrorStream(true).start()
                androidLogcatProcess = process
                _state.update { it.copy(androidLogcat = it.androidLogcat.copy(available = true, running = true, paused = false, message = null, packageName = packageName)) }
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { raw ->
                        if (!_state.value.androidLogcat.paused) appendAndroidLogLine(raw)
                    }
                }
            }.onFailure { error ->
                _state.update { it.copy(androidLogcat = it.androidLogcat.copy(running = false, message = error.message ?: str(R.string.an_logcat_stopped))) }
            }
            androidLogcatProcess = null
            _state.update { it.copy(androidLogcat = it.androidLogcat.copy(running = false)) }
        }
    }

    private fun appendAndroidLogLine(raw: String) {
        val levelChar = Regex("\\s([VDIWEF])\\s").find(raw)?.groupValues?.getOrNull(1)
        val level = when (levelChar) {
            "D" -> AndroidLogLevel.DEBUG; "I" -> AndroidLogLevel.INFO; "W" -> AndroidLogLevel.WARNING
            "E" -> AndroidLogLevel.ERROR; "F" -> AndroidLogLevel.FATAL; else -> AndroidLogLevel.VERBOSE
        }
        val location = Regex("([A-Za-z0-9_]+\\.(?:kt|java)):(\\d+)").find(raw)
        _state.update { current ->
            val line = AndroidLogLine(
                id = System.nanoTime(), text = raw, level = level,
                filePath = location?.groupValues?.getOrNull(1), line = location?.groupValues?.getOrNull(2)?.toIntOrNull(),
            )
            current.copy(androidLogcat = current.androidLogcat.copy(lines = (current.androidLogcat.lines + line).takeLast(MAX_LOGCAT_LINES)))
        }
    }

    fun stopAndroidLogcat() {
        androidLogcatProcess?.destroy()
        androidLogcatJob?.cancel()
        androidLogcatJob = null
        _state.update { it.copy(androidLogcat = it.androidLogcat.copy(running = false)) }
    }

    fun toggleAndroidLogcatPause() = _state.update { it.copy(androidLogcat = it.androidLogcat.copy(paused = !it.androidLogcat.paused)) }
    fun clearAndroidLogcat() = _state.update { it.copy(androidLogcat = it.androidLogcat.copy(lines = emptyList())) }
    fun setAndroidLogcatQuery(value: String) = _state.update { it.copy(androidLogcat = it.androidLogcat.copy(query = value)) }
    fun setAndroidLogcatLevel(value: AndroidLogLevel) = _state.update { it.copy(androidLogcat = it.androidLogcat.copy(minimumLevel = value)) }

    fun openAndroidLogLine(line: AndroidLogLine) {
        val fileName = line.filePath ?: return
        openAndroidIssue(AndroidBuildIssue(title = "Logcat", detail = line.text, filePath = fileName, line = line.line))
    }



}
