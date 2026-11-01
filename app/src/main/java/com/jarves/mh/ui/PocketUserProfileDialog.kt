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
internal fun MindSparQUserProfileDialog(
    state: AppUiState,
    onDismiss: () -> Unit,
    onStartGoogleAuth: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onStartGitHubLogin: () -> Unit,
    onDisconnectGitHub: () -> Unit,
    onOpenFileManager: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F141C),
            border = BorderStroke(1.dp, Color(0xFF1E2638)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.mindsparq_logo),
                                    contentDescription = "MindSparQ Logo",
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Developer Profile", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFF0F6FC))
                            Text("MindSparQ Cloud & Git Identities", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Google Account Card
                val isGoogleConnected = state.antigravityAuth.status == AntigravityAuthStatus.SIGNED_IN
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, if (isGoogleConnected) Color(0xFF2EA043).copy(alpha = 0.4f) else Color(0xFF222F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("G", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4285F4))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Google Account", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        if (isGoogleConnected) state.antigravityAuth.accountEmail ?: "Signed in" else "Not connected",
                                        fontSize = 11.sp,
                                        color = if (isGoogleConnected) Color(0xFF2EA043) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isGoogleConnected) {
                                TextButton(onClick = onGoogleSignOut) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = onStartGoogleAuth,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Sign In", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Authenticates official Google Antigravity & Gemini 2.5/3.0 intelligence models.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                // GitHub Account Card
                val isGitHubConnected = state.githubAuthStatus == GitHubAuthStatus.CONNECTED
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, if (isGitHubConnected) Color(0xFF2EA043).copy(alpha = 0.4f) else Color(0xFF222F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("GitHub Account", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        if (isGitHubConnected) "@${state.githubLogin}" else "Not connected",
                                        fontSize = 11.sp,
                                        color = if (isGitHubConnected) Color(0xFF2EA043) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isGitHubConnected) {
                                TextButton(onClick = onDisconnectGitHub) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = onStartGitHubLogin,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043))
                                ) {
                                    Text("Connect", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enables 1-click publishing of created projects directly to your GitHub profile.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        if (state.githubAuthStatus == GitHubAuthStatus.AWAITING_USER && state.githubUserCode != null) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    clipboard.setText(AnnotatedString(state.githubUserCode.orEmpty()))
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        state.githubUserCode.orEmpty(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        letterSpacing = 2.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text("Tap to copy code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(state.githubUserCode.orEmpty()))
                                    Toast.makeText(context, "Opening GitHub...", Toast.LENGTH_SHORT).show()
                                    val uri = Uri.parse(state.githubVerificationUri ?: "https://github.com/login/device")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open GitHub Verification", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Public Documents Directory Info
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161E2E).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF222F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Public Documents Directory", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                            Text("Documents/MindSparQ-Mobile-Jarvis", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        }
                        IconButton(onClick = onOpenFileManager) {
                            Icon(Icons.Default.Folder, contentDescription = "Open Folder", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}


