package com.jarves.mh.ui

import android.os.SystemClock
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.runtime.AgentUpdateInfo
import com.jarves.mh.runtime.AndroidAction
import com.jarves.mh.runtime.AndroidApkInfo
import com.jarves.mh.runtime.AndroidBuildIssue
import com.jarves.mh.runtime.AndroidBuildPhase
import com.jarves.mh.runtime.AndroidBuildStage
import com.jarves.mh.runtime.AndroidHealthCheck
import com.jarves.mh.runtime.AndroidLogcatState
import com.jarves.mh.runtime.AntigravityAuthState
import com.jarves.mh.runtime.IntegrityScheduler
import com.jarves.mh.update.AppUpdateInfo
import java.util.UUID

enum class StartupStage { CHECKING, SETUP_REQUIRED, INSTALLING, MODEL_SETUP, INITIALIZING, READY, ERROR }

enum class ApiPingStatus { IDLE, PINGING, OK, FAILED }
enum class AppUpdateStatus { AVAILABLE, PERMISSION_REQUIRED, DOWNLOADING, INSTALLING, ERROR }
enum class GitHubAuthStatus { DISCONNECTED, STARTING, AWAITING_USER, CONNECTED, ERROR }

data class ExportSelection(
    val defaultIncluded: Boolean = true,
    val overrides: Map<String, Boolean> = emptyMap(),
) {
    fun includes(path: String): Boolean {
        val normalized = path.trim('/')
        return overrides.entries
            .asSequence()
            .filter { (rule, _) -> normalized == rule || normalized.startsWith("$rule/") }
            .maxByOrNull { it.key.length }
            ?.value ?: defaultIncluded
    }

    fun canContainIncluded(path: String): Boolean {
        val normalized = path.trim('/')
        return includes(normalized) || overrides.any { (rule, included) ->
            included && rule.startsWith("$normalized/")
        }
    }

    fun toggled(path: String, directory: Boolean): ExportSelection {
        val normalized = path.trim('/')
        val nextValue = !includes(normalized)
        val next = overrides.toMutableMap()
        if (directory) next.keys.filter { it.startsWith("$normalized/") }.forEach(next::remove)
        next[normalized] = nextValue
        return copy(overrides = next)
    }
}

data class DeveloperDiagnostics(
    val loading: Boolean = false,
    val appPssBytes: Long = 0L,
    val javaHeapBytes: Long = 0L,
    val javaHeapMaxBytes: Long = 0L,
    val activeProcesses: List<String> = emptyList(),
    val messageCount: Int = 0,
    val liveActivityCount: Int = 0,
    val terminalBufferBytes: Long = 0L,
    val buildLogBytes: Long = 0L,
    val logcatLines: Int = 0,
    val fileEntries: Int = 0,
    val runtimeCacheBytes: Long = 0L,
    val recentExit: String? = null,
    val refreshedAtMillis: Long? = null,
)

/** Counts UTF-8 storage without allocating a second byte array for large logs. */
internal fun utf8SizeInBytes(value: String): Long {
    var bytes = 0L
    var index = 0
    while (index < value.length) {
        val char = value[index]
        when {
            char.code < 0x80 -> bytes += 1
            char.code < 0x800 -> bytes += 2
            char.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate() -> {
                bytes += 4
                index += 1
            }
            else -> bytes += 3
        }
        index += 1
    }
    return bytes
}

enum class FileReadOnlyReason { TOO_LARGE, BINARY, INVALID_UTF8, UNSAFE, READ_ERROR }

data class OpenedFileState(
    val path: String,
    val content: String? = null,
    val draft: String = "",
    val loading: Boolean = true,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    val conflict: Boolean = false,
    val readOnlyReason: FileReadOnlyReason? = null,
    val originalFingerprint: String? = null,
    val lineEnding: String = "\n",
    val hadTrailingNewline: Boolean = false,
    val targetLine: Int? = null,
    val targetColumn: Int? = null,
)

data class TerminalOutputLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val output: String,
    val exitCode: Int = 0,
)

private val ANSI_TERMINAL_SEQUENCE = Regex("\\u001B(?:\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)|\\[[0-?]*[ -/]*[@-~]|[()][A-Z0-9])")

internal fun sanitizeTerminalOutput(text: String): String = text
    .replace(ANSI_TERMINAL_SEQUENCE, "")
    .filter { it == '\n' || it == '\r' || it == '\t' || it.code >= 0x20 }

internal val ANTIGRAVITY_MODEL_EFFORT = Regex("^(.*)-(low|medium|high)$")

internal fun antigravityEffortFromModel(model: String): String? =
    ANTIGRAVITY_MODEL_EFFORT.matchEntire(model)?.groupValues?.get(2)

internal fun antigravityModelWithEffort(model: String, effort: String): String? {
    val match = ANTIGRAVITY_MODEL_EFFORT.matchEntire(model) ?: return null
    return "${match.groupValues[1]}-$effort"
}

internal data class ProjectTerminalSnapshot(
    val lines: List<TerminalOutputLine> = emptyList(),
    val cwd: String = "/workspace",
)

internal data class ProjectTerminalResult(
    val output: String,
    val exitCode: Int,
    val cwd: String,
)

internal data class RuntimeRetryRequest(
    val runtime: com.jarves.mh.runtime.RuntimeBridge,
    val project: Project,
    val prompt: String,
    val history: List<ChatMessage>,
    val provider: ProviderProfile,
)

data class QueuedFollowUp(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val prompt: String,
    val attachments: List<ChatAttachment>,
)

internal data class TranscriptWrite(
    val projectId: String,
    val chatId: String,
    val messages: List<ChatMessage>,
)

internal data class ImportedZipProject(
    val project: Project,
    val sourceAttachment: ChatAttachment,
)

data class HandoverCandidate(
    val prompt: String,
    val failedAgent: AgentKind,
    val failedReason: String,
    val targetAgent: AgentKind,
    val targetProvider: ProviderProfile,
)

data class AppUiState(
    val startupStage: StartupStage = StartupStage.CHECKING,
    val startupProgress: Float = 0f,
    val startupMessage: String = "Checking this device…",
    val startupBytes: Pair<Long, Long>? = null,
    val startupLogs: List<String> = emptyList(),
    val startupIndeterminate: Boolean = false,
    val startupError: String? = null,
    val startupErrorIsOffline: Boolean = false,
    val showDetailedSetupProgress: Boolean = false,
    val onboardingComplete: Boolean = false,
    val backgroundSetupComplete: Boolean = false,
    val initialLanguageSelected: Boolean = false,
    val provider: ProviderProfile = ProviderProfile(ProviderKind.ANTHROPIC),
    val activeApiKeyName: String? = null,
    val themeMode: com.jarves.mh.ui.theme.AppThemeMode = com.jarves.mh.ui.theme.AppThemeMode.DARK,
    val languageCode: String = "system",
    val apiPingStatus: ApiPingStatus = ApiPingStatus.IDLE,
    val apiPingMessage: String? = null,
    val projects: List<Project> = emptyList(),
    val projectImporting: Boolean = false,
    val projectImportMessage: String? = null,
    val gitCloneRunning: Boolean = false,
    val gitCloneMessage: String? = null,
    val githubAuthStatus: GitHubAuthStatus = GitHubAuthStatus.DISCONNECTED,
    val githubLogin: String? = null,
    val githubUserCode: String? = null,
    val githubVerificationUri: String? = null,
    val githubMessage: String? = null,
    val githubRepositories: List<GitHubRepository> = emptyList(),
    val githubRepositoriesLoading: Boolean = false,
    val activeProject: Project? = null,
    val workspaceVisible: Boolean = false,
    val readOnlyProject: Project? = null,
    val readOnlyProjectChats: List<ProjectChat> = emptyList(),
    val readOnlyChatId: String? = null,
    val readOnlyMessages: List<ChatMessage> = emptyList(),
    val projectChats: List<ProjectChat> = emptyList(),
    val activeChatId: String? = null,
    val workspaceFiles: List<WorkspaceEntry> = emptyList(),
    val androidProjectDetected: Boolean = false,
    val filesLoading: Boolean = false,
    val diagnostics: DeveloperDiagnostics = DeveloperDiagnostics(),
    val openedFile: OpenedFileState? = null,
    val projectExportRunning: Boolean = false,
    val projectExportSucceededAtMillis: Long? = null,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change."),
    ),
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val pendingApproval: ToolRequest? = null,
    val changes: List<ChangeItem> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val liveProcess: List<ActivityItem> = emptyList(),
    val liveThinking: Boolean = false,
    val activeThinkingBlockId: Long? = null,
    val taskStartedAtMillis: Long? = null,
    val taskFinishedAtMillis: Long? = null,
    val workSegmentStartedAtMillis: Long? = null,
    val currentTaskRequest: String? = null,
    val previewReady: Boolean = false,
    val previewUrl: String? = null,
    val isRunning: Boolean = false,
    val queuedFollowUps: List<QueuedFollowUp> = emptyList(),
    val activeSessionId: String? = null,
    val handoverCandidate: HandoverCandidate? = null,
    val toastMessage: String? = null,
    val projectTerminalLines: List<TerminalOutputLine> = emptyList(),
    val projectTerminalLiveOutput: String = "",
    val projectTerminalRunning: Boolean = false,
    val projectTerminalCwd: String = "/workspace",
    val projectTerminalCommand: String? = null,
    val projectTerminalDraft: String? = null,
    val pendingTerminalCommand: String? = null,
    val suggestedProjectRoot: String? = null,
    val selectedDevStacks: Set<DevStack> = emptySet(),
    val installedDevStacks: Set<DevStack> = emptySet(),
    val devStackInstalling: DevStack? = null,
    val devStackRemoving: Boolean = false,
    val devStackMessage: String? = null,
    val devStackProgress: Float = 0f,
    val devStackBytes: Pair<Long, Long>? = null,
    val devStackBytesPerSecond: Long? = null,
    val agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    val primaryAgentKind: AgentKind = AgentKind.CLAUDE_CODE,
    val installedAgentVersions: Map<AgentKind, String> = emptyMap(),
    val agentInstalling: AgentKind? = null,
    val agentMessage: String? = null,
    val agentProgress: Float = 0f,
    val agentDownloadedBytes: Long? = null,
    val agentTotalBytes: Long? = null,
    val agentBytesPerSecond: Long? = null,
    val agentUpdates: Map<AgentKind, AgentUpdateInfo> = emptyMap(),
    val agentUpdatesChecking: Boolean = false,
    val agentUpdating: AgentKind? = null,
    val agentUpdateMessage: String? = null,
    val agentUpdateProgress: Float = 0f,
    val agentUpdateIndeterminate: Boolean = false,
    val agentUpdateDownloadedBytes: Long? = null,
    val agentUpdateTotalBytes: Long? = null,
    val agentUpdateBytesPerSecond: Long? = null,
    val antigravityAuth: AntigravityAuthState = AntigravityAuthState(),
    val antigravityModel: String = "",
    val antigravityEffort: String = "high",
    val antigravityModels: List<String> = emptyList(),
    val antigravityModelsLoading: Boolean = false,
    val androidBuildRunning: Boolean = false,
    val androidBuildMessage: String? = null,
    val androidBuildPhase: AndroidBuildPhase = AndroidBuildPhase.IDLE,
    val androidBuildAction: AndroidAction = AndroidAction.NONE,
    val androidBuildStage: AndroidBuildStage = AndroidBuildStage.IDLE,
    val androidBuildIssues: List<AndroidBuildIssue> = emptyList(),
    val androidBuildLog: String = "",
    val androidBuildStartedAtMillis: Long? = null,
    val androidBuildFinishedAtMillis: Long? = null,
    val androidBuildApkPath: String? = null,
    val androidBuildApkSizeBytes: Long? = null,
    val androidBuildRecentDurationsMillis: List<Long> = emptyList(),
    val androidApkInfo: AndroidApkInfo? = null,
    val androidHealthChecks: List<AndroidHealthCheck> = emptyList(),
    val androidHealthRunning: Boolean = false,
    val androidLogcat: AndroidLogcatState = AndroidLogcatState(),
    val appUpdate: AppUpdateInfo? = null,
    val appUpdateStatus: AppUpdateStatus? = null,
    val appUpdateDownloadedBytes: Long = 0L,
    val appUpdateTotalBytes: Long = -1L,
    val appUpdateError: String? = null,
    // --- Integrity check state ---
    val integrityResults: Map<String, IntegrityScheduler.IntegrityResult> = emptyMap(),
    val integrityChecking: Boolean = false,
    val integrityCheckIntervalMinutes: Long = 30,
)

