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
internal fun AntigravityOnboardingScreen(
    state: AppUiState,
    currentLanguageCode: String,
    onSetLanguage: (String) -> Unit,
    onStartLogin: () -> Unit,
    onSubmitCode: (String) -> Unit,
    onContinue: () -> Unit,
    onSelectAgent: (AgentKind) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }
    if (showAgentPicker) {
        AgentSwitchSheet(
            selected = AgentKind.ANTIGRAVITY,
            onSelect = { agent ->
                showAgentPicker = false
                onSelectAgent(agent)
            },
            onDismiss = { showAgentPicker = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ws_agy_setup_title)) },
                actions = {
                    LanguagePickerAction(currentLanguageCode, onSetLanguage)
                    IconButton(onClick = onToggleTheme) { Icon(Icons.Default.DarkMode, stringResource(R.string.theme_toggle)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.ws_agy_connect_google), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.ws_agy_connect_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (state.antigravityAuth.status) {
                AntigravityAuthStatus.SIGNED_OUT, AntigravityAuthStatus.ERROR -> {
                    state.antigravityAuth.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = onStartLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.agent_google_signin))
                    }
                }
                AntigravityAuthStatus.STARTING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.ws_agy_starting_login))
                }
                AntigravityAuthStatus.COMPLETING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.ws_agy_completing_signin))
                }
                AntigravityAuthStatus.AWAITING_CODE -> {
                    Text(stringResource(R.string.ws_agy_signin_opened))
                    state.antigravityAuth.authorizationUrl?.let { url ->
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(url)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ws_agy_copy_signin_url))
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(R.string.ws_agy_authorization_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onSubmitCode(code); code = "" },
                        enabled = code.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.ws_agy_complete_signin)) }
                }
                AntigravityAuthStatus.SIGNED_IN -> {
                    Surface(color = PocketGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                        Text(
                            state.antigravityAuth.accountEmail?.let { stringResource(R.string.ws_agy_connected_as, it) } ?: stringResource(R.string.ws_agy_google_connected),
                            Modifier.fillMaxWidth().padding(16.dp),
                            color = PocketGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            }
            TextButton(
                onClick = { showAgentPicker = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.provider_use_another_agent), fontSize = 12.sp)
            }
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp)) {
                Text(
                    stringResource(R.string.ws_agy_auto_approval_warning),
                    Modifier.fillMaxWidth().padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InitialLanguageSetupScreen(
    currentLanguageCode: String,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("MindSparQ Harness", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.theme_toggle),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.language_welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.language_welcome_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    AppLanguage.entries.forEachIndexed { index, lang ->
                        val isSelected = (currentLanguageCode == lang.code) ||
                            (currentLanguageCode.isBlank() && lang == AppLanguage.SYSTEM)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLanguage(lang.code) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.SYSTEM) {
                                        stringResource(R.string.language_system_default)
                                    } else {
                                        lang.nativeName
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                if (lang != AppLanguage.SYSTEM) {
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.ws_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (index != AppLanguage.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(R.string.action_continue),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
internal fun LanguagePickerAction(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
) {
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { showLanguageDialog = true }) {
        Icon(
            Icons.Default.Language,
            contentDescription = stringResource(R.string.language_choose_title),
        )
    }
    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguageCode = currentLanguageCode,
            onSelectLanguage = {
                onSelectLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
internal fun LanguagePickerDialog(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Language, contentDescription = null) },
        title = { Text(stringResource(R.string.language_choose_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    val selected = AppLanguage.fromCode(currentLanguageCode) == language
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLanguage(language.code) }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (language == AppLanguage.SYSTEM) stringResource(R.string.language_system_default)
                            else language.nativeName,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundTaskSetupScreen(
    currentLanguageCode: String = "system",
    onSetLanguage: (String) -> Unit = {},
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val powerManager = context.getSystemService(PowerManager::class.java)
    fun notificationsAllowed(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun batteryUnrestricted(): Boolean = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var notificationGranted by remember { mutableStateOf(notificationsAllowed()) }
    var batteryGranted by remember { mutableStateOf(batteryUnrestricted()) }
    var notificationDenied by rememberSaveable { mutableStateOf(false) }
    var taskProtectionConfirmed by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = notificationsAllowed()
        notificationDenied = !granted
        if (notificationGranted) currentStep = 1
    }
    val notificationSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationGranted = notificationsAllowed()
        if (notificationGranted) currentStep = 1
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryGranted = batteryUnrestricted()
        if (batteryGranted) currentStep = 2
    }

    LaunchedEffect(Unit) {
        notificationGranted = notificationsAllowed()
        batteryGranted = batteryUnrestricted()
    }

    val currentIcon = when (currentStep) {
        0 -> Icons.Default.Notifications
        1 -> Icons.Default.BatterySaver
        else -> Icons.Default.Shield
    }
    val currentTitle = when (currentStep) {
        0 -> stringResource(R.string.bg_notifications_title)
        1 -> stringResource(R.string.bg_reliability_title)
        else -> stringResource(R.string.bg_protection_title)
    }
    val currentDescription = when (currentStep) {
        0 -> stringResource(R.string.bg_notifications_desc)
        1 -> stringResource(R.string.bg_reliability_desc)
        else -> stringResource(R.string.bg_protection_desc)
    }
    val currentPrivacyNote = when (currentStep) {
        0 -> stringResource(R.string.bg_notifications_privacy)
        1 -> stringResource(R.string.bg_reliability_privacy)
        else -> stringResource(R.string.bg_protection_privacy)
    }
    val currentGranted = when (currentStep) {
        0 -> notificationGranted
        1 -> batteryGranted
        else -> taskProtectionConfirmed
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("MindSparQ Harness", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    LanguagePickerAction(currentLanguageCode, onSetLanguage)
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.theme_toggle),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.bg_setup_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.bg_setup_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(18.dp))
            StepDots(currentStep)
            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    PermissionSummaryRow(Icons.Default.Notifications, stringResource(R.string.bg_step_notifications), notificationGranted, currentStep == 0)
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PermissionSummaryRow(Icons.Default.BatterySaver, stringResource(R.string.bg_step_background), batteryGranted, currentStep == 1)
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PermissionSummaryRow(Icons.Default.Shield, stringResource(R.string.bg_step_task_protection), taskProtectionConfirmed, currentStep == 2)
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(currentIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.bg_step_indicator, currentStep + 1, 3), color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Text(currentTitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (currentGranted) Icon(Icons.Default.Check, stringResource(R.string.bg_status_granted), tint = PocketGreen)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(currentDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(currentPrivacyNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            when (currentStep) {
                                0 -> when {
                                    notificationGranted -> currentStep = 1
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationDenied -> {
                                        // Targets below API 33 can have notification prompts tied to
                                        // channel creation. Create channels only after this explicit tap.
                                        RuntimeExecutionService.ensureNotificationChannels(context)
                                        RuntimeSetupService.ensureNotificationChannel(context)
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    else -> notificationSettingsLauncher.launch(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                            Settings.EXTRA_APP_PACKAGE,
                                            context.packageName,
                                        ),
                                    )
                                }
                                1 -> if (batteryGranted) {
                                    currentStep = 2
                                } else {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    runCatching { batteryLauncher.launch(intent) }
                                        .onFailure {
                                            batteryLauncher.launch(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:${context.packageName}"),
                                                ),
                                            )
                                        }
                                }
                                else -> {
                                    taskProtectionConfirmed = true
                                    onContinue()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            when (currentStep) {
                                0 -> if (notificationGranted) stringResource(R.string.action_next) else if (notificationDenied) stringResource(R.string.bg_notifications_settings) else stringResource(R.string.bg_notifications_allow)
                                1 -> if (batteryGranted) stringResource(R.string.action_next) else stringResource(R.string.bg_reliability_settings)
                                else -> stringResource(R.string.bg_protection_enable)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                    if (currentStep < 2 && !currentGranted) {
                        TextButton(
                            onClick = { currentStep += 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (currentStep == 0) stringResource(R.string.bg_notifications_skip) else stringResource(R.string.bg_reliability_skip))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.bg_setup_footer),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PermissionSummaryRow(
    icon: ImageVector,
    title: String,
    complete: Boolean,
    active: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            tint = if (active) MaterialTheme.colorScheme.primary else if (complete) PocketGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
        when {
            complete -> Icon(Icons.Default.Check, stringResource(R.string.bg_status_granted), tint = PocketGreen, modifier = Modifier.size(18.dp))
            active -> Text(stringResource(R.string.bg_status_required), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            else -> Text(stringResource(R.string.bg_status_next), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}


