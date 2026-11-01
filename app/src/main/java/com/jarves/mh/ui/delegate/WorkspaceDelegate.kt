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

internal data class FolderMetadata(val modifiedAtMillis: Long, val childCount: Int)

class WorkspaceDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    internal val folderMetadataCache = ConcurrentHashMap<String, FolderMetadata>()
    internal val folderCountsInFlight = ConcurrentHashMap.newKeySet<String>()

    internal fun clearFolderMetadataCache() {
        folderMetadataCache.clear()
    }
    override fun refreshProjectFiles() {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(filesLoading = true) }
        viewModelScope.launch {
            val (entries, suggestedRoot, androidProjectDetected) = withContext(Dispatchers.IO) {
                Triple(
                    readWorkspaceDirectory(project, ""),
                    if (project.rootPath.isBlank()) projectDelegate.detectNestedProjectRoot(project) else null,
                    findAndroidProjectRoot(projectWorkspaceRoot(project)) != null,
                )
            }
            if (_state.value.activeProject?.id == project.id) {
                _state.update {
                    it.copy(
                        workspaceFiles = entries,
                        filesLoading = false,
                        suggestedProjectRoot = suggestedRoot,
                        androidProjectDetected = androidProjectDetected,
                    )
                }
                loadMissingFolderCounts(project, entries)
            }
        }
    }

    fun loadProjectDirectory(relativePath: String) {
        val project = _state.value.activeProject ?: return
        val existingEntries = _state.value.workspaceFiles
        viewModelScope.launch {
            val (children, updatedEntries) = withContext(Dispatchers.IO) {
                val loadedChildren = readWorkspaceDirectory(project, relativePath)
                val prefix = relativePath.trim('/').let { if (it.isBlank()) "" else "$it/" }
                val retained = existingEntries.filterNot { entry ->
                    entry.path.startsWith(prefix) && entry.path.removePrefix(prefix).let { !it.contains('/') }
                }
                val sorted = sortWorkspaceEntries((retained + loadedChildren).distinctBy { it.path }).map { entry ->
                    if (entry.path == relativePath) entry.copy(childCount = loadedChildren.size) else entry
                }
                loadedChildren to sorted
            }
            if (_state.value.activeProject?.id != project.id) return@launch
            _state.update { current -> current.copy(workspaceFiles = updatedEntries) }
            updateFolderMetadata(project, relativePath, children.size)
            loadMissingFolderCounts(project, children)
        }
    }

    fun openFile(entry: WorkspaceEntry) {
        if (entry.isDirectory) return
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(openedFile = OpenedFileState(path = entry.path)) }
        loadOpenedFile(project, entry.path)
    }

    internal fun loadOpenedFile(project: Project, path: String, targetLine: Int? = null, targetColumn: Int? = null) {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val file = safeWorkspaceFile(project, path)
                    ?: return@withContext OpenedFileState(path = path, loading = false, readOnlyReason = FileReadOnlyReason.UNSAFE)
                runCatching {
                    val tooLarge = file.length() > MAX_EDITABLE_FILE_BYTES
                    val bytes = if (tooLarge) file.readHeadBytes(MAX_EDITABLE_FILE_BYTES.toInt()) else file.readBytes()
                    if (bytes.any { it == 0.toByte() }) {
                        OpenedFileState(path = path, loading = false, readOnlyReason = FileReadOnlyReason.BINARY)
                    } else {
                        val decoder = Charsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                        val decoded = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }.getOrNull()
                        if (decoded == null) {
                            OpenedFileState(path = path, loading = false, readOnlyReason = FileReadOnlyReason.INVALID_UTF8)
                        } else {
                            val content = if (tooLarge) decoded + "\n\n…" else decoded
                            OpenedFileState(
                                path = path,
                                content = content,
                                draft = content,
                                loading = false,
                                readOnlyReason = FileReadOnlyReason.TOO_LARGE.takeIf { tooLarge },
                                originalFingerprint = if (tooLarge) null else fingerprint(file),
                                lineEnding = if (decoded.contains("\r\n")) "\r\n" else "\n",
                                hadTrailingNewline = decoded.endsWith("\n") || decoded.endsWith("\r"),
                            )
                        }
                    }
                }.getOrElse {
                    OpenedFileState(path = path, loading = false, readOnlyReason = FileReadOnlyReason.READ_ERROR)
                }
            }
            if (_state.value.openedFile?.path == path) {
                _state.update { it.copy(openedFile = loaded.copy(targetLine = targetLine, targetColumn = targetColumn)) }
            }
        }
    }

    fun closeFile() {
        _state.update { it.copy(openedFile = null) }
    }

    fun beginFileEdit() {
        val current = _state.value
        val opened = current.openedFile ?: return
        if (opened.loading || opened.readOnlyReason != null || opened.content == null ||
            current.isRunning || current.projectTerminalRunning || current.androidBuildRunning
        ) return
        _state.update { it.copy(openedFile = opened.copy(editing = true, draft = opened.content, dirty = false, conflict = false)) }
    }

    fun updateFileDraft(value: String) {
        val opened = _state.value.openedFile ?: return
        if (!opened.editing || opened.saving) return
        _state.update { it.copy(openedFile = opened.copy(draft = value, dirty = value != opened.content, conflict = false)) }
    }

    fun cancelFileEdit() {
        val opened = _state.value.openedFile ?: return
        _state.update { it.copy(openedFile = opened.copy(editing = false, draft = opened.content.orEmpty(), dirty = false, conflict = false)) }
    }

    fun reloadOpenedFile() {
        val project = _state.value.activeProject ?: return
        val path = _state.value.openedFile?.path ?: return
        _state.update { it.copy(openedFile = OpenedFileState(path = path)) }
        loadOpenedFile(project, path)
    }

    fun dismissFileConflict() {
        val opened = _state.value.openedFile ?: return
        _state.update { it.copy(openedFile = opened.copy(conflict = false, saving = false)) }
    }

    fun saveOpenedFile() {
        val current = _state.value
        val project = current.activeProject ?: return
        val opened = current.openedFile ?: return
        if (!opened.editing || opened.saving || !opened.dirty || current.isRunning ||
            current.projectTerminalRunning || current.androidBuildRunning
        ) return
        _state.update { it.copy(openedFile = opened.copy(saving = true, conflict = false)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val file = safeWorkspaceFile(project, opened.path) ?: error("unsafe_path")
                    if (opened.originalFingerprint == null || fingerprint(file) != opened.originalFingerprint) {
                        return@runCatching false
                    }
                    var normalized = opened.draft.replace("\r\n", "\n").replace('\r', '\n')
                    normalized = normalized.trimEnd('\n') + if (opened.hadTrailingNewline) "\n" else ""
                    if (opened.lineEnding == "\r\n") normalized = normalized.replace("\n", "\r\n")
                    val temporary = File(file.parentFile, ".${file.name}.${UUID.randomUUID()}.tmp")
                    try {
                        temporary.writeText(normalized, Charsets.UTF_8)
                        if (file.canExecute()) temporary.setExecutable(true, false)
                        runCatching {
                            Files.move(
                                temporary.toPath(), file.toPath(),
                                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
                            )
                        }.getOrElse {
                            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    } finally {
                        temporary.delete()
                    }
                    true
                }
            }
            when {
                result.getOrNull() == false -> _state.update {
                    it.copy(openedFile = it.openedFile?.copy(saving = false, conflict = true))
                }
                result.isSuccess -> {
                    _state.update { it.copy(toastMessage = str(R.string.file_saved)) }
                    invalidateFolderMetadata(project.id, listOf(opened.path))
                    refreshProjectFiles()
                    loadOpenedFile(project, opened.path)
                }
                else -> _state.update {
                    it.copy(
                        openedFile = it.openedFile?.copy(saving = false),
                        toastMessage = str(R.string.file_save_failed),
                    )
                }
            }
        }
    }


    private fun readWorkspaceDirectory(project: Project, relativePath: String): List<WorkspaceEntry> {
        val root = projectWorkspaceRoot(project)
        if (!root.isDirectory) return emptyList()
        val rootPath = root.canonicalFile.toPath()
        val directory = if (relativePath.isBlank()) root else File(root, relativePath)
        if (!directory.isDirectory || Files.isSymbolicLink(directory.toPath()) ||
            !runCatching { directory.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
        ) return emptyList()
        fun isVisibleSafeEntry(file: File): Boolean {
            val relative = file.relativeTo(root).invariantSeparatorsPath
            return !isClaudeRuntimeMetadata(relative) &&
                !Files.isSymbolicLink(file.toPath()) &&
                runCatching { file.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
        }
        return directory.listFiles().orEmpty()
            .asSequence()
            .filter(::isVisibleSafeEntry)
            .map { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                WorkspaceEntry(
                    path = relative,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    depth = relative.count { it == '/' },
                    sizeBytes = if (file.isFile) file.length() else 0,
                    childCount = if (file.isDirectory) {
                        folderMetadataCache[folderMetadataKey(project.id, relative)]
                            ?.takeIf { it.modifiedAtMillis == file.lastModified() }
                            ?.childCount
                    } else {
                        null
                    },
                )
            }
            .sortedWith(compareByDescending<WorkspaceEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            .toList()
    }

    private fun loadMissingFolderCounts(project: Project, entries: List<WorkspaceEntry>) {
        entries.asSequence()
            .filter { it.isDirectory && it.childCount == null && !isDeferredMetadataPath(it.path) }
            .forEach { entry ->
                val key = folderMetadataKey(project.id, entry.path)
                if (!folderCountsInFlight.add(key)) return@forEach
                viewModelScope.launch {
                    val metadata = withContext(Dispatchers.IO) {
                        val directory = safeWorkspaceDirectory(project, entry.path)
                        directory?.let {
                            FolderMetadata(
                                modifiedAtMillis = it.lastModified(),
                                childCount = countVisibleChildren(project, it),
                            )
                        }
                    }
                    folderCountsInFlight.remove(key)
                    if (metadata == null || _state.value.activeProject?.id != project.id) return@launch
                    folderMetadataCache[key] = metadata
                    _state.update { current ->
                        current.copy(
                            workspaceFiles = current.workspaceFiles.map { item ->
                                if (item.path == entry.path) item.copy(childCount = metadata.childCount) else item
                            },
                        )
                    }
                }
            }
    }

    private fun updateFolderMetadata(project: Project, relativePath: String, count: Int) {
        val directory = safeWorkspaceDirectory(project, relativePath) ?: return
        folderMetadataCache[folderMetadataKey(project.id, relativePath)] = FolderMetadata(
            modifiedAtMillis = directory.lastModified(),
            childCount = count,
        )
    }

    private fun countVisibleChildren(project: Project, directory: File): Int {
        val root = projectWorkspaceRoot(project).canonicalFile.toPath()
        return directory.listFiles().orEmpty().count { file ->
            val relative = runCatching { file.relativeTo(projectWorkspaceRoot(project)).invariantSeparatorsPath }
                .getOrNull() ?: return@count false
            !isClaudeRuntimeMetadata(relative) &&
                !Files.isSymbolicLink(file.toPath()) &&
                runCatching { file.canonicalFile.toPath().startsWith(root) }.getOrDefault(false)
        }
    }

    private fun safeWorkspaceDirectory(project: Project, relativePath: String): File? {
        val root = projectWorkspaceRoot(project).canonicalFile
        if (relativePath.isBlank()) return root.takeIf(File::isDirectory)
        return safeWorkspacePath(project, relativePath)?.takeIf(File::isDirectory)
    }

    private fun folderMetadataKey(projectId: String, path: String): String = "$projectId:${path.trim('/')}"

    internal fun invalidateFolderMetadata(projectId: String, paths: List<String>) {
        paths.forEach { changedPath ->
            val normalized = changedPath.trim('/')
            val parent = normalized.substringBeforeLast('/', "")
            folderMetadataCache.remove(folderMetadataKey(projectId, parent))
            folderMetadataCache.remove(folderMetadataKey(projectId, normalized))
        }
    }

    private fun isDeferredMetadataPath(path: String): Boolean {
        val segments = path.split('/')
        return segments.any { it in DEFERRED_FOLDER_METADATA_NAMES }
    }

    private fun sortWorkspaceEntries(entries: List<WorkspaceEntry>): List<WorkspaceEntry> {
        val childrenByParent = entries.groupBy { entry -> entry.path.substringBeforeLast('/', "") }
        val siblingOrder = compareByDescending<WorkspaceEntry> { it.isDirectory }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy { it.name }
        return buildList(entries.size) {
            fun appendChildren(parentPath: String) {
                childrenByParent[parentPath].orEmpty().sortedWith(siblingOrder).forEach { entry ->
                    add(entry)
                    appendChildren(entry.path)
                }
            }
            appendChildren("")
        }
    }

    internal fun isClaudeRuntimeMetadata(relativePath: String): Boolean {
        return relativePath == ".claude" ||
            relativePath == ".claude.json" ||
            relativePath.startsWith(".claude/")
    }

    private fun safeWorkspaceFile(project: Project, relativePath: String): File? {
        if (relativePath.isBlank() || isClaudeRuntimeMetadata(relativePath)) return null
        val root = projectWorkspaceRoot(project).canonicalFile
        var cursor = root
        for (segment in relativePath.replace('\\', '/').split('/')) {
            if (segment.isBlank() || segment == "." || segment == "..") return null
            cursor = File(cursor, segment)
            if (Files.isSymbolicLink(cursor.toPath())) return null
        }
        val canonical = runCatching { cursor.canonicalFile }.getOrNull() ?: return null
        return canonical.takeIf { it.toPath().startsWith(root.toPath()) && it.isFile }
    }

    private fun fingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ---- Manual file management (Files tab) ----

    /** Resolves a workspace path (file or folder) inside the project root, rejecting symlinks and traversal. */
    private fun safeWorkspacePath(project: Project, relativePath: String): File? {
        val root = projectWorkspaceRoot(project).canonicalFile
        val clean = relativePath.replace('\\', '/').trim('/')
        if (clean.isBlank()) return root
        if (isClaudeRuntimeMetadata(clean)) return null
        var cursor = root
        for (segment in clean.split('/')) {
            if (segment.isBlank() || segment == "." || segment == "..") return null
            cursor = File(cursor, segment)
            if (Files.isSymbolicLink(cursor.toPath())) return null
        }
        val canonical = runCatching { cursor.canonicalFile }.getOrNull() ?: return null
        return canonical.takeIf { it.toPath().startsWith(root.toPath()) }
    }

    private fun isValidEntryName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." && name.length <= 255 &&
            name.none { it == '/' || it == '\\' || it == '\u0000' } &&
            !isClaudeRuntimeMetadata(name)

    private fun fileOpsBlocked(): Boolean {
        val current = _state.value
        val busy = current.isRunning || current.projectTerminalRunning || current.androidBuildRunning ||
            current.projectExportRunning
        if (busy) _state.update { it.copy(toastMessage = str(R.string.fm_busy)) }
        return busy
    }

    private fun parentPathOf(path: String): String = path.trim('/').substringBeforeLast('/', "")

    private fun childPath(parent: String, name: String): String =
        parent.trim('/').let { if (it.isBlank()) name else "$it/$name" }

    /** Reloads one folder after a change; drops stale entries under a removed path first. */
    private fun reloadWorkspaceFolder(project: Project, parent: String, removedPath: String? = null) {
        invalidateFolderMetadata(project.id, listOfNotNull(parent, removedPath))
        if (removedPath != null) {
            _state.update { current ->
                current.copy(
                    workspaceFiles = current.workspaceFiles.filterNot {
                        it.path == removedPath || it.path.startsWith("$removedPath/")
                    },
                )
            }
        }
        loadProjectDirectory(parent)
        if (parent.isNotBlank()) {
            val grandParent = parentPathOf(parent)
            invalidateFolderMetadata(project.id, listOf(grandParent))
            loadProjectDirectory(grandParent)
        }
    }

    private fun runFileOperation(
        project: Project,
        reloadParent: String,
        removedPath: String? = null,
        operation: () -> String,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(operation) }
            if (_state.value.activeProject?.id != project.id) return@launch
            result
                .onSuccess { message -> _state.update { it.copy(toastMessage = message) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(toastMessage = str(R.string.fm_failed, error.message ?: str(R.string.vm_unknown_error)))
                    }
                }
            reloadWorkspaceFolder(project, reloadParent, removedPath.takeIf { result.isSuccess })
        }
    }

    fun createWorkspaceEntry(parentPath: String, name: String, isDirectory: Boolean) {
        val project = _state.value.activeProject ?: return
        val trimmed = name.trim()
        if (!isValidEntryName(trimmed)) {
            _state.update { it.copy(toastMessage = str(R.string.fm_invalid_name)) }
            return
        }
        if (fileOpsBlocked()) return
        runFileOperation(project, parentPath) {
            val parent = safeWorkspacePath(project, parentPath)?.takeIf(File::isDirectory)
                ?: error(str(R.string.fm_protected))
            val target = File(parent, trimmed)
            check(!target.exists()) { str(R.string.fm_exists, trimmed) }
            val created = if (isDirectory) target.mkdir() else target.createNewFile()
            check(created) { str(R.string.fm_protected) }
            str(R.string.fm_created, trimmed)
        }
    }

    fun renameWorkspaceEntry(path: String, newName: String) {
        val project = _state.value.activeProject ?: return
        val trimmed = newName.trim()
        if (!isValidEntryName(trimmed)) {
            _state.update { it.copy(toastMessage = str(R.string.fm_invalid_name)) }
            return
        }
        if (trimmed == path.substringAfterLast('/')) return
        if (fileOpsBlocked()) return
        val parentPath = parentPathOf(path)
        runFileOperation(project, parentPath, removedPath = path) {
            val source = safeWorkspacePath(project, path)?.takeIf { it.exists() && path.isNotBlank() }
                ?: error(str(R.string.fm_protected))
            val target = File(source.parentFile, trimmed)
            // Allow case-only renames on case-insensitive storage.
            check(!target.exists() || target.canonicalPath.equals(source.canonicalPath, ignoreCase = true)) {
                str(R.string.fm_exists, trimmed)
            }
            Files.move(source.toPath(), target.toPath())
            str(R.string.fm_renamed, trimmed)
        }
    }

    fun deleteWorkspaceEntry(path: String) {
        val project = _state.value.activeProject ?: return
        if (path.isBlank() || fileOpsBlocked()) return
        val name = path.substringAfterLast('/')
        runFileOperation(project, parentPathOf(path), removedPath = path) {
            val target = safeWorkspacePath(project, path)?.takeIf(File::exists) ?: error(str(R.string.fm_protected))
            check(target != projectWorkspaceRoot(project).canonicalFile) { str(R.string.fm_protected) }
            // deleteRecursively does not follow symlinks out of the folder; it deletes the link itself.
            check(target.deleteRecursively()) { str(R.string.fm_protected) }
            str(R.string.fm_deleted, name)
        }
    }

    fun uploadToWorkspace(parentPath: String, uris: List<Uri>) {
        val project = _state.value.activeProject ?: return
        if (uris.isEmpty() || fileOpsBlocked()) return
        val resolver = getApplication<Application>().contentResolver
        runFileOperation(project, parentPath) {
            val parent = safeWorkspacePath(project, parentPath)?.takeIf(File::isDirectory)
                ?: error(str(R.string.fm_protected))
            var uploaded = 0
            uris.forEach { uri ->
                var displayName = "upload"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                            ?.let { displayName = cursor.getString(it) ?: displayName }
                    }
                }
                val safeName = chatDelegate.sanitizeAttachmentName(displayName)
                val stem = safeName.substringBeforeLast('.', safeName)
                val extension = safeName.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
                var destination = File(parent, safeName)
                var suffix = 2
                while (destination.exists()) destination = File(parent, "$stem-${suffix++}$extension")
                var copied = 0L
                try {
                    resolver.openInputStream(uri)?.buffered()?.use { input ->
                        destination.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                copied += count
                                check(copied <= MAX_UPLOAD_BYTES) { str(R.string.fm_too_large, displayName) }
                                output.write(buffer, 0, count)
                            }
                        }
                    } ?: error(str(R.string.vm_read_named_failed, displayName))
                } catch (error: Throwable) {
                    destination.delete()
                    throw error
                }
                uploaded++
            }
            AppStrings.context(getApplication()).resources.getQuantityString(R.plurals.fm_uploaded, uploaded, uploaded)
        }
    }

    fun shareWorkspaceFile(path: String) {
        val project = _state.value.activeProject ?: return
        runCatching {
            val file = safeWorkspaceFile(project, path) ?: error(str(R.string.fm_protected))
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase())
                ?: "application/octet-stream"
            app.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }, str(R.string.fm_share_title, file.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { error ->
            _state.update { it.copy(toastMessage = str(R.string.fm_failed, error.message ?: str(R.string.vm_unknown_error))) }
        }
    }

    fun saveWorkspaceFileCopy(path: String, destination: Uri) {
        val project = _state.value.activeProject ?: return
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val file = safeWorkspaceFile(project, path) ?: error(str(R.string.fm_protected))
                    resolver.openOutputStream(destination, "wt")?.use { output ->
                        file.inputStream().buffered().use { it.copyTo(output) }
                    } ?: error(str(R.string.fm_protected))
                    file.name
                }
            }
            _state.update {
                it.copy(
                    toastMessage = result.fold(
                        onSuccess = { name -> str(R.string.fm_saved_copy, name) },
                        onFailure = { error -> str(R.string.fm_failed, error.message ?: str(R.string.vm_unknown_error)) },
                    ),
                )
            }
        }
    }


}
