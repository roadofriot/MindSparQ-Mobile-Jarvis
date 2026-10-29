package com.jarves.mh.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.runtime.DownloadStatus
import com.jarves.mh.runtime.OfflineModelManager
import com.jarves.mh.runtime.OfflineModelSpec
import kotlinx.coroutines.launch

@Composable
fun OfflineModelSection(
    modifier: Modifier = Modifier,
    onModelSelected: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hwProfile by remember { mutableStateOf(OfflineModelManager.detectHardware(context)) }
    val modelStates by OfflineModelManager.modelStates.collectAsState()
    var activeModelId by remember { mutableStateOf(OfflineModelManager.getActiveModelId()) }
    var lastCleanMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        OfflineModelManager.syncModelStatuses(context)
        hwProfile = OfflineModelManager.detectHardware(context)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with hardware specs and cache cleaner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "On-Device Offline AI Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "100% Local · Zero API Key · Air-Gapped",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Cache Cleaner button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        val result = OfflineModelManager.clearAppCache(context)
                        hwProfile = OfflineModelManager.detectHardware(context)
                        val msg = "Cleared ${result.formattedFreed} cache · ${String.format("%.1f", result.remainingStorageGb)} GB free"
                        lastCleanMessage = msg
                        Toast.makeText(context, "⚡ $msg", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Clean Cache",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Clean Cache",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Hardware Profile Bento Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${hwProfile.manufacturer} ${hwProfile.model}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                "${String.format("%.1f", hwProfile.totalRamGb)} GB RAM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Avail: ${String.format("%.1f", hwProfile.availRamGb)} GB",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${hwProfile.cpuCores} Cores (${hwProfile.primaryAbi})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${String.format("%.1f", hwProfile.freeStorageGb)} GB Free",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Hardware-specific Recommendation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            hwProfile.recommendationReason,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Models List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OfflineModelManager.AVAILABLE_MODELS.forEach { model ->
                    val state = modelStates[model.id]
                    val isDownloading = state?.status == DownloadStatus.DOWNLOADING
                    val isPaused = state?.status == DownloadStatus.PAUSED
                    val isDownloaded = state?.status == DownloadStatus.DOWNLOADED
                    val isActive = activeModelId == model.id
                    val isRecommended = model.id == hwProfile.recommendedModelId

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isActive) 0.35f else 0.2f),
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
                                            model.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                        )
                                        if (isRecommended) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            ) {
                                                Text(
                                                    "RECOMMENDED",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${model.formattedSize} · ${model.quantization} · ${model.parameterSize}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
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
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                model.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 15.sp,
                            )

                            // Progress Bar when downloading or paused
                            if (isDownloading || isPaused) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state?.progress ?: 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isPaused) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val downloadedMb = (state?.bytesDownloaded ?: 0L).toFloat() / (1024f * 1024f)
                                    val totalMb = (state?.totalBytes ?: model.fileSizeBytes).toFloat() / (1024f * 1024f)
                                    Text(
                                        if (isPaused) "Paused · ${String.format("%.1f", downloadedMb)} / ${String.format("%.1f", totalMb)} MB"
                                        else "Downloading: ${((state?.progress ?: 0f) * 100).toInt()}% (${String.format("%.1f", downloadedMb)} / ${String.format("%.1f", totalMb)} MB)",
                                        fontSize = 10.5.sp,
                                        color = if (isPaused) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                    )
                                    if (isDownloading && (state?.speedMbps ?: 0f) > 0f) {
                                        Text(
                                            "${String.format("%.1f", state?.speedMbps)} Mbps",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            if (state?.status == DownloadStatus.ERROR) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    state.errorMessage ?: "Download interrupted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Action Buttons: Download, Pause, Resume, Delete, Set Active
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isDownloaded) {
                                    IconButton(
                                        onClick = {
                                            OfflineModelManager.deleteModel(context, model)
                                            if (activeModelId == model.id) {
                                                activeModelId = null
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (!isActive) {
                                        OutlinedButton(
                                            onClick = {
                                                activeModelId = model.id
                                                OfflineModelManager.setActiveModelId(model.id)
                                                onModelSelected(model.id)
                                                Toast.makeText(context, "Activated ${model.name} for offline coding", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Text("Set Active", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else if (isDownloading) {
                                    OutlinedButton(
                                        onClick = {
                                            OfflineModelManager.pauseDownload(model.id)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) {
                                        Icon(Icons.Default.Pause, null, Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pause", fontSize = 11.sp)
                                    }
                                } else if (isPaused) {
                                    IconButton(
                                        onClick = {
                                            OfflineModelManager.deleteModel(context, model)
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            val job = scope.launch {
                                                OfflineModelManager.startDownload(context, model)
                                            }
                                            OfflineModelManager.registerJob(model.id, job)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val job = scope.launch {
                                                OfflineModelManager.startDownload(context, model)
                                            }
                                            OfflineModelManager.registerJob(model.id, job)
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

