package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.R
import com.jarves.mh.data.ApiKeyInfo
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.ui.theme.PocketOrange
import com.mindsparq.jarvis.core.designsystem.components.OfflineModelCard
import com.mindsparq.jarvis.core.designsystem.components.OfflineModelUiItem
import kotlinx.coroutines.launch

enum class AiExecutionMode {
    CLOUD,
    OFFLINE_LOCAL,
    ANTIGRAVITY_CLI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAiProviderSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSelectProvider: (ProviderKind) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveProvider: (ProviderProfile, String) -> Unit,
    onPing: () -> Unit,
    getSavedApiKeys: (ProviderKind) -> List<ApiKeyInfo>,
    onAddApiKey: (ProviderKind, String, String) -> List<ApiKeyInfo>,
    onActivateApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onRemoveApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onDiscoverModels: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    isModelDownloaded: (String) -> Boolean = { false },
    onDownloadOfflineModel: (String, (Float) -> Unit, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteOfflineModel: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var executionMode by rememberSaveable {
        mutableStateOf(
            if (state.agentKind == AgentKind.ANTIGRAVITY) AiExecutionMode.ANTIGRAVITY_CLI
            else if (state.provider.kind == ProviderKind.OLLAMA) AiExecutionMode.OFFLINE_LOCAL
            else AiExecutionMode.CLOUD
        )
    }

    var providerDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var addKeyExpanded by rememberSaveable { mutableStateOf(false) }
    var newKeyName by rememberSaveable { mutableStateOf("") }
    var newApiKey by rememberSaveable { mutableStateOf("") }
    var newKeyVisible by rememberSaveable { mutableStateOf(false) }
    var isDiscoveringModels by remember { mutableStateOf(false) }
    var discoveredModels by remember { mutableStateOf<List<DiscoveredModel>>(emptyList()) }
    var modelDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // Offline Model States
    var llamaDownloading by remember { mutableStateOf(false) }
    var llamaProgress by remember { mutableFloatStateOf(0f) }
    var llamaDownloaded by rememberSaveable { mutableStateOf(isModelDownloaded("llama-3.2-3b-q4")) }

    val savedKeys = remember(state.provider.kind, addKeyExpanded) {
        getSavedApiKeys(state.provider.kind)
    }

    val activeKey = savedKeys.firstOrNull { it.isActive } ?: savedKeys.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Unified AI Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Configure providers, models, multi-key pool & offline intelligence",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            when (state.apiPingStatus) {
                                ApiPingStatus.OK -> Color(0xFF10B981).copy(alpha = 0.15f)
                                ApiPingStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ApiPingStatus.PINGING -> PocketOrange.copy(alpha = 0.15f)
                                ApiPingStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        when (state.apiPingStatus) {
                            ApiPingStatus.OK -> "HEALTHY"
                            ApiPingStatus.FAILED -> "FAILED"
                            ApiPingStatus.PINGING -> "TESTING..."
                            ApiPingStatus.IDLE -> "READY"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (state.apiPingStatus) {
                            ApiPingStatus.OK -> Color(0xFF10B981)
                            ApiPingStatus.FAILED -> MaterialTheme.colorScheme.error
                            ApiPingStatus.PINGING -> PocketOrange
                            ApiPingStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // 1. Execution Mode Segmented Control
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Cloud Mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                executionMode = AiExecutionMode.CLOUD
                                if (state.provider.kind == ProviderKind.OLLAMA) {
                                    onSelectProvider(ProviderKind.GEMINI)
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (executionMode == AiExecutionMode.CLOUD) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (executionMode == AiExecutionMode.CLOUD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Cloud API",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (executionMode == AiExecutionMode.CLOUD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Offline Local Mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                executionMode = AiExecutionMode.OFFLINE_LOCAL
                                onSelectProvider(ProviderKind.OLLAMA)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (executionMode == AiExecutionMode.OFFLINE_LOCAL) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (executionMode == AiExecutionMode.OFFLINE_LOCAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Offline Local",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (executionMode == AiExecutionMode.OFFLINE_LOCAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Antigravity CLI Mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                executionMode = AiExecutionMode.ANTIGRAVITY_CLI
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (executionMode == AiExecutionMode.ANTIGRAVITY_CLI) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (executionMode == AiExecutionMode.ANTIGRAVITY_CLI) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Google CLI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (executionMode == AiExecutionMode.ANTIGRAVITY_CLI) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Mode Details
            when (executionMode) {
                AiExecutionMode.CLOUD -> {
                    // Cloud Provider Selector
                    Text("ACTIVE CLOUD PROVIDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { providerDropdownExpanded = !providerDropdownExpanded },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(state.provider.kind.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(state.provider.kind.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if (providerDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        }
                    }

                    AnimatedVisibility(providerDropdownExpanded) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        ) {
                            Column {
                                listOf(
                                    ProviderKind.GEMINI,
                                    ProviderKind.LLM_ROUTER,
                                    ProviderKind.NVIDIA_NIM,
                                    ProviderKind.DEEPSEEK,
                                    ProviderKind.OPENCODE_ZEN,
                                    ProviderKind.ANTHROPIC,
                                ).forEachIndexed { index, provider ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectProvider(provider)
                                                providerDropdownExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(provider.title, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                                            Text(provider.subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (state.provider.kind == provider) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (index < 5) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }

                    // Model Selection
                    Text("ACTIVE MODEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.provider.model,
                        onValueChange = onSelectModel,
                        label = { Text("Model Name / ID") },
                        trailingIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    isDiscoveringModels = true
                                    val key = activeKey?.let { "" } ?: ""
                                    val res = onDiscoverModels(state.provider, key)
                                    if (res is ModelDiscoveryResult.Success) {
                                        discoveredModels = res.models
                                    }
                                    isDiscoveringModels = false
                                }
                            }) {
                                if (isDiscoveringModels) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Search, "Discover models")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Multi-Key Management
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("API KEY POOL (AUTO-FAILOVER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { addKeyExpanded = !addKeyExpanded }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (addKeyExpanded) "Cancel" else "Add Key", fontSize = 12.sp)
                        }
                    }

                    if (savedKeys.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ) {
                            Column {
                                savedKeys.forEachIndexed { index, key ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onActivateApiKey(state.provider.kind, key.id) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(key.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            Text(
                                                if (key.isActive) "Active · Primary execution" else "Backup Key · Failover eligible",
                                                fontSize = 10.5.sp,
                                                color = if (key.isActive) PocketOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (key.isActive) {
                                            Icon(Icons.Default.Check, null, tint = PocketOrange, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onRemoveApiKey(state.provider.kind, key.id) }) {
                                            Icon(Icons.Default.Delete, "Remove key", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (index != savedKeys.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(addKeyExpanded) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = newKeyName,
                                    onValueChange = { newKeyName = it },
                                    label = { Text("Key Label (e.g. Personal, Work, Backup)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = newApiKey,
                                    onValueChange = { newApiKey = it },
                                    label = { Text("API Secret Key") },
                                    singleLine = true,
                                    visualTransformation = if (newKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { newKeyVisible = !newKeyVisible }) {
                                            Icon(if (newKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        onAddApiKey(state.provider.kind, newKeyName, newApiKey)
                                        newKeyName = ""
                                        newApiKey = ""
                                        addKeyExpanded = false
                                    },
                                    enabled = newKeyName.isNotBlank() && newApiKey.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Save to Encrypted Pool")
                                }
                            }
                        }
                    }

                    // Test Connection Button
                    OutlinedButton(
                        onClick = onPing,
                        enabled = state.apiPingStatus != ApiPingStatus.PINGING,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.apiPingStatus == ApiPingStatus.PINGING) "Verifying Endpoint & Key..." else "Test Active Connection")
                    }
                }

                AiExecutionMode.OFFLINE_LOCAL -> {
                    Text("LOCAL OFFLINE INTELLIGENCE (LLAMA 3.2)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OfflineModelCard(
                        model = OfflineModelUiItem(
                            id = "llama-3.2-3b-q4",
                            title = "Llama 3.2 3B Instruct",
                            size = "1.95 GB",
                            quantization = "Q4_K_M",
                            tag = "Recommended for SCV45 (12GB RAM)",
                            description = "High capability offline coding & reasoning model. Operates with zero network access.",
                            isRecommended = true,
                            recommendationBadge = "OPTIMAL",
                            isDownloaded = llamaDownloaded,
                            isDownloading = llamaDownloading,
                            progress = llamaProgress,
                        ),
                        isActive = state.provider.kind == ProviderKind.OLLAMA,
                        onDownload = {
                            llamaDownloading = true
                            onDownloadOfflineModel(
                                "llama-3.2-3b-q4",
                                { prog -> llamaProgress = prog },
                                { success ->
                                    llamaDownloading = false
                                    if (success) llamaDownloaded = true
                                }
                            )
                        },
                        onPause = {},
                        onResume = {},
                        onDelete = {
                            onDeleteOfflineModel("llama-3.2-3b-q4")
                            llamaDownloaded = false
                        },
                        onSetActive = {
                            onSelectProvider(ProviderKind.OLLAMA)
                            onSelectModel("llama-3.2-3b-q4")
                        },
                    )

                    Spacer(Modifier.height(8.dp))

                    OfflineModelCard(
                        model = OfflineModelUiItem(
                            id = "qwen2.5-coder-1.5b-q4",
                            title = "Qwen 2.5 Coder 1.5B",
                            size = "1.05 GB",
                            quantization = "Q4_K_M",
                            tag = "Fast & Lightweight",
                            description = "Optimized for quick autocomplete, function scaffolding, and light reasoning.",
                            isRecommended = false,
                            isDownloaded = false,
                        ),
                        isActive = false,
                        onDownload = {},
                        onPause = {},
                        onResume = {},
                        onDelete = {},
                        onSetActive = {},
                    )
                }

                AiExecutionMode.ANTIGRAVITY_CLI -> {
                    Text("GOOGLE ANTIGRAVITY CLI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(0xFF4285F4), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.antigravityAuth.accountEmail?.let { "Connected: $it" } ?: "Google Account Connected",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                )
                            }
                            Text(
                                "Executes official Google Antigravity CLI (agy) inside private Linux PRoot. Consumes Antigravity cloud subscription credits.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PocketOrange.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    "Notice: If credits run out, MindSparQ auto-switches to your Gemini/OpenRouter API key seamlessly.",
                                    fontSize = 10.5.sp,
                                    color = PocketOrange,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
