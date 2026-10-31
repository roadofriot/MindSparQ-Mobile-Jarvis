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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAiProviderSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSelectAgent: (AgentKind) -> Unit = {},
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

    var agentDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var providerDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var addKeyExpanded by rememberSaveable { mutableStateOf(false) }
    var newKeyName by rememberSaveable { mutableStateOf("") }
    var newApiKey by rememberSaveable { mutableStateOf("") }
    var newKeyVisible by rememberSaveable { mutableStateOf(false) }
    var isDiscoveringModels by remember { mutableStateOf(false) }
    var discoveredModels by remember { mutableStateOf<List<DiscoveredModel>>(emptyList()) }
    var showOfflineModelsSection by rememberSaveable { mutableStateOf(false) }

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
                        "Universal AI Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Multi-Agent CLI · Multi-Key Failover · Offline Models",
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

            // =========================================================================
            // 1. ACTIVE CLI AGENT DROPDOWN
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "COMMAND LINE CLI AGENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agentDropdownExpanded = !agentDropdownExpanded },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    when (state.agentKind) {
                                        AgentKind.CLAUDE_CODE -> Color(0xFFD97757).copy(alpha = 0.15f)
                                        AgentKind.DEEPSEEK_HARNESS -> Color(0xFF4D6BFE).copy(alpha = 0.15f)
                                        AgentKind.ANTIGRAVITY -> Color(0xFF4285F4).copy(alpha = 0.15f)
                                        AgentKind.OPENCODE -> Color(0xFF00B4D8).copy(alpha = 0.15f)
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = when (state.agentKind) {
                                    AgentKind.ANTIGRAVITY -> Icons.Default.SmartToy
                                    AgentKind.OPENCODE -> Icons.Default.AutoAwesome
                                    else -> Icons.Default.Terminal
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = when (state.agentKind) {
                                    AgentKind.CLAUDE_CODE -> Color(0xFFD97757)
                                    AgentKind.DEEPSEEK_HARNESS -> Color(0xFF4D6BFE)
                                    AgentKind.ANTIGRAVITY -> Color(0xFF4285F4)
                                    AgentKind.OPENCODE -> Color(0xFF00B4D8)
                                },
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.agentKind.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(state.agentKind.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (agentDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                }

                AnimatedVisibility(agentDropdownExpanded) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Column {
                            listOf(
                                AgentKind.DEEPSEEK_HARNESS,
                                AgentKind.CLAUDE_CODE,
                                AgentKind.ANTIGRAVITY,
                                AgentKind.OPENCODE,
                            ).forEachIndexed { index, agent ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectAgent(agent)
                                            agentDropdownExpanded = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(agent.title, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                                        Text(agent.subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (state.agentKind == agent) {
                                        Icon(Icons.Default.Check, null, tint = PocketOrange, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < 3) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. AI PROVIDER & PROTOCOL SELECTION
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "AI BACKEND PROVIDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { providerDropdownExpanded = !providerDropdownExpanded },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (state.provider.kind == ProviderKind.OLLAMA) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else PocketOrange.copy(alpha = 0.15f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (state.provider.kind == ProviderKind.OLLAMA) Icons.Default.Storage else Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (state.provider.kind == ProviderKind.OLLAMA) Color(0xFF10B981) else PocketOrange,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.provider.kind.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (state.provider.kind == ProviderKind.OLLAMA) "100% Free · Local Offline · Zero API Key"
                                else state.provider.kind.subtitle,
                                fontSize = 11.sp,
                                color = if (state.provider.kind == ProviderKind.OLLAMA) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(if (providerDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                }

                AnimatedVisibility(providerDropdownExpanded) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Column {
                            listOf(
                                ProviderKind.GEMINI,
                                ProviderKind.LLM_ROUTER,
                                ProviderKind.NVIDIA_NIM,
                                ProviderKind.OLLAMA,
                                ProviderKind.DEEPSEEK,
                                ProviderKind.OPENCODE_ZEN,
                                ProviderKind.ANTHROPIC,
                                ProviderKind.CUSTOM,
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(provider.title, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                                            if (provider == ProviderKind.OLLAMA) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                ) {
                                                    Text(
                                                        "OFFLINE · NO KEY",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    )
                                                }
                                            } else if (provider == ProviderKind.GEMINI || provider == ProviderKind.LLM_ROUTER || provider == ProviderKind.NVIDIA_NIM) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                                ) {
                                                    Text(
                                                        "FREE TIER",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF3B82F6),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Text(provider.subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (state.provider.kind == provider) {
                                        Icon(Icons.Default.Check, null, tint = PocketOrange, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < 7) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. ACTIVE MODEL SELECTION & DISCOVERED PRESETS
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ACTIVE MODEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.provider.model,
                    onValueChange = onSelectModel,
                    label = { Text("Model Name / ID") },
                    trailingIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isDiscoveringModels = true
                                val res = onDiscoverModels(state.provider, "")
                                if (res is ModelDiscoveryResult.Success) {
                                    discoveredModels = res.models
                                }
                                isDiscoveringModels = false
                            }
                        }) {
                            if (isDiscoveringModels) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Search, "Discover models from provider")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // Quick Model Suggestions or Discovered Models List
                val displayedModels = remember(discoveredModels, state.provider.kind) {
                    if (discoveredModels.isNotEmpty()) {
                        discoveredModels
                    } else {
                        when (state.provider.kind) {
                            ProviderKind.GEMINI -> listOf(
                                DiscoveredModel("gemini-4.0-flash", "Gemini 4.0 Flash (Next-Gen Autonomous)", isFree = true),
                                DiscoveredModel("gemini-4-argon", "Gemini 4 Argon (Experimental Reasoning)", isFree = true),
                                DiscoveredModel("gemini-3.8-pro", "Gemini 3.8 Pro (Ultra-Deep Logic)", isFree = true),
                                DiscoveredModel("gemini-3.8-flash", "Gemini 3.8 Flash (High-Speed)", isFree = true),
                                DiscoveredModel("gemini-2.5-flash", "Gemini 2.5 Flash (Fast & Multimodal)", isFree = true),
                                DiscoveredModel("gemini-2.5-pro", "Gemini 2.5 Pro (Deep Reasoning)", isFree = true),
                                DiscoveredModel("gemini-2.0-flash", "Gemini 2.0 Flash", isFree = true),
                            )
                            ProviderKind.LLM_ROUTER -> listOf(
                                DiscoveredModel("qwen/qwen-2.5-coder-32b-instruct:free", "Qwen 2.5 Coder 32B (Verified Free)", isFree = true),
                                DiscoveredModel("deepseek/deepseek-r1:free", "DeepSeek R1 (Verified Free)", isFree = true),
                                DiscoveredModel("deepseek/deepseek-chat:free", "DeepSeek Chat (Verified Free)", isFree = true),
                                DiscoveredModel("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash Exp (Verified Free)", isFree = true),
                                DiscoveredModel("google/gemini-2.0-flash-thinking-exp:free", "Gemini 2.0 Flash Thinking (Verified Free)", isFree = true),
                                DiscoveredModel("mistralai/mistral-small-24b-instruct-2501:free", "Mistral Small 24B (Verified Free)", isFree = true),
                                DiscoveredModel("meta-llama/llama-3.2-3b-instruct:free", "Llama 3.2 3B (Verified Free)", isFree = true),
                                DiscoveredModel("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B (Verified Free)", isFree = true),
                            )
                            ProviderKind.OLLAMA -> listOf(
                                DiscoveredModel("qwen2.5-coder:1.5b", "Qwen 2.5 Coder 1.5B (Local Offline)", isFree = true),
                                DiscoveredModel("qwen2.5-coder:0.5b", "Qwen 2.5 Coder 0.5B (Ultralight)", isFree = true),
                                DiscoveredModel("deepseek-r1:1.5b", "DeepSeek R1 1.5B (Local Reasoning)", isFree = true),
                            )
                            ProviderKind.NVIDIA_NIM -> listOf(
                                DiscoveredModel("meta/llama-3.3-70b-instruct", "NVIDIA Llama 3.3 70B (Free Tier)", isFree = true),
                                DiscoveredModel("deepseek-ai/deepseek-r1", "NVIDIA DeepSeek R1 (Free Tier)", isFree = true),
                            )
                            ProviderKind.DEEPSEEK -> listOf(
                                DiscoveredModel("deepseek-chat", "DeepSeek V3 (Chat/Coding)", isFree = false),
                                DiscoveredModel("deepseek-reasoner", "DeepSeek R1 (Reasoner)", isFree = false),
                            )
                            ProviderKind.ANTHROPIC -> listOf(
                                DiscoveredModel("claude-sonnet-4-6", "Claude Sonnet 4.6", isFree = false),
                                DiscoveredModel("claude-3-7-sonnet-latest", "Claude 3.7 Sonnet", isFree = false),
                            )
                            else -> emptyList()
                        }
                    }
                }

                if (displayedModels.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (discoveredModels.isNotEmpty()) "DISCOVERED MODELS (${displayedModels.size})" else "RECOMMENDED MODELS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (discoveredModels.isEmpty()) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isDiscoveringModels = true
                                                val res = onDiscoverModels(state.provider, "")
                                                if (res is ModelDiscoveryResult.Success) {
                                                    discoveredModels = res.models
                                                }
                                                isDiscoveringModels = false
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text("Discover live", fontSize = 11.sp, color = PocketOrange)
                                    }
                                }
                            }
                            displayedModels.take(8).forEach { model ->
                                val isSelected = state.provider.model == model.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectModel(model.id) }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.displayName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) PocketOrange else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            model.id,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (model.isFree) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                        ) {
                                            Text(
                                                "FREE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PocketOrange,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. MULTI-KEY POOL (GMAIL KEY ROTATION / AUTO-FAILOVER)
            // =========================================================================
            if (state.provider.kind != ProviderKind.OLLAMA) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "API KEY POOL (${savedKeys.size} KEYS)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Auto-failover on rate limits (HTTP 429) across your Gmail keys",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
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
                                                if (key.isActive) "Active · Primary Key" else "Backup Key · Failover Ready",
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
                                    label = { Text("Account Label (e.g. Gmail 1, Gmail 2, Work)") },
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
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.apiPingStatus == ApiPingStatus.PINGING) "Verifying Endpoint & Key..." else "Test Active Key Connection")
                    }
                }
            } else {
                // Ollama Notice
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Local Offline Inference", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF10B981))
                            Text(
                                "No API keys required. Operates completely offline on 127.0.0.1:11434 with your downloaded models.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 5. OFFLINE INTELLIGENCE & DOWNLOADED MODELS (LINE GAKO BELEMA)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOfflineModelsSection = !showOfflineModelsSection },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = PocketOrange)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "OFFLINE MODELS & AUTO-SWITCH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "When internet is disconnected, tasks auto-switch to offline models",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(if (showOfflineModelsSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                }

                AnimatedVisibility(showOfflineModelsSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            isActive = state.provider.kind == ProviderKind.OLLAMA && state.provider.model == "llama-3.2-3b-q4",
                            onDownload = {
                                llamaDownloading = true
                                onDownloadOfflineModel(
                                    "llama-3.2-3b-q4",
                                    { prog -> llamaProgress = prog },
                                    { success ->
                                        llamaDownloading = false
                                        if (success) llamaDownloaded = true
                                    },
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

                        OfflineModelCard(
                            model = OfflineModelUiItem(
                                id = "qwen2.5-coder-1.5b-q4",
                                title = "Qwen 2.5 Coder 1.5B",
                                size = "1.05 GB",
                                quantization = "Q4_K_M",
                                tag = "Fast & Lightweight",
                                description = "Optimized for quick autocomplete, function scaffolding, and offline coding.",
                                isRecommended = false,
                                isDownloaded = false,
                            ),
                            isActive = state.provider.kind == ProviderKind.OLLAMA && state.provider.model.contains("qwen"),
                            onDownload = {},
                            onPause = {},
                            onResume = {},
                            onDelete = {},
                            onSetActive = {
                                onSelectProvider(ProviderKind.OLLAMA)
                                onSelectModel("qwen2.5-coder:1.5b")
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
