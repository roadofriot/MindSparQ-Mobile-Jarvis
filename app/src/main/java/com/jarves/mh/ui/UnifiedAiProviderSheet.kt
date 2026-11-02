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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.jarves.mh.model.providersForAgent
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
    onAddKeyDetailed: ((ProviderKind, String, String, String, String, String, Int) -> List<ApiKeyInfo>)? = null,
    onActivateApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onRemoveApiKey: (ProviderKind, String) -> List<ApiKeyInfo>,
    onDiscoverModels: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    isModelDownloaded: (String) -> Boolean = { false },
    onDownloadOfflineModel: (String, (Float) -> Unit, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteOfflineModel: (String) -> Unit = {},
    onGetCachedModels: (ProviderKind) -> List<DiscoveredModel>? = { null },
    getFailoverChain: () -> List<ProviderKind> = { emptyList() },
    saveFailoverChain: (List<ProviderKind>) -> Unit = { },
    onToggleFailoverParallel: () -> Unit = { },
    isFailoverParallel: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var agentDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var providerDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var addKeyExpanded by rememberSaveable { mutableStateOf(false) }
    var newKeyName by rememberSaveable { mutableStateOf("") }
    var newAccountLabel by rememberSaveable { mutableStateOf("") }
    var newQuotaScopeId by rememberSaveable { mutableStateOf("") }
    var newBillingTier by rememberSaveable { mutableStateOf(if (state.provider.kind == ProviderKind.GEMINI) "PAID" else "FREE") }
    var newApiKey by rememberSaveable { mutableStateOf("") }
    var newKeyVisible by rememberSaveable { mutableStateOf(false) }
    var isDiscoveringModels by remember { mutableStateOf(false) }
    var discoveredModels by remember(state.provider.kind) { mutableStateOf<List<DiscoveredModel>>(onGetCachedModels(state.provider.kind) ?: emptyList()) }
    var showOfflineModelsSection by rememberSaveable { mutableStateOf(false) }

    // Offline Model States
    var llamaDownloading by remember { mutableStateOf(false) }
    var llamaProgress by remember { mutableFloatStateOf(0f) }
    var llamaDownloaded by rememberSaveable { mutableStateOf(isModelDownloaded("llama-3.2-3b-q4")) }
    var qwenDownloading by remember { mutableStateOf(false) }
    var qwenProgress by remember { mutableFloatStateOf(0f) }
    var qwenDownloaded by rememberSaveable { mutableStateOf(isModelDownloaded("qwen2.5-coder-1.5b-q4")) }

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
                            when {
                                state.provider.kind == ProviderKind.OLLAMA -> Color(0xFF00ACC1).copy(alpha = 0.15f)
                                state.apiPingStatus == ApiPingStatus.OK -> Color(0xFF10B981).copy(alpha = 0.15f)
                                state.apiPingStatus == ApiPingStatus.FAILED -> PocketOrange.copy(alpha = 0.15f)
                                state.apiPingStatus == ApiPingStatus.PINGING -> PocketOrange.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        when {
                            state.provider.kind == ProviderKind.OLLAMA -> "OFFLINE READY"
                            state.apiPingStatus == ApiPingStatus.OK -> "HEALTHY"
                            state.apiPingStatus == ApiPingStatus.FAILED -> "FALLBACK READY"
                            state.apiPingStatus == ApiPingStatus.PINGING -> "TESTING..."
                            else -> "READY"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            state.provider.kind == ProviderKind.OLLAMA -> Color(0xFF00ACC1)
                            state.apiPingStatus == ApiPingStatus.OK -> Color(0xFF10B981)
                            state.apiPingStatus == ApiPingStatus.FAILED -> PocketOrange
                            state.apiPingStatus == ApiPingStatus.PINGING -> PocketOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.fillMaxWidth(),
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
                                .background(Color(0xFF00B4D8).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF00B4D8),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(AgentKind.OPENCODE.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(AgentKind.OPENCODE.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // -------------------------------------------------------------
                // Dynamic Model Catalogue with Capability Filtering
                // -------------------------------------------------------------
                var modelFilterQuery by rememberSaveable { mutableStateOf("") }
                var selectedCapabilityFilter by rememberSaveable { mutableStateOf("ALL") }

                val allAvailableModels = remember(discoveredModels, state.provider.kind) {
                    if (discoveredModels.isNotEmpty()) {
                        discoveredModels
                    } else {
                        val cached = onGetCachedModels(state.provider.kind)
                        if (!cached.isNullOrEmpty()) {
                            cached
                        } else {
                            val fromRegistry = com.mindsparq.jarvis.ai.gateway.registry.ModelRegistry.instance
                                .getModelsForProvider(state.provider.kind.name)
                            if (fromRegistry.isNotEmpty()) {
                                fromRegistry.map { DiscoveredModel(it.modelId, it.displayName, it.isFree) }
                            } else {
                                emptyList()
                            }
                        }
                    }
                }

                val displayedModels = remember(allAvailableModels, modelFilterQuery, selectedCapabilityFilter) {
                    allAvailableModels.filter { model ->
                        val matchesQuery = modelFilterQuery.isBlank() ||
                            model.displayName.contains(modelFilterQuery, ignoreCase = true) ||
                            model.id.contains(modelFilterQuery, ignoreCase = true)
                        val matchesFilter = when (selectedCapabilityFilter) {
                            "FREE" -> model.isFree
                            "REASONING" -> model.id.contains("r1", ignoreCase = true) ||
                                model.id.contains("reason", ignoreCase = true) ||
                                model.id.contains("thinking", ignoreCase = true)
                            "TOOLS" -> !model.id.contains("r1", ignoreCase = true)
                            else -> true
                        }
                        matchesQuery && matchesFilter
                    }
                }

                if (allAvailableModels.isNotEmpty()) {
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
                                    if (discoveredModels.isNotEmpty()) "DISCOVERED MODELS (${displayedModels.size})"
                                    else "VERIFIED CATALOGUE (${displayedModels.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

                            // Capability Filter Chips
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    "ALL" to "All",
                                    "FREE" to "Free Tier",
                                    "TOOLS" to "Tools",
                                    "REASONING" to "Reasoning"
                                ).forEach { (key, label) ->
                                    val isFilterSelected = selectedCapabilityFilter == key
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isFilterSelected) PocketOrange else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { selectedCapabilityFilter = key }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isFilterSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }

                            // Optional Search input if catalog has more than 5 models
                            if (allAvailableModels.size > 5) {
                                OutlinedTextField(
                                    value = modelFilterQuery,
                                    onValueChange = { modelFilterQuery = it },
                                    placeholder = { Text("Filter models...", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }

                            displayedModels.take(12).forEach { model ->
                                val isSelected = state.provider.model == model.id
                                val isThinking = model.id.contains("r1", ignoreCase = true) ||
                                    model.id.contains("reason", ignoreCase = true) ||
                                    model.id.contains("thinking", ignoreCase = true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectModel(model.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) PocketOrange else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            model.id,
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (model.isFree) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            modifier = Modifier.padding(horizontal = 3.dp),
                                        ) {
                                            Text(
                                                "FREE",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                            )
                                        }
                                    }
                                    if (isThinking) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                            modifier = Modifier.padding(horizontal = 3.dp),
                                        ) {
                                            Text(
                                                "REASON",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
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
            // 4. MULTI-KEY & MULTI-ACCOUNT POOL (QUOTA-AWARE HIERARCHICAL FAILOVER)
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
                                "MULTI-KEY & ACCOUNT POOL (${savedKeys.size} KEYS)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Quota-scope aware failover: Key → Project/Account → Provider",
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
                        val groupedByScope = remember(savedKeys) {
                            savedKeys.groupBy { it.quotaScopeId.ifBlank { "default-project" } }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupedByScope.forEach { (scopeId, keysInScope) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                ) {
                                    Column {
                                        // Quota Scope Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Storage,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = PocketOrange,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "SCOPE / PROJECT: $scopeId",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (keysInScope.any { it.state == "EXHAUSTED" }) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else Color(0xFF10B981).copy(alpha = 0.15f),
                                            ) {
                                                Text(
                                                    if (keysInScope.any { it.state == "EXHAUSTED" }) "QUOTA EXHAUSTED" else "QUOTA AVAILABLE",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (keysInScope.any { it.state == "EXHAUSTED" }) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                )
                                            }
                                        }

                                        keysInScope.forEachIndexed { index, key ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onActivateApiKey(state.provider.kind, key.id) }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(key.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                        Spacer(Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (key.billingTier == "PAID") Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f),
                                                        ) {
                                                            Text(
                                                                key.billingTier,
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (key.billingTier == "PAID") Color(0xFF8B5CF6) else Color(0xFF3B82F6),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        buildString {
                                                            append("Account: ${key.accountLabel} · ")
                                                            when (key.state) {
                                                                "COOLING" -> append("Rate Limited (Cooling)")
                                                                "EXHAUSTED" -> append("Project Quota Exhausted")
                                                                "INVALID" -> append("Authentication Failed")
                                                                else -> if (key.isActive) append("Active Primary") else append("Standby Ready")
                                                            }
                                                        },
                                                        fontSize = 10.5.sp,
                                                        color = when {
                                                            key.state == "EXHAUSTED" || key.state == "INVALID" -> MaterialTheme.colorScheme.error
                                                            key.state == "COOLING" -> PocketOrange
                                                            key.isActive -> PocketOrange
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                    )
                                                }
                                                if (key.isActive) {
                                                    Icon(Icons.Default.Check, null, tint = PocketOrange, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { onRemoveApiKey(state.provider.kind, key.id) }) {
                                                    Icon(Icons.Default.Delete, "Remove key", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (index != keysInScope.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        }
                                    }
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
                                    label = { Text("Key Label (e.g. Gemini Key 1, Work Key)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = newAccountLabel,
                                    onValueChange = { newAccountLabel = it },
                                    label = { Text("Account Label (e.g. Google Account A, Work Team)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = newQuotaScopeId,
                                    onValueChange = { newQuotaScopeId = it },
                                    label = { Text("Cloud Project / Quota Scope ID (e.g. gcp-proj-1)") },
                                    supportingText = { Text("Keys sharing this ID share quota. Distinct IDs provide independent capacity.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = { newBillingTier = "PAID" },
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (newBillingTier == "PAID") PocketOrange.copy(alpha = 0.15f) else Color.Transparent,
                                        ),
                                    ) {
                                        Text("Paid Tier", color = if (newBillingTier == "PAID") PocketOrange else MaterialTheme.colorScheme.onSurface)
                                    }
                                    OutlinedButton(
                                        onClick = { newBillingTier = "FREE" },
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (newBillingTier == "FREE") Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent,
                                        ),
                                    ) {
                                        Text("Free Tier", color = if (newBillingTier == "FREE") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
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
                                        if (onAddKeyDetailed != null) {
                                            onAddKeyDetailed(
                                                state.provider.kind,
                                                newKeyName.trim(),
                                                newApiKey.trim(),
                                                newAccountLabel.trim().ifBlank { "Account 1" },
                                                newQuotaScopeId.trim().ifBlank { "project-1" },
                                                newBillingTier,
                                                10,
                                            )
                                        } else {
                                            onAddApiKey(state.provider.kind, newKeyName, newApiKey)
                                        }
                                        newKeyName = ""
                                        newAccountLabel = ""
                                        newQuotaScopeId = ""
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
            // 4b. MULTI-API FAILOVER CHAIN (SELECTED CLI → MULTIPLE PROVIDERS)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            stringResource(R.string.st_multi_api_failover),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.st_multi_api_failover_help),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isFailoverParallel) "Parallel" else "Sequence",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFailoverParallel) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onToggleFailoverParallel) {
                            Icon(
                                imageVector = if (isFailoverParallel) Icons.Default.FastForward else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isFailoverParallel) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                val chain = getFailoverChain()
                if (chain.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(R.string.st_failover_chain_empty),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.st_failover_providers_help),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(R.string.st_failover_providers),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.st_failover_chain_active, chain.joinToString { it.title }),
                                    fontSize = 10.sp,
                                    color = PocketOrange,
                                )
                            }
                            chain.forEachIndexed { index, providerKind ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { /* tap to view */ }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape),
                                        ) {
                                            Text(
                                                (index + 1).toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                providerKind.title,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                if (providerKind == ProviderKind.OLLAMA) "Local offline · no key" else providerKind.subtitle,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            )
                                        }
                                        IconButton(onClick = { saveFailoverChain(chain.filterIndexed { i, _ -> i != index }) }) {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                        }
                                    }
                                    if (index < chain.lastIndex) {
                                        Icon(Icons.Default.KeyboardArrowRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Add provider to chain dropdown
                var addDropdown by remember { mutableStateOf(false) }
                val availableProviders = (providersForAgent(state.agentKind) - chain).toList()
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { addDropdown = !addDropdown },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = PocketOrange)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(R.string.st_add_failover_provider),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "CLI: ${state.agentKind.title}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                        Icon(if (addDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                }

                AnimatedVisibility(addDropdown) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            if (availableProviders.isEmpty()) {
                                Row(
                                    modifier = Modifier.padding(14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "All available providers already in chain.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                availableProviders.forEach { provider: ProviderKind ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                saveFailoverChain(chain + provider)
                                                addDropdown = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (provider == ProviderKind.OLLAMA) Color(0xFF10B981).copy(alpha = 0.15f)
                                                    else PocketOrange.copy(alpha = 0.15f),
                                                    CircleShape,
                                                ),
                                        ) {
                                            Icon(
                                                imageVector = if (provider == ProviderKind.OLLAMA) Icons.Default.Storage else Icons.Default.Cloud,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (provider == ProviderKind.OLLAMA) Color(0xFF10B981) else PocketOrange,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(provider.title, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                                            Text(
                                                if (provider == ProviderKind.OLLAMA) "100% Free · Local Offline · Zero API Key"
                                                else provider.subtitle,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { saveFailoverChain(emptyList()) },
                    enabled = chain.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.st_failover_clear), fontSize = 13.sp)
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
                            onPause = {
                                llamaDownloading = false
                            },
                            onResume = {
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
                                isDownloaded = qwenDownloaded,
                                isDownloading = qwenDownloading,
                                progress = qwenProgress,
                            ),
                            isActive = state.provider.kind == ProviderKind.OLLAMA && state.provider.model.contains("qwen"),
                            onDownload = {
                                qwenDownloading = true
                                onDownloadOfflineModel(
                                    "qwen2.5-coder-1.5b-q4",
                                    { prog -> qwenProgress = prog },
                                    { success ->
                                        qwenDownloading = false
                                        if (success) qwenDownloaded = true
                                    },
                                )
                            },
                            onPause = {
                                qwenDownloading = false
                            },
                            onResume = {
                                qwenDownloading = true
                                onDownloadOfflineModel(
                                    "qwen2.5-coder-1.5b-q4",
                                    { prog -> qwenProgress = prog },
                                    { success ->
                                        qwenDownloading = false
                                        if (success) qwenDownloaded = true
                                    },
                                )
                            },
                            onDelete = {
                                onDeleteOfflineModel("qwen2.5-coder-1.5b-q4")
                                qwenDownloaded = false
                            },
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
