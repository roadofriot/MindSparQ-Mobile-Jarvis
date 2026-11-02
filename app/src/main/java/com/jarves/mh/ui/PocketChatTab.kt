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

@Composable
internal fun ChatTab(
    messages: List<ChatMessage>,
    approval: ToolRequest?,
    liveProcess: List<ActivityItem>,
    isRunning: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onApproval: (Boolean) -> Unit,
    listState: LazyListState,
    taskStartedAtMillis: Long?,
    taskFinishedAtMillis: Long?,
    thinkingActive: Boolean,
    agentKind: AgentKind,
    pendingAttachments: List<ChatAttachment>,
    onAttach: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenAttachment: (ChatAttachment) -> Unit,
    onRunInTerminal: (String) -> Unit,
    onTakePhoto: () -> Unit = {},
    queuedFollowUps: List<QueuedFollowUp> = emptyList(),
    onSteerFollowUp: (String) -> Unit = {},
    onRemoveFollowUp: (String) -> Unit = {},
    readOnly: Boolean = false,
    readOnlyBlocked: Boolean = false,
    onContinueHere: () -> Unit = {},
    handoverCandidate: HandoverCandidate? = null,
    onExecuteHandover: () -> Unit = {},
    onDismissHandover: () -> Unit = {},
) {
    val view = LocalView.current
    // Keep the screen on while the selected agent is working in this chat. Released automatically
    // when the task finishes or the user leaves the chat tab.
    DisposableEffect(isRunning) {
        view.keepScreenOn = isRunning
        onDispose { view.keepScreenOn = false }
    }
    var prompt by rememberSaveable { mutableStateOf("") }
    var lastSentPrompt by rememberSaveable { mutableStateOf("") }
    val userPrompts = remember(messages, lastSentPrompt) {
        val list = mutableListOf<String>()
        if (lastSentPrompt.isNotBlank()) list.add(lastSentPrompt)
        messages.filter { it.fromUser && it.text.isNotBlank() }.map { it.text }.reversed().forEach { msg ->
            if (!list.contains(msg)) list.add(msg)
        }
        list
    }
    val candidateToRestore = remember(userPrompts) { userPrompts.firstOrNull() }
    var historyMenuOpen by remember { mutableStateOf(false) }
    val chatSpeechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                prompt = if (prompt.isBlank()) spokenText else "$prompt $spokenText"
            }
        }
    }
    val chatScope = rememberCoroutineScope()
    // True while the newest item (message, live panel, or approval card) is on screen.
    val readerAtBottom by remember {
        derivedStateOf {
            !listState.canScrollForward
        }
    }
    val visibleMessages = remember(messages) {
        messages.filter { message ->
            (message.text.isNotBlank() && message.text != "null") || message.workItems.isNotEmpty() || message.attachments.isNotEmpty()
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleMessages, key = { it.id }) { message ->
                    if (message.workItems.isNotEmpty()) {
                        WorkBlockCard(message)
                    } else {
                        MessageBubble(message, onRunInTerminal, onOpenAttachment)
                    }
                }
                if (liveProcess.isNotEmpty() || thinkingActive) {
                    item(key = "live-claude-process") {
                        LiveClaudeProcess(
                            processItems = liveProcess,
                            isRunning = isRunning,
                            startedAtMillis = taskStartedAtMillis,
                            finishedAtMillis = taskFinishedAtMillis,
                            thinkingActive = thinkingActive,
                        )
                    }
                }
                approval?.let { request -> item { ApprovalCard(request, onApproval) } }
            }
            if (!readerAtBottom) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clickable {
                            chatScope.launch {
                                listState.animateScrollToItem(
                                    (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0),
                                )
                            }
                        },
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.padding(start = 13.dp, end = 15.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.ws_latest),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (readOnly) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ws_readonly_history), fontWeight = FontWeight.SemiBold)
                        Text(
                            if (readOnlyBlocked) {
                                stringResource(R.string.ws_readonly_blocked)
                            } else {
                                stringResource(R.string.ws_readonly_finished)
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!readOnlyBlocked) {
                        TextButton(onClick = onContinueHere) { Text(stringResource(R.string.ws_open)) }
                    }
                }
            }
        } else Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (queuedFollowUps.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        queuedFollowUps.forEach { followUp ->
                            QueuedFollowUpCard(
                                followUp = followUp,
                                onSteer = { onSteerFollowUp(followUp.id) },
                                onRemove = { onRemoveFollowUp(followUp.id) },
                            )
                        }
                    }
                }
                if (pendingAttachments.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pendingAttachments.forEach { attachment ->
                            AttachmentChip(
                                attachment = attachment,
                                onOpen = null,
                                onRemove = { onRemoveAttachment(attachment.id) },
                            )
                        }
                    }
                }

                // Agent Handover Banner (Safe Context Continuity)
                val handover = handoverCandidate
                if (handover != null && !isRunning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Handover",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Handover to ${handover.targetAgent.title}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                IconButton(
                                    onClick = onDismissHandover,
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${handover.failedAgent.title} stopped: ${handover.failedReason.take(80)}. Tap below to resume execution without re-entering instructions.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                lineHeight = 15.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = onExecuteHandover,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resume with ${handover.targetAgent.title}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Quick Restore Hint for previous content
                if (prompt.isBlank() && candidateToRestore != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { prompt = candidateToRestore },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restore",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Tap to restore: \"${candidateToRestore.replace('\n', ' ').take(45)}${if (candidateToRestore.length > 45) "…" else ""}\"",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // Mobile Coding Quick Prompt Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val promptShortcuts = listOf(
                        "Tab", "`", "```", "/", "{ }", "( )", "git status", "run build", "fix error", "explain code"
                    )
                    promptShortcuts.forEach { shortcut ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                prompt = when (shortcut) {
                                    "Tab" -> prompt + "    "
                                    "{ }" -> prompt + "{  }"
                                    "( )" -> prompt + "( )"
                                    "```" -> prompt + "\n```\n\n```"
                                    "git status" -> if (prompt.isBlank()) "git status" else "$prompt git status"
                                    "run build" -> if (prompt.isBlank()) "Please run build and check for errors" else "$prompt and run build"
                                    "fix error" -> if (prompt.isBlank()) "Fix the following error:\n" else "$prompt fix this error"
                                    "explain code" -> if (prompt.isBlank()) "Explain this architecture and code structure" else "$prompt explain this"
                                    else -> prompt + shortcut
                                }
                            }
                        ) {
                            Text(
                                text = shortcut,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                val canSend = prompt.isNotBlank() || pendingAttachments.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (canSend) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        var attachMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { attachMenuOpen = true },
                                enabled = pendingAttachments.size < 5,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = stringResource(R.string.chat_attach_files),
                                    modifier = Modifier.size(20.dp),
                                    tint = if (pendingAttachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_take_photo)) },
                                    leadingIcon = { Icon(Icons.Default.PhotoCamera, null) },
                                    onClick = { attachMenuOpen = false; onTakePhoto() },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_choose_files)) },
                                    leadingIcon = { Icon(Icons.Default.AttachFile, null) },
                                    onClick = { attachMenuOpen = false; onAttach() },
                                )
                            }
                        }

                        BasicTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .heightIn(min = 20.dp, max = 130.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (prompt.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.chat_message_agent, stringResource(agentKind.titleRes())),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak message to agent...")
                                }
                                try {
                                    chatSpeechLauncher.launch(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Dictation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (userPrompts.isNotEmpty()) {
                            Box {
                                IconButton(
                                    onClick = { historyMenuOpen = true },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Previous Messages",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = historyMenuOpen,
                                    onDismissRequest = { historyMenuOpen = false },
                                    modifier = Modifier.widthIn(max = 280.dp),
                                ) {
                                    Text(
                                        text = "Previous Prompts (Tap to reuse)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                    userPrompts.take(6).forEach { pastPrompt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = pastPrompt,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 12.sp,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            },
                                            onClick = {
                                                prompt = pastPrompt
                                                historyMenuOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(2.dp))

                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape,
                                    )
                                    .clickable(onClick = onStop),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.ws_stop_ai_task),
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            if (canSend) Spacer(Modifier.width(6.dp))
                        }
                        if (!isRunning || canSend) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                    )
                                    .clickable(
                                        enabled = canSend,
                                        onClick = {
                                            if (canSend) {
                                                lastSentPrompt = prompt
                                                onSend(prompt)
                                                prompt = ""
                                            }
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(R.string.chat_send),
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueuedFollowUpCard(
    followUp: QueuedFollowUp,
    onSteer: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    followUp.prompt,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (followUp.attachments.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.an_attachments, followUp.attachments.size, followUp.attachments.size),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onSteer) { Text(stringResource(R.string.an_steer)) }
            IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.an_remove_queued_follow_up),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LiveClaudeProcess(
    processItems: List<ActivityItem>,
    isRunning: Boolean,
    startedAtMillis: Long?,
    finishedAtMillis: Long?,
    thinkingActive: Boolean,
) {
    val elapsedSeconds = startedAtMillis?.let { rememberLiveElapsedSeconds(it).toLong() } ?: 0L
    ClaudeActivityDisclosure(
        items = processItems,
        headline = activityHeadline(processItems, elapsedSeconds, thinkingActive),
        isRunning = isRunning,
    )
}

@Composable
private fun WorkBlockCard(message: ChatMessage) {
    val seconds = (message.workedMillis / 1_000L).coerceAtLeast(1L)
    Column {
        ClaudeActivityDisclosure(
            items = message.workItems,
            headline = activityHeadline(message.workItems, seconds, message.workItems.isEmpty()),
        )
        if (message.workItems.lastOrNull()?.title?.startsWith("Task stopped") == true) {
            Text(
                text = stringResource(R.string.ws_worked_for, formatDuration(seconds)),
                modifier = Modifier.padding(start = 29.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ClaudeActivityDisclosure(
    items: List<ActivityItem>,
    headline: String,
    isRunning: Boolean = false,
) {
    var expandedItems by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    // Older saved chats may contain hundreds of polling steps. Rendering all of them inside
    // one lazy-list item can monopolize the UI thread, so show the latest useful window.
    val visibleItems = items.takeLast(32)
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
        if (visibleItems.isEmpty()) {
            ActivitySummaryRow(
                item = null,
                text = headline,
                expanded = 0 in expandedItems,
                showProgress = isRunning,
                onToggle = {
                    expandedItems = if (0 in expandedItems) expandedItems - 0 else expandedItems + 0
                },
            )
            if (0 in expandedItems) ActivityExpandedDetail(null, stringResource(R.string.ws_activity_reviewing_request))
        } else {
            visibleItems.forEachIndexed { index, item ->
                ActivitySummaryRow(
                    item = item,
                    text = compactActivityText(item),
                    expanded = index in expandedItems,
                    showProgress = isRunning && !item.isComplete,
                    onToggle = {
                        expandedItems = if (index in expandedItems) expandedItems - index else expandedItems + index
                    },
                )
                if (index in expandedItems) ActivityExpandedDetail(item, activityDetail(item))
            }
        }
    }
}

@Composable
internal fun AnimatedThinkingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val transition = rememberInfiniteTransition(label = "thinking_dots")
    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "dot1",
    )
    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180),
        ),
        label = "dot2",
    )
    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360),
        ),
        label = "dot3",
    )

    val dot1Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "dot1_alpha",
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180),
        ),
        label = "dot2_alpha",
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360),
        ),
        label = "dot3_alpha",
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot1Offset * density }
                .background(dotColor.copy(alpha = dot1Alpha), CircleShape),
        )
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot2Offset * density }
                .background(dotColor.copy(alpha = dot2Alpha), CircleShape),
        )
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot3Offset * density }
                .background(dotColor.copy(alpha = dot3Alpha), CircleShape),
        )
    }
}

@Composable
private fun ActivitySummaryRow(
    item: ActivityItem?,
    text: String,
    expanded: Boolean,
    showProgress: Boolean,
    onToggle: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            activityIcon(item),
            null,
            Modifier.size(16.dp),
            tint = muted,
        )
        Spacer(Modifier.width(9.dp))
        Text(text, Modifier.weight(1f), fontSize = 13.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (showProgress) {
            AnimatedThinkingDots(dotColor = muted)
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            if (expanded) stringResource(R.string.ws_collapse_activity) else stringResource(R.string.ws_expand_activity),
            Modifier.size(18.dp),
            tint = muted,
        )
    }
}

@Composable
private fun activityIcon(item: ActivityItem?): ImageVector {
    // "Files changed" is stored in the language active when it was recorded; match both
    // the current translation and the English title kept in older chat history.
    val filesChangedTitle = stringResource(R.string.vm_files_changed)
    if (item == null) return Icons.Default.AutoAwesome
    val task = item.title
        .removePrefix("Running ")
        .removeSuffix(" completed")
        .trim()
    return when {
        item.isCommand || task.equals("Bash", ignoreCase = true) -> Icons.Default.Terminal
        task.equals("Write", ignoreCase = true) ||
            task.equals("Edit", ignoreCase = true) ||
            task.equals("NotebookEdit", ignoreCase = true) -> Icons.Default.Edit
        task.equals("Read", ignoreCase = true) -> Icons.Default.Description
        task.equals("Glob", ignoreCase = true) ||
            task.equals("Grep", ignoreCase = true) -> Icons.Default.Search
        item.title == filesChangedTitle || task.contains("file", ignoreCase = true) -> Icons.Default.Description
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
private fun ActivityExpandedDetail(item: ActivityItem?, detail: String) {
    if (item?.isCommand == true) {
        Text(
            detail,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 8.dp, bottom = 8.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
    } else {
        MarkdownText(
            markdown = detail,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun compactActivityText(item: ActivityItem): String =
    "${activityDisplayName(item)} · ${activityDetail(item).take(180).replace(Regex("\\s+"), " ").take(105)}"

@Composable
private fun activityDetail(item: ActivityItem): String {
    if (item.title == "Think" && item.detail.contains("reasoning tokens processed", true)) {
        return stringResource(R.string.ws_activity_reviewed_request)
    }
    return item.detail.ifBlank { activityDisplayName(item) }
}

@Composable
private fun activityHeadline(items: List<ActivityItem>, seconds: Long, thinking: Boolean): String {
    val latest = items.lastOrNull()
    val thinkLabel = stringResource(R.string.ws_activity_think)
    if (latest == null) return "$thinkLabel · ${stringResource(R.string.ws_activity_analyzing_request)} · ${formatDuration(seconds)}"
    if (thinking && latest.title == "Think") return "$thinkLabel · ${latest.detail} · ${formatDuration(seconds)}"
    val latestName = activityDisplayName(latest)
    val detail = latest.detail.take(180).replace(Regex("\\s+"), " ").trim().ifBlank { latestName }
    return "$latestName · ${detail.take(100)} · ${formatDuration(seconds)}"
}

/**
 * Activity titles are stored as written: internal English markers ("Think", "Task stopped",
 * "Running <tool>", "<tool> completed") that the logic above parses, titles already localized
 * by MainViewModel, or English titles persisted by older versions. Translate at display time.
 */
@Composable
private fun activityDisplayName(item: ActivityItem): String {
    val legacyRes = legacyActivityTitles[item.title]
    return when {
        item.title == "Think" -> stringResource(R.string.ws_activity_think)
        item.title.startsWith("Task stopped") -> stringResource(R.string.ws_task_stopped)
        legacyRes != null -> stringResource(legacyRes)
        // "Running <tool>" / "<tool> completed" display as the tool name (a language-neutral ID).
        else -> activityName(item)
    }
}

/** English activity titles emitted by runtime bridges or saved by earlier app versions. */
private val legacyActivityTitles: Map<String, Int> = mapOf(
    "Understanding your request" to R.string.vm_understanding_request,
    "Changes kept" to R.string.vm_changes_kept,
    "Changes undone" to R.string.vm_changes_undone,
    "Undo unavailable" to R.string.vm_undo_unavailable,
    "Waiting for approval" to R.string.vm_waiting_approval,
    "Applying approved changes" to R.string.vm_applying_approved,
    "Action approved" to R.string.vm_action_approved,
    "Action rejected" to R.string.vm_action_rejected,
    "Files changed" to R.string.vm_files_changed,
    "Preview ready" to R.string.vm_preview_ready,
    "Task completed" to R.string.vm_task_completed,
    "Task interrupted" to R.string.vm_task_interrupted,
    "API key switched" to R.string.vm_api_key_switched,
    "Permission denied" to R.string.md_activity_permission_denied,
)

private fun activityName(item: ActivityItem): String = item.title
    .removePrefix("Running ")
    .removeSuffix(" completed")
    .replaceFirstChar { it.uppercase() }

@Composable
private fun completedProcessSummary(
    processItems: List<ActivityItem>,
    startedAtMillis: Long?,
    finishedAtMillis: Long?,
): String {
    val stopped = processItems.lastOrNull()?.title?.startsWith("Task stopped") == true
    val outcome = if (stopped) stringResource(R.string.ws_task_stopped) else stringResource(R.string.ws_task_completed)
    val steps = if (processItems.size == 1) stringResource(R.string.ws_steps_one) else stringResource(R.string.ws_steps_many, processItems.size)
    val duration = startedAtMillis?.let { start ->
        val end = finishedAtMillis ?: System.currentTimeMillis()
        formatDuration(((end - start) / 1000L).coerceAtLeast(0))
    }
    return if (duration != null) "$outcome · $duration · $steps" else "$outcome · $steps"
}

@Composable
internal fun rememberLiveElapsedSeconds(startedAtMillis: Long): Int {
    var seconds by remember(startedAtMillis) {
        mutableIntStateOf(((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(1_000)
            seconds = ((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        }
    }
    return seconds
}

@Composable
private fun MessageBubble(message: ChatMessage, onRunInTerminal: (String) -> Unit, onOpenAttachment: (ChatAttachment) -> Unit) {
    val displayedText = if (!message.fromUser && message.text == "Hi! Tell me what you want to build or change.") {
        stringResource(R.string.chat_welcome)
    } else {
        message.text
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(if (message.fromUser) .82f else .92f),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                SelectionContainer {
                    if (message.fromUser) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        MarkdownText(
                            markdown = displayedText,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            onRunCode = onRunInTerminal,
                        )
                    }
                }
                if (!message.fromUser && message.workedMillis > 0L) {
                    Text(
                        text = stringResource(R.string.ws_worked_for, formatDuration((message.workedMillis / 1_000L).coerceAtLeast(1L))),
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                if (message.attachments.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        message.attachments.forEach { attachment ->
                            AttachmentChip(attachment = attachment, onOpen = { onOpenAttachment(attachment) }, onRemove = null)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: ChatAttachment,
    onOpen: (() -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    val icon = when {
        attachment.mimeType.startsWith("image/") -> Icons.Default.PhotoCamera
        else -> Icons.Default.Description
    }
    Surface(
        modifier = Modifier.then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(start = 9.dp, end = if (onRemove == null) 10.dp else 3.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(17.dp), tint = PocketOrange)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(attachment.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatFileSize(attachment.sizeBytes), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.ws_remove_attachment), Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun ApprovalCard(request: ToolRequest, onApproval: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = PocketOrange)
                Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ws_review_action), fontWeight = FontWeight.Bold)
            }
            Text(request.explanation)
            request.affectedPaths.forEach { Text("• $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onApproval(false) }, Modifier.weight(1f)) { Text(stringResource(R.string.ws_reject)) }
                Button(onClick = { onApproval(true) }, Modifier.weight(1f)) { Text(stringResource(R.string.ws_allow_once)) }
            }
        }
    }
}

