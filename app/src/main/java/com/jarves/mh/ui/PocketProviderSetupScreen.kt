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
internal fun ProviderSetupScreen(
    initial: ProviderProfile,
    onboarding: Boolean,
    agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    initialStep: Int = if (onboarding) 0 else 1,
    onBack: (() -> Unit)? = null,
    currentLanguageCode: String = "system",
    onSetLanguage: ((String) -> Unit)? = null,
    onSave: (ProviderProfile, String) -> Unit,
    onDiscover: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    onValidate: suspend (ProviderProfile, String, List<DiscoveredModel>) -> ConnectionValidation,
    onSelectAgent: (AgentKind) -> Unit,
    onToggleTheme: (() -> Unit)? = null,
    themeMode: AppThemeMode = AppThemeMode.DARK,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    var selected by rememberSaveable { mutableStateOf(initial.kind) }
    var baseUrl by rememberSaveable { mutableStateOf(initial.baseUrl.ifBlank { initial.kind.defaultBaseUrl }) }
    var model by rememberSaveable { mutableStateOf(initial.model.ifBlank { initial.kind.defaultModel }) }
    var dshApi by rememberSaveable { mutableStateOf(initial.dshApi.ifBlank { "anthropic-messages" }) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }

    if (showAgentPicker) {
        AgentSwitchSheet(
            selected = agentKind,
            onSelect = { agent ->
                showAgentPicker = false
                onSelectAgent(agent)
            },
            onDismiss = { showAgentPicker = false },
        )
    }

    val handleBack: (() -> Unit)? = when {
        step > 1 -> { { step = 1 } }
        onBack != null -> onBack
        else -> null
    }

    if (handleBack != null) {
        BackHandler(onBack = handleBack)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (onboarding) R.string.setup_mobile_harness else R.string.provider_setup_settings_title)) },
                navigationIcon = {
                    if (handleBack != null) {
                        IconButton(onClick = handleBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (onSetLanguage != null) {
                        LanguagePickerAction(
                            currentLanguageCode = currentLanguageCode,
                            onSelectLanguage = onSetLanguage,
                        )
                    }
                    if (onToggleTheme != null) {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.theme_toggle),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            if (onboarding) StepDots(step)
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> DeviceCheckStep(context, onContinue = { step = 1 })
                1 -> ProviderChoiceStep(
                    selected = selected,
                    agentKind = agentKind,
                    onSelected = {
                        if (selected != it) {
                            selected = it
                            baseUrl = it.defaultBaseUrl
                            model = it.defaultModel
                            apiKey = ""
                        }
                    },
                    onContinue = { step = 2 },
                    onChangeAgent = { showAgentPicker = true },
                )
                else -> ProviderCredentialsStep(
                    provider = selected,
                    agentKind = agentKind,
                    baseUrl = baseUrl,
                    model = model,
                    dshApi = dshApi,
                    apiKey = apiKey,
                    onBaseUrl = {
                        baseUrl = it
                        if (agentKind == AgentKind.DEEPSEEK_HARNESS && selected == ProviderKind.CUSTOM) {
                            dshApi = inferredDshApiForUrl(it)
                        }
                    },
                    onModel = { model = it },
                    onDshApi = { dshApi = it },
                    onApiKey = { apiKey = it },
                    hasStoredSecret = initial.kind == selected && initial.hasSecret,
                    onDiscover = {
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onDiscover(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey)
                    },
                    onValidate = { models ->
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onValidate(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey, models)
                    },
                    onSave = {
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onSave(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey)
                    },
                    onChangeAgent = { showAgentPicker = true },
                )
            }
        }
    }
}

@Composable
private fun DshApiProtocolPicker(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("anthropic-messages", "openai-completions", "openai-responses")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.provider_gateway_protocol),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(option) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .border(
                            width = if (selected == option) 2.dp else 1.dp,
                            color = if (selected == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == option) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Spacer(Modifier.width(10.dp))
                Text(option, fontSize = 13.sp)
            }
        }
        Text(
            stringResource(R.string.provider_gateway_protocol_help),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

@Composable
internal fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                Modifier.height(5.dp).weight(1f)
                    .background(if (index <= step) PocketOrange else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}

@Composable
private fun DeviceCheckStep(context: Context, onContinue: () -> Unit) {
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val memoryInfo = remember { ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo) }
    val totalRamGb = memoryInfo.totalMem.toDouble() / 1_073_741_824.0
    val totalRamLabel = String.format(java.util.Locale.US, "%.1f", totalRamGb)
    val arm64 = supportsArm64Runtime(Build.SUPPORTED_ABIS, System.getProperty("os.arch"))
    val compatible = arm64
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BrandMark()
        Text(stringResource(R.string.provider_device_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.provider_device_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
        CheckRow(Icons.Default.Memory, stringResource(R.string.provider_memory), stringResource(R.string.provider_memory_mode, totalRamLabel, stringResource(if (totalRamGb >= 7.5) R.string.provider_full_mode else R.string.provider_lite_mode)), true)
        CheckRow(Icons.Default.Code, stringResource(R.string.runtime_setup_processor), Build.SUPPORTED_ABIS.firstOrNull() ?: stringResource(R.string.provider_unknown), arm64)
        CheckRow(Icons.Default.Storage, "Android", stringResource(R.string.provider_android_version, Build.VERSION.RELEASE), true)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Text(
                stringResource(R.string.provider_security_warning),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onContinue, enabled = compatible, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(if (compatible) R.string.provider_continue else R.string.runtime_setup_device_not_supported))
        }
    }
}

@Composable
private fun CheckRow(icon: ImageVector, title: String, value: String, passed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(11.dp).size(22.dp), tint = if (passed) PocketGreen else MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Icon(if (passed) Icons.Default.Check else Icons.Default.Warning, null, tint = if (passed) PocketGreen else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ProviderChoiceStep(
    selected: ProviderKind,
    agentKind: AgentKind,
    onSelected: (ProviderKind) -> Unit,
    onContinue: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    val visibleProviders = remember(agentKind) { providersForAgent(agentKind) }
    Column(Modifier.fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.provider_step_of, 2, 3),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PocketOrange,
                letterSpacing = 1.1.sp,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                color = PocketGreen.copy(alpha = 0.10f),
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Shield, null, tint = PocketGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.provider_secure_setup), color = PocketGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.provider_connect_ai), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.provider_connect_ai_desc),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(visibleProviders) { index, provider ->
                    ProviderChoiceRow(
                        provider = provider,
                        selected = selected == provider,
                        onClick = { onSelected(provider) },
                    )
                    if (index != visibleProviders.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.provider_keys_secure),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.provider_continue), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
        TextButton(
            onClick = onChangeAgent,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp),
        ) {
            Text(stringResource(R.string.provider_use_another_agent), fontSize = 12.sp)
        }
    }
}

@StringRes
private fun providerTitleResource(provider: ProviderKind): Int = when (provider) {
    ProviderKind.GEMINI -> R.string.provider_title_gemini
    ProviderKind.OLLAMA -> R.string.provider_title_ollama
    ProviderKind.CLAUDE -> R.string.provider_title_claude
    ProviderKind.ANTHROPIC -> R.string.provider_title_anthropic
    ProviderKind.LLM_ROUTER -> R.string.provider_title_openrouter
    ProviderKind.DEEPSEEK -> R.string.provider_title_deepseek
    ProviderKind.KIMI -> R.string.provider_title_kimi
    ProviderKind.OPENCODE_ZEN -> R.string.provider_title_opencode
    ProviderKind.NVIDIA_NIM -> R.string.provider_title_nvidia
    ProviderKind.CUSTOM -> R.string.provider_title_custom
}

@StringRes
private fun providerSubtitleResource(provider: ProviderKind): Int = when (provider) {
    ProviderKind.GEMINI -> R.string.provider_subtitle_gemini
    ProviderKind.OLLAMA -> R.string.provider_subtitle_ollama
    ProviderKind.CLAUDE -> R.string.provider_subtitle_claude
    ProviderKind.ANTHROPIC -> R.string.provider_subtitle_anthropic
    ProviderKind.LLM_ROUTER -> R.string.provider_subtitle_openrouter
    ProviderKind.DEEPSEEK -> R.string.provider_subtitle_deepseek
    ProviderKind.KIMI, ProviderKind.CUSTOM -> R.string.provider_subtitle_anthropic_compatible
    ProviderKind.OPENCODE_ZEN -> R.string.provider_subtitle_opencode
    ProviderKind.NVIDIA_NIM -> R.string.provider_subtitle_nvidia
}

@Composable
private fun ProviderChoiceRow(
    provider: ProviderKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (provider) {
        ProviderKind.GEMINI -> Color(0xFF4285F4)
        ProviderKind.OLLAMA -> Color(0xFF10B981)
        ProviderKind.CLAUDE -> Color(0xFFD97757)
        ProviderKind.ANTHROPIC -> Color(0xFFE7A26D)
        ProviderKind.LLM_ROUTER -> Color(0xFF5B8DEF)
        ProviderKind.DEEPSEEK -> Color(0xFF4D6BFE)
        ProviderKind.KIMI -> Color(0xFF8B7CF6)
        ProviderKind.OPENCODE_ZEN -> Color(0xFF22C55E)
        ProviderKind.NVIDIA_NIM -> Color(0xFF76B900)
        ProviderKind.CUSTOM -> PocketOrange
    }
    val mark = when (provider) {
        ProviderKind.GEMINI -> "G"
        ProviderKind.OLLAMA -> "OL"
        ProviderKind.CLAUDE -> "C"
        ProviderKind.ANTHROPIC -> "A"
        ProviderKind.LLM_ROUTER -> "OR"
        ProviderKind.DEEPSEEK -> "DS"
        ProviderKind.KIMI -> "K"
        ProviderKind.OPENCODE_ZEN -> "Z"
        ProviderKind.NVIDIA_NIM -> "NV"
        ProviderKind.CUSTOM -> "<>"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(9.dp))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(mark, color = accent, fontSize = if (mark.length > 1) 9.sp else 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(providerTitleResource(provider)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (provider.experimental) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            stringResource(R.string.provider_beta),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(
                stringResource(providerSubtitleResource(provider)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(19.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun ProviderCredentialsStep(
    provider: ProviderKind,
    agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    baseUrl: String,
    model: String,
    dshApi: String = "anthropic-messages",
    apiKey: String,
    onBaseUrl: (String) -> Unit,
    onModel: (String) -> Unit,
    onDshApi: (String) -> Unit = {},
    onApiKey: (String) -> Unit,
    hasStoredSecret: Boolean,
    onDiscover: suspend () -> ModelDiscoveryResult,
    onValidate: suspend (List<DiscoveredModel>) -> ConnectionValidation,
    onSave: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var models by remember(baseUrl) { mutableStateOf(emptyList<DiscoveredModel>()) }
    var isDiscovering by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusDetails by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var showModels by rememberSaveable { mutableStateOf(false) }
    var modelSearch by rememberSaveable { mutableStateOf("") }
    val modelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Custom API servers that are self-hosted (llama.cpp, LM Studio…) often need no key.
    val hasKey = apiKey.isNotBlank() || hasStoredSecret || provider == ProviderKind.CUSTOM
    val filteredModels = remember(models, modelSearch) {
        val query = modelSearch.trim()
        if (query.isEmpty()) models else models.filter {
            it.id.contains(query, ignoreCase = true) || it.displayName.contains(query, ignoreCase = true)
        }
    }

    if (provider == ProviderKind.CLAUDE) {
        ClaudeSubscriptionCredentialsStep(
            token = apiKey,
            hasStoredToken = hasStoredSecret,
            onToken = onApiKey,
            onSave = onSave,
            onChangeAgent = onChangeAgent,
        )
        return
    }

    fun discoverModels(openWhenReady: Boolean = true) {
        scope.launch {
            isDiscovering = true
            status = null
            statusDetails = null
            when (val result = onDiscover()) {
                is ModelDiscoveryResult.Success -> {
                    models = result.models
                    statusOk = true
                    status = context.getString(R.string.provider_models_found, result.models.size)
                    if (model.isBlank() && result.models.isNotEmpty()) onModel(result.models.first().id)
                    if (openWhenReady && result.models.isNotEmpty()) showModels = true
                }
                is ModelDiscoveryResult.Failure -> {
                    statusOk = false
                    status = result.message
                    statusDetails = result.providerMessage
                }
            }
            isDiscovering = false
        }
    }

    if (showModels) {
        ModalBottomSheet(
            onDismissRequest = { showModels = false },
            sheetState = modelSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.provider_available_models), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.provider_models_count, filteredModels.size, models.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(
                        onClick = { discoverModels(openWhenReady = false) },
                        enabled = !isDiscovering,
                    ) {
                        if (isDiscovering) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, stringResource(R.string.provider_refresh_models))
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = modelSearch,
                    onValueChange = { modelSearch = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text(stringResource(R.string.provider_search_models)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (filteredModels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.provider_no_matching_models), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(filteredModels, key = { it.id }) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModel(option.id)
                                        status = null
                                        modelSearch = ""
                                        showModels = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.displayName, modifier = Modifier.weight(1f, fill = false), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (option.isFree) Text(stringResource(R.string.provider_free), color = Color(0xFF58C99C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (option.displayName != option.id) {
                                        Text(option.id, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Box(
                                    Modifier.size(20.dp).border(
                                        if (model == option.id) 2.dp else 1.dp,
                                        if (model == option.id) PocketOrange else MaterialTheme.colorScheme.outline,
                                        CircleShape,
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (model == option.id) Box(Modifier.size(9.dp).background(PocketOrange, CircleShape))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.provider_step_of, 3, 3), color = PocketOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.provider_encrypted_locally), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(providerTitleResource(provider)), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    agentKind == AgentKind.DEEPSEEK_HARNESS -> stringResource(R.string.provider_endpoint_deepseek)
                    provider.protocol.name.startsWith("OPENAI") -> stringResource(R.string.provider_endpoint_translate)
                    else -> stringResource(R.string.provider_endpoint_claude)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        baseUrl,
                        { onBaseUrl(it); status = null; statusDetails = null; models = emptyList() },
                        label = { Text(stringResource(R.string.provider_base_url)) },
                        supportingText = {
                            if (provider.fixedBaseUrl) Text(stringResource(R.string.provider_fixed_by, stringResource(providerTitleResource(provider))))
                        },
                        readOnly = provider.fixedBaseUrl,
                        enabled = !provider.fixedBaseUrl,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (agentKind == AgentKind.DEEPSEEK_HARNESS && provider in DSH_PROTOCOL_PROVIDERS && !provider.fixedProtocol) {
                        DshApiProtocolPicker(selected = dshApi, onSelected = { onDshApi(it); status = null })
                    }
                    OutlinedTextField(
                        apiKey,
                        { onApiKey(it); status = null; statusDetails = null },
                        label = { Text(stringResource(R.string.provider_api_key)) },
                        placeholder = { Text(stringResource(if (hasStoredSecret) R.string.provider_saved_key_placeholder else R.string.provider_enter_key)) },
                        supportingText = {
                            if (hasStoredSecret && apiKey.isBlank()) Text(stringResource(R.string.provider_saved_key_ready))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        model,
                        { onModel(it); status = null; statusDetails = null },
                        label = { Text(stringResource(R.string.provider_model_name)) },
                        supportingText = { Text(stringResource(R.string.provider_model_help)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    if (models.isEmpty()) discoverModels() else showModels = true
                },
                enabled = baseUrl.isNotBlank() && hasKey && !isDiscovering && !isValidating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isDiscovering) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(if (models.isEmpty()) Icons.Default.Search else Icons.Default.KeyboardArrowDown, null, Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (models.isEmpty()) stringResource(R.string.provider_find_models) else stringResource(R.string.provider_available_models_count, models.size))
            }
        }
        if (status != null) {
            item {
                Text(
                    status.orEmpty(),
                    color = if (statusOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
                statusDetails?.takeIf(String::isNotBlank)?.let { details ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        details,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
        item {
            Button(
                    onClick = {
                        scope.launch {
                            isValidating = true
                            status = context.getString(R.string.provider_checking_settings)
                            statusDetails = null
                            statusOk = true
                            when (val result = onValidate(models)) {
                                is ConnectionValidation.Success -> {
                                    status = result.message
                                    statusOk = true
                                    onSave()
                                }
                                is ConnectionValidation.Failure -> {
                                    status = result.message
                                    statusDetails = result.providerMessage
                                    statusOk = false
                                }
                            }
                            isValidating = false
                        }
                    },
                    enabled = baseUrl.isNotBlank() && model.isNotBlank() && hasKey && !isDiscovering && !isValidating,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(stringResource(if (isValidating) R.string.provider_checking else R.string.provider_continue))
            }
        }
        item {
            TextButton(
                onClick = onChangeAgent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.provider_use_another_agent), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ClaudeSubscriptionCredentialsStep(
    token: String,
    hasStoredToken: Boolean,
    onToken: (String) -> Unit,
    onSave: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    val hasToken = token.isNotBlank() || hasStoredToken

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.provider_step_of, 3, 3), color = PocketOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.provider_encrypted_locally), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.provider_title_claude), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.provider_claude_connect_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.provider_claude_step_one), fontSize = 13.sp)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "claude setup-token",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            color = PocketOrange,
                        )
                    }
                    Text(stringResource(R.string.provider_claude_step_two), fontSize = 13.sp)
                    OutlinedTextField(
                        value = token,
                        onValueChange = onToken,
                        label = { Text(stringResource(R.string.provider_claude_token)) },
                        placeholder = { Text(stringResource(if (hasStoredToken) R.string.provider_saved_key_placeholder else R.string.provider_paste_token)) },
                        supportingText = if (hasStoredToken && token.isBlank()) ({ Text(stringResource(R.string.provider_saved_token_ready)) }) else null,
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, stringResource(R.string.provider_toggle_token_visibility))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            Button(
                onClick = onSave,
                enabled = hasToken,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(stringResource(R.string.provider_save_continue))
            }
        }
        item {
            TextButton(onClick = onChangeAgent, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.provider_use_another_agent), fontSize = 12.sp)
            }
        }
    }
}
