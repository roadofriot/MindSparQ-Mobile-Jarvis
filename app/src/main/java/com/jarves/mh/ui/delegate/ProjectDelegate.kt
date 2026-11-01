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

    @Volatile internal var githubAuthProcess: Process? = null
    internal var githubAuthJob: Job? = null
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

    private fun detectImportedProjectMetadata(root: File): Pair<String, String> {
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

    fun clonePublicGitRepository(url: String) {
        cloneGitRepository(url = url, repositoryName = null, branch = null, useGitHubCli = false)
    }

    fun cloneGitHubRepository(repository: GitHubRepository) {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubMessage = str(R.string.vm_github_connect_again)) }
            return
        }
        cloneGitRepository(repository.cloneUrl, repository.fullName, repository.defaultBranch, useGitHubCli = true)
    }

    private fun cloneGitRepository(url: String, repositoryName: String?, branch: String?, useGitHubCli: Boolean) {
        if (_state.value.gitCloneRunning || _state.value.projectImporting || _state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        val normalized = runCatching { validateGitUrl(url) }.getOrElse { error ->
            _state.update { it.copy(toastMessage = error.message ?: str(R.string.vm_invalid_git_url)) }
            return
        }
        _state.update { it.copy(gitCloneRunning = true, gitCloneMessage = str(R.string.vm_connecting_git)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
                    val projectId = UUID.randomUUID().toString()
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/$projectId").apply { mkdirs() }
                    val output = File(getApplication<Application>().cacheDir, "git-clone-${System.nanoTime()}.log")
                    try {
                        val environment = mutableMapOf(
                            "GIT_TERMINAL_PROMPT" to "0",
                            "GIT_LFS_SKIP_SMUDGE" to "1",
                            "GH_PROMPT_DISABLED" to "1",
                            "GH_NO_UPDATE_NOTIFIER" to "1",
                        )
                        val installed = installer.installedRuntime()
                        val command = if (useGitHubCli && repositoryName != null) {
                            buildList {
                                addAll(listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH, "repo", "clone", repositoryName, ".", "--", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                            }
                        } else {
                            buildList {
                                addAll(listOf("git", "clone", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                                add(normalized)
                                add(".")
                            }
                        }
                        _state.update { it.copy(gitCloneMessage = str(R.string.vm_cloning_named, repositoryName ?: normalized.substringAfterLast('/').removeSuffix(".git"))) }
                        val process = installer.process(
                            installed.proot,
                            installed.rootfs,
                            workspace,
                            environment,
                            command,
                            guestWorkspacePath = "/workspace/${identity.slug}",
                            outputFile = output,
                        )
                        val exit = process.waitFor()
                        val details = output.readTailText(MAX_PROCESS_OUTPUT_BYTES).trim()
                        check(exit == 0) { details.takeLast(600).ifBlank { str(R.string.vm_git_clone_failed_exit, exit) } }
                        val metadata = detectImportedProjectMetadata(workspace)
                        Project(
                            id = projectId,
                            name = identity.displayName,
                            description = repositoryName?.let { "GitHub · $it" } ?: "Imported Git repository",
                            language = metadata.second,
                            slug = identity.slug,
                            kind = ProjectKind.QUICK_PROJECT,
                        )
                    } catch (error: Throwable) {
                        workspace.deleteRecursively()
                        throw error
                    } finally {
                        output.delete()
                    }
                }
            }
            result.onSuccess { project ->
                val chat = ProjectChat(title = str(R.string.vm_new_chat))
                preferences.saveProjectChats(project.id, listOf(chat))
                _state.update { current -> current.copy(projects = listOf(project) + current.projects) }
                preferences.saveProjects(_state.value.projects)
                openProject(project)
                _state.update { it.copy(gitCloneRunning = false, gitCloneMessage = null, toastMessage = str(R.string.vm_repository_cloned)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        gitCloneRunning = false,
                        gitCloneMessage = null,
                        toastMessage = str(R.string.vm_clone_failed, error.message?.lineSequence()?.lastOrNull()?.take(180) ?: str(R.string.vm_unknown_error)),
                    )
                }
            }
        }
    }

    private fun validateGitUrl(value: String): String {
        val clean = value.trim()
        val uri = URI(clean)
        require(uri.scheme.equals("https", ignoreCase = true)) { str(R.string.vm_git_https_only) }
        require(uri.userInfo == null && uri.fragment == null && uri.host?.isNotBlank() == true) { str(R.string.vm_git_no_credentials) }
        require(uri.host != "localhost" && uri.host != "127.0.0.1" && uri.host != "::1") { str(R.string.vm_git_local_unsupported) }
        require(uri.path.count { it == '/' } >= 2) { str(R.string.vm_git_url_not_repo) }
        return uri.toASCIIString()
    }

    fun startGitHubLogin() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING || _state.value.githubAuthStatus == GitHubAuthStatus.AWAITING_USER) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Connecting to GitHub...") }
        githubAuthJob = viewModelScope.launch {
            try {
                val client = GitHubClient(getApplication())
                val customClientId = preferences.githubClientId.takeIf(String::isNotBlank)
                val deviceCode = withContext(Dispatchers.IO) {
                    if (customClientId != null) client.requestDeviceCode(customClientId) else client.requestDeviceCode()
                }

                // Immediately update state and copy code to clipboard
                withContext(Dispatchers.Main) {
                    val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("GitHub Code", deviceCode.userCode))
                    // Build auto-fill URL: GitHub supports ?user_code=XXXX-XXXX to pre-fill the code field
                    val autoFillUrl = buildString {
                        append(deviceCode.verificationUri.trimEnd('/'))
                        if (!deviceCode.verificationUri.contains('?')) append("?user_code=") else append("&user_code=")
                        append(deviceCode.userCode)
                    }
                    _state.update {
                        it.copy(
                            githubAuthStatus = GitHubAuthStatus.AWAITING_USER,
                            githubUserCode = deviceCode.userCode,
                            githubVerificationUri = autoFillUrl,
                            githubMessage = "Code ${deviceCode.userCode} is ready. Tap the button below — GitHub will open with your code pre-filled. Just press Authorize.",
                            toastMessage = "GitHub Code: ${deviceCode.userCode} — Auto-filling on GitHub!",
                        )
                    }
                    // Open GitHub with code pre-filled — user just needs to press Authorize
                    openExternalUrl(autoFillUrl)
                }

                // Poll for authorization completion in background
                val intervalMs = (deviceCode.intervalSeconds.coerceAtLeast(5L)) * 1000L
                val startTime = System.currentTimeMillis()
                val timeoutMs = deviceCode.expiresInSeconds * 1000L
                var token: String? = null

                withContext(Dispatchers.IO) {
                    while (System.currentTimeMillis() - startTime < timeoutMs) {
                        delay(intervalMs)
                        when (val poll = client.pollDeviceToken(clientId = customClientId ?: GitHubClient.DEFAULT_CLIENT_ID, deviceCode = deviceCode.deviceCode)) {
                            is GitHubTokenPoll.Success -> {
                                token = poll.accessToken
                                break
                            }
                            is GitHubTokenPoll.Pending -> {
                                // Continue polling
                            }
                            is GitHubTokenPoll.Failure -> {
                                error(poll.message)
                            }
                        }
                    }
                }

                val finalToken = token ?: error("GitHub authorization timed out. Please try again.")
                val account = withContext(Dispatchers.IO) {
                    client.account(finalToken)
                }
                preferences.githubLogin = account.login
                preferences.githubToken = finalToken

                // Configure PRoot git credentials
                runCatching {
                    val runtime = installer.installedRuntime()
                    val ghDir = File(runtime.rootfs, "root/.config/gh").apply { mkdirs() }
                    File(ghDir, "hosts.yml").writeText(
                        "github.com:\n    user: ${account.login}\n    oauth_token: $finalToken\n    git_protocol: https\n"
                    )
                }

                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.CONNECTED,
                        githubLogin = account.login,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = "Connected as @${account.login}",
                        toastMessage = "Successfully connected to GitHub as @${account.login}!",
                    )
                }
                refreshGitHubRepositories()
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.ERROR,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = error.message?.take(240) ?: "GitHub connection failed",
                        toastMessage = "GitHub connection error: ${error.message?.take(100)}",
                    )
                }
            } finally {
                githubAuthJob = null
            }
        }
    }

    fun connectGitHubWithToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Connecting with GitHub token...") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val client = GitHubClient(getApplication())
                    val account = client.account(trimmed)
                    preferences.githubLogin = account.login
                    preferences.githubToken = trimmed

                    // Configure PRoot git credentials
                    runCatching {
                        val runtime = installer.installedRuntime()
                        val ghDir = File(runtime.rootfs, "root/.config/gh").apply { mkdirs() }
                        File(ghDir, "hosts.yml").writeText(
                            "github.com:\n    user: ${account.login}\n    oauth_token: $trimmed\n    git_protocol: https\n"
                        )
                    }
                    account.login
                }
            }
            result.onSuccess { login ->
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.CONNECTED,
                        githubLogin = login,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = "Connected as @$login",
                        toastMessage = "Connected to GitHub as @$login",
                    )
                }
                refreshGitHubRepositories()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.ERROR,
                        githubMessage = err.message ?: "Invalid GitHub token",
                        toastMessage = "GitHub connection failed: ${err.message?.take(80)}",
                    )
                }
            }
        }
    }

    fun generateNewGitHubCode() {
        githubAuthJob?.cancel()
        githubAuthJob = null
        _state.update {
            it.copy(
                githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                githubUserCode = null,
                githubVerificationUri = null,
                githubMessage = str(R.string.vm_github_new_code),
            )
        }
        startGitHubLogin()
    }

    fun refreshGitHubRepositories() {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) return
        if (_state.value.githubRepositoriesLoading) return
        _state.update { it.copy(githubRepositoriesLoading = true, githubMessage = str(R.string.vm_loading_repositories)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val token = preferences.githubToken
                    if (token.isNotBlank()) {
                        GitHubClient(getApplication()).repositories(token)
                    } else {
                        githubRepositoriesFromCli()
                    }
                }
            }
            result.onSuccess { repositories ->
                _state.update {
                    it.copy(
                        githubRepositories = repositories,
                        githubRepositoriesLoading = false,
                        githubMessage = plural(R.plurals.vm_repositories_available, repositories.size, repositories.size),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        githubRepositoriesLoading = false,
                        githubMessage = error.message ?: str(R.string.vm_github_repos_failed),
                    )
                }
            }
        }
    }

    fun disconnectGitHub() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING) return
        val login = _state.value.githubLogin
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = str(R.string.vm_github_signing_out)) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                preferences.githubLogin = ""
                preferences.githubToken = ""
                runCatching {
                    val runtime = installer.installedRuntime()
                    File(runtime.rootfs, "root/.config/gh/hosts.yml").delete()
                }
            }
            _state.update {
                it.copy(
                    githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                    githubLogin = null,
                    githubUserCode = null,
                    githubVerificationUri = null,
                    githubRepositories = emptyList(),
                    githubMessage = str(R.string.vm_signed_out),
                    toastMessage = "Disconnected from GitHub",
                )
            }
        }
    }

    fun pushProjectToGitHub(
        project: Project,
        repoName: String,
        isPrivate: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> },
    ) {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
            onResult(false, str(R.string.ws_github_signin))
            return
        }
        val githubUser = _state.value.githubLogin.orEmpty().ifBlank { "developer" }
        val token = preferences.githubToken

        val targetRepoName = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-").ifBlank { project.slug }
        _state.update { it.copy(gitCloneRunning = true, gitCloneMessage = "Publishing to GitHub: $targetRepoName...") }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val runtime = installer.installedRuntime()
                    val workspace = projectWorkspaceRoot(project)

                    // 1. Pre-create repository via GitHub REST API if token exists
                    if (token.isNotBlank()) {
                        runCatching {
                            GitHubClient(getApplication()).createRepository(
                                token = token,
                                name = targetRepoName,
                                description = "Generated by MindSparQ Jarvis",
                                isPrivate = isPrivate,
                            )
                        }
                    }

                    val env = githubCliEnvironment() + mapOf(
                        "GIT_TERMINAL_PROMPT" to "0",
                        "GIT_COMMITTER_NAME" to githubUser,
                        "GIT_COMMITTER_EMAIL" to "$githubUser@users.noreply.github.com",
                        "GIT_AUTHOR_NAME" to githubUser,
                        "GIT_AUTHOR_EMAIL" to "$githubUser@users.noreply.github.com",
                    )

                    fun runWorkspaceCmd(cmd: String): Pair<Int, String> {
                        val outputFile = File(getApplication<Application>().cacheDir, "gh-push-${System.nanoTime()}.log")
                        return try {
                            val proc = installer.process(
                                runtime.proot,
                                runtime.rootfs,
                                workspace,
                                env,
                                listOf("/usr/bin/bash", "-lc", cmd),
                                projectGuestRoot(project),
                                outputFile = outputFile,
                            )
                            val exit = proc.waitFor()
                            val out = sanitizeTerminalOutput(outputFile.readTailText(64 * 1024)).trim()
                            exit to out
                        } finally {
                            outputFile.delete()
                        }
                    }

                    // 2. Initialize git if repository doesn't have .git
                    val (isGitExit, _) = runWorkspaceCmd("git rev-parse --is-inside-work-tree")
                    if (isGitExit != 0) {
                        runWorkspaceCmd("git init -b main")
                    }
                    runWorkspaceCmd("git config user.name \"$githubUser\"")
                    runWorkspaceCmd("git config user.email \"$githubUser@users.noreply.github.com\"")

                    // 3. Stage and commit all project files
                    runWorkspaceCmd("git add -A")
                    runWorkspaceCmd("git commit -m \"Initial commit from MindSparQ Jarvis\"")

                    // 4. Configure authenticated remote origin and push
                    val authenticatedRemote = if (token.isNotBlank()) {
                        "https://$githubUser:$token@github.com/$githubUser/$targetRepoName.git"
                    } else {
                        "https://github.com/$githubUser/$targetRepoName.git"
                    }
                    runWorkspaceCmd("git remote remove origin")
                    runWorkspaceCmd("git remote add origin $authenticatedRemote")

                    val (pExit, pOut) = runWorkspaceCmd("git push -u origin HEAD")
                    check(pExit == 0) { pOut.ifBlank { "Git push failed with exit code $pExit" } }
                    "https://github.com/$githubUser/$targetRepoName"
                }
            }
            _state.update { it.copy(gitCloneRunning = false, gitCloneMessage = null) }
            result.onSuccess { url ->
                _state.update { it.copy(toastMessage = "Published to GitHub: $url") }
                refreshGitHubRepositories()
                onResult(true, url)
            }.onFailure { err ->
                val errorMsg = err.message?.lineSequence()?.lastOrNull { it.isNotBlank() } ?: "Push failed"
                _state.update { it.copy(toastMessage = "GitHub Push failed: $errorMsg") }
                onResult(false, errorMsg)
            }
        }
    }

    internal suspend fun refreshGitHubConnection() = withContext(Dispatchers.IO) {
        if (!installer.isGitHubCliInstalled()) return@withContext
        val login = runCatching { githubAccountLogin() }.getOrNull()
        if (login.isNullOrBlank()) {
            preferences.githubLogin = ""
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubLogin = null) }
        } else {
            preferences.githubLogin = login
            _state.update {
                it.copy(
                    githubAuthStatus = GitHubAuthStatus.CONNECTED,
                    githubLogin = login,
                    githubMessage = str(R.string.vm_github_connected_as, login),
                )
            }
        }
    }

    private fun githubCliEnvironment(): Map<String, String> = mapOf(
        "GH_PROMPT_DISABLED" to "1",
        "GH_NO_UPDATE_NOTIFIER" to "1",
        // Android PRoot has no Secret Service. This keeps the official gh-owned
        // credential in PocketDev's private Linux home instead of exporting it.
        "BROWSER" to "/bin/false",
    )

    private fun runGitHubCli(arguments: List<String>): Pair<Int, String> {
        check(installer.isGitHubCliInstalled()) { str(R.string.vm_github_cli_missing) }
        val runtime = installer.installedRuntime()
        val outputFile = File(getApplication<Application>().cacheDir, "github-cli-${System.nanoTime()}.log")
        val workspace = File(getApplication<Application>().filesDir, "workspaces/github-auth").apply { mkdirs() }
        return try {
            val process = installer.process(
                runtime.proot,
                runtime.rootfs,
                workspace,
                githubCliEnvironment(),
                listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH) + arguments,
                guestWorkspacePath = "/workspace/github-auth",
                outputFile = outputFile,
            )
            val exit = process.waitFor()
            exit to sanitizeTerminalOutput(outputFile.readTailText(MAX_PROCESS_OUTPUT_BYTES)).trim()
        } finally {
            outputFile.delete()
        }
    }

    private fun githubAccountLogin(): String? {
        val (exit, output) = runGitHubCli(listOf("api", "user", "--jq", ".login"))
        return output.lineSequence().lastOrNull { it.isNotBlank() }?.trim().takeIf { exit == 0 && !it.isNullOrBlank() }
    }

    private fun githubRepositoriesFromCli(): List<GitHubRepository> {
        val endpoint = "user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&per_page=100"
        val (exit, output) = runGitHubCli(listOf("api", "--paginate", "--slurp", endpoint))
        check(exit == 0) { output.lineSequence().lastOrNull { it.isNotBlank() } ?: str(R.string.vm_github_repos_failed) }
        val pages = JSONArray(output)
        val repositories = LinkedHashMap<String, GitHubRepository>()
        for (pageIndex in 0 until pages.length()) {
            val page = pages.optJSONArray(pageIndex) ?: continue
            for (index in 0 until page.length()) {
                val item = page.optJSONObject(index) ?: continue
                val fullName = item.optString("full_name").takeIf(String::isNotBlank) ?: continue
                repositories[fullName] = GitHubRepository(
                    fullName = fullName,
                    cloneUrl = item.optString("clone_url", "https://github.com/$fullName.git"),
                    private = item.optBoolean("private"),
                    defaultBranch = item.optString("default_branch", "main"),
                    description = item.optString("description"),
                    updatedAt = item.optString("updated_at"),
                )
            }
        }
        return repositories.values.toList()
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            _state.update { state -> state.copy(toastMessage = str(R.string.vm_browser_open_failed_url)) }
        }
    }

    private fun startGitHubForegroundOperation() {
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_START)
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_PROJECT_NAME, str(R.string.vm_notif_github_signin))
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_TITLE, str(R.string.vm_notif_connecting_github))
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_CAN_STOP, false),
        )
    }

    private fun stopGitHubForegroundOperation() {
        runCatching {
            getApplication<Application>().startService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                    .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_CANCELLED),
            )
        }.onFailure {
            getApplication<Application>().stopService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java),
            )
        }
    }

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
