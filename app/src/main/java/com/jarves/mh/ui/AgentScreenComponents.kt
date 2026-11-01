package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.R
import com.jarves.mh.data.ApiKeyInfo
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.DSH_PROTOCOL_PROVIDERS
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.defaultDshApiForProvider
import com.jarves.mh.model.inferredDshApiForUrl
import com.jarves.mh.model.providersForAgent
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.ui.theme.PocketOrange

/**
 * Modern Antigravity Configuration Bento Card.
 */
@Composable
internal fun AgentAntigravityCard(
    state: AppUiState,
    code: String,
    onCode: (String) -> Unit,
    onStartLogin: () -> Unit,
    onSubmitCode: () -> Unit,
    onLogout: () -> Unit,
    onRefreshModels: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onSetEffort: (String) -> Unit,
    onTest: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val auth = state.antigravityAuth

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.st_google_account), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            // Google Account Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF34A853).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF34A853).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("G", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    val email = auth.accountEmail
                    Text(
                        email ?: if (auth.status == AntigravityAuthStatus.SIGNED_IN) stringResource(R.string.st_connected) else stringResource(R.string.st_not_signed_in),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (auth.status == AntigravityAuthStatus.SIGNED_IN) stringResource(R.string.st_connected_with_google) else stringResource(R.string.st_required_for_antigravity),
                        fontSize = 11.sp,
                        color = if (auth.status == AntigravityAuthStatus.SIGNED_IN) Color(0xFF2E9D72) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (auth.status == AntigravityAuthStatus.SIGNED_IN) {
                    Text(
                        stringResource(R.string.st_disconnect),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onLogout() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            // Authentication actions if not signed in
            when (auth.status) {
                AntigravityAuthStatus.STARTING, AntigravityAuthStatus.COMPLETING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                AntigravityAuthStatus.AWAITING_CODE -> {
                    auth.authorizationUrl?.let { url ->
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(url)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.st_copy_google_signin_url))
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCode,
                        label = { Text(stringResource(R.string.st_one_time_auth_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Button(
                        onClick = onSubmitCode,
                        enabled = code.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.st_complete_signin))
                    }
                }
                AntigravityAuthStatus.SIGNED_OUT, AntigravityAuthStatus.ERROR -> {
                    Button(
                        onClick = onStartLogin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (auth.status == AntigravityAuthStatus.ERROR) stringResource(R.string.st_reconnect_google) else stringResource(R.string.agent_google_signin))
                    }
                }
                AntigravityAuthStatus.SIGNED_IN -> {}
            }

            if (auth.status == AntigravityAuthStatus.SIGNED_IN) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Active Intelligence Model Tile
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.st_active_intelligence_model),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = !state.antigravityModelsLoading) { onRefreshModels() }
                                .padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                        ) {
                            if (state.antigravityModelsLoading) {
                                CircularProgressIndicator(Modifier.size(11.dp), strokeWidth = 1.4.dp)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                stringResource(R.string.st_sync),
                                fontSize = 11.sp,
                                color = PocketOrange,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenModelSheet() },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(PocketOrange.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PocketOrange,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                val currentModel = state.antigravityModel.ifBlank { stringResource(R.string.st_select_model_placeholder) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatAntigravityModelName(currentModel),
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    currentModel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.st_choose_model),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Reasoning Depth (Effort) Segmented Capsule
                Column(modifier = Modifier.fillMaxWidth()) {
                    val effortCaption = when (state.antigravityEffort) {
                        "low" -> stringResource(R.string.st_effort_low_caption)
                        "medium" -> stringResource(R.string.st_effort_medium_caption)
                        "high" -> stringResource(R.string.st_effort_high_caption)
                        else -> stringResource(R.string.st_effort_medium_caption)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.st_reasoning_depth),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            effortCaption,
                            fontSize = 11.sp,
                            color = PocketOrange,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            listOf("low", "medium", "high").forEach { effort ->
                                val isSelected = state.antigravityEffort == effort
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) PocketOrange else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSetEffort(effort) },
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            when (effort) {
                                                "low" -> stringResource(R.string.st_effort_low)
                                                "medium" -> stringResource(R.string.st_effort_medium)
                                                "high" -> stringResource(R.string.st_effort_high)
                                                else -> effort
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF241107) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (auth.status == AntigravityAuthStatus.SIGNED_IN) {
                OutlinedButton(
                    onClick = onTest,
                    enabled = state.apiPingStatus != ApiPingStatus.PINGING,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, PocketOrange.copy(alpha = 0.7f)),
                ) {
                    if (state.apiPingStatus == ApiPingStatus.PINGING) {
                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 1.8.dp, color = PocketOrange)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = PocketOrange)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (state.apiPingStatus == ApiPingStatus.PINGING) stringResource(R.string.st_testing_connection) else stringResource(R.string.agent_test_connection),
                        color = PocketOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                state.apiPingMessage?.takeIf { state.apiPingStatus != ApiPingStatus.IDLE }?.let { message ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (state.apiPingStatus == ApiPingStatus.FAILED) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else Color(0xFF2E9D72),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            message,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.agent_approval_footer),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun AgentProviderCard(
    onTest: () -> Unit = {},
    state: AppUiState,
    selectedKind: ProviderKind,
    baseUrl: String,
    model: String,
    dshApi: String,
    openRouterProviderOrder: String,
    openRouterAllowFallbacks: Boolean,
    apiKey: String,
    models: List<DiscoveredModel>,
    isDiscovering: Boolean,
    isValidating: Boolean,
    status: String?,
    statusOk: Boolean,
    statusProviderMessage: String?,
    keyConnectionStatuses: Map<String, KeyConnectionStatus>,
    savedKeys: List<ApiKeyInfo>,
    newKeyName: String,
    newApiKey: String,
    newKeyVisible: Boolean,
    onProvider: (ProviderKind) -> Unit,
    onBaseUrl: (String) -> Unit,
    onModel: (String) -> Unit,
    onDshApi: (String) -> Unit,
    onOpenRouterProviderOrder: (String) -> Unit,
    onOpenRouterAllowFallbacks: (Boolean) -> Unit,
    onNewKeyName: (String) -> Unit,
    onNewApiKey: (String) -> Unit,
    onToggleNewKey: () -> Unit,
    onAddKey: () -> Unit,
    onActivateKey: (String) -> Unit,
    onRemoveKey: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onDiscover: () -> Unit,
    onValidate: () -> Unit,
) {
    val visibleKinds = remember(state.agentKind) { providersForAgent(state.agentKind) }
    var connectionExpanded by rememberSaveable(selectedKind) { mutableStateOf(false) }
    // Keep this state across provider changes so selecting Custom API can
    // immediately reveal its required setup instead of resetting on recomposition.
    var endpointExpanded by rememberSaveable(state.agentKind) { mutableStateOf(false) }
    var keysExpanded by rememberSaveable(state.agentKind, selectedKind) { mutableStateOf(false) }
    var addKeyExpanded by rememberSaveable(savedKeys.isEmpty()) { mutableStateOf(savedKeys.isEmpty()) }
    val activeKey = savedKeys.firstOrNull { it.isActive }
    val activeKeyStatus = activeKey?.let { keyConnectionStatuses[it.id] }
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(stringResource(R.string.agent_provider_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            PremiumSummaryRow(
                icon = Icons.Default.Link,
                title = stringResource(selectedKind.titleRes()),
                subtitle = stringResource(selectedKind.subtitleRes()),
                expanded = connectionExpanded,
                onClick = { connectionExpanded = !connectionExpanded },
            )

            AnimatedVisibility(connectionExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    ) {
                        Column {
                            visibleKinds.forEachIndexed { index, kind ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onProvider(kind)
                                            connectionExpanded = false
                                            endpointExpanded = kind == ProviderKind.CUSTOM
                                        }
                                        .padding(horizontal = 13.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(kind.titleRes()), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(stringResource(kind.subtitleRes()), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    AgentSelectionDot(selectedKind == kind)
                                }
                                if (index != visibleKinds.lastIndex) {
                                    HorizontalDivider(Modifier.padding(start = 13.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                }
            }

            if (selectedKind != ProviderKind.CLAUDE) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                PremiumSummaryRow(
                    icon = Icons.Default.Info,
                    title = stringResource(if (selectedKind == ProviderKind.CUSTOM) R.string.agent_custom_api_settings else R.string.agent_endpoint_protocol),
                    subtitle = buildString {
                        append(baseUrl.ifBlank { stringResource(R.string.st_base_url_required) })
                        if (state.agentKind == AgentKind.DEEPSEEK_HARNESS && selectedKind in DSH_PROTOCOL_PROVIDERS) {
                            append(" · ")
                            append(if (selectedKind.fixedProtocol) defaultDshApiForProvider(selectedKind) else dshApi)
                        }
                    },
                    expanded = endpointExpanded,
                    onClick = { endpointExpanded = !endpointExpanded },
                )

                AnimatedVisibility(endpointExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selectedKind == ProviderKind.CUSTOM) {
                            Text(
                                stringResource(R.string.st_custom_endpoint_hint),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { if (!selectedKind.fixedBaseUrl) onBaseUrl(it) },
                            label = { Text(stringResource(R.string.provider_base_url)) },
                            supportingText = if (selectedKind.fixedBaseUrl) ({ Text(stringResource(R.string.provider_fixed_by, stringResource(selectedKind.titleRes()))) }) else null,
                            readOnly = selectedKind.fixedBaseUrl,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        if (selectedKind == ProviderKind.LLM_ROUTER) {
                            OutlinedTextField(
                                value = openRouterProviderOrder,
                                onValueChange = onOpenRouterProviderOrder,
                                label = { Text(stringResource(R.string.st_provider_order)) },
                                placeholder = { Text("exacto, together") },
                                supportingText = {
                                    Text(stringResource(R.string.st_provider_order_help))
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.st_allow_provider_fallbacks), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        stringResource(R.string.st_allow_provider_fallbacks_help),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = openRouterAllowFallbacks,
                                    onCheckedChange = onOpenRouterAllowFallbacks,
                                    enabled = openRouterProviderOrder.isNotBlank(),
                                )
                            }
                        }
                        if (state.agentKind == AgentKind.DEEPSEEK_HARNESS && selectedKind in DSH_PROTOCOL_PROVIDERS && !selectedKind.fixedProtocol) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.provider_gateway_protocol), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(5.dp))
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
                                    Column {
                                        listOf("anthropic-messages", "openai-completions", "openai-responses").forEach { option ->
                                            Row(
                                                Modifier.fillMaxWidth().clickable { onDshApi(option) }.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(option, Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                AgentSelectionDot(dshApi == option)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.agent_model_access), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        if (isDiscovering) stringResource(R.string.agent_discovering) else if (models.isEmpty()) stringResource(R.string.agent_discover_models) else stringResource(R.string.agent_models_count, models.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PocketOrange,
                        modifier = Modifier.clickable(enabled = !isDiscovering, onClick = onDiscover).padding(6.dp),
                    )
                }

                PremiumSummaryRow(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.agent_ai_model),
                    subtitle = model.ifBlank { stringResource(R.string.agent_select_model) },
                    expanded = false,
                    onClick = onOpenModelSheet,
                )
            } else {
                Text(
                    stringResource(R.string.st_claude_setup_token_hint),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            if (status != null) {
                Column(modifier = Modifier.padding(start = 48.dp, end = 8.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (statusOk) Icons.Default.Info else Icons.Default.Warning, null, tint = if (statusOk) PocketOrange else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(status, fontSize = 10.sp, lineHeight = 14.sp, color = if (statusOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                    statusProviderMessage?.let { providerMessage ->
                        Text(stringResource(R.string.st_provider_message, providerMessage), fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 21.dp, top = 4.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            PremiumSummaryRow(
                icon = Icons.Default.Key,
                title = stringResource(if (selectedKind == ProviderKind.CLAUDE) R.string.agent_subscription_token else R.string.agent_credentials),
                subtitle = buildString {
                    append(activeKey?.name ?: if (selectedKind == ProviderKind.CLAUDE) stringResource(R.string.st_no_subscription_token_saved) else stringResource(R.string.st_no_api_key_saved))
                    if (activeKey != null) {
                        append(" · ")
                        append(stringResource(R.string.st_active))
                    }
                    activeKeyStatus?.let {
                        append(" · ")
                        append(when (it.successful) { true -> stringResource(R.string.agent_status_verified); false -> it.label; null -> stringResource(R.string.provider_checking) })
                    }
                },
                positive = activeKeyStatus?.successful == true,
                error = activeKeyStatus?.successful == false,
                expanded = keysExpanded,
                onClick = { keysExpanded = !keysExpanded },
            )

            activeKeyStatus?.let { keyStatus ->
                if (!keysExpanded) {
                    Text(
                        keyStatus.message,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = when (keyStatus.successful) {
                            true -> Color(0xFF2E9D72)
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(start = 48.dp, end = 8.dp, bottom = 8.dp),
                    )
                    keyStatus.providerMessage?.let { providerMessage ->
                        Text(
                            stringResource(R.string.st_provider_message, providerMessage),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 48.dp, end = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(keysExpanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (selectedKind == ProviderKind.CLAUDE) stringResource(R.string.st_saved_tokens_count, savedKeys.size) else stringResource(R.string.st_saved_keys_count, savedKeys.size), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(
                            if (addKeyExpanded) stringResource(R.string.action_cancel) else stringResource(R.string.st_add_key_plus),
                            fontSize = 11.sp,
                            color = PocketOrange,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { addKeyExpanded = !addKeyExpanded }.padding(6.dp),
                        )
                    }
                    savedKeys.forEach { key ->
                        val keyStatus = keyConnectionStatuses[key.id]
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onActivateKey(key.id) }.padding(start = 12.dp, top = 7.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(key.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(if (key.isActive) stringResource(R.string.st_active) else stringResource(R.string.st_tap_to_activate), fontSize = 10.sp, color = if (key.isActive) PocketOrange else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AgentSelectionDot(key.isActive)
                                    IconButton(onClick = { onRemoveKey(key.id) }) {
                                        Icon(Icons.Default.DeleteSweep, stringResource(R.string.action_remove), Modifier.size(17.dp))
                                    }
                                }
                                keyStatus?.let {
                                    Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (it.successful == null) CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.5.dp)
                                        else Icon(if (it.successful) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (it.successful) Color(0xFF2E9D72) else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(it.message, fontSize = 10.sp, lineHeight = 14.sp, color = if (it.successful == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                            it.providerMessage?.let { providerMessage ->
                                                Text(stringResource(R.string.st_provider_message, providerMessage), fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(addKeyExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newKeyName,
                                onValueChange = { input ->
                                    if ((input.startsWith("sk-") || input.startsWith("ant-") || input.length > 30) && !input.contains(" ") && newApiKey.isBlank()) {
                                        onNewApiKey(input.trim())
                                        onNewKeyName(context.getString(R.string.st_key_default_name, context.getString(selectedKind.titleRes())))
                                    } else onNewKeyName(input)
                                },
                                label = { Text(if (selectedKind == ProviderKind.CLAUDE) stringResource(R.string.st_token_name) else stringResource(R.string.provider_key_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                            OutlinedTextField(
                                value = newApiKey,
                                onValueChange = onNewApiKey,
                                label = { Text(if (selectedKind == ProviderKind.CLAUDE) stringResource(R.string.provider_claude_token) else stringResource(R.string.provider_api_key)) },
                                singleLine = true,
                                visualTransformation = if (newKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = { IconButton(onClick = onToggleNewKey) { Icon(if (newKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, stringResource(R.string.st_toggle_visibility)) } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                            Button(
                                onClick = { onAddKey(); addKeyExpanded = false },
                                enabled = newKeyName.isNotBlank() && newApiKey.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text(if (selectedKind == ProviderKind.CLAUDE) stringResource(R.string.st_save_token) else stringResource(R.string.st_save_api_key)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onValidate,
                enabled = (apiKey.isNotBlank() || selectedKind == ProviderKind.CUSTOM) && !isDiscovering && !isValidating &&
                    (selectedKind == ProviderKind.CLAUDE || (baseUrl.isNotBlank() && model.isNotBlank())),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, PocketOrange.copy(alpha = 0.7f)),
            ) {
                if (isValidating) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 1.8.dp, color = PocketOrange)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(if (selectedKind == ProviderKind.CLAUDE) Icons.Default.Check else Icons.Default.Refresh, null, Modifier.size(16.dp), tint = PocketOrange)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    when {
                        isValidating && selectedKind == ProviderKind.CLAUDE -> stringResource(R.string.st_saving_token)
                        isValidating -> stringResource(R.string.st_testing_connection)
                        selectedKind == ProviderKind.CLAUDE -> stringResource(R.string.st_save_subscription_token)
                        else -> stringResource(R.string.agent_test_connection)
                    },
                    color = PocketOrange,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Claude subscriptions can't be pinged over HTTP; this runs a one-word Claude Code prompt.
            if (selectedKind == ProviderKind.CLAUDE && savedKeys.isNotEmpty() && state.provider.kind == ProviderKind.CLAUDE) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onTest,
                    enabled = state.apiPingStatus != ApiPingStatus.PINGING && !isValidating,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, PocketOrange.copy(alpha = 0.7f)),
                ) {
                    if (state.apiPingStatus == ApiPingStatus.PINGING) {
                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 1.8.dp, color = PocketOrange)
                    } else {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = PocketOrange)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (state.apiPingStatus == ApiPingStatus.PINGING) R.string.st_testing_connection else R.string.agent_test_connection),
                        color = PocketOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                state.apiPingMessage?.takeIf { state.apiPingStatus != ApiPingStatus.IDLE }?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (state.apiPingStatus == ApiPingStatus.FAILED) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else Color(0xFF2E9D72),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            message,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumSummaryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    positive: Boolean = false,
    error: Boolean = false,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(PocketOrange.copy(alpha = 0.10f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = PocketOrange, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = when {
                    error -> MaterialTheme.colorScheme.error
                    positive -> Color(0xFF2E9D72)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) stringResource(R.string.st_collapse) else stringResource(R.string.st_expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Compact Runtime Updates Card.
 */
@Composable
internal fun AgentUpdateBlock(
    state: AppUiState,
    onCheck: () -> Unit,
    onUpdate: (AgentKind) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.agent_runtime_updates), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    state.agentUpdateMessage ?: stringResource(R.string.agent_updates_current),
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val canCheck = !state.agentUpdatesChecking && state.agentUpdating == null && state.agentInstalling == null
            Row(
                modifier = Modifier
                    .clickable(enabled = canCheck, onClick = onCheck)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.agentUpdatesChecking) {
                    CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.6.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PocketOrange, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.agent_check_updates), color = PocketOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        state.agentUpdates.forEach { (agent, update) ->
                val updating = state.agentUpdating == agent
                val downloaded = state.agentUpdateDownloadedBytes
                val total = state.agentUpdateTotalBytes
                val fraction = if (updating && downloaded != null && total != null && total > 0L) {
                    (downloaded.toFloat() / total).coerceIn(0f, 1f)
                } else state.agentUpdateProgress.coerceIn(0f, 1f)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PocketOrange.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PocketOrange.copy(alpha = 0.28f)),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(agent.localizedTitle(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("v${update.installedVersion} → v${update.latestVersion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(stringResource(R.string.st_update_badge), color = PocketOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (updating) {
                            state.agentUpdateMessage?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (state.agentUpdateIndeterminate) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (downloaded != null || total != null) {
                                        buildString {
                                            append(formatAgentBytes(downloaded ?: 0L))
                                            total?.let { append(" / ${formatAgentBytes(it)}") }
                                        }
                                    } else {
                                        stringResource(R.string.st_processing_files)
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    buildString {
                                        append("${(fraction * 100).toInt()}%")
                                        state.agentUpdateBytesPerSecond?.takeIf { it > 0L }?.let {
                                            append(" · ${formatAgentBytes(it)}/s")
                                        }
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Button(
                                onClick = { onUpdate(agent) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.agentUpdating == null && state.agentInstalling == null,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(stringResource(R.string.st_update_agent, agent.localizedTitle()), fontSize = 12.sp)
                            }
                        }
                    }
                }
        }
    }
}

internal fun formatAgentBytes(bytes: Long): String = when {
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

/**
 * Fixed-size three-dot wave. All three dots are always laid out, only their
 * alpha animates — so surrounding text never shifts while loading.
 */
@Composable
internal fun AgentTypingDots(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 6.dp,
    spacing: androidx.compose.ui.unit.Dp = 4.dp,
) {
    val transition = rememberInfiniteTransition(label = "agent-typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wave",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(3) { index ->
            val alpha = 0.25f + 0.75f * ((phase + index / 3f) % 1f)
            Box(Modifier.size(dotSize).background(color.copy(alpha = alpha), CircleShape))
        }
    }
}

@Composable
internal fun AgentSelectionDot(selected: Boolean) {
    Box(
        Modifier.size(22.dp).border(if (selected) 2.dp else 1.5.dp, if (selected) PocketOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(PocketOrange, CircleShape))
    }
}

/** Provides popular default models for providers when discovery hasn't been run or is unavailable. */
internal fun defaultModelsForProvider(kind: ProviderKind, context: android.content.Context): List<DiscoveredModel> = when (kind) {
    ProviderKind.GEMINI -> listOf(
        DiscoveredModel("gemini-4.0-flash", "Gemini 4.0 Flash (Next-Gen Autonomous)"),
        DiscoveredModel("gemini-4-argon", "Gemini 4 Argon (Experimental Reasoning)"),
        DiscoveredModel("gemini-3.8-pro", "Gemini 3.8 Pro (Ultra-Deep Logic)"),
        DiscoveredModel("gemini-3.8-flash", "Gemini 3.8 Flash (High-Speed)"),
        DiscoveredModel("gemini-2.5-flash", "Gemini 2.5 Flash (Fast, Latest)"),
        DiscoveredModel("gemini-2.5-pro", "Gemini 2.5 Pro (Deep Reasoning)"),
        DiscoveredModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite (Low Latency)"),
        DiscoveredModel("gemini-2.0-flash", "Gemini 2.0 Flash (Stable)"),
    )
    ProviderKind.LLM_ROUTER -> listOf(
        DiscoveredModel("qwen/qwen-2.5-coder-32b-instruct:free", "Qwen 2.5 Coder 32B (Verified Free)"),
        DiscoveredModel("deepseek/deepseek-r1:free", "DeepSeek R1 (Verified Free · Reasoning)"),
        DiscoveredModel("deepseek/deepseek-chat:free", "DeepSeek Chat (Verified Free)"),
        DiscoveredModel("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash Exp (Verified Free)"),
        DiscoveredModel("google/gemini-2.0-flash-thinking-exp:free", "Gemini 2.0 Flash Thinking (Verified Free)"),
        DiscoveredModel("mistralai/mistral-small-24b-instruct-2501:free", "Mistral Small 24B (Verified Free)"),
        DiscoveredModel("meta-llama/llama-3.2-3b-instruct:free", "Llama 3.2 3B (Verified Free)"),
        DiscoveredModel("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B (Verified Free)"),
    )
    ProviderKind.OLLAMA -> listOf(
        DiscoveredModel("qwen2.5-coder:1.5b", "Qwen 2.5 Coder 1.5B (Local · Fast)"),
        DiscoveredModel("qwen2.5-coder:7b", "Qwen 2.5 Coder 7B (Local)"),
        DiscoveredModel("deepseek-r1:1.5b", "DeepSeek R1 1.5B (Local · Reasoning)"),
        DiscoveredModel("llama3.2:3b", "Llama 3.2 3B (Local)"),
    )
    ProviderKind.NVIDIA_NIM -> listOf(
        DiscoveredModel("meta/llama-3.3-70b-instruct", "Llama 3.3 70B (NVIDIA Free)"),
        DiscoveredModel("deepseek-ai/deepseek-r1", "DeepSeek R1 (NVIDIA Free)"),
        DiscoveredModel("mistralai/mistral-large-2-instruct", "Mistral Large 2 (NVIDIA Free)"),
        DiscoveredModel("qwen/qwen2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B (NVIDIA Free)"),
    )
    ProviderKind.DEEPSEEK -> listOf(
        DiscoveredModel("deepseek-v4-flash", "DeepSeek-V4 Flash"),
    )
    ProviderKind.OPENCODE_ZEN -> listOf(
        DiscoveredModel("deepseek-v4-flash", "DeepSeek-V4 Flash (Zen Fast)"),
        DiscoveredModel("qwen/qwen-2.5-coder-32b-instruct:free", "Qwen 2.5 Coder 32B (Verified Free)"),
        DiscoveredModel("deepseek/deepseek-r1:free", "DeepSeek R1 (Verified Free · Reasoning)"),
        DiscoveredModel("claude-3-5-sonnet", "Claude 3.5 Sonnet (Zen Free Tier)"),
        DiscoveredModel("gpt-4o-mini", "GPT-4o Mini (Zen Free Tier)"),
        DiscoveredModel("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B (Verified Free)"),
    )
    ProviderKind.ANTHROPIC -> listOf(
        DiscoveredModel("claude-3-7-sonnet-20250219", "Claude 3.7 Sonnet (Hybrid)"),
        DiscoveredModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet v2"),
        DiscoveredModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku"),
    )
    ProviderKind.KIMI -> listOf(
        DiscoveredModel("kimi-k2.6", "Kimi K2.6"),
        DiscoveredModel("moonshot-v1-8k", "Moonshot v1 8K"),
        DiscoveredModel("moonshot-v1-32k", "Moonshot v1 32K"),
    )
    else -> if (kind.defaultModel.isNotBlank()) listOf(
        DiscoveredModel(kind.defaultModel, context.getString(R.string.md_model_default, context.getString(kind.titleRes()), kind.defaultModel))
    ) else emptyList()
}
