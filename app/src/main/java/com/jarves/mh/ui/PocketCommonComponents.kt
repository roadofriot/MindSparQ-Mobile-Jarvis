@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.jarves.mh.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarves.mh.R

internal enum class RootScreen(@StringRes val labelRes: Int, val icon: ImageVector) {
    PROJECTS(R.string.tab_projects, Icons.Default.Folder),
    AGENT(R.string.tab_agent, Icons.Default.SmartToy),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings),
    INTEGRITY(R.string.st_integrity_title, Icons.Default.Shield),
}

internal enum class WorkspaceTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    CHAT(R.string.tab_chat, Icons.Default.AutoAwesome),
    FILES(R.string.tab_files, Icons.Default.Folder),
    TERMINAL(R.string.tab_terminal, Icons.Default.Terminal),
    CHANGES(R.string.tab_changes, Icons.Default.Code),
    PREVIEW(R.string.tab_preview, Icons.Default.Preview),
}

@Composable
internal fun EmptyState(icon: ImageVector, title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val size = if (compact) 34.dp else 52.dp
    val cornerRadius = if (compact) 9.dp else 14.dp

    Image(
        painter = painterResource(R.drawable.mindsparq_logo),
        contentDescription = "MindSparQ Official Brand Logo",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun formatDuration(totalSeconds: Long): String = when {
    totalSeconds >= 3_600 -> stringResource(R.string.ws_duration_hm, totalSeconds / 3_600, (totalSeconds % 3_600) / 60)
    totalSeconds >= 60 -> stringResource(R.string.ws_duration_ms, totalSeconds / 60, totalSeconds % 60)
    else -> stringResource(R.string.ws_duration_s, totalSeconds)
}

internal fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "%.1f MB".format(bytes / 1_048_576.0)
}

internal fun formatMegabytes(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)

