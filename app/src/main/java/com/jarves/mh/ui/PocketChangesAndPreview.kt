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
internal fun ChangesTab(
    changes: List<ChangeItem>,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    onUndoFile: (String) -> Unit,
    onKeepFile: (String) -> Unit,
) {
    var expandedPath by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.tab_changes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.ws_review_ai_changes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (changes.isEmpty()) item { EmptyState(Icons.Default.Code, stringResource(R.string.ws_no_changes_yet), stringResource(R.string.ws_no_changes_desc)) }
        items(changes, key = { it.path }) { change ->
            val expanded = expandedPath == change.path
            Card(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { expandedPath = if (expanded) null else change.path }.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Description, null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(change.path, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                if (expanded) stringResource(R.string.ws_hide_diff) else stringResource(R.string.ws_tap_review_diff),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("+${change.additions}", color = PocketGreen)
                        Spacer(Modifier.width(7.dp))
                        Text("-${change.deletions}", color = MaterialTheme.colorScheme.error)
                    }
                    if (expanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.fillMaxWidth().background(Color(0xFF0B0E14)).horizontalScroll(rememberScrollState()),
                        ) {
                            change.diffLines.forEach { line -> DiffLineRow(line) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    expandedPath = null
                                    onUndoFile(change.path)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.ws_undo_file)) }
                            Button(
                                onClick = {
                                    expandedPath = null
                                    onKeepFile(change.path)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.ws_keep_file)) }
                        }
                    }
                }
            }
        }
        if (changes.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onUndo, Modifier.weight(1f)) { Text(stringResource(R.string.ws_undo_task)) }
                Button(onClick = onKeep, Modifier.weight(1f)) { Text(stringResource(R.string.chat_keep_changes)) }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val marker = when (line.type) {
        DiffLineType.ADDITION -> "+"
        DiffLineType.DELETION -> "-"
        DiffLineType.CONTEXT -> " "
        DiffLineType.INFO -> "·"
    }
    val background = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF123226)
        DiffLineType.DELETION -> Color(0xFF3A1D22)
        else -> Color.Transparent
    }
    val foreground = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF83E6B8)
        DiffLineType.DELETION -> Color(0xFFFFA4A4)
        DiffLineType.INFO -> Color(0xFF8993A4)
        DiffLineType.CONTEXT -> Color(0xFFD5DAE3)
    }
    val oldNumber = line.oldLine?.toString().orEmpty().padStart(4)
    val newNumber = line.newLine?.toString().orEmpty().padStart(4)
    Text(
        text = "$oldNumber $newNumber  $marker ${line.text}",
        modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 8.dp, vertical = 2.dp),
        color = foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        softWrap = false,
    )
}

@Composable
internal fun PreviewTab(ready: Boolean, url: String?) {
    var address by rememberSaveable(url) { mutableStateOf(if (ready) url.orEmpty() else "") }
    var activeUrl by rememberSaveable(url) { mutableStateOf(if (ready) url else null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val localUrlError = stringResource(R.string.ws_preview_use_local_url)
    val externalBlockedError = stringResource(R.string.ws_preview_external_blocked)

    val navigate = {
        val normalized = normalizePreviewUrl(address)
        if (normalized == null) {
            addressError = localUrlError
        } else {
            addressError = null
            address = normalized
            activeUrl = normalized
        }
    }

    LaunchedEffect(ready, url) {
        if (ready && !url.isNullOrBlank() && activeUrl == null) {
            normalizePreviewUrl(url)?.let {
                address = it
                activeUrl = it
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            addressError = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.workspace_preview_url)) },
                        placeholder = { Text("localhost:3000") },
                        leadingIcon = {
                            Box(
                                Modifier.size(8.dp).background(
                                    if (activeUrl != null) PocketGreen else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = navigate) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.ws_open_url))
                            }
                        },
                        isError = addressError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { navigate() }),
                    )
                    IconButton(
                        onClick = { webView?.reload() ?: navigate() },
                        enabled = address.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ws_refresh_preview))
                    }
                }
                if (addressError != null) {
                    Text(
                        addressError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 3.dp),
                    )
                } else if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 5.dp))
                }
            }
        }
        val targetUrl = activeUrl
        if (targetUrl == null) {
            EmptyState(Icons.Default.PlayArrow, stringResource(R.string.workspace_preview_not_running), stringResource(R.string.workspace_preview_not_running_description))
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loading = newProgress < 100
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url ?: return true
                                if (!target.isLoopbackPreviewUrl()) {
                                    addressError = externalBlockedError
                                    return true
                                }
                                address = target.toString()
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val target = request?.url ?: return blockedPreviewResponse()
                                return if (target.isLoopbackPreviewUrl()) null else blockedPreviewResponse()
                            }
                        }
                        loadUrl(targetUrl)
                    }
                },
                update = { current ->
                    webView = current
                    if (current.url != targetUrl) current.loadUrl(targetUrl)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun normalizePreviewUrl(input: String): String? {
    val raw = input.trim()
    if (raw.isBlank()) return null
    val withScheme = if ("://" in raw) raw else "http://$raw"
    val parsed = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
    if (!parsed.isLoopbackPreviewUrl() || parsed.host.isNullOrBlank()) return null
    return if (parsed.host == "0.0.0.0") {
        parsed.buildUpon().encodedAuthority(
            buildString {
                append("127.0.0.1")
                if (parsed.port >= 0) append(":${parsed.port}")
            },
        ).build().toString()
    } else {
        parsed.toString()
    }
}

private fun Uri.isLoopbackPreviewUrl(): Boolean =
    scheme in setOf("data", "blob", "about") ||
        (scheme in setOf("http", "https", "ws", "wss") && host in setOf("127.0.0.1", "localhost", "0.0.0.0"))

private fun blockedPreviewResponse(): WebResourceResponse =
    WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))



