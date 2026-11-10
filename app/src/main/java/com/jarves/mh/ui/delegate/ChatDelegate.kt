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
import com.mindsparq.jarvis.core.model.EvidenceKind
import com.mindsparq.jarvis.core.model.ExecutionMode
import com.mindsparq.jarvis.core.model.TaskState
import com.mindsparq.jarvis.engine.loop.TaskContext
import com.mindsparq.jarvis.engine.loop.TaskOrchestrator
import com.mindsparq.jarvis.engine.loop.TaskSnapshot
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectType
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ProviderProtocol
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
import com.jarves.mh.runtime.AutonomousWorkflowCoordinator
import com.jarves.mh.runtime.AutonomousWorkflowResult
import com.jarves.mh.runtime.ReviewVerdict
import com.jarves.mh.runtime.RuntimeBridge
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val AUTONOMOUS_IMPLEMENTATION_OVERALL_LIMIT_MS = 30L * 60 * 1000 // 30 minutes
private const val AUTONOMOUS_IMPLEMENTATION_INACTIVITY_LIMIT_MS = 5L * 60 * 1000 // 5 minutes without progress
private const val AUTONOMOUS_POLL_INTERVAL_MS = 10_000L // 10 seconds between deadline checks
private const val MAX_REVIEW_DIFF_CHARS = 24_000

class ChatDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    internal var activeRuntimeRequest: RuntimeRetryRequest? = null
    @Volatile internal var activeTaskOrchestrator: TaskOrchestrator? = null
    @Volatile private var autonomousWorkflowJob: Job? = null
    @Volatile private var autonomousWorkflowRuntime: RuntimeBridge? = null
    @Volatile private var autonomousWorkflowPhase: AutonomousPhase = AutonomousPhase.IDLE
    @Volatile private var autonomousImplementerSessionId: String? = null
    @Volatile private var autonomousImplementationResult: CompletableDeferred<Boolean>? = null
    /** Last millis at which the autonomous implementer session emitted any runtime event. Used for inactivity timeout. */
    @Volatile private var lastAutonomousActivityAt: Long = 0L
    private val autonomousWorkflowCoordinator = AutonomousWorkflowCoordinator()
    internal val pendingFollowUps = ArrayDeque<QueuedFollowUp>()
    @Volatile internal var steeringToFollowUp: Boolean = false
    internal val failedApiKeyIds = mutableSetOf<String>()
    internal val transcriptWrites = Channel<TranscriptWrite>(Channel.UNLIMITED)

    private enum class AutonomousPhase { IDLE, PLANNING, IMPLEMENTING, REVIEWING }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (pending in transcriptWrites) {
                delay(TRANSCRIPT_WRITE_DEBOUNCE_MS)
                val latestByChat = linkedMapOf((pending.projectId to pending.chatId) to pending)
                while (true) {
                    val next = transcriptWrites.tryReceive().getOrNull() ?: break
                    latestByChat[next.projectId to next.chatId] = next
                }
                latestByChat.values.forEach { latest ->
                    runCatching {
                        preferences.saveMessages(latest.projectId, latest.chatId, latest.messages)
                    }.onFailure { err ->
                        android.util.Log.e("ChatDelegate", "Background saveMessages failed for chat ${latest.chatId}", err)
                    }
                }
            }
        }
    }
    fun createChat() {
        val project = _state.value.activeProject ?: return
        if (_state.value.isRunning) return
        persistMessages()
        val chat = ProjectChat(title = str(R.string.vm_new_chat))
        val chats = listOf(chat) + _state.value.projectChats
        preferences.saveProjectChats(project.id, chats)
        _state.update {
            it.copy(
                projectChats = chats,
                activeChatId = chat.id,
                messages = listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")),
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                pendingApproval = null,
                pendingAttachments = emptyList(),
            )
        }
    }

    fun switchChat(chatId: String) {
        val current = _state.value
        val project = current.activeProject ?: return
        if (current.isRunning || current.activeChatId == chatId) return
        val chat = current.projectChats.firstOrNull { it.id == chatId } ?: return
        persistMessages()
        val saved = preferences.loadMessages(project.id, chat.id)
        _state.update {
            it.copy(
                activeChatId = chat.id,
                messages = saved.ifEmpty { listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")) },
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                pendingApproval = null,
                pendingAttachments = emptyList(),
            )
        }
    }



    fun addChatAttachments(uris: List<Uri>) {
        val current = _state.value
        val project = current.activeProject ?: return
        val chatId = current.activeChatId ?: return
        if (uris.isEmpty()) return
        val remaining = (MAX_ATTACHMENTS_PER_MESSAGE - current.pendingAttachments.size).coerceAtLeast(0)
        if (remaining == 0) {
            _state.update { it.copy(toastMessage = str(R.string.vm_max_attachments, MAX_ATTACHMENTS_PER_MESSAGE)) }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val added = mutableListOf<ChatAttachment>()
                val errors = mutableListOf<String>()
                uris.take(remaining).forEach { uri ->
                    runCatching { copyChatAttachment(project, chatId, uri) }
                        .onSuccess(added::add)
                        .onFailure { errors += (it.message ?: str(R.string.vm_attach_failed)) }
                }
                added to errors
            }
            val (added, errors) = result
            _state.update { state ->
                state.copy(
                    pendingAttachments = state.pendingAttachments + added,
                    toastMessage = errors.firstOrNull() ?: if (uris.size > remaining) plural(R.plurals.vm_more_files_can_be_added, remaining, remaining) else null,
                )
            }
            if (added.isNotEmpty()) refreshProjectFiles()
        }
    }

    fun removePendingAttachment(attachmentId: String) {
        val current = _state.value
        val project = current.activeProject ?: return
        val attachment = current.pendingAttachments.firstOrNull { it.id == attachmentId } ?: return
        _state.update { it.copy(pendingAttachments = it.pendingAttachments.filterNot { item -> item.id == attachmentId }) }
        viewModelScope.launch(Dispatchers.IO) {
            val root = projectWorkspaceRoot(project)
            val file = File(root, attachment.relativePath).canonicalFile
            if (file.toPath().startsWith(root.canonicalFile.toPath())) file.delete()
        }
    }

    fun openChatAttachment(attachment: ChatAttachment) {
        val project = _state.value.activeProject ?: return
        runCatching {
            val root = projectWorkspaceRoot(project).canonicalFile
            val file = File(root, attachment.relativePath).canonicalFile
            require(file.isFile && file.toPath().startsWith(root.toPath())) { str(R.string.vm_attachment_unavailable) }
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType.ifBlank { "application/octet-stream" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }.onFailure { error ->
            _state.update { it.copy(toastMessage = error.message ?: str(R.string.vm_no_app_for_attachment)) }
        }
    }

    private fun copyChatAttachment(project: Project, chatId: String, uri: Uri): ChatAttachment {
        val resolver = getApplication<Application>().contentResolver
        var displayName = "attachment"
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = cursor.getString(it) ?: displayName }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { declaredSize = cursor.getLong(it) }
            }
        }
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val supportedTextExtensions = setOf(
            "txt", "md", "markdown", "json", "jsonl", "csv", "tsv", "xml", "yaml", "yml", "log",
            "kt", "kts", "java", "py", "js", "mjs", "cjs", "ts", "tsx", "jsx", "html", "htm",
            "css", "scss", "sass", "less", "c", "cc", "cpp", "h", "hpp", "sh", "bash", "zsh",
            "gradle", "properties", "toml", "ini", "conf", "sql",
        )
        val supported = mimeType.startsWith("image/") ||
            mimeType.startsWith("text/") || mimeType == "application/json" || mimeType == "application/xml" ||
            mimeType.endsWith("+json") || mimeType.endsWith("+xml") || extension in supportedTextExtensions
        require(supported) { str(R.string.vm_attachment_unsupported_type) }
        require(declaredSize <= MAX_ATTACHMENT_BYTES || declaredSize < 0) { str(R.string.vm_attachment_too_large, displayName) }
        val safeName = sanitizeAttachmentName(displayName)
        val root = projectWorkspaceRoot(project).canonicalFile
        val folder = File(root, "attachments/$chatId").apply { mkdirs() }.canonicalFile
        require(folder.toPath().startsWith(root.toPath())) { str(R.string.vm_unsafe_attachment_folder) }
        val stem = safeName.substringBeforeLast('.', safeName)
        val safeExtension = safeName.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
        var destination = File(folder, safeName)
        var suffix = 2
        while (destination.exists()) destination = File(folder, "$stem-${suffix++}$safeExtension")
        var copied = 0L
        try {
            resolver.openInputStream(uri)?.buffered()?.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_ATTACHMENT_BYTES) { str(R.string.vm_attachment_too_large, displayName) }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error(str(R.string.vm_read_named_failed, displayName))
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
        return ChatAttachment(
            displayName = displayName.take(120),
            relativePath = destination.relativeTo(root).invariantSeparatorsPath,
            mimeType = mimeType,
            sizeBytes = copied,
        )
    }

    internal fun sanitizeAttachmentName(name: String): String {
        val clean = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().trim('.').take(100)
        return clean.ifBlank { "attachment-${UUID.randomUUID().toString().take(8)}" }
    }

    fun sendPrompt(prompt: String) {
        if (autonomousWorkflowJob?.isActive == true && !_state.value.isRunning) {
            _state.update { it.copy(toastMessage = "The previous autonomous workflow is still stopping. Please wait.") }
            return
        }
        val project = state.value.activeProject ?: return
        if (_state.value.agentKind == AgentKind.ANTIGRAVITY &&
            _state.value.antigravityAuth.status != AntigravityAuthStatus.SIGNED_IN) {
            val hasApiKey = vault.contains(state.value.provider.kind.name) ||
                state.value.provider.kind == ProviderKind.OLLAMA ||
                state.value.provider.kind == ProviderKind.CUSTOM ||
                vault.contains(ProviderKind.GEMINI.name) ||
                vault.contains(ProviderKind.LLM_ROUTER.name)
            if (!hasApiKey) {
                _state.update { it.copy(toastMessage = str(R.string.vm_antigravity_signin_before_task)) }
                return
            }
        }
        if (_state.value.agentKind == AgentKind.DEEPSEEK_HARNESS && _state.value.provider.kind == ProviderKind.CLAUDE) {
            _state.update { it.copy(toastMessage = str(R.string.vm_dsh_claude_unsupported)) }
            return
        }
        val attachments = state.value.pendingAttachments
        if (prompt.isBlank() && attachments.isEmpty()) return
        if (state.value.isRunning || state.value.androidBuildRunning) {
            queueFollowUp(project, prompt, attachments)
            return
        }
        if (state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = str(R.string.an_wait_terminal_then_send)) }
            return
        }
        if (_state.value.autonomousWorkflowEnabled) {
            autonomousWorkflowConfigurationError()?.let { message ->
                _state.update { it.copy(toastMessage = message) }
                return
            }
        }
        startPrompt(project, prompt, attachments)
    }

    private fun autonomousWorkflowConfigurationError(): String? {
        val current = _state.value
        if (!installer.isAgentInstalled(current.agentKind)) {
            return "Install ${current.agentKind.title} in Agent settings to use the autonomous workflow."
        }
        val provider = current.provider
        if (provider.kind.protocol == ProviderProtocol.CLAUDE_LOGIN && current.agentKind != AgentKind.CLAUDE_CODE) {
            return "Planning and review need an API-key or open model provider. Select an API or local provider instead of Claude subscription login."
        }
        if (!isDeviceOnline() && provider.kind != ProviderKind.OLLAMA && provider.kind != ProviderKind.CUSTOM) {
            return "The autonomous workflow needs a network connection or local model for planning and review."
        }
        val key = vault.get(provider.kind.name).orEmpty()
            .ifBlank { if (current.agentKind == AgentKind.ANTIGRAVITY) vault.get(ProviderKind.GEMINI.name).orEmpty() else "" }
            .ifBlank { localServerPlaceholderKey(provider) }
        if (key.isBlank() && current.agentKind != AgentKind.ANTIGRAVITY && provider.kind != ProviderKind.OLLAMA && provider.kind != ProviderKind.CUSTOM) {
            return "Save an API key for ${provider.kind.title} before starting the autonomous workflow."
        }
        if (provider.resolvedBaseUrl.isBlank() && current.agentKind != AgentKind.ANTIGRAVITY) {
            return "Select a provider URL and model before starting the autonomous workflow."
        }
        return null
    }

    private fun queueFollowUp(project: Project, prompt: String, attachments: List<ChatAttachment>) {
        val requestText = prompt.trim().ifBlank { "Please review the attached files." }
        pendingFollowUps.addLast(QueuedFollowUp(projectId = project.id, prompt = requestText, attachments = attachments))
        _state.update {
            it.copy(
                pendingAttachments = emptyList(),
                queuedFollowUps = pendingFollowUps.toList(),
                toastMessage = str(R.string.an_follow_up_queued),
            )
        }
    }

    fun steerQueuedFollowUp(id: String) {
        if (steeringToFollowUp) return
        val selected = pendingFollowUps.firstOrNull { it.id == id } ?: return
        pendingFollowUps.remove(selected)
        pendingFollowUps.addFirst(selected)
        _state.update {
            it.copy(
                queuedFollowUps = pendingFollowUps.toList(),
                toastMessage = str(R.string.an_steering_to_follow_up),
            )
        }
        when {
            _state.value.isRunning -> {
                steeringToFollowUp = true
                viewModelScope.launch { activeRuntime().stopActiveSession() }
            }
            _state.value.androidBuildRunning -> {
                steeringToFollowUp = true
                androidDelegate.cancelAndroidBuild()
            }
            else -> startNextFollowUp()
        }
    }

    fun removeQueuedFollowUp(id: String) {
        val selected = pendingFollowUps.firstOrNull { it.id == id } ?: return
        pendingFollowUps.remove(selected)
        _state.update { it.copy(queuedFollowUps = pendingFollowUps.toList()) }
    }

    fun isDeviceOnline(): Boolean = runCatching {
        val app = getApplication<Application>()
        val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)

    private fun startPrompt(project: Project, prompt: String, attachments: List<ChatAttachment>) {
        val taskId = UUID.randomUUID().toString()
        val taskContext = TaskContext(
            taskId = taskId,
            projectId = project.id,
            title = prompt.trim().take(100),
            mode = ExecutionMode.WORK
        )
        val orchestrator = TaskOrchestrator(
            context = taskContext,
            onTransition = { snapshot ->
                preferences.dbBridge.updateTaskState(
                    id = snapshot.taskId,
                    state = snapshot.state.name,
                    step = snapshot.currentStep.toLong(),
                    repairAttempts = snapshot.repairAttempts.toLong(),
                    endedAt = if (snapshot.state == TaskState.COMPLETED ||
                        snapshot.state == TaskState.FAILED ||
                        snapshot.state == TaskState.CANCELLED) System.currentTimeMillis() else null,
                    reportMd = snapshot.failureReason
                )
            }
        )
        activeTaskOrchestrator = orchestrator
        preferences.dbBridge.saveTask(
            id = taskId,
            projectId = project.id,
            chatId = state.value.activeChatId,
            title = prompt.trim().take(100),
            state = TaskState.QUEUED.name,
            startedAt = System.currentTimeMillis()
        )
        orchestrator.startInspection()

        // requestText is sent to the agent, so it stays English; the chat title is user-facing.
        val requestText = prompt.trim().ifBlank { "Please review the attached files." }
        updateActiveChatTitle(prompt.trim().ifBlank { str(R.string.md_chat_title_attachments) })
        _state.update {
            val startedAt = System.currentTimeMillis()
            it.copy(
                messages = it.messages + ChatMessage(fromUser = true, text = prompt.trim(), attachments = attachments),
                pendingAttachments = emptyList(),
                isRunning = true,
                activity = listOf(ActivityItem(str(R.string.vm_understanding_request), str(R.string.vm_preparing_safe_plan), false)) + it.activity,
                liveProcess = listOf(ActivityItem("Think", requestPlanningSummary(requestText, it.agentKind), false)),
                liveThinking = true,
                activeThinkingBlockId = null,
                taskStartedAtMillis = startedAt,
                taskFinishedAtMillis = null,
                workSegmentStartedAtMillis = startedAt,
                currentTaskRequest = requestText,
            )
        }
        touchProject(project.id)
        persistMessages()
        val history = state.value.messages // includes all messages up to now
        val runtimePrompt = if (attachments.isEmpty()) requestText else buildString {
            appendLine(requestText)
            appendLine()
            appendLine("<attached_files>")
            attachments.forEach { attachment ->
                appendLine("- ${attachment.displayName}: ${projectGuestRoot(project)}/${attachment.relativePath} (${attachment.mimeType})")
            }
            appendLine("These files were explicitly attached by the user. Inspect them only as needed for the request.")
            appendLine("</attached_files>")
        }
        failedApiKeyIds.clear()
        val isOnline = isDeviceOnline()
        val effectiveProvider = if (
            !isOnline &&
            !_state.value.autonomousWorkflowEnabled &&
            _state.value.provider.kind != ProviderKind.OLLAMA
        ) {
            val offlineProfile = preferences.loadProvider(vault, state.value.agentKind).copy(
                kind = ProviderKind.OLLAMA,
                model = "qwen2.5-coder:1.5b",
                baseUrl = "http://127.0.0.1:11434/v1",
            )
            _state.update {
                it.copy(
                    provider = offlineProfile,
                    toastMessage = "No internet connection detected. Auto-switched to Offline Local Model (qwen2.5-coder:1.5b).",
                    liveProcess = listOf(ActivityItem("Offline Mode", "No internet connection. Executing via local Ollama engine.", true)),
                )
            }
            offlineProfile
        } else {
            state.value.provider
        }

        val runtimeToUse = if ((_state.value.agentKind == AgentKind.ANTIGRAVITY && !isOnline) ||
            (_state.value.agentKind == AgentKind.ANTIGRAVITY &&
            effectiveProvider.kind != ProviderKind.CUSTOM &&
            (vault.contains(effectiveProvider.kind.name) || effectiveProvider.kind == ProviderKind.OLLAMA) &&
            _state.value.antigravityAuth.status != AntigravityAuthStatus.SIGNED_IN)) {
            if (installer.isAgentInstalled(AgentKind.DEEPSEEK_HARNESS)) dshRuntime else claudeRuntime
        } else {
            activeRuntime()
        }
        activeRuntimeRequest = RuntimeRetryRequest(
            runtime = runtimeToUse,
            project = project,
            prompt = runtimePrompt,
            history = history,
            provider = effectiveProvider,
        )
        if (_state.value.autonomousWorkflowEnabled) {
            autonomousWorkflowRuntime = runtimeToUse
            autonomousWorkflowPhase = AutonomousPhase.PLANNING
            autonomousWorkflowJob = viewModelScope.launch {
                runAutonomousWorkflow(
                    project = project,
                    task = runtimePrompt,
                    history = history,
                    provider = effectiveProvider,
                    runtime = runtimeToUse,
                )
            }
        } else {
            viewModelScope.launch {
                activeRuntimeRequest?.let { request ->
                    request.runtime.startSession(
                        request.project.id,
                        request.project.slug,
                        request.project.kind,
                        request.prompt,
                        request.history,
                        request.provider,
                    )
                }
            }
        }
    }

    private suspend fun runAutonomousWorkflow(
        project: Project,
        task: String,
        history: List<ChatMessage>,
        provider: ProviderProfile,
        runtime: RuntimeBridge,
    ) {
        val currentAgent = _state.value.agentKind
        val agentTitle = currentAgent.title
        val apiKey = vault.get(provider.kind.name).orEmpty()
            .ifBlank { if (currentAgent == AgentKind.ANTIGRAVITY) vault.get(ProviderKind.GEMINI.name).orEmpty() else "" }
            .ifBlank { localServerPlaceholderKey(provider) }
        try {
            val result = autonomousWorkflowCoordinator.run(
                task = task,
                planner = { systemPrompt, userPrompt ->
                    setAutonomousPhase(AutonomousPhase.PLANNING, "Planning agent", "Preparing a bounded implementation plan.")
                    providerApi.generateText(
                        profile = provider,
                        apiKey = apiKey,
                        systemPrompt = systemPrompt,
                        userPrompt = userPrompt,
                        maxTokens = 1_800,
                    ).also { output -> appendAutonomousMessage("Planner · ${provider.model}", output) }
                },
                implementer = { implementationPrompt ->
                    setAutonomousPhase(
                        AutonomousPhase.IMPLEMENTING,
                        "$agentTitle implementer",
                        "Project edits are policy-screened; risky commands require approval.",
                    )
                    val completion = CompletableDeferred<Boolean>()
                    autonomousImplementationResult = completion
                    autonomousImplementerSessionId = null
                    lastAutonomousActivityAt = System.currentTimeMillis()
                    runtime.startSession(
                        projectId = project.id,
                        projectSlug = project.slug,
                        projectKind = project.kind,
                        prompt = implementationPrompt,
                        conversationHistory = history,
                        provider = provider,
                    )
                    val completed = try {
                        withTimeout(AUTONOMOUS_IMPLEMENTATION_OVERALL_LIMIT_MS) {
                            while (true) {
                                // Inactivity deadline: fail if no progress event within the window.
                                val inactiveFor = System.currentTimeMillis() - lastAutonomousActivityAt
                                if (inactiveFor >= AUTONOMOUS_IMPLEMENTATION_INACTIVITY_LIMIT_MS) {
                                    error("$agentTitle implementation stalled; no progress for ${AUTONOMOUS_IMPLEMENTATION_INACTIVITY_LIMIT_MS / 60_000} minutes.")
                                }
                                // Poll for completion so deadlines can be re-checked between polls.
                                val result = withTimeoutOrNull(AUTONOMOUS_POLL_INTERVAL_MS) {
                                    completion.await()
                                }
                                if (result != null) return@withTimeout result
                                delay(AUTONOMOUS_POLL_INTERVAL_MS)
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        error("$agentTitle implementation exceeded the overall time limit of ${AUTONOMOUS_IMPLEMENTATION_OVERALL_LIMIT_MS / 60_000} minutes.")
                    }
                    if (completed != true) error("$agentTitle did not complete the implementation phase.")
                },
                reviewer = { systemPrompt, userPrompt ->
                    setAutonomousPhase(AutonomousPhase.REVIEWING, "Independent reviewer", "Reviewing the project diff.")
                    val diff = workflowDiff(runtime, project)
                    val reviewInput = buildString {
                        appendLine(userPrompt)
                        appendLine()
                        appendLine("Project diff:")
                        appendLine(diff.ifBlank { "(No project diff was reported by the implementation runtime.)" })
                    }
                    val response = providerApi.generateText(
                        profile = provider,
                        apiKey = apiKey,
                        systemPrompt = systemPrompt,
                        userPrompt = reviewInput,
                        maxTokens = 1_600,
                    )
                    autonomousWorkflowCoordinator.parseReview(response)
                },
            )
            appendAutonomousMessage(
                "Reviewer · ${provider.model}",
                buildString {
                    appendLine("Verdict: ${result.review.verdict}")
                    if (result.revisionMade) appendLine("One bounded revision was completed.")
                    appendLine()
                    append(result.review.findings)
                },
            )
            completeAutonomousWorkflow(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failAutonomousWorkflow(error.message ?: "Autonomous workflow failed.")
        } finally {
            autonomousWorkflowPhase = AutonomousPhase.IDLE
            autonomousImplementerSessionId = null
            autonomousImplementationResult = null
            autonomousWorkflowRuntime = null
            autonomousWorkflowJob = null
        }
    }

    private suspend fun workflowDiff(runtime: RuntimeBridge, project: Project): String {
        val changes = runtime.loadPendingChanges(project.id)
        if (changes.isEmpty()) return ""
        val budget = StringBuilder()
        for (change in changes) {
            if (budget.length >= MAX_REVIEW_DIFF_CHARS) break
            budget.appendLine("File: ${change.path} (+${change.additions}/-${change.deletions})")
            for (line in change.diffLines) {
                if (budget.length >= MAX_REVIEW_DIFF_CHARS) break
                budget.appendLine("${line.type}: ${line.text}")
            }
            if (change.binary) budget.appendLine("(binary file)")
            budget.appendLine()
        }
        if (budget.length > MAX_REVIEW_DIFF_CHARS) {
            budget.setLength(MAX_REVIEW_DIFF_CHARS)
            budget.appendLine("\n[Diff truncated for review]")
        }
        return budget.toString()
    }

    private fun setAutonomousPhase(phase: AutonomousPhase, title: String, detail: String) {
        autonomousWorkflowPhase = phase
        _state.update { current ->
            if (!current.isRunning) current else current.copy(
                liveProcess = listOf(ActivityItem(title, detail, isComplete = false)),
                liveThinking = phase != AutonomousPhase.IMPLEMENTING,
                activeThinkingBlockId = null,
            )
        }
    }

    private fun appendAutonomousMessage(title: String, text: String) {
        _state.update { current ->
            if (!current.isRunning) current else current.copy(
                messages = current.messages + ChatMessage(fromUser = false, text = "$title\n\n$text"),
            )
        }
        persistMessages()
    }

    private fun completeAutonomousWorkflow(result: AutonomousWorkflowResult) {
        val finishedAt = System.currentTimeMillis()
        val finalDetail = if (result.review.verdict == ReviewVerdict.APPROVE) {
            if (result.revisionMade) "Review approved after one bounded revision." else "Review approved."
        } else {
            "The review still has findings; no additional revision was attempted."
        }
        activeTaskOrchestrator?.complete(finalDetail)
        activeTaskOrchestrator?.recordEvidence(EvidenceKind.APPROVAL, finalDetail)
        _state.update { current ->
            if (!current.isRunning) current else attachTaskDuration(finishWorkSegment(current, finishedAt), finishedAt).copy(
                isRunning = false,
                activeSessionId = null,
                pendingApproval = null,
                liveThinking = false,
                activity = listOf(ActivityItem("Autonomous workflow complete", finalDetail)) +
                    current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                taskFinishedAtMillis = finishedAt,
                currentTaskRequest = null,
            )
        }
        activeRuntimeRequest = null
        activeTaskOrchestrator = null
        failedApiKeyIds.clear()
        touchProject(_state.value.activeProject?.id.orEmpty())
        refreshProjectFiles()
        persistMessages(includeLiveProcess = true)
        if (pendingFollowUps.isNotEmpty()) {
            viewModelScope.launch {
                delay(150)
                if (!_state.value.isRunning) startNextFollowUp()
            }
        }
    }

    private fun failAutonomousWorkflow(reason: String) {
        val finishedAt = System.currentTimeMillis()
        val message = localizedFailureReason(reason).take(500)
        activeTaskOrchestrator?.fail(message)
        _state.update { current ->
            if (!current.isRunning) current else attachTaskDuration(
                appendWorkItem(current, ActivityItem("Autonomous workflow failed", message)),
                finishedAt,
            ).copy(
                isRunning = false,
                activeSessionId = null,
                pendingApproval = null,
                liveThinking = false,
                activity = listOf(ActivityItem("Autonomous workflow failed", message)) +
                    current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                taskFinishedAtMillis = finishedAt,
                currentTaskRequest = null,
            )
        }
        activeRuntimeRequest = null
        activeTaskOrchestrator = null
        failedApiKeyIds.clear()
        persistMessages(includeLiveProcess = true)
        if (pendingFollowUps.isNotEmpty()) {
            viewModelScope.launch {
                delay(150)
                if (!_state.value.isRunning) startNextFollowUp()
            }
        }
    }

    fun answerApproval(approved: Boolean) {
        val request = state.value.pendingApproval ?: return
        viewModelScope.launch { activeRuntime().respondToApproval(request, approved) }
    }

    fun stopTask() {
        if (!_state.value.isRunning) return
        steeringToFollowUp = false
        pendingFollowUps.clear()
        _state.update { it.copy(queuedFollowUps = emptyList()) }
        if (autonomousWorkflowJob?.isActive == true) {
            autonomousWorkflowJob?.cancel()
            viewModelScope.launch { autonomousWorkflowRuntime?.stopActiveSession() }
            failAutonomousWorkflow("Autonomous workflow was stopped by the user.")
            return
        }
        viewModelScope.launch { activeRuntime().stopActiveSession() }
    }

    internal fun startNextFollowUp() {
        val next = if (pendingFollowUps.isEmpty()) null else pendingFollowUps.removeFirst()
        if (next == null) {
            _state.update { it.copy(queuedFollowUps = emptyList()) }
            return
        }
        val project = _state.value.activeProject
        if (project == null || project.id != next.projectId) {
            pendingFollowUps.clear()
            _state.update { it.copy(queuedFollowUps = emptyList()) }
            return
        }
        _state.update { it.copy(queuedFollowUps = pendingFollowUps.toList()) }
        startPrompt(project, next.prompt, next.attachments)
    }

    fun undoLastChanges() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            val restored = activeRuntime().undoLastChanges(project.id)
            _state.update { current ->
                current.copy(
                    changes = if (restored) emptyList() else current.changes,
                    activity = listOf(
                        ActivityItem(
                            if (restored) str(R.string.vm_changes_undone) else str(R.string.vm_undo_unavailable),
                            if (restored) str(R.string.vm_files_restored) else str(R.string.vm_no_checkpoint),
                        ),
                    ) + current.activity,
                )
            }
            if (restored) refreshProjectFiles()
        }
    }

    fun keepLastChanges() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            activeRuntime().acceptLastChanges(project.id)
            _state.update {
                it.copy(
                    changes = emptyList(),
                    activity = listOf(ActivityItem(str(R.string.vm_changes_kept), str(R.string.vm_changes_kept_detail))) + it.activity,
                )
            }
        }
    }

    fun undoFileChange(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            if (activeRuntime().undoFileChange(project.id, path)) {
                _state.update { current -> current.copy(changes = current.changes.filterNot { it.path == path }) }
                refreshProjectFiles()
            }
        }
    }

    fun keepFileChange(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            if (activeRuntime().acceptFileChange(project.id, path)) {
                _state.update { current -> current.copy(changes = current.changes.filterNot { it.path == path }) }
            }
        }
    }

    private fun isNoisyRuntimeItem(item: ActivityItem): Boolean {
        // Never allocate or scan an entire command/result just to classify the row.
        val combined = "${item.title} ${item.detail.take(NOISY_ITEM_SCAN_CHARS)}"
        return combined.contains("Starting Claude Code", true) ||
            combined.contains("Agent process started", true) ||
            combined.contains("Claude Code connected", true) ||
            combined.contains("Runtime warning", true) ||
            combined.contains("unrecognized_model", true) ||
            combined.contains("Writing response", true) ||
            combined.contains("Claude Code finished", true) ||
            combined.contains("Task completed", true) ||
            combined.contains(str(R.string.vm_task_completed), true)
    }

    private fun toolPlanSummary(toolName: String, detail: String): String {
        val clean = detail.replace(Regex("\\s+"), " ").trim()
        val short = clean.take(90).ifBlank { str(R.string.vm_plan_current_project) }
        return when (toolName) {
            "Write" -> str(R.string.vm_plan_create, clean.substringAfterLast('/').ifBlank { str(R.string.vm_plan_a_project_file) })
            "Edit", "NotebookEdit" -> str(R.string.vm_plan_update, clean.substringAfterLast('/').ifBlank { str(R.string.vm_plan_a_project_file) })
            "Read" -> str(R.string.vm_plan_inspect, clean.substringAfterLast('/').ifBlank { str(R.string.vm_plan_a_project_file) })
            "Glob" -> str(R.string.vm_plan_find)
            "Grep" -> str(R.string.vm_plan_search, short)
            "Bash" -> if (clean.contains("cat ", true) || clean.contains("printf ", true) || clean.contains(" >")) {
                str(R.string.vm_plan_bash_files)
            } else {
                str(R.string.vm_plan_run, short)
            }
            else -> str(R.string.vm_plan_use_tool, toolName)
        }
    }

    private fun requestPlanningSummary(
        request: String,
        agentKind: AgentKind,
        toolName: String? = null,
        detail: String = "",
    ): String {
        val agentName = agentKind.localizedTitle(getApplication())
        val cleanRequest = request.replace(Regex("\\s+"), " ").trim().take(110)
        val requestPart = if (cleanRequest.isBlank()) {
            str(R.string.vm_plan_agent_reviewing, agentName)
        } else {
            str(R.string.vm_plan_user_asking, cleanRequest)
        }
        return if (toolName == null) {
            str(R.string.vm_plan_deciding, requestPart, agentName)
        } else {
            str(R.string.vm_plan_with_tool, requestPart, toolPlanSummary(toolName, detail))
        }
    }

    private fun finishWorkSegment(current: AppUiState, finishedAt: Long = System.currentTimeMillis()): AppUiState {
        val meaningfulItems = current.liveProcess.filterNot(::isNoisyRuntimeItem)
            .map(::boundedActivityItem)
            .map { if (it.isComplete) it else it.copy(isComplete = true) }
            .takeLast(MAX_VISIBLE_WORK_ITEMS)
        if (!current.liveThinking && meaningfulItems.isEmpty()) {
            return current.copy(liveProcess = emptyList(), workSegmentStartedAtMillis = null)
        }
        val startedAt = current.workSegmentStartedAtMillis ?: current.taskStartedAtMillis ?: finishedAt
        val block = ChatMessage(
            fromUser = false,
            text = "",
            workItems = meaningfulItems,
            workedMillis = (finishedAt - startedAt).coerceAtLeast(0L),
        )
        return current.copy(
            messages = current.messages + block,
            liveProcess = emptyList(),
            liveThinking = false,
            activeThinkingBlockId = null,
            workSegmentStartedAtMillis = null,
        )
    }

    /** Adds the complete request duration to the response produced after the latest user message. */
    private fun attachTaskDuration(current: AppUiState, finishedAt: Long): AppUiState {
        val startedAt = current.taskStartedAtMillis ?: return current
        val lastUserIndex = current.messages.indexOfLast { it.fromUser }
        val responseIndex = current.messages.indices.lastOrNull { index ->
            index > lastUserIndex && !current.messages[index].fromUser && current.messages[index].text.isNotBlank()
        } ?: return current
        val updated = current.messages.toMutableList()
        updated[responseIndex] = updated[responseIndex].copy(
            workedMillis = (finishedAt - startedAt).coerceAtLeast(1L),
        )
        return current.copy(messages = updated)
    }

    private fun appendWorkItem(current: AppUiState, item: ActivityItem): AppUiState {
        val boundedItem = boundedActivityItem(item)
        if (isNoisyRuntimeItem(boundedItem)) return current
        return current.copy(
            liveProcess = (current.liveProcess
                .map { if (!it.isComplete) it.copy(isComplete = true) else it } + boundedItem)
                .takeLast(MAX_VISIBLE_WORK_ITEMS),
            liveThinking = false,
            workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
        )
    }

    internal fun onRuntimeEvent(event: RuntimeEvent) {
        // Track activity for the autonomous implementer timeout policy. Any event
        // from the active implementation session resets the inactivity clock.
        if (autonomousWorkflowPhase == AutonomousPhase.IMPLEMENTING &&
            autonomousImplementerSessionId != null &&
            event.sessionId == autonomousImplementerSessionId) {
            lastAutonomousActivityAt = System.currentTimeMillis()
        }
        if (event is RuntimeEvent.SessionStarted && autonomousWorkflowPhase == AutonomousPhase.IMPLEMENTING) {
            autonomousImplementerSessionId = event.sessionId
            lastAutonomousActivityAt = System.currentTimeMillis()
        }
        val autonomousImplementerTerminal =
            (event is RuntimeEvent.SessionCompleted || event is RuntimeEvent.SessionFailed) &&
                autonomousImplementerSessionId == event.sessionId
        if (autonomousImplementerTerminal) {
            autonomousImplementationResult?.complete(event is RuntimeEvent.SessionCompleted)
        }
        val terminalEventForActiveSession =
            (event is RuntimeEvent.SessionCompleted || event is RuntimeEvent.SessionFailed) &&
                _state.value.isRunning &&
                !(autonomousImplementerTerminal && event is RuntimeEvent.SessionCompleted) &&
                (_state.value.activeSessionId == null || _state.value.activeSessionId == event.sessionId)
        if (event is RuntimeEvent.SessionFailed && _state.value.agentKind == AgentKind.ANTIGRAVITY &&
            (event.reason.contains("sign-in", true) || event.reason.contains("authentication", true))) {
            antigravityAuthController.invalidateSession(event.reason)
        }
        if (event is RuntimeEvent.SessionFailed && !autonomousImplementerTerminal && retryWithNextApiKey(event)) return

        // Dispatch state machine transitions and record evidence to TaskOrchestrator
        when (event) {
            is RuntimeEvent.SessionStarted -> activeTaskOrchestrator?.proceedToPlanning()
            is RuntimeEvent.ReasoningProgress, is RuntimeEvent.ReasoningSummary -> activeTaskOrchestrator?.proceedToPlanning()
            is RuntimeEvent.ToolStarted -> {
                if (activeTaskOrchestrator?.snapshot?.value?.state == TaskState.PLANNING ||
                    activeTaskOrchestrator?.snapshot?.value?.state == TaskState.AWAITING_APPROVAL) {
                    activeTaskOrchestrator?.onApprovalGranted()
                }
            }
            is RuntimeEvent.ToolRequested -> activeTaskOrchestrator?.requestApproval(event.request.explanation)
            is RuntimeEvent.ToolApproved -> activeTaskOrchestrator?.onApprovalGranted()
            is RuntimeEvent.ToolRejected -> activeTaskOrchestrator?.onApprovalDenied("Rejected by user")
            is RuntimeEvent.ToolCompleted -> {
                val isTest = event.toolName.contains("test", ignoreCase = true)
                val kind = if (isTest) EvidenceKind.TEST_RUN else EvidenceKind.COMMAND
                val exitCode = if (event.summary.contains("fail", ignoreCase = true)) 1 else 0
                activeTaskOrchestrator?.recordEvidence(kind, summary = "${event.toolName}: ${event.summary}", exitCode = exitCode)
                activeTaskOrchestrator?.snapshot?.value?.taskId?.let { tid ->
                    runCatching {
                        preferences.dbBridge.recordTaskEvidence(
                            taskId = tid,
                            kind = kind.name,
                            summary = "${event.toolName}: ${event.summary}",
                            exitCode = exitCode.toLong(),
                            projectIdFallback = _state.value.activeProject?.id
                        )
                    }.onFailure { android.util.Log.e("ChatDelegate", "Failed to record task evidence", it) }
                }
            }
            is RuntimeEvent.FilesChanged -> {
                activeTaskOrchestrator?.recordEvidence(EvidenceKind.DIFF, summary = "Files modified: ${event.paths.take(6).joinToString()}")
            }
            is RuntimeEvent.SessionCompleted -> {
                if (!autonomousImplementerTerminal) {
                    activeTaskOrchestrator?.complete("Task completed successfully")
                }
            }
            is RuntimeEvent.SessionFailed -> {
                activeTaskOrchestrator?.fail(event.reason.take(200))
            }
            else -> {}
        }

        _state.update { current ->
            if (!current.isRunning) {
                current
            } else if (current.activeSessionId != null && current.activeSessionId != event.sessionId) {
                current
            } else when (event) {
                is RuntimeEvent.SessionStarted -> current.copy(
                    activeSessionId = event.sessionId,
                    activity = current.activity.mapIndexed { index, item -> if (index == 0) item.copy(isComplete = true) else item },
                )
                is RuntimeEvent.AssistantDelta -> {
                    if (event.text.isBlank() || event.text == "null") return@update current
                    val timeline = if (current.liveThinking || current.liveProcess.any { !isNoisyRuntimeItem(it) }) {
                        finishWorkSegment(current)
                    } else {
                        current
                    }
                    val lastMessage = timeline.messages.lastOrNull()
                    if (lastMessage != null && !lastMessage.fromUser && lastMessage.workItems.isEmpty() && lastMessage.workedMillis == 0L) {
                        timeline.copy(messages = timeline.messages.dropLast(1) + lastMessage.copy(text = lastMessage.text + event.text))
                    } else {
                        timeline.copy(messages = timeline.messages + ChatMessage(fromUser = false, text = event.text))
                    }
                }
                is RuntimeEvent.ReasoningProgress -> {
                    val existingIndex = current.liveProcess.indexOfLast { it.title == "Think" }
                    // The request-level Think summary is seeded once in sendPrompt.
                    // After that segment has been committed to the timeline, later
                    // agent turns must not repeat the same request summary.
                    if (existingIndex < 0) return@update current
                    val reasoning = ActivityItem(
                        title = "Think",
                        detail = current.liveProcess.getOrNull(existingIndex)?.detail
                            ?: requestPlanningSummary(current.currentTaskRequest.orEmpty(), current.agentKind),
                        isComplete = false,
                    )
                    val process = if (existingIndex >= 0) {
                        current.liveProcess.toMutableList().also { it[existingIndex] = reasoning }
                    } else {
                        current.liveProcess + reasoning
                    }
                    current.copy(
                        liveProcess = process,
                        liveThinking = true,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.ReasoningSummary -> {
                    val summary = event.summary.trim()
                    val process = current.liveProcess.toMutableList()
                    val existingIndex = process.indexOfLast { !it.isComplete && it.title == "Think" }
                    if (event.startsNewBlock) {
                        process.indices.forEach { index ->
                            if (!process[index].isComplete) process[index] = process[index].copy(isComplete = true)
                        }
                        val initial = summary.ifBlank { str(R.string.vm_thinking) }
                        val replaceFallback = current.activeThinkingBlockId == null &&
                            process.size == 1 && process.first().title == "Think"
                        if (replaceFallback) {
                            process[0] = ActivityItem("Think", initial, event.isFinal)
                        } else {
                            process += ActivityItem("Think", initial, event.isFinal)
                        }
                    } else if (current.activeThinkingBlockId == event.blockId && existingIndex >= 0 && summary.isNotBlank()) {
                        process[existingIndex] = process[existingIndex].copy(
                            detail = summary,
                            isComplete = event.isFinal,
                        )
                    } else {
                        return@update current
                    }
                    current.copy(
                        liveProcess = process,
                        liveThinking = !event.isFinal,
                        activeThinkingBlockId = if (event.isFinal) null else event.blockId,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.ToolStarted -> {
                    val planned = current.copy(
                        liveProcess = current.liveProcess.map { item ->
                            if (!item.isComplete) item.copy(isComplete = true) else item
                        },
                        liveThinking = false,
                        activeThinkingBlockId = null,
                        activity = (listOf(
                            ActivityItem("Running ${event.toolName}", event.detail, false, isCommand = event.toolName == "Bash"),
                        ) + current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it })
                            .take(MAX_VISIBLE_WORK_ITEMS),
                    )
                    appendWorkItem(
                        planned,
                        ActivityItem("Running ${event.toolName}", event.detail, false, isCommand = event.toolName == "Bash"),
                    )
                }
                is RuntimeEvent.ToolProgress -> {
                    val runningIndex = current.liveProcess.indexOfLast {
                        !it.isComplete && it.title == "Running ${event.toolName}"
                    }
                    if (runningIndex < 0) current else current.copy(
                        liveProcess = current.liveProcess.toMutableList().also { items ->
                            items[runningIndex] = boundedActivityItem(
                                items[runningIndex].copy(detail = event.detail),
                            )
                        },
                    )
                }
                is RuntimeEvent.RuntimeLog -> appendWorkItem(
                    current.copy(activity = listOf(ActivityItem(event.title, event.detail)) + current.activity),
                    ActivityItem(event.title, event.detail),
                )
                is RuntimeEvent.ToolRequested -> appendWorkItem(current.copy(
                    pendingApproval = event.request,
                    activity = listOf(ActivityItem(str(R.string.vm_waiting_approval), event.request.explanation, false)) + current.activity,
                ), ActivityItem(str(R.string.vm_waiting_approval), event.request.explanation, false))
                is RuntimeEvent.ToolApproved -> appendWorkItem(current.copy(
                    pendingApproval = null,
                    activity = listOf(ActivityItem(str(R.string.vm_applying_approved), str(R.string.vm_editing_project_files), false)) + current.activity,
                ), ActivityItem(str(R.string.vm_action_approved), str(R.string.vm_agent_continuing, current.agentKind.localizedTitle(getApplication())), false))
                is RuntimeEvent.ToolRejected -> appendWorkItem(current.copy(
                    pendingApproval = null,
                ), ActivityItem(str(R.string.vm_action_rejected), str(R.string.vm_agent_continue_without, current.agentKind.localizedTitle(getApplication()))))
                is RuntimeEvent.ToolCompleted -> {
                    // Bridges fill empty tool results with English placeholders; translate for display.
                    val summary = when (event.summary) {
                        "Tool failed" -> str(R.string.md_tool_failed)
                        "Completed successfully" -> str(R.string.md_tool_completed_successfully)
                        else -> event.summary
                    }
                    val runningIndex = current.liveProcess.indexOfLast {
                        !it.isComplete && it.title == "Running ${event.toolName}"
                    }
                    val process = if (runningIndex >= 0) {
                        current.liveProcess.toMutableList().also { items ->
                            val runningItem = items[runningIndex]
                            items[runningIndex] = ActivityItem(
                                "${event.toolName} completed",
                                runningItem.detail.ifBlank { summary },
                                isCommand = event.toolName == "Bash",
                            ).let(::boundedActivityItem)
                        }
                    } else {
                        current.liveProcess + boundedActivityItem(ActivityItem(
                            "${event.toolName} completed",
                            summary,
                            isCommand = event.toolName == "Bash",
                        ))
                    }
                    current.copy(
                        activity = (listOf(ActivityItem(summary, event.toolName)) + current.activity)
                            .take(MAX_VISIBLE_WORK_ITEMS),
                        liveProcess = process.takeLast(MAX_VISIBLE_WORK_ITEMS),
                        liveThinking = false,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.FilesChanged -> current.copy(
                    changes = event.changes,
                    liveThinking = false,
                    liveProcess = if (event.paths.isEmpty()) current.liveProcess else current.liveProcess +
                        ActivityItem(
                            str(R.string.vm_files_changed),
                            event.paths.take(4).joinToString(", ") + if (event.paths.size > 4) " " + str(R.string.vm_files_more, event.paths.size - 4) else "",
                        ),
                    workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                )
                is RuntimeEvent.PreviewStarted -> current.copy(
                    previewReady = true,
                    previewUrl = event.url,
                    activity = listOf(ActivityItem(str(R.string.vm_preview_ready), event.url)) + current.activity,
                    liveProcess = current.liveProcess + ActivityItem(str(R.string.vm_preview_ready), event.url),
                    liveThinking = false,
                    workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                )
                is RuntimeEvent.SessionCompleted -> {
                    if (autonomousImplementerTerminal) {
                        current.copy(
                            activeSessionId = null,
                            liveThinking = false,
                            activity = listOf(ActivityItem("Implementation complete", "Starting independent review.")) +
                                current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                        )
                    } else {
                    val finishedAt = System.currentTimeMillis()
                    attachTaskDuration(finishWorkSegment(current, finishedAt), finishedAt).copy(
                        isRunning = false,
                        activeSessionId = null,
                        activity = listOf(ActivityItem(str(R.string.vm_task_completed), str(R.string.vm_agent_finished, current.agentKind.localizedTitle(getApplication())))) +
                            current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                        taskFinishedAtMillis = finishedAt,
                        currentTaskRequest = null,
                    )
                    }
                }
                is RuntimeEvent.SessionFailed -> {
                    val finishedAt = System.currentTimeMillis()
                    val displayReason = localizedFailureReason(event.reason)
                    val failedPrompt = activeRuntimeRequest?.prompt.orEmpty().ifBlank { _state.value.currentTaskRequest.orEmpty() }
                    val currentAgent = current.agentKind
                    val alternateAgent = when {
                        currentAgent == AgentKind.DEEPSEEK_HARNESS && installer.isAgentInstalled(AgentKind.CLAUDE_CODE) -> AgentKind.CLAUDE_CODE
                        currentAgent == AgentKind.DEEPSEEK_HARNESS && installer.isAgentInstalled(AgentKind.ANTIGRAVITY) -> AgentKind.ANTIGRAVITY
                        currentAgent == AgentKind.CLAUDE_CODE && installer.isAgentInstalled(AgentKind.DEEPSEEK_HARNESS) -> AgentKind.DEEPSEEK_HARNESS
                        currentAgent == AgentKind.ANTIGRAVITY && installer.isAgentInstalled(AgentKind.DEEPSEEK_HARNESS) -> AgentKind.DEEPSEEK_HARNESS
                        else -> null
                    }
                    val candidate = if (failedPrompt.isNotBlank() && alternateAgent != null) {
                        val targetProfile = preferences.loadProvider(vault, alternateAgent)
                        HandoverCandidate(
                            prompt = failedPrompt,
                            failedAgent = currentAgent,
                            failedReason = displayReason,
                            targetAgent = alternateAgent,
                            targetProvider = targetProfile,
                        )
                    } else null
                    attachTaskDuration(
                        finishWorkSegment(
                            appendWorkItem(current, ActivityItem("Task stopped", displayReason)),
                            finishedAt,
                        ),
                        finishedAt,
                    ).copy(
                        isRunning = false,
                        activeSessionId = null,
                        pendingApproval = null,
                        handoverCandidate = candidate,
                        toastMessage = displayReason.takeIf {
                            event.reason.contains("user not found", true) ||
                                event.reason.contains("API key", true) ||
                                event.reason.contains("authentication", true)
                        },
                        activity = listOf(ActivityItem("Task stopped", displayReason)) + current.activity,
                        taskFinishedAtMillis = finishedAt,
                        currentTaskRequest = null,
                    )
                }
            }
        }
        if (terminalEventForActiveSession) {
            activeRuntimeRequest = null
            failedApiKeyIds.clear()
            steeringToFollowUp = false
            if (pendingFollowUps.isNotEmpty()) {
                viewModelScope.launch {
                    delay(150)
                    if (!_state.value.isRunning) startNextFollowUp()
                }
            }
        }
        if (event is RuntimeEvent.FilesChanged || event is RuntimeEvent.SessionCompleted) {
            _state.value.activeProject?.id?.let { projectId ->
                if (event is RuntimeEvent.FilesChanged) workspaceDelegate.invalidateFolderMetadata(projectId, event.paths)
                touchProject(projectId)
            }
            refreshProjectFiles()
        }
        // Save every visible reasoning/tool transition, not only assistant text and
        // final results. If Android kills the process, the last displayed timeline
        // is restored as an interrupted work block rather than disappearing.
        persistMessages(includeLiveProcess = true)
    }

    fun resumeOrchestratorForProject(projectId: String): Boolean {
        val activeTask = preferences.dbBridge.loadActiveTaskForProject(projectId) ?: return false
        val taskState = runCatching { TaskState.valueOf(activeTask.state) }.getOrDefault(TaskState.QUEUED)
        val taskContext = TaskContext(
            taskId = activeTask.id,
            projectId = activeTask.project_id,
            title = activeTask.title
        )
        val initialSnapshot = TaskSnapshot(
            taskId = activeTask.id,
            state = taskState,
            currentStep = activeTask.step.toInt(),
            repairAttempts = activeTask.repair_attempts.toInt(),
            evidenceList = emptyList(),
            failureReason = activeTask.report_md
        )
        val orchestrator = TaskOrchestrator(
            context = taskContext,
            initialSnapshot = initialSnapshot,
            onTransition = { snapshot ->
                preferences.dbBridge.updateTaskState(
                    id = snapshot.taskId,
                    state = snapshot.state.name,
                    step = snapshot.currentStep.toLong(),
                    repairAttempts = snapshot.repairAttempts.toLong(),
                    endedAt = if (snapshot.state == TaskState.COMPLETED ||
                        snapshot.state == TaskState.FAILED ||
                        snapshot.state == TaskState.CANCELLED) System.currentTimeMillis() else null,
                    reportMd = snapshot.failureReason
                )
            }
        )
        activeTaskOrchestrator = orchestrator
        return orchestrator.resume()
    }

    fun getActiveTaskSnapshot(): TaskSnapshot? = activeTaskOrchestrator?.snapshot?.value

    private fun retryWithNextApiKey(event: RuntimeEvent.SessionFailed): Boolean {
        val current = _state.value
        if (!current.isRunning || current.activeSessionId != event.sessionId) return false
        val request = activeRuntimeRequest ?: return false

        // 1. Antigravity credit exhaustion fallback
        if (current.agentKind == AgentKind.ANTIGRAVITY) {
            val isCreditFailure = event.reason.contains("credit", ignoreCase = true) ||
                event.reason.contains("quota", ignoreCase = true) ||
                event.reason.contains("out of credits", ignoreCase = true) ||
                event.reason.contains("not logged in", ignoreCase = true) ||
                event.reason.contains("sign-in", ignoreCase = true)

            if (isCreditFailure) {
                val candidateKinds = listOf(ProviderKind.GEMINI, ProviderKind.LLM_ROUTER, ProviderKind.DEEPSEEK)
                val fallbackKind = candidateKinds.firstOrNull { vault.get(it.name).isNullOrBlank().not() }
                if (fallbackKind != null) {
                    val targetAgent = if (installer.isAgentInstalled(AgentKind.DEEPSEEK_HARNESS)) AgentKind.DEEPSEEK_HARNESS else AgentKind.CLAUDE_CODE
                    val fallbackProfile = preferences.loadProvider(vault, targetAgent).copy(kind = fallbackKind)
                    preferences.agentKind = targetAgent.stableId
                    preferences.saveProvider(fallbackProfile, targetAgent)

                    _state.update {
                        it.copy(
                            agentKind = targetAgent,
                            provider = fallbackProfile,
                            activeSessionId = null,
                            toastMessage = "Antigravity credits unavailable. Auto-switched to ${fallbackKind.title} via API key.",
                            liveProcess = it.liveProcess + ActivityItem("Route Fallback", "Antigravity credit limit reached. Continuing with ${fallbackKind.title}.", true),
                        )
                    }
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(400)
                        activeRuntime().startSession(
                            request.project.id,
                            request.project.slug,
                            request.project.kind,
                            request.prompt,
                            request.history,
                            fallbackProfile,
                        )
                    }
                    return true
                }
            }
            return false
        }

        // 2. Intra-provider model fallback if quota/rate limit exhausted on current model
        if (isApiKeyFailure(event.reason)) {
            val fallbackModel = nextModelFallback(request.provider.kind, request.provider.model)
            if (fallbackModel != null && fallbackModel != request.provider.model) {
                val nextModelProfile = request.provider.copy(model = fallbackModel)
                preferences.saveProvider(nextModelProfile, current.agentKind)
                _state.update {
                    it.copy(
                        provider = nextModelProfile,
                        activeSessionId = null,
                        toastMessage = "Quota reached for ${request.provider.model}. Auto-switched to backup model $fallbackModel.",
                        liveProcess = it.liveProcess + ActivityItem("Model Fallback", "Switching to $fallbackModel.", true),
                    )
                }
                viewModelScope.launch {
                    kotlinx.coroutines.delay(300)
                    activeRuntime().startSession(
                        request.project.id,
                        request.project.slug,
                        request.project.kind,
                        request.prompt,
                        request.history,
                        nextModelProfile,
                    )
                }
                return true
            }
        }

        // 3. Multi-key pool rotation for active provider
        if (!isApiKeyFailure(event.reason)) return false
        val credentials = vault.credentials(request.provider.kind.name)
        val active = credentials.firstOrNull { it.isActive } ?: return false
        failedApiKeyIds += active.id
        keyPoolManager.reportRateLimit(active.id, 60_000L)

        val candidate = keyPoolManager.acquireCandidateKey(request.provider.kind.name)
        val nextKeyId = candidate?.id ?: credentials.firstOrNull { it.id !in failedApiKeyIds }?.id
        if (nextKeyId != null && vault.activate(request.provider.kind.name, nextKeyId)) {
            val nextName = credentials.firstOrNull { it.id == nextKeyId }?.name ?: candidate?.label ?: "Backup Key"
            _state.update {
                it.copy(
                    activeSessionId = null,
                    activeApiKeyName = nextName,
                    toastMessage = str(R.string.vm_key_switched_toast, active.name, nextName),
                    liveProcess = it.liveProcess + ActivityItem(str(R.string.vm_api_key_switched), str(R.string.vm_using_key, nextName), true),
                )
            }
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                request.runtime.startSession(
                    request.project.id,
                    request.project.slug,
                    request.project.kind,
                    request.prompt,
                    request.history,
                    request.provider,
                )
            }
            return true
        }

        // 4. Multi-provider cascade if all keys and models for active provider are exhausted
        val fallbackCandidates = listOf(
            ProviderKind.GEMINI,
            ProviderKind.LLM_ROUTER,
            ProviderKind.NVIDIA_NIM,
            ProviderKind.GROQ,
            ProviderKind.DEEPSEEK,
            ProviderKind.OLLAMA,
        ).filter {
            it != request.provider.kind && (
                it == ProviderKind.OLLAMA ||
                it == ProviderKind.LLM_ROUTER ||
                it == ProviderKind.NVIDIA_NIM ||
                vault.get(it.name).isNullOrBlank().not()
            )
        }
        val nextProviderKind = fallbackCandidates.firstOrNull()
        if (nextProviderKind != null) {
            val nextProfile = preferences.loadProvider(vault, current.agentKind).copy(
                kind = nextProviderKind,
                model = nextProviderKind.defaultModel,
                baseUrl = nextProviderKind.defaultBaseUrl,
            )
            preferences.saveProvider(nextProfile, current.agentKind)
            _state.update {
                it.copy(
                    provider = nextProfile,
                    activeSessionId = null,
                    toastMessage = "Quota exhausted on ${request.provider.kind.title}. Falling back to ${nextProviderKind.title} (${nextProfile.model}).",
                    liveProcess = it.liveProcess + ActivityItem("Provider Fallback", "Auto-switched to ${nextProviderKind.title} (${nextProfile.model}).", true),
                )
            }
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                activeRuntime().startSession(
                    request.project.id,
                    request.project.slug,
                    request.project.kind,
                    request.prompt,
                    request.history,
                    nextProfile,
                )
            }
            return true
        }

        return false
    }

    private fun nextModelFallback(kind: ProviderKind, currentModel: String): String? = when (kind) {
        ProviderKind.GEMINI -> when (currentModel) {
            "gemini-4-argon" -> "gemini-3.8-flash"
            "gemini-3.8-flash" -> "gemini-3.6-flash"
            "gemini-3.6-flash" -> "gemini-3.5-flash-lite"
            "gemini-3.5-flash-lite" -> "gemini-2.5-flash"
            else -> null
        }
        ProviderKind.LLM_ROUTER -> when (currentModel) {
            "cohere/north-mini-code:free" -> "recursive/flint-v1.0:free"
            "recursive/flint-v1.0:free" -> "nvidia/nemotron-3-ultra-550b-a55b:free"
            "nvidia/nemotron-3-ultra-550b-a55b:free" -> "openrouter/free"
            "openrouter/free" -> "meta-llama/llama-3.3-70b-instruct:free"
            else -> if (currentModel != "openrouter/free") "openrouter/free" else null
        }
        ProviderKind.NVIDIA_NIM -> when (currentModel) {
            "nemotron-3.5-super-vl-120b-a12b" -> "nemotron-3.5-lightning-30b-a3b"
            "nemotron-3.5-lightning-30b-a3b" -> "minimax-m3.1-flash"
            "minimax-m3.1-flash" -> "meta/llama-3.3-70b-instruct"
            else -> null
        }
        ProviderKind.GROQ -> when (currentModel) {
            "openai/gpt-oss-120b" -> "openai/gpt-oss-20b"
            "openai/gpt-oss-20b" -> "qwen/qwen3.8-27b"
            else -> null
        }
        else -> null
    }

    fun handoverTask() {
        val candidate = _state.value.handoverCandidate ?: return
        _state.update { it.copy(handoverCandidate = null) }
        preferences.agentKind = candidate.targetAgent.stableId
        preferences.saveProvider(candidate.targetProvider, candidate.targetAgent)
        _state.update {
            it.copy(
                agentKind = candidate.targetAgent,
                provider = candidate.targetProvider,
                toastMessage = "Handing over to ${candidate.targetAgent.title}...",
                liveProcess = it.liveProcess + ActivityItem("Agent Handover", "Continuing task with ${candidate.targetAgent.title}.", true),
            )
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            sendPrompt(candidate.prompt)
        }
    }

    fun dismissHandover() {
        _state.update { it.copy(handoverCandidate = null) }
    }

    /**
     * Runtime bridges report failures in English (matched by [isApiKeyFailure] and the toast
     * check), so known reasons are translated only for display.
     */
    private fun localizedFailureReason(reason: String): String {
        val trimmed = reason.trim()
        Regex("^No API key is saved for (.+)\\.$").matchEntire(trimmed)?.let { match ->
            val name = match.groupValues[1]
            val kind = ProviderKind.entries.firstOrNull { it.title == name }
            return str(R.string.md_reason_no_api_key, kind?.localizedTitle(getApplication()) ?: name)
        }
        Regex("^(Claude Code|DeepSeek Harness) stopped with exit code (-?\\d+)$").matchEntire(trimmed)?.let { match ->
            return str(R.string.md_reason_agent_exit_code, match.groupValues[1], match.groupValues[2].toInt())
        }
        Regex("^Antigravity exited with code (-?\\d+)$").matchEntire(trimmed)?.let { match ->
            return str(R.string.md_reason_antigravity_exit_code, match.groupValues[1].toInt())
        }
        return when (trimmed) {
            "Stopped by user" -> str(R.string.md_reason_stopped_by_user)
            "User not found. Check the API key and provider account." -> str(R.string.md_reason_user_not_found)
            "The provider rejected the saved API key." -> str(R.string.md_reason_key_rejected)
            "Runtime verification failed. Nothing unverified was executed." -> str(R.string.md_reason_verification_failed)
            "The real Claude Code runtime could not start." -> str(R.string.md_reason_claude_could_not_start)
            "Claude Code reported an error" -> str(R.string.md_reason_claude_reported_error)
            "Antigravity CLI is not installed." -> str(R.string.md_reason_antigravity_cli_missing_short)
            "Antigravity needs Google sign-in. Open Settings → Coding agent." -> str(R.string.md_reason_antigravity_signin)
            "Your Antigravity account is out of credits. Check the account plan or wait for credits to reset." -> str(R.string.md_reason_antigravity_no_credits)
            "Antigravity reached the 60-minute task limit. Your files were kept." -> str(R.string.md_reason_antigravity_time_limit)
            "The selected Antigravity model is unavailable. Refresh models in Settings." -> str(R.string.md_reason_antigravity_model_unavailable)
            "Antigravity could not complete the task." -> str(R.string.md_reason_antigravity_failed)
            "Claude subscription login is not supported by DeepSeek Harness. Pick a key-based provider in Settings." -> str(R.string.vm_dsh_claude_unsupported)
            "DeepSeek Harness stopped before processing the prompt" -> str(R.string.md_reason_dsh_stopped_before_prompt)
            "No API key reached DeepSeek Harness. Re-save the provider key in Settings." -> str(R.string.md_reason_dsh_no_key)
            "DeepSeek Harness could not start." -> str(R.string.md_reason_dsh_could_not_start)
            "DeepSeek Harness reported an unspecified error" -> str(R.string.md_reason_dsh_unspecified)
            "DeepSeek Harness turn failed" -> str(R.string.md_reason_dsh_turn_failed)
            "DeepSeek Harness was blocked from completing the task" -> str(R.string.md_reason_dsh_blocked)
            "No Claude subscription token is saved. Add one from Agent → AI provider." -> str(R.string.md_reason_no_claude_token)
            "DeepSeek Harness is not installed. Open Settings → Coding agent to install it." -> str(R.string.md_reason_dsh_not_installed)
            "Antigravity CLI is not installed. Open Settings → Coding agent to install it." -> str(R.string.md_reason_antigravity_not_installed)
            "Antigravity did not answer. Try again." -> str(R.string.vm_antigravity_no_answer)
            else -> reason
        }
    }

    private fun isApiKeyFailure(reason: String): Boolean {
        val value = reason.lowercase()
        return "api key" in value || "authentication" in value || "user not found" in value ||
            "http 401" in value || "http 403" in value || "http 429" in value ||
            "expired" in value || "quota" in value || "rate limit" in value ||
            "credits" in value || "credit" in value
    }

    private fun touchProject(projectId: String) {
        val now = System.currentTimeMillis()
        _state.update { current ->
            val updatedProjects = current.projects.map { p ->
                if (p.id == projectId) p.copy(updatedAtMillis = now) else p
            }
            val active = if (current.activeProject?.id == projectId) current.activeProject?.copy(updatedAtMillis = now) else current.activeProject
            current.copy(projects = updatedProjects, activeProject = active)
        }
        preferences.saveProjects(_state.value.projects)
    }

    override fun persistMessages(includeLiveProcess: Boolean) {
        val current = _state.value
        val project = current.activeProject ?: return
        val chatId = current.activeChatId ?: return
        val liveItems = if (includeLiveProcess) {
            current.liveProcess.takeLast(MAX_VISIBLE_WORK_ITEMS)
                .map(::boundedActivityItem)
                .filterNot(::isNoisyRuntimeItem)
        } else {
            emptyList()
        }
        val messages = if (liveItems.isEmpty() && !current.liveThinking) {
            current.messages
        } else {
            val startedAt = current.workSegmentStartedAtMillis ?: current.taskStartedAtMillis ?: System.currentTimeMillis()
            current.messages + ChatMessage(
                id = "interrupted-${current.activeSessionId ?: chatId}",
                fromUser = false,
                text = "",
                workItems = liveItems.map { it.copy(isComplete = true) } + ActivityItem(
                    str(R.string.vm_task_interrupted),
                    str(R.string.vm_task_interrupted_detail),
                ),
                workedMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
            )
        }
        transcriptWrites.trySend(TranscriptWrite(project.id, chatId, messages))
    }

    private fun boundedActivityItem(item: ActivityItem): ActivityItem {
        if (item.detail.length <= MAX_ACTIVITY_DETAIL_CHARS) return item
        return item.copy(
            detail = "… Earlier output omitted …\n" + item.detail.takeLast(MAX_ACTIVITY_DETAIL_CHARS),
        )
    }

    private fun updateActiveChatTitle(prompt: String) {
        val project = _state.value.activeProject ?: return
        val chatId = _state.value.activeChatId ?: return
        val now = System.currentTimeMillis()
        val newChatTitle = str(R.string.vm_new_chat)
        val title = prompt.replace(Regex("\\s+"), " ").trim().let {
            if (it.length <= 42) it else it.take(39).trimEnd() + "…"
        }
        _state.update { current ->
            val chats = current.projectChats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        title = if (chat.title == "New chat" || chat.title == newChatTitle) title else chat.title,
                        updatedAtMillis = now,
                    )
                } else chat
            }.sortedByDescending { it.updatedAtMillis }
            current.copy(projectChats = chats)
        }
    }
}
