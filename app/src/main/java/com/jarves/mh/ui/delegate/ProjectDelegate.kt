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

class ProjectDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    fun integrityResultFor(projectId: String): IntegrityScheduler.IntegrityResult? =
        integrityScheduler.lastResult(projectId)

    /** Returns all integrity results. */
    fun allIntegrityResults(): List<IntegrityScheduler.IntegrityResult> =
        integrityScheduler.allResults()

    /** Returns the configured check interval in minutes. */
    fun integrityCheckIntervalMinutes(): Long = integrityScheduler.getCheckIntervalMinutes()

    /** Sets the check interval in minutes. */
    fun setIntegrityCheckIntervalMinutes(minutes: Long) {
        integrityScheduler.setCheckInterval(minutes)
        _state.update { it.copy(integrityCheckIntervalMinutes = minutes) }
    }

    /** Runs an integrity check for a single project now. */
    suspend fun runIntegrityCheck(projectId: String, projectName: String, workspace: File) {
        _state.update { it.copy(integrityChecking = true) }
        try {
            val result = integrityScheduler.checkNow(projectId, projectName, workspace)
            _state.update { current ->
                current.copy(
                    integrityChecking = false,
                    integrityResults = current.integrityResults + (projectId to result),
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(integrityChecking = false) }
        }
    }

    /** Runs integrity checks for all projects with workspaces. */
    suspend fun runIntegrityCheckAll() {
        _state.update { it.copy(integrityChecking = true) }
        try {
            val projects = _state.value.projects.mapNotNull { project ->
                val workspace = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
                if (workspace.isDirectory) Triple(project.id, project.name, workspace) else null
            }
            val results = integrityScheduler.checkAll(projects)
            val resultMap = results.associate { it.projectId to it }
            _state.update { current ->
                current.copy(
                    integrityChecking = false,
                    integrityResults = current.integrityResults + resultMap,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(integrityChecking = false) }
        }
    }

    /** Resets the baseline snapshot for a project (next check will be a first-check). */
    fun resetIntegrityBaseline(projectId: String) {
        integrityScheduler.resetBaseline(projectId)
        _state.update { current ->
            current.copy(integrityResults = current.integrityResults - projectId)
        }
    }



    override fun openProject(project: Project) {
        val current = _state.value
        if (current.activeProject?.id == project.id) {
            _state.update {
                it.copy(
                    workspaceVisible = true,
                    readOnlyProject = null,
                    readOnlyProjectChats = emptyList(),
                    readOnlyChatId = null,
                    readOnlyMessages = emptyList(),
                )
            }
            return
        }
        if (current.isRunning || current.projectTerminalRunning || current.androidBuildRunning) {
            val chats = preferences.loadProjectChats(project.id).ifEmpty {
                listOf(ProjectChat(title = str(R.string.vm_main_chat)))
            }
            val chat = chats.first()
            _state.update {
                it.copy(
                    readOnlyProject = project,
                    readOnlyProjectChats = chats,
                    readOnlyChatId = chat.id,
                    readOnlyMessages = preferences.loadMessages(project.id, chat.id),
                )
            }
            return
        }
        agentDelegate.configureBridgeRoots(project.id, project.rootPath)
        val terminal = terminalDelegate.loadProjectTerminal(project)
        val suggestedRoot = if (project.rootPath.isBlank()) detectNestedProjectRoot(project) else null
        val chats = preferences.loadProjectChats(project.id).ifEmpty {
            listOf(ProjectChat(title = str(R.string.vm_main_chat))).also { preferences.saveProjectChats(project.id, it) }
        }
        val activeChat = chats.first()
        val saved = preferences.loadMessages(project.id, activeChat.id)
        val msgs = saved.ifEmpty { listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")) }
        val androidBuild = androidDelegate.loadAndroidBuildRecord(project.id)
        _state.update {
            it.copy(
                activeProject = project,
                workspaceVisible = true,
                readOnlyProject = null,
                readOnlyProjectChats = emptyList(),
                readOnlyChatId = null,
                readOnlyMessages = emptyList(),
                projectChats = chats,
                activeChatId = activeChat.id,
                messages = msgs,
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                projectTerminalLines = terminal.lines,
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = terminal.cwd,
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = suggestedRoot,
                previewReady = false,
                previewUrl = null,
                pendingAttachments = emptyList(),
                androidBuildRunning = false,
                androidBuildPhase = androidBuild.phase,
                androidBuildAction = androidBuild.action,
                androidBuildStage = androidBuild.stage,
                androidBuildIssues = androidBuild.issues,
                androidBuildMessage = androidBuild.message,
                androidBuildLog = androidBuild.log,
                androidBuildStartedAtMillis = androidBuild.startedAtMillis,
                androidBuildFinishedAtMillis = androidBuild.finishedAtMillis,
                androidBuildApkPath = androidBuild.apkPath,
                androidBuildApkSizeBytes = androidBuild.apkSizeBytes,
                androidBuildRecentDurationsMillis = androidBuild.recentDurationsMillis,
            )
        }
        refreshProjectFiles()
        viewModelScope.launch {
            val pending = activeRuntime().loadPendingChanges(project.id)
            if (_state.value.activeProject?.id == project.id) _state.update { it.copy(changes = pending) }
        }
    }

    fun closeProject() {
        val active = _state.value.activeProject
        persistMessages()
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) {
            _state.update {
                it.copy(
                    workspaceVisible = false,
                    toastMessage = when {
                        it.isRunning -> str(R.string.vm_task_continues_background)
                        it.androidBuildRunning -> str(R.string.an_build_continues_background)
                        else -> str(R.string.vm_terminal_continues_background)
                    },
                )
            }
            return
        }

        if (active != null) {
            val chats = preferences.loadProjectChats(active.id)
            val userMessages = chats.sumOf { preferences.loadMessages(active.id, it.id).count { m -> m.fromUser } }
            val workspaceDir = File(getApplication<Application>().filesDir, "workspaces/${active.id}")
            val userFiles = if (workspaceDir.isDirectory) {
                workspaceDir.walkTopDown().filter { file ->
                    file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json"
                }.count()
            } else 0

            if (userMessages == 0 && userFiles == 0 && !_state.value.isRunning && !_state.value.projectTerminalRunning) {
                // Unused empty project; delete immediately so it does not clutter the project list.
                _state.update { current -> current.copy(projects = current.projects.filterNot { it.id == active.id }) }
                preferences.saveProjects(_state.value.projects)
                viewModelScope.launch(Dispatchers.IO) {
                    workspaceDir.deleteRecursively()
                    terminalDelegate.terminalHistoryFile(active.id).delete()
                    preferences.deleteProjectChats(active.id)
                }
            }
        }

        _state.update {
            it.copy(
                activeProject = null,
                workspaceVisible = false,
                projectChats = emptyList(),
                activeChatId = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = false,
                isRunning = false,
                activeSessionId = null,
                pendingApproval = null,
                projectTerminalLines = emptyList(),
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = "/workspace",
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = null,
                previewReady = false,
                previewUrl = null,
                pendingAttachments = emptyList(),
            )
        }
    }

    fun closeReadOnlyProject() {
        _state.update {
            it.copy(
                readOnlyProject = null,
                readOnlyProjectChats = emptyList(),
                readOnlyChatId = null,
                readOnlyMessages = emptyList(),
            )
        }
    }

    fun switchReadOnlyChat(chatId: String) {
        val project = _state.value.readOnlyProject ?: return
        if (_state.value.readOnlyProjectChats.none { it.id == chatId }) return
        _state.update {
            it.copy(
                readOnlyChatId = chatId,
                readOnlyMessages = preferences.loadMessages(project.id, chatId),
            )
        }
    }

    fun activateReadOnlyProject() {
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        val project = _state.value.readOnlyProject ?: return
        closeReadOnlyProject()
        openProject(project)
    }

    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    fun createProject(name: String) {
        if (name.isBlank()) return
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) {
            _state.update { it.copy(toastMessage = str(R.string.vm_stop_before_new_project)) }
            return
        }
        val baseSlug = projectSlug(name)
        val usedSlugs = _state.value.projects.mapTo(mutableSetOf()) { it.slug }
        val slug = generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) baseSlug else "$baseSlug-$number" }
            .first { it !in usedSlugs }
        val project = Project(
            name = name.trim(),
            description = str(R.string.vm_starter_web_project),
            language = "TypeScript",
            slug = slug,
        )
        agentDelegate.configureBridgeRoots(project.id, project.rootPath)
        val guestRoot = projectGuestRoot(project)
        _state.update {
            it.copy(
                projects = listOf(project) + it.projects,
                activeProject = project,
                workspaceVisible = true,
                messages = listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")),
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                projectTerminalLines = emptyList(),
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = guestRoot,
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = null,
                previewReady = false,
                previewUrl = null,
            )
        }
        preferences.saveProjects(_state.value.projects)
        val wsDir = File(getApplication<Application>().filesDir, "workspaces/${project.id}").apply { mkdirs() }
        ProjectStorageManager.syncWorkspaceToPublic(getApplication(), project.name, wsDir)
        val firstChat = ProjectChat(title = str(R.string.vm_new_chat))
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projectChats = listOf(firstChat), activeChatId = firstChat.id) }
        refreshProjectFiles()
    }

    fun openProjectInFileManager(projectName: String) {
        val active = _state.value.activeProject ?: _state.value.projects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }
        if (active != null) {
            val workspace = File(getApplication<Application>().filesDir, "workspaces/${active.id}")
            if (workspace.exists()) {
                ProjectStorageManager.syncWorkspaceToPublic(getApplication(), projectName, workspace)
            }
        }
        ProjectStorageManager.openInFileManager(getApplication(), projectName)
    }

    fun scaffoldVoiceArchitectProject(
        name: String,
        template: AndroidTemplate?,
        prd: String,
        trd: String,
        architecture: String,
        decisions: String = "",
    ) {
        // Persist formal plan artifacts to dedicated Plans folder
        ProjectStorageManager.writePlanArtifacts(
            context = getApplication(),
            planId = name,
            prdContent = prd,
            trdContent = trd,
            architectureContent = architecture,
            decisionsContent = decisions,
        )

        if (template != null) {
            createAndroidProject(name, template)
        } else {
            createProject(name)
        }
        val active = _state.value.activeProject
        if (active != null) {
            val workspace = File(getApplication<Application>().filesDir, "workspaces/${active.id}")
            ProjectStorageManager.writeProjectArtifacts(
                context = getApplication(),
                projectName = active.name,
                internalWorkspace = workspace,
                prdContent = prd,
                trdContent = trd,
                architectureContent = architecture,
                readmeContent = "# ${active.name}\n\nAutomated blueprint generated by MindSparQ Live Voice Studio.\n\nSee `PRD.md`, `TRD.md`, `ARCHITECTURE.md`, and `DECISIONS.md`."
            )
            if (decisions.isNotBlank()) {
                File(workspace, "DECISIONS.md").writeText(decisions)
                val publicDir = ProjectStorageManager.getProjectPublicDir(getApplication(), active.name)
                File(publicDir, "DECISIONS.md").writeText(decisions)
            }
            refreshProjectFiles()
        }
    }

    fun createAndroidProject(name: String, template: AndroidTemplate) {
        if (name.isBlank()) return
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) {
            _state.update { it.copy(toastMessage = str(R.string.an_stop_before_new_project)) }
            return
        }
        val cleanName = name.replace(Regex("\\s+"), " ").trim().take(60)
        val baseSlug = projectSlug(cleanName)
        val usedSlugs = _state.value.projects.mapTo(mutableSetOf()) { it.slug }
        val slug = generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) baseSlug else "$baseSlug-$number" }
            .first { it !in usedSlugs }
        val project = Project(
            name = cleanName,
            description = if (template == AndroidTemplate.COMPOSE) "Android · Jetpack Compose" else "Android · Kotlin + XML",
            language = "Kotlin",
            slug = slug,
            type = ProjectType.ANDROID,
            androidTemplate = template,
        )
        val workspace = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
        runCatching { AndroidProjectTemplateGenerator.generate(workspace, cleanName, template) }
            .onFailure { error ->
                _state.update { it.copy(toastMessage = str(R.string.an_could_not_create_android_project)) }
                return
            }
        ProjectStorageManager.syncWorkspaceToPublic(getApplication(), project.name, workspace)
        val firstChat = ProjectChat(title = str(R.string.vm_new_chat))
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projects = listOf(project) + it.projects) }
        preferences.saveProjects(_state.value.projects)
        openProject(project)
        if (!installer.isStackInstalled(DevStack.ANDROID)) {
            _state.update { it.copy(toastMessage = str(R.string.an_android_project_created_install_tools)) }
        }
    }

    fun createTemplatedProject(name: String, template: SupportedProjectTemplate) {
        if (template == SupportedProjectTemplate.COMPOSE) {
            createAndroidProject(name, AndroidTemplate.COMPOSE)
            return
        }
        if (template == SupportedProjectTemplate.XML) {
            createAndroidProject(name, AndroidTemplate.XML)
            return
        }
        if (name.isBlank()) return
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) {
            _state.update { it.copy(toastMessage = str(R.string.vm_stop_before_new_project)) }
            return
        }
        val cleanName = name.replace(Regex("\\s+"), " ").trim().take(60)
        val baseSlug = projectSlug(cleanName)
        val usedSlugs = _state.value.projects.mapTo(mutableSetOf()) { it.slug }
        val slug = generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) baseSlug else "$baseSlug-$number" }
            .first { it !in usedSlugs }
        val project = Project(
            name = cleanName,
            description = "${template.category} · ${template.title}",
            language = template.defaultLanguage,
            slug = slug,
            type = ProjectType.GENERAL,
        )
        agentDelegate.configureBridgeRoots(project.id, project.rootPath)
        val guestRoot = projectGuestRoot(project)
        _state.update {
            it.copy(
                projects = listOf(project) + it.projects,
                activeProject = project,
                workspaceVisible = true,
                messages = listOf(ChatMessage(fromUser = false, text = "⚡ Project created with ${template.title} template! How can I assist you with implementation?")),
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                projectTerminalLines = emptyList(),
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = guestRoot,
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = null,
                previewReady = false,
                previewUrl = null,
            )
        }
        preferences.saveProjects(_state.value.projects)
        val wsDir = File(getApplication<Application>().filesDir, "workspaces/${project.id}").apply { mkdirs() }
        MultiPlatformProjectTemplateGenerator.generate(wsDir, cleanName, template)
        ProjectStorageManager.syncWorkspaceToPublic(getApplication(), project.name, wsDir)
        val firstChat = ProjectChat(title = str(R.string.vm_new_chat))
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projectChats = listOf(firstChat), activeChatId = firstChat.id) }
        refreshProjectFiles()
    }

    fun createQuickProject() {
        if (_state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) {
            _state.update { it.copy(toastMessage = str(R.string.vm_stop_before_new_project)) }
            return
        }
        val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
        val project = Project(
            name = identity.displayName,
            description = "Quick project workspace",
            language = str(R.string.vm_language_general),
            slug = identity.slug,
            kind = ProjectKind.QUICK_PROJECT,
        )
        val firstChat = ProjectChat(title = str(R.string.vm_new_chat))
        File(getApplication<Application>().filesDir, "workspaces/${project.id}").mkdirs()
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projects = listOf(project) + it.projects) }
        preferences.saveProjects(_state.value.projects)
        openProject(project)
    }

    fun importZipProject(uri: Uri) {
        if (_state.value.projectImporting || _state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        _state.update { it.copy(projectImporting = true, projectImportMessage = str(R.string.vm_reading_archive)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { extractImportedProject(uri) } }
            result.onSuccess { imported ->
                val project = imported.project
                val firstChat = ProjectChat(title = str(R.string.vm_new_chat))
                preferences.saveProjectChats(project.id, listOf(firstChat))
                _state.update { current ->
                    current.copy(
                        projects = listOf(project) + current.projects,
                        projectImporting = false,
                        projectImportMessage = null,
                        toastMessage = str(R.string.vm_project_imported, project.name),
                    )
                }
                preferences.saveProjects(_state.value.projects)
                openProject(project)
                _state.update { it.copy(pendingAttachments = listOf(imported.sourceAttachment)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        projectImporting = false,
                        projectImportMessage = null,
                        toastMessage = str(R.string.vm_import_failed, error.message?.take(180) ?: str(R.string.vm_invalid_zip)),
                    )
                }
            }
        }
    }

    private fun extractImportedProject(uri: Uri): ImportedZipProject {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        var archiveName = "Imported project.zip"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { index ->
                    archiveName = cursor.getString(index) ?: archiveName
                }
            }
        }
        val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
        val projectId = UUID.randomUUID().toString()
        val destination = File(app.filesDir, "workspaces/$projectId")
        destination.mkdirs()
        val destinationPath = destination.canonicalFile.toPath()
        val availableLimit = (destination.usableSpace * 8L / 10L).coerceAtMost(MAX_IMPORTED_PROJECT_BYTES)
        var extractedBytes = 0L
        var entries = 0
        try {
            val source = resolver.openInputStream(uri) ?: error(str(R.string.vm_zip_open_failed))
            source.buffered().use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        require(entries <= MAX_IMPORTED_ZIP_ENTRIES) { str(R.string.vm_zip_too_many_files) }
                        val entryName = entry.name.replace('\\', '/').trimStart('/')
                        require(entryName.isNotBlank() && '\u0000' !in entryName) { str(R.string.vm_zip_invalid_path) }
                        if (entryName.startsWith("__MACOSX/") || entryName.endsWith("/.DS_Store") || entryName == ".DS_Store") {
                            zip.closeEntry()
                            continue
                        }
                        val target = File(destination, entryName).canonicalFile
                        require(target.toPath().startsWith(destinationPath)) { str(R.string.vm_zip_unsafe_path) }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().buffered().use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    extractedBytes += count
                                    require(extractedBytes <= availableLimit) { str(R.string.vm_zip_too_large) }
                                    output.write(buffer, 0, count)
                                }
                            }
                            if (entry.time > 0) target.setLastModified(entry.time)
                        }
                        zip.closeEntry()
                    }
                }
            }
            require(entries > 0 && destination.walkTopDown().any { it.isFile }) { str(R.string.vm_zip_no_files) }
            val preliminary = Project(
                id = projectId,
                name = identity.displayName,
                description = "Imported project workspace",
                language = str(R.string.vm_language_general),
                slug = identity.slug,
                kind = ProjectKind.QUICK_PROJECT,
            )
            val nestedRoot = detectNestedProjectRoot(preliminary)
            val projectRoot = nestedRoot?.let { File(destination, it) } ?: destination
            val metadata = detectImportedProjectMetadata(projectRoot)
            val safeArchiveName = chatDelegate.sanitizeAttachmentName(archiveName).let { name ->
                if (name.endsWith(".zip", ignoreCase = true)) name else "$name.zip"
            }
            val archiveFolder = File(projectRoot, ".pocketdev/imports").apply { mkdirs() }
            val archivedSource = File(archiveFolder, safeArchiveName)
            var sourceBytes = 0L
            resolver.openInputStream(uri)?.buffered()?.use { input ->
                archivedSource.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sourceBytes += count
                        require(extractedBytes + sourceBytes <= availableLimit) { str(R.string.vm_import_too_large) }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error(str(R.string.vm_zip_preserve_failed))
            val project = preliminary.copy(
                description = metadata.first,
                language = metadata.second,
                rootPath = nestedRoot.orEmpty(),
            )
            return ImportedZipProject(
                project = project,
                sourceAttachment = ChatAttachment(
                    displayName = archiveName.take(120),
                    relativePath = archivedSource.relativeTo(projectRoot).invariantSeparatorsPath,
                    mimeType = "application/zip",
                    sizeBytes = sourceBytes,
                ),
            )
        } catch (error: Throwable) {
            destination.deleteRecursively()
            throw error
        }
    }

    internal fun detectImportedProjectMetadata(root: File): Pair<String, String> {
        val names = root.walkTopDown().maxDepth(3).filter(File::isFile).map { it.name.lowercase() }.toSet()
        return when {
            names.any { it == "settings.gradle.kts" || it == "build.gradle.kts" } -> "Imported Gradle project" to "Kotlin"
            names.any { it == "settings.gradle" || it == "build.gradle" } -> "Imported Gradle project" to "Java"
            "package.json" in names && names.any { it == "tsconfig.json" || it.endsWith(".ts") || it.endsWith(".tsx") } -> "Imported web project" to "TypeScript"
            "package.json" in names -> "Imported web project" to "JavaScript"
            names.any { it == "pyproject.toml" || it == "requirements.txt" || it.endsWith(".py") } -> "Imported Python project" to "Python"
            names.any { it == "cargo.toml" || it.endsWith(".rs") } -> "Imported Rust project" to "Rust"
            names.any { it == "go.mod" || it.endsWith(".go") } -> "Imported Go project" to "Go"
            else -> "Imported ZIP project" to str(R.string.vm_language_general)
        }
    }

    // ------------------------------------------------------------------------
    // Git & GitHub Operations (Delegated to ProjectGitDelegate)
    // ------------------------------------------------------------------------

    fun clonePublicGitRepository(url: String) = gitDelegate.clonePublicGitRepository(url)

    fun cloneGitHubRepository(repository: GitHubRepository) = gitDelegate.cloneGitHubRepository(repository)

    fun startGitHubLogin() = gitDelegate.startGitHubLogin()

    fun connectGitHubWithToken(token: String) = gitDelegate.connectGitHubWithToken(token)

    fun generateNewGitHubCode() = gitDelegate.generateNewGitHubCode()

    fun refreshGitHubRepositories() = gitDelegate.refreshGitHubRepositories()

    fun disconnectGitHub() = gitDelegate.disconnectGitHub()

    fun pushProjectToGitHub(
        project: Project,
        repoName: String,
        isPrivate: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> },
    ) = gitDelegate.pushProjectToGitHub(project, repoName, isPrivate, onResult)

    internal suspend fun refreshGitHubConnection() = gitDelegate.refreshGitHubConnection()

    fun renameProject(projectId: String, newName: String) {
        val clean = newName.replace(Regex("\\s+"), " ").trim().take(60)
        if (clean.isBlank()) return
        _state.update { current ->
            val projects = current.projects.map { project ->
                if (project.id == projectId) project.copy(name = clean) else project
            }
            val active = current.activeProject?.let { project ->
                if (project.id == projectId) project.copy(name = clean) else project
            }
            current.copy(projects = projects, activeProject = active)
        }
        preferences.saveProjects(_state.value.projects)
    }

    fun deleteProject(projectId: String) {
        val project = _state.value.projects.firstOrNull { it.id == projectId } ?: return
        if (_state.value.activeProject?.id == projectId || _state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        _state.update { current -> current.copy(projects = current.projects.filterNot { it.id == projectId }) }
        preferences.saveProjects(_state.value.projects)
        workspaceDelegate.folderMetadataCache.keys.removeAll { it.startsWith("$projectId:") }
        viewModelScope.launch(Dispatchers.IO) {
            val filesDir = getApplication<Application>().filesDir
            File(filesDir, "workspaces/${project.id}").deleteRecursively()
            terminalDelegate.terminalHistoryFile(project.id).delete()
            androidDelegate.androidBuildRecordFile(project.id).delete()
            preferences.deleteProjectChats(project.id)
        }
    }

    internal fun detectNestedProjectRoot(project: Project): String? {
        val base = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
        if (!base.isDirectory) return null
        val visible = base.listFiles().orEmpty().filterNot { file ->
            file.name == ".claude" || file.name == ".claude.json"
        }
        val onlyDirectory = visible.singleOrNull()?.takeIf(File::isDirectory) ?: return null
        val containsProjectFiles = onlyDirectory.walkTopDown()
            .maxDepth(2)
            .any { it.isFile && it.name !in setOf(".DS_Store", ".claude.json") }
        return onlyDirectory.name.takeIf { containsProjectFiles && !it.contains("..") }
    }

    fun useSuggestedProjectRoot() {
        val current = _state.value
        val project = current.activeProject ?: return
        val root = current.suggestedProjectRoot ?: return
        if (current.isRunning || current.projectTerminalRunning) return
        val updated = project.copy(rootPath = root)
        agentDelegate.configureBridgeRoots(updated.id, updated.rootPath)
        val projects = current.projects.map { if (it.id == updated.id) updated else it }
        val guestRoot = projectGuestRoot(updated)
        preferences.saveProjects(projects)
        terminalDelegate.saveProjectTerminal(updated.id, guestRoot, current.projectTerminalLines)
        _state.update {
            it.copy(
                projects = projects,
                activeProject = updated,
                suggestedProjectRoot = null,
                projectTerminalCwd = guestRoot,
                changes = emptyList(),
                toastMessage = str(R.string.vm_project_root_set, root),
            )
        }
        refreshProjectFiles()
    }

    fun exportActiveProject(uri: Uri, selection: ExportSelection) {
        val current = _state.value
        val project = current.activeProject ?: return
        if (current.isRunning || current.projectTerminalRunning || current.androidBuildRunning ||
            current.projectExportRunning || current.openedFile?.saving == true
        ) {
            _state.update { it.copy(toastMessage = str(R.string.export_busy)) }
            return
        }
        _state.update { it.copy(projectExportRunning = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val root = projectWorkspaceRoot(project)
                    val rootPath = root.canonicalFile.toPath()
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error(str(R.string.vm_export_location_failed))
                    output.buffered().use { stream ->
                        ZipOutputStream(stream).use { zip ->
                            zip.putNextEntry(ZipEntry("${project.slug}/"))
                            zip.closeEntry()
                            root.walkTopDown()
                                .onEnter { directory ->
                                    if (directory == root) {
                                        true
                                    } else {
                                        val relative = directory.relativeTo(root).invariantSeparatorsPath
                                        selection.canContainIncluded(relative) &&
                                            !workspaceDelegate.isClaudeRuntimeMetadata(relative) &&
                                            !Files.isSymbolicLink(directory.toPath()) &&
                                            runCatching { directory.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
                                    }
                                }
                                .drop(1)
                                .filter { file ->
                                    !Files.isSymbolicLink(file.toPath()) &&
                                        runCatching { file.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false) &&
                                        !workspaceDelegate.isClaudeRuntimeMetadata(file.relativeTo(root).invariantSeparatorsPath) &&
                                        if (file.isDirectory) {
                                            selection.canContainIncluded(file.relativeTo(root).invariantSeparatorsPath)
                                        } else {
                                            selection.includes(file.relativeTo(root).invariantSeparatorsPath)
                                        }
                                }
                                .forEach { file ->
                                    val relative = file.relativeTo(root).invariantSeparatorsPath
                                    val entryName = "${project.slug}/$relative" + if (file.isDirectory) "/" else ""
                                    zip.putNextEntry(ZipEntry(entryName).apply { time = file.lastModified() })
                                    if (file.isFile) file.inputStream().buffered().use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                        }
                    }
                }
            }
            _state.update {
                it.copy(
                    projectExportRunning = false,
                    projectExportSucceededAtMillis = result.getOrNull()?.let { System.currentTimeMillis() },
                    toastMessage = result.fold(
                        onSuccess = { str(R.string.export_success, "${project.slug}.zip") },
                        onFailure = { error -> str(R.string.export_failed, error.message ?: str(R.string.unknown_error)) },
                    ),
                )
            }
        }
    }


}
