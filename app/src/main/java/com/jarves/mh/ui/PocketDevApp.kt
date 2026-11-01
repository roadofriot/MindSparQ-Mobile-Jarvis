@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.jarves.mh.ui

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.speech.RecognizerIntent
import androidx.compose.material.icons.filled.Mic
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Toast
import com.jarves.mh.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.DEEPSEEK_HARNESS_PROVIDERS
import com.jarves.mh.model.DSH_PROTOCOL_PROVIDERS
import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectType
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.inferredDshApiForUrl
import com.jarves.mh.model.providersForAgent
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.projectSlug
import com.jarves.mh.runtime.RuntimeExecutionService
import com.jarves.mh.runtime.SupportedProjectTemplate
import com.jarves.mh.runtime.RuntimeSetupService
import com.jarves.mh.runtime.supportsArm64Runtime
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.runtime.AndroidAction
import com.jarves.mh.runtime.AndroidBuildIssue
import com.jarves.mh.runtime.AndroidBuildStage
import com.jarves.mh.runtime.AndroidHealthCheck
import com.jarves.mh.runtime.AndroidHealthFix
import com.jarves.mh.runtime.AndroidHealthStatus
import com.jarves.mh.runtime.AndroidLogLevel
import com.jarves.mh.runtime.AndroidLogLine
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.ui.theme.PocketBlue
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import java.io.ByteArrayInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import com.jarves.mh.R
import com.jarves.mh.ui.theme.AppLanguage
import com.jarves.mh.ui.theme.AppThemeMode
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExtendedFloatingActionButton



@Composable
fun PocketDevApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val projectsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var showUnifiedAiSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.consumeToast()
        }
    }
    when {
        state.startupStage == StartupStage.CHECKING -> StartupLoadingScreen(
            state = state,
            themeMode = state.themeMode,
            onToggleTheme = viewModel::toggleTheme,
        )
        !state.initialLanguageSelected && (state.startupStage == StartupStage.SETUP_REQUIRED || !state.onboardingComplete) ->
            InitialLanguageSetupScreen(
                currentLanguageCode = state.languageCode,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
                onSelectLanguage = viewModel::setLanguage,
                onContinue = viewModel::finishInitialLanguageSetup,
            )
        !state.backgroundSetupComplete && state.startupStage == StartupStage.SETUP_REQUIRED ->
            BackgroundTaskSetupScreen(
                currentLanguageCode = state.languageCode,
                onSetLanguage = viewModel::setLanguage,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
                onContinue = viewModel::finishBackgroundSetup,
            )
        state.startupStage == StartupStage.SETUP_REQUIRED -> RuntimeSetupPromptScreen(
            selectedStacks = state.selectedDevStacks,
            selectedAgent = state.agentKind,
            themeMode = state.themeMode,
            currentLanguageCode = state.languageCode,
            onSetLanguage = viewModel::setLanguage,
            onToggleTheme = viewModel::toggleTheme,
            onToggleStack = viewModel::toggleDevStack,
            onSelectAgent = viewModel::selectAgent,
            onDownload = viewModel::startRuntimeSetup,
        )
        state.startupStage == StartupStage.INSTALLING && state.showDetailedSetupProgress ->
            RuntimeInstallationScreen(
                state = state,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.INSTALLING || state.startupStage == StartupStage.INITIALIZING ->
            StartupLoadingScreen(
                state = state,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.ERROR -> StartupErrorScreen(
            message = state.startupError,
            isOffline = state.startupErrorIsOffline,
            logs = state.startupLogs,
            themeMode = state.themeMode,
            onToggleTheme = viewModel::toggleTheme,
            onRetry = viewModel::retryStartup,
        )
        state.startupStage == StartupStage.MODEL_SETUP && state.agentKind == AgentKind.ANTIGRAVITY ->
            AntigravityOnboardingScreen(
                state = state,
                currentLanguageCode = state.languageCode,
                onSetLanguage = viewModel::setLanguage,
                onStartLogin = viewModel::startAntigravityLogin,
                onSubmitCode = viewModel::submitAntigravityCode,
                onContinue = viewModel::finishAntigravityOnboarding,
                onSelectAgent = viewModel::chooseOnboardingAgent,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.MODEL_SETUP -> ProviderSetupScreen(
            initial = state.provider,
            onboarding = true,
            agentKind = state.agentKind,
            initialStep = 1,
            currentLanguageCode = state.languageCode,
            onSetLanguage = viewModel::setLanguage,
            onSave = viewModel::finishOnboarding,
            onDiscover = viewModel::discoverModels,
            onValidate = viewModel::validateProvider,
            onSelectAgent = viewModel::chooseOnboardingAgent,
            onToggleTheme = viewModel::toggleTheme,
            themeMode = state.themeMode,
        )
        state.startupStage == StartupStage.READY && !state.backgroundSetupComplete ->
            BackgroundTaskSetupScreen(
                currentLanguageCode = state.languageCode,
                onSetLanguage = viewModel::setLanguage,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
                onContinue = viewModel::finishBackgroundSetup,
            )
        state.readOnlyProject != null -> ReadOnlyProjectScreen(
            state = state,
            onBack = viewModel::closeReadOnlyProject,
            onSwitchChat = viewModel::switchReadOnlyChat,
            onContinueHere = viewModel::activateReadOnlyProject,
        )
        state.activeProject != null && state.workspaceVisible -> WorkspaceScreen(
            state = state,
            onBack = viewModel::closeProject,
            onSend = viewModel::sendPrompt,
            onStop = viewModel::stopTask,
            onSteerFollowUp = viewModel::steerQueuedFollowUp,
            onRemoveFollowUp = viewModel::removeQueuedFollowUp,
            onApproval = viewModel::answerApproval,
            onRefreshFiles = viewModel::refreshProjectFiles,
            onLoadProjectDirectory = viewModel::loadProjectDirectory,
            onCreateWorkspaceEntry = viewModel::createWorkspaceEntry,
            onRenameWorkspaceEntry = viewModel::renameWorkspaceEntry,
            onDeleteWorkspaceEntry = viewModel::deleteWorkspaceEntry,
            onUploadToWorkspace = viewModel::uploadToWorkspace,
            onShareWorkspaceFile = viewModel::shareWorkspaceFile,
            onSaveWorkspaceFileCopy = viewModel::saveWorkspaceFileCopy,
            onOpenFile = viewModel::openFile,
            onCloseFile = viewModel::closeFile,
            onBeginFileEdit = viewModel::beginFileEdit,
            onUpdateFileDraft = viewModel::updateFileDraft,
            onCancelFileEdit = viewModel::cancelFileEdit,
            onSaveFile = viewModel::saveOpenedFile,
            onReloadFile = viewModel::reloadOpenedFile,
            onDismissFileConflict = viewModel::dismissFileConflict,
            onUndoChanges = viewModel::undoLastChanges,
            onKeepChanges = viewModel::keepLastChanges,
            onUndoFileChange = viewModel::undoFileChange,
            onKeepFileChange = viewModel::keepFileChange,
            onCreateChat = viewModel::createChat,
            onSwitchChat = viewModel::switchChat,
            onTerminalRun = viewModel::requestProjectTerminalCommand,
            onTerminalInput = viewModel::sendProjectTerminalInput,
            onTerminalInterrupt = viewModel::interruptProjectTerminalCommand,
            onTerminalPrepare = viewModel::prepareProjectTerminalCommand,
            onTerminalDraftConsumed = viewModel::consumeProjectTerminalDraft,
            onTerminalOpened = viewModel::openProjectTerminal,
            onTerminalStop = viewModel::stopProjectTerminalCommand,
            onTerminalClear = viewModel::clearProjectTerminal,
            onTerminalConfirm = viewModel::confirmProjectTerminalCommand,
            onTerminalCancel = viewModel::cancelProjectTerminalCommand,
            onUseSuggestedProjectRoot = viewModel::useSuggestedProjectRoot,
            onExportProject = viewModel::exportActiveProject,
            onAddAttachments = viewModel::addChatAttachments,
            onRemoveAttachment = viewModel::removePendingAttachment,
            onOpenAttachment = viewModel::openChatAttachment,
            onBuildAndRunAndroid = viewModel::buildAndRunAndroidApp,
            onAndroidAction = viewModel::performAndroidAction,
            onRefreshAndroidDashboard = viewModel::refreshAndroidDashboard,
            onShareAndroidApk = viewModel::shareAndroidApk,
            onSaveAndroidApk = viewModel::saveAndroidApk,
            onOpenAndroidAppDetails = viewModel::openInstalledAndroidAppDetails,
            onUninstallAndroidApp = viewModel::uninstallAndroidApp,
            onOpenAndroidIssue = viewModel::openAndroidIssue,
            onAndroidHealthFix = viewModel::performAndroidHealthFix,
            onRefreshAndroidHealth = viewModel::refreshAndroidHealth,
            onStartAndroidLogcat = viewModel::startAndroidLogcat,
            onStopAndroidLogcat = viewModel::stopAndroidLogcat,
            onToggleAndroidLogcatPause = viewModel::toggleAndroidLogcatPause,
            onClearAndroidLogcat = viewModel::clearAndroidLogcat,
            onAndroidLogcatQuery = viewModel::setAndroidLogcatQuery,
            onAndroidLogcatLevel = viewModel::setAndroidLogcatLevel,
            onOpenAndroidLogLine = viewModel::openAndroidLogLine,
            onCancelAndroidBuild = viewModel::cancelAndroidBuild,
            onCleanAndroid = viewModel::cleanAndroidProject,
            onInstallAndroidTools = { viewModel.installDevStack(DevStack.ANDROID) },
            onOpenAiHub = { showUnifiedAiSheet = true },
            onSelectAgent = viewModel::selectAgent,
            onExecuteHandover = viewModel::handoverTask,
            onDismissHandover = viewModel::dismissHandover,
        )
        else -> RootScreenHost(
            state = state,
            viewModel = viewModel,
            projectsListState = projectsListState,
            onOpenAiHub = { showUnifiedAiSheet = true },
        )
    }

    if (showUnifiedAiSheet) {
        UnifiedAiProviderSheet(
            state = state,
            onDismiss = { showUnifiedAiSheet = false },
            onSelectAgent = viewModel::selectAgent,
            onSelectProvider = { providerKind ->
                val profile = ProviderProfile(kind = providerKind)
                val currentKey = viewModel.getSavedApiKey(providerKind)
                viewModel.updateProvider(profile, currentKey)
            },
            onSelectModel = { modelName ->
                val currentProfile = state.provider.copy(model = modelName)
                val currentKey = viewModel.getSavedApiKey(currentProfile.kind)
                viewModel.updateProvider(currentProfile, currentKey)
            },
            onSaveProvider = { profile, key ->
                viewModel.updateProvider(profile, key)
            },
            onPing = viewModel::pingApi,
            getSavedApiKeys = viewModel::getSavedApiKeys,
            onAddApiKey = viewModel::addApiKey,
            onAddKeyDetailed = viewModel::addApiKey,
            onActivateApiKey = viewModel::activateApiKey,
            onRemoveApiKey = viewModel::removeApiKey,
            onDiscoverModels = viewModel::discoverModels,
            isModelDownloaded = { modelId -> viewModel.offlineModelManager.isDownloaded(modelId) },
            onDownloadOfflineModel = { modelId, onProgress, onComplete ->
                viewModel.downloadOfflineModel(modelId, onProgress, onComplete)
            },
            onDeleteOfflineModel = { modelId ->
                viewModel.deleteOfflineModel(modelId)
            },
            onGetCachedModels = viewModel::getCachedModels,
            getFailoverChain = viewModel::getFailoverChain,
            saveFailoverChain = viewModel::saveFailoverChain,
            onToggleFailoverParallel = viewModel::toggleFailoverParallel,
            isFailoverParallel = viewModel.isFailoverParallel(),
        )
    }
}

@Composable
private fun RootScreenHost(
    state: AppUiState,
    viewModel: MainViewModel,
    projectsListState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    onOpenAiHub: () -> Unit = {},
) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(RootScreen.PROJECTS) }
    var showQuickTerminal by rememberSaveable { mutableStateOf(false) }
    var lastBackPressMillis by remember { mutableStateOf(0L) }

    BackHandler {
        when {
            showQuickTerminal -> {
                showQuickTerminal = false
            }
            screen != RootScreen.PROJECTS -> {
                screen = RootScreen.PROJECTS
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressMillis < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressMillis = now
                    Toast.makeText(context, "Press back again to exit MindSparQ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()
    val isTerminalRunning by viewModel.isTerminalRunning.collectAsStateWithLifecycle()
    val terminalLiveOutput by viewModel.terminalLiveOutput.collectAsStateWithLifecycle()
    val terminalCurrentCommand by viewModel.terminalCurrentCommand.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!keyboardVisible) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                RootScreen.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = screen == tab,
                        onClick = { screen = tab },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (screen == RootScreen.PROJECTS && !keyboardVisible && !showQuickTerminal) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickTerminal = true },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    text = { Text(stringResource(R.string.projects_terminal), fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                RootScreen.PROJECTS -> ProjectsScreen(
                    state = state,
                    listState = projectsListState,
                    onOpen = viewModel::openProject,
                    onCreate = viewModel::createProject,
                    onCreateAndroid = viewModel::createAndroidProject,
                    onCreateTemplatedProject = viewModel::createTemplatedProject,
                    onScaffoldVoiceArchitect = viewModel::scaffoldVoiceArchitectProject,
                    onOpenInFileManager = viewModel::openProjectInFileManager,
                    onCreateQuickProject = viewModel::createQuickProject,
                    onImportZip = viewModel::importZipProject,
                    onCloneGit = viewModel::clonePublicGitRepository,
                    onStartGitHubLogin = viewModel::startGitHubLogin,
                    onGenerateNewGitHubCode = viewModel::generateNewGitHubCode,
                    onRefreshGitHub = viewModel::refreshGitHubRepositories,
                    onDisconnectGitHub = viewModel::disconnectGitHub,
                    onConnectGitHubWithToken = viewModel::connectGitHubWithToken,
                    onGetGeminiApiKey = {
                        val geminiKey = viewModel.getSavedApiKey(ProviderKind.GEMINI)
                        val openRouterKey = viewModel.getSavedApiKey(ProviderKind.LLM_ROUTER)
                        when {
                            geminiKey.isNotBlank() -> geminiKey
                            openRouterKey.isNotBlank() -> openRouterKey
                            else -> ""
                        }
                    },
                    onSaveGeminiApiKey = { key ->
                        val clean = key.trim()
                        if (clean.startsWith("sk-or-")) {
                            viewModel.saveApiKey(ProviderKind.LLM_ROUTER, clean)
                            viewModel.saveApiKey(ProviderKind.GEMINI, clean)
                        } else {
                            viewModel.saveGeminiApiKey(clean)
                        }
                    },
                    onCloneGitHub = viewModel::cloneGitHubRepository,
                    onRenameProject = viewModel::renameProject,
                    onDeleteProject = viewModel::deleteProject,
                    onSettings = { screen = RootScreen.SETTINGS },
                    onPing = viewModel::pingApi,
                    onToggleTheme = viewModel::toggleTheme,
                    onInstallUpdate = viewModel::installAppUpdate,
                    onStartGoogleAuth = viewModel::startAntigravityLogin,
                    onGoogleSignOut = viewModel::logoutAntigravity,
                    onPushProjectToGitHub = viewModel::pushProjectToGitHub,
                    onOpenAiHub = onOpenAiHub,
                    onOpenTerminal = { showQuickTerminal = true },
                )
                RootScreen.AGENT -> AgentScreen(
                    state = state,
                    onSaveProvider = { profile, key ->
                        viewModel.updateProvider(profile, key)
                    },
                    onDiscoverModels = viewModel::discoverModels,
                    onValidateProvider = viewModel::validateProvider,
                    onPing = viewModel::pingApi,
                    getSavedApiKey = viewModel::getSavedApiKey,
                    getSavedApiKeys = viewModel::getSavedApiKeys,
                    onAddApiKey = viewModel::addApiKey,
                    onActivateApiKey = viewModel::activateApiKey,
                    onRemoveApiKey = viewModel::removeApiKey,
                    onSelectAgent = viewModel::selectAgent,
                    onInstallAgent = viewModel::installAgent,
                    onCheckAgentUpdates = viewModel::checkAgentUpdates,
                    onUpdateAgent = viewModel::updateAgent,
                    onStartAntigravityLogin = viewModel::startAntigravityLogin,
                    onSubmitAntigravityCode = viewModel::submitAntigravityCode,
                    onLogoutAntigravity = viewModel::logoutAntigravity,
                    onRefreshAntigravityModels = viewModel::refreshAntigravityModels,
                    onSetAntigravityModel = viewModel::setAntigravityModel,
                    onSetAntigravityEffort = viewModel::setAntigravityEffort,
                    onGetCachedModels = viewModel::getCachedModels,
                )
                RootScreen.SETTINGS -> SettingsScreen(
                    state = state,
                    onSaveProvider = { profile, key ->
                        viewModel.updateProvider(profile, key)
                    },
                    onDiscoverModels = viewModel::discoverModels,
                    onValidateProvider = viewModel::validateProvider,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetLanguage = viewModel::setLanguage,
                    onPing = viewModel::pingApi,
                    onClearTerminal = viewModel::clearTerminal,
                    getSavedApiKey = viewModel::getSavedApiKey,
                    getSavedApiKeys = viewModel::getSavedApiKeys,
                    onAddApiKey = viewModel::addApiKey,
                    onActivateApiKey = viewModel::activateApiKey,
                    onRemoveApiKey = viewModel::removeApiKey,
                    onInstallDevStack = viewModel::installDevStack,
                    onRemoveDevStack = viewModel::removeDevStack,
                    onInstallAgent = viewModel::installAgent,
                    onCheckAgentUpdates = viewModel::checkAgentUpdates,
                    onUpdateAgent = viewModel::updateAgent,
                    onStartAntigravityLogin = viewModel::startAntigravityLogin,
                    onSubmitAntigravityCode = viewModel::submitAntigravityCode,
                    onLogoutAntigravity = viewModel::logoutAntigravity,
                    onRefreshAntigravityModels = viewModel::refreshAntigravityModels,
                    onSetAntigravityModel = viewModel::setAntigravityModel,
                    onSetAntigravityEffort = viewModel::setAntigravityEffort,
                    initialDebugUpdateManifestUrl = viewModel.debugUpdateManifestUrl(),
                    onSetDebugUpdateManifestUrl = viewModel::setDebugUpdateManifestUrl,
                    onClearDebugUpdateManifestUrl = viewModel::clearDebugUpdateManifestUrl,
                    onRefreshDiagnostics = viewModel::refreshDeveloperDiagnostics,
                    onStopInactiveProcesses = viewModel::stopInactiveDeveloperProcesses,
                    onClearRuntimeCache = viewModel::clearDeveloperRuntimeCache,
                    onOpenAiHub = onOpenAiHub,
                    onOpenIntegrity = { screen = RootScreen.INTEGRITY },
                )
                RootScreen.INTEGRITY -> IntegrityScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = RootScreen.PROJECTS },
                )
            }
        }
    }
    if (showQuickTerminal) {
        QuickTerminalSheet(
            onDismiss = { showQuickTerminal = false },
        ) {
            TerminalScreen(
                lines = terminalLines,
                isRunning = isTerminalRunning,
                onRun = viewModel::runTerminalCommand,
                onInput = viewModel::sendTerminalInput,
                onInterrupt = viewModel::interruptTerminalCommand,
                onClear = viewModel::clearTerminal,
                onToggleTheme = viewModel::toggleTheme,
                themeMode = state.themeMode,
                liveOutput = terminalLiveOutput,
                currentCommand = terminalCurrentCommand,
                showThemeAction = false,
                showQuickCommands = true,
                compactHeader = true,
            )
        }
    }
}

@Composable
private fun QuickTerminalSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var sheetFraction by rememberSaveable { mutableFloatStateOf(0.60f) }
    BackHandler(onBack = onDismiss)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
        LaunchedEffect(keyboardVisible) {
            if (keyboardVisible) sheetFraction = 0.85f
        }
        val dragState = rememberDraggableState { dragAmount ->
            sheetFraction = (sheetFraction - dragAmount / maxHeightPx)
                .coerceIn(0.40f, 0.85f)
        }
        Box(Modifier.fillMaxSize()) {
            // Dimmed backdrop — tap to dismiss.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
            // This container consumes the keyboard inset outside the sheet rather
            // than turning it into empty padding inside the terminal.
            Box(Modifier.fillMaxSize().imePadding()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(sheetFraction)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            startDragImmediately = false,
                        ),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    // Plain theme surface without tonal elevation: the app theme doesn't define
                    // surfaceContainer* (falls back to M3 lavender) and tonal elevation tints
                    // every nested Surface orange.
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 28.dp,
                ) {
                    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), CircleShape),
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    }
                }
            }
        }
    }
}
