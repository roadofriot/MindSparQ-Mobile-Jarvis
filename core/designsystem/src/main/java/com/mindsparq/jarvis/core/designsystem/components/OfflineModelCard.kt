package com.mindsparq.jarvis.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OfflineModelUiItem(
    val id: String,
    val title: String,
    val size: String,
    val quantization: String,
    val tag: String,
    val isRecommended: Boolean = false,
)

@Composable
fun OfflineModelDownloadSection(
    modifier: Modifier = Modifier,
    detectedRamGb: Int = 12,
    activeModelId: String? = null,
    onSetActiveModel: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedActive by remember { mutableStateOf(activeModelId ?: "llama-3.2-3b") }
    val downloadedMap = remember { mutableStateMapOf("llama-3.2-3b" to false, "qwen2.5-coder-1.5b" to true) }
    val downloadingProgress = remember { mutableStateMapOf<String, Float>() }

    val models = listOf(
        OfflineModelUiItem(
            id = "llama-3.2-3b",
            title = "Llama 3.2 3B Instruct",
            size = "1.95 GB",
            quantization = "Q4_K_M",
            tag = "High Capability Coding",
            isRecommended = detectedRamGb >= 10,
        ),
        OfflineModelUiItem(
            id = "deepseek-r1-1.5b",
            title = "DeepSeek R1 Distill 1.5B",
            size = "1.12 GB",
            quantization = "Q4_K_M",
            tag = "Offline Reasoning & Math",
            isRecommended = false,
        ),
        OfflineModelUiItem(
            id = "qwen2.5-coder-1.5b",
            title = "Qwen 2.5 Coder 1.5B",
            size = "1.05 GB",
            quantization = "Q4_K_M",
            tag = "Fast Code Synthesis",
            isRecommended = detectedRamGb in 4..8,
        ),
        OfflineModelUiItem(
            id = "qwen2.5-coder-0.5b",
            title = "Qwen 2.5 Coder 0.5B",
            size = "398 MB",
            quantization = "Q4_K_M",
            tag = "Ultralight Low-RAM",
            isRecommended = detectedRamGb < 4,
        ),
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "On-Device Offline AI Models",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        "${detectedRamGb} GB RAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Run autonomous coding directly inside local PRoot with llama.cpp. Zero API key, zero quota, works 100% offline.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                models.forEach { model ->
                    val isDownloaded = downloadedMap[model.id] == true
                    val progress = downloadingProgress[model.id]
                    val isDownloading = progress != null
                    val isActive = selectedActive == model.id

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            model.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                        )
                                        if (model.isRecommended) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            ) {
                                                Text(
                                                    "RECOMMENDED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${model.size} · ${model.quantization} · ${model.tag}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Active",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            }

                            if (isDownloading) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress ?: 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Downloading model… ${(progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isDownloaded) {
                                    IconButton(
                                        onClick = {
                                            downloadedMap[model.id] = false
                                            if (selectedActive == model.id) selectedActive = ""
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    if (!isActive) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedActive = model.id
                                                onSetActiveModel(model.id)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Text("Set Active", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else if (!isDownloading) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                downloadingProgress[model.id] = 0.05f
                                                for (i in 1..10) {
                                                    delay(150)
                                                    downloadingProgress[model.id] = i * 0.1f
                                                }
                                                downloadingProgress.remove(model.id)
                                                downloadedMap[model.id] = true
                                                selectedActive = model.id
                                                onSetActiveModel(model.id)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) {
                                        Icon(Icons.Default.Download, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
