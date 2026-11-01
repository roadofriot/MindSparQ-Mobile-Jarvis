@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.jarves.mh.ui

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarves.mh.BuildConfig
import com.jarves.mh.R
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.DEEPSEEK_HARNESS_PROVIDERS
import com.jarves.mh.model.DSH_PROTOCOL_PROVIDERS
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectType
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.inferredDshApiForUrl
import com.jarves.mh.model.projectSlug
import com.jarves.mh.model.providersForAgent
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.runtime.AndroidAction
import com.jarves.mh.runtime.AndroidBuildIssue
import com.jarves.mh.runtime.AndroidBuildStage
import com.jarves.mh.runtime.AndroidHealthCheck
import com.jarves.mh.runtime.AndroidHealthFix
import com.jarves.mh.runtime.AndroidHealthStatus
import com.jarves.mh.runtime.AndroidLogLevel
import com.jarves.mh.runtime.AndroidLogLine
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.runtime.RuntimeExecutionService
import com.jarves.mh.runtime.RuntimeSetupService
import com.jarves.mh.runtime.SupportedProjectTemplate
import com.jarves.mh.runtime.supportsArm64Runtime
import com.jarves.mh.ui.theme.AppLanguage
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.PocketBlue
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun WorkspaceScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onSteerFollowUp: (String) -> Unit,
    onRemoveFollowUp: (String) -> Unit,
    onApproval: (Boolean) -> Unit,
    onRefreshFiles: () -> Unit,
    onLoadProjectDirectory: (String) -> Unit,
    onCreateWorkspaceEntry: (String, String, Boolean) -> Unit,
    onRenameWorkspaceEntry: (String, String) -> Unit,
    onDeleteWorkspaceEntry: (String) -> Unit,
    onUploadToWorkspace: (String, List<Uri>) -> Unit,
    onShareWorkspaceFile: (String) -> Unit,
    onSaveWorkspaceFileCopy: (String, Uri) -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onCloseFile: () -> Unit,
    onBeginFileEdit: () -> Unit,
    onUpdateFileDraft: (String) -> Unit,
    onCancelFileEdit: () -> Unit,
    onSaveFile: () -> Unit,
    onReloadFile: () -> Unit,
    onDismissFileConflict: () -> Unit,
    onUndoChanges: () -> Unit,
    onKeepChanges: () -> Unit,
    onUndoFileChange: (String) -> Unit,
    onKeepFileChange: (String) -> Unit,
    onCreateChat: () -> Unit,
    onSwitchChat: (String) -> Unit,
    onTerminalRun: (String) -> Unit,
    onTerminalInput: (String) -> Unit,
    onTerminalInterrupt: () -> Unit,
    onTerminalPrepare: (String) -> Unit,
    onTerminalDraftConsumed: () -> Unit,
    onTerminalOpened: () -> Unit,
    onTerminalStop: () -> Unit,
    onTerminalClear: () -> Unit,
    onTerminalConfirm: () -> Unit,
    onTerminalCancel: () -> Unit,
    onUseSuggestedProjectRoot: () -> Unit,
    onExportProject: (Uri, ExportSelection) -> Unit,
    onAddAttachments: (List<Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenAttachment: (ChatAttachment) -> Unit,
    onBuildAndRunAndroid: () -> Unit,
    onAndroidAction: (AndroidAction) -> Unit,
    onRefreshAndroidDashboard: () -> Unit,
    onShareAndroidApk: () -> Unit,
    onSaveAndroidApk: (Uri) -> Unit,
    onOpenAndroidAppDetails: () -> Unit,
    onUninstallAndroidApp: () -> Unit,
    onOpenAndroidIssue: (AndroidBuildIssue) -> Unit,
    onAndroidHealthFix: (AndroidHealthFix) -> Unit,
    onRefreshAndroidHealth: () -> Unit,
    onStartAndroidLogcat: () -> Unit,
    onStopAndroidLogcat: () -> Unit,
    onToggleAndroidLogcatPause: () -> Unit,
    onClearAndroidLogcat: () -> Unit,
    onAndroidLogcatQuery: (String) -> Unit,
    onAndroidLogcatLevel: (AndroidLogLevel) -> Unit,
    onOpenAndroidLogLine: (AndroidLogLine) -> Unit,
    onCancelAndroidBuild: () -> Unit,
    onCleanAndroid: () -> Unit,
    onInstallAndroidTools: () -> Unit,
    onOpenAiHub: () -> Unit = {},
    onSelectAgent: (AgentKind) -> Unit = {},
    onExecuteHandover: () -> Unit = {},
    onDismissHandover: () -> Unit = {},
) {
    var agentMenuExpanded by remember { mutableStateOf(false) }
    var exportSelection by remember(state.activeProject?.id) { mutableStateOf(ExportSelection()) }
    var exportSelectionMode by remember(state.activeProject?.id) { mutableStateOf(false) }
    BackHandler {
        when {
            state.openedFile != null -> onCloseFile()
            exportSelectionMode -> exportSelectionMode = false
            else -> onBack()
        }
    }
    val context = LocalContext.current
    val isAndroidProject = state.androidProjectDetected
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var pendingAndroidAction by remember { mutableStateOf<AndroidAction?>(null) }
    val exportProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> if (uri != null) onExportProject(uri, exportSelection) },
    )
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAddAttachments,
    )
    var pendingUploadParent by rememberSaveable { mutableStateOf<String?>(null) }
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            pendingUploadParent?.let { parent -> onUploadToWorkspace(parent, uris) }
            pendingUploadParent = null
        },
    )
    var pendingCopyPath by rememberSaveable { mutableStateOf<String?>(null) }
    val saveCopyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            val path = pendingCopyPath
            if (uri != null && path != null) onSaveWorkspaceFileCopy(path, uri)
            pendingCopyPath = null
        },
    )
    var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { saved ->
            val uri = pendingPhotoUri?.let(Uri::parse)
            pendingPhotoUri = null
            if (saved && uri != null) onAddAttachments(listOf(uri))
        },
    )
    val unknownAppsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                pendingAndroidAction?.let(onAndroidAction)
            } else {
                Toast.makeText(context, context.getString(R.string.ws_allow_app_installs), Toast.LENGTH_LONG).show()
            }
            pendingAndroidAction = null
        },
    )
    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
        onResult = { uri -> if (uri != null) onSaveAndroidApk(uri) },
    )
    val chatListState = rememberLazyListState()
    var userScrolledUp by rememberSaveable { mutableStateOf(false) }

    val chatItemCount = state.messages.size +
        (if (state.liveProcess.isNotEmpty() || state.liveThinking) 1 else 0) +
        (if (state.pendingApproval != null) 1 else 0)

    LaunchedEffect(state.activeChatId) {
        userScrolledUp = false
        if (chatItemCount > 0) chatListState.scrollToItem(chatItemCount - 1)
    }

    // When the user actively scrolls/touches the screen, detect if they scrolled up to read thinking/messages.
    LaunchedEffect(chatListState.isScrollInProgress) {
        if (chatListState.isScrollInProgress) {
            if (chatListState.canScrollForward) {
                userScrolledUp = true
            }
        } else {
            // If user scrolled back down to the very bottom, re-enable follow mode
            if (!chatListState.canScrollForward) {
                userScrolledUp = false
            }
        }
    }

    // Follow new tokens/updates only when user is at the bottom and has not scrolled up to read.
    LaunchedEffect(
        state.messages.size,
        state.messages.lastOrNull()?.text?.length,
        state.liveProcess.size,
        state.liveProcess.lastOrNull()?.detail,
        state.pendingApproval,
    ) {
        if (!state.isRunning || chatItemCount <= 0 || userScrolledUp || chatListState.isScrollInProgress) return@LaunchedEffect
        if (!chatListState.canScrollForward) {
            chatListState.scrollToItem(chatItemCount - 1)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(WorkspaceTab.CHAT) }
    var showChats by rememberSaveable { mutableStateOf(false) }
    var showAndroidBuild by rememberSaveable { mutableStateOf(false) }
    val activeChat = state.projectChats.firstOrNull { it.id == state.activeChatId }
    val requestAndroidAction: (AndroidAction) -> Unit = { action ->
        val mayInstall = action in setOf(AndroidAction.INSTALL, AndroidAction.RUN, AndroidAction.BUILD_AND_RUN)
        if (mayInstall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            pendingAndroidAction = action
            unknownAppsLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        } else {
            onAndroidAction(action)
        }
    }

    LaunchedEffect(state.projectExportSucceededAtMillis) {
        if (state.projectExportSucceededAtMillis != null) {
            exportSelectionMode = false
            exportSelection = ExportSelection()
        }
    }

    if (showAndroidBuild) {
        AndroidDashboardScreen(
            state = state,
            onClose = { onStopAndroidLogcat(); showAndroidBuild = false },
            onAction = requestAndroidAction,
            onCancelBuild = onCancelAndroidBuild,
            onRefresh = onRefreshAndroidDashboard,
            onInstallTools = onInstallAndroidTools,
            onShareApk = onShareAndroidApk,
            onSaveApk = { state.androidApkInfo?.let { saveApkLauncher.launch(it.fileName) } },
            onOpenAppDetails = onOpenAndroidAppDetails,
            onUninstallApp = onUninstallAndroidApp,
            onOpenIssue = { issue -> showAndroidBuild = false; onOpenAndroidIssue(issue) },
            onHealthFix = onAndroidHealthFix,
            onRefreshHealth = onRefreshAndroidHealth,
            onStartLogcat = onStartAndroidLogcat,
            onStopLogcat = onStopAndroidLogcat,
            onToggleLogcatPause = onToggleAndroidLogcatPause,
            onClearLogcat = onClearAndroidLogcat,
            onLogcatQuery = onAndroidLogcatQuery,
            onLogcatLevel = onAndroidLogcatLevel,
            onOpenLogLine = { line -> showAndroidBuild = false; onOpenAndroidLogLine(line) },
        )
        return
    }

    // If a file is open, show the FileViewerScreen on top
    if (state.openedFile != null) {
        FileViewerScreen(
            file = state.openedFile,
            editingAllowed = !state.isRunning && !state.projectTerminalRunning && !state.androidBuildRunning,
            onClose = {
                onCloseFile()
                selectedTab = WorkspaceTab.FILES
            },
            onBeginEdit = onBeginFileEdit,
            onDraftChange = onUpdateFileDraft,
            onCancelEdit = onCancelFileEdit,
            onSave = onSaveFile,
            onReload = onReloadFile,
            onDismissConflict = onDismissFileConflict,
        )
        return
    }

    if (showChats) {
        ChatSwitcherDialog(
            chats = state.projectChats,
            activeChatId = state.activeChatId,
            switchingEnabled = !state.isRunning,
            onDismiss = { showChats = false },
            onCreate = {
                onCreateChat()
                showChats = false
                selectedTab = WorkspaceTab.CHAT
            },
            onSwitch = { chatId ->
                onSwitchChat(chatId)
                showChats = false
                selectedTab = WorkspaceTab.CHAT
            },
        )
    }
    state.pendingTerminalCommand?.let { command ->
        AlertDialog(
            onDismissRequest = onTerminalCancel,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.ws_destructive_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.ws_destructive_desc))
                    Surface(color = Color(0xFF14171E), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            command,
                            Modifier.fillMaxWidth().padding(10.dp),
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = onTerminalConfirm) { Text(stringResource(R.string.ws_run_anyway)) } },
            dismissButton = { TextButton(onClick = onTerminalCancel) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            state.activeProject?.name.orEmpty(),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    Toast.makeText(context, state.activeProject?.name.orEmpty(), Toast.LENGTH_LONG).show()
                                },
                            ),
                        )
                        // Agent first so it always stays visible; a long chat title is cut with "…".
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable { agentMenuExpanded = true },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = when (state.agentKind) {
                                                        AgentKind.CLAUDE_CODE -> Color(0xFFD97706)
                                                        AgentKind.DEEPSEEK_HARNESS -> Color(0xFF3B82F6)
                                                        AgentKind.ANTIGRAVITY -> Color(0xFF10B981)
                                                        AgentKind.OPENCODE -> Color(0xFF00B4D8)
                                                    },
                                                    shape = CircleShape,
                                                ),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            when (state.agentKind) {
                                                AgentKind.CLAUDE_CODE -> "Claude"
                                                AgentKind.DEEPSEEK_HARNESS -> "DSH"
                                                AgentKind.ANTIGRAVITY -> "AGY"
                                                AgentKind.OPENCODE -> "OpenCode"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Switch Agent",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = agentMenuExpanded,
                                    onDismissRequest = { agentMenuExpanded = false },
                                ) {
                                    Text(
                                        "SELECT CODING AGENT CLI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    )
                                    AgentKind.entries.forEach { agent ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = when (agent) {
                                                                    AgentKind.CLAUDE_CODE -> Color(0xFFD97706)
                                                                    AgentKind.DEEPSEEK_HARNESS -> Color(0xFF3B82F6)
                                                                    AgentKind.ANTIGRAVITY -> Color(0xFF10B981)
                                                                    AgentKind.OPENCODE -> Color(0xFF00B4D8)
                                                                },
                                                                shape = CircleShape,
                                                            ),
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        agent.localizedTitle(),
                                                        fontWeight = if (agent == state.agentKind) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                if (agent == state.agentKind) {
                                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = PocketOrange, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                agentMenuExpanded = false
                                                onSelectAgent(agent)
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Tune, contentDescription = null, tint = PocketOrange, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("AI Providers & Keys...", fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            agentMenuExpanded = false
                                            onOpenAiHub()
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenAiHub() }
                            ) {
                                val shortModel = (if (state.agentKind == AgentKind.ANTIGRAVITY) state.antigravityModel else state.provider.model)
                                    .substringAfterLast('/')
                                    .take(16)
                                Text(
                                    "· $shortModel",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "AI Settings",
                                    modifier = Modifier.size(11.dp),
                                    tint = PocketOrange,
                                )
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tab_projects)) } },
                actions = {
                    IconButton(onClick = onOpenAiHub) {
                        Icon(Icons.Default.Tune, "AI Hub", tint = PocketOrange)
                    }
                    if (isAndroidProject) {
                        IconButton(
                            onClick = { showAndroidBuild = true },
                            enabled = !state.isRunning && !state.projectTerminalRunning,
                        ) {
                            if (state.androidBuildRunning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.PlayArrow, stringResource(R.string.ws_build_run_android))
                        }
                    }
                    IconButton(onClick = { showChats = true }) { Icon(Icons.Default.History, stringResource(R.string.ws_project_chats)) }
                    if (state.isRunning) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!keyboardVisible) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                WorkspaceTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab == WorkspaceTab.FILES) onRefreshFiles()
                            if (tab == WorkspaceTab.TERMINAL) onTerminalOpened()
                        },
                        icon = {
                            if (tab == WorkspaceTab.CHANGES && state.changes.isNotEmpty()) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Text("${state.changes.size}")
                                    }
                                }) {
                                    Icon(tab.icon, stringResource(tab.labelRes))
                                }
                            } else {
                                Icon(tab.icon, stringResource(tab.labelRes))
                            }
                        },
                        label = { Text(stringResource(tab.labelRes), fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                WorkspaceTab.CHAT -> ChatTab(
                    state.messages,
                    state.pendingApproval,
                    state.liveProcess,
                    state.isRunning,
                    onSend,
                    onStop,
                    onApproval,
                    listState = chatListState,
                    taskStartedAtMillis = state.workSegmentStartedAtMillis ?: state.taskStartedAtMillis,
                    taskFinishedAtMillis = state.taskFinishedAtMillis,
                    thinkingActive = state.liveThinking,
                    agentKind = state.agentKind,
                    pendingAttachments = state.pendingAttachments,
                    onAttach = {
                        attachmentLauncher.launch(arrayOf("image/*", "text/*", "application/json", "application/xml"))
                    },
                    onTakePhoto = {
                        runCatching {
                            val uri = createCameraPhotoUri(context)
                            pendingPhotoUri = uri.toString()
                            takePhotoLauncher.launch(uri)
                        }.onFailure {
                            pendingPhotoUri = null
                            Toast.makeText(context, context.getString(R.string.chat_camera_unavailable), Toast.LENGTH_LONG).show()
                        }
                    },
                    onRemoveAttachment = onRemoveAttachment,
                    onOpenAttachment = onOpenAttachment,
                    onRunInTerminal = { command ->
                        selectedTab = WorkspaceTab.TERMINAL
                        onTerminalOpened()
                        onTerminalPrepare(command)
                    },
                    queuedFollowUps = state.queuedFollowUps,
                    onSteerFollowUp = onSteerFollowUp,
                    onRemoveFollowUp = onRemoveFollowUp,
                    handoverCandidate = state.handoverCandidate,
                    onExecuteHandover = onExecuteHandover,
                    onDismissHandover = onDismissHandover,
                )
                WorkspaceTab.FILES -> FilesTab(
                    files = state.workspaceFiles,
                    loading = state.filesLoading,
                    suggestedProjectRoot = state.suggestedProjectRoot,
                    onRefresh = onRefreshFiles,
                    onLoadDirectory = onLoadProjectDirectory,
                    onOpenFile = onOpenFile,
                    onUseSuggestedProjectRoot = onUseSuggestedProjectRoot,
                    selectionMode = exportSelectionMode,
                    exportSelection = exportSelection,
                    exportRunning = state.projectExportRunning,
                    exportEnabled = !state.isRunning && !state.projectTerminalRunning && !state.androidBuildRunning,
                    onEnterSelection = {
                        exportSelection = ExportSelection()
                        exportSelectionMode = true
                    },
                    onCancelSelection = {
                        exportSelectionMode = false
                        exportSelection = ExportSelection()
                    },
                    onToggleSelection = { entry -> exportSelection = exportSelection.toggled(entry.path, entry.isDirectory) },
                    onSelectAll = { exportSelection = ExportSelection() },
                    onDownloadSelection = {
                        exportProjectLauncher.launch("${state.activeProject?.slug ?: "project"}.zip")
                    },
                    fileOpsEnabled = !state.isRunning && !state.projectTerminalRunning &&
                        !state.androidBuildRunning && !state.projectExportRunning,
                    onCreateEntry = onCreateWorkspaceEntry,
                    onRenameEntry = onRenameWorkspaceEntry,
                    onDeleteEntry = onDeleteWorkspaceEntry,
                    onUploadInto = { parent ->
                        pendingUploadParent = parent
                        uploadLauncher.launch(arrayOf("*/*"))
                    },
                    onShareFile = onShareWorkspaceFile,
                    onSaveFileCopy = { path ->
                        pendingCopyPath = path
                        saveCopyLauncher.launch(path.substringAfterLast('/'))
                    },
                )
                WorkspaceTab.TERMINAL -> TerminalScreen(
                    lines = state.projectTerminalLines,
                    isRunning = state.projectTerminalRunning,
                    onRun = onTerminalRun,
                    onInput = onTerminalInput,
                    onInterrupt = onTerminalInterrupt,
                    onClear = onTerminalClear,
                    onToggleTheme = {},
                    themeMode = state.themeMode,
                    title = stringResource(R.string.workspace_project_terminal),
                    subtitle = "${state.projectTerminalCwd} · Ubuntu PRoot",
                    liveOutput = state.projectTerminalLiveOutput,
                    currentCommand = state.projectTerminalCommand,
                    commandDraft = state.projectTerminalDraft,
                    onCommandDraftConsumed = onTerminalDraftConsumed,
                    promptPath = state.projectTerminalCwd,
                    onStop = onTerminalStop,
                    showThemeAction = false,
                    showQuickCommands = false,
                    compactHeader = true,
                )
                WorkspaceTab.CHANGES -> ChangesTab(
                    state.changes,
                    onUndoChanges,
                    onKeepChanges,
                    onUndoFileChange,
                    onKeepFileChange,
                )
                WorkspaceTab.PREVIEW -> PreviewTab(state.previewReady, state.previewUrl)
            }
        }
    }
}

@Composable
internal fun ChatSwitcherDialog(
    chats: List<ProjectChat>,
    activeChatId: String?,
    switchingEnabled: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onSwitch: (String) -> Unit,
    allowCreate: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ws_project_chats)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowCreate) {
                    Button(onClick = onCreate, enabled = switchingEnabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ws_new_chat))
                    }
                }
                if (!switchingEnabled) {
                    Text(stringResource(R.string.ws_finish_task_before_switch), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chats, key = { it.id }) { chat ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = switchingEnabled) { onSwitch(chat.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (chat.id == activeChatId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chat.localizedTitle(), fontWeight = if (chat.id == activeChatId) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                                    Text(
                                        if (chat.id == activeChatId) stringResource(R.string.ws_current_chat) else stringResource(R.string.ws_saved_conversation),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (chat.id == activeChatId) Icon(Icons.Default.Check, stringResource(R.string.ws_current), tint = PocketGreen)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

