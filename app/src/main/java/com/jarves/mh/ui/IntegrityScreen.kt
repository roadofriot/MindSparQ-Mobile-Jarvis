package com.jarves.mh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.jarves.mh.R
import com.jarves.mh.runtime.IntegrityScheduler
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityScreen(
    state: AppUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Projects with workspaces
    val projectsWithWorkspace = state.projects.filter { p ->
        File(context.filesDir, "workspaces/${p.id}").isDirectory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.st_integrity_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Subtitle
            Text(
                stringResource(R.string.st_integrity_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Check interval setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(R.string.st_integrity_check_interval),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${state.integrityCheckIntervalMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val next = (state.integrityCheckIntervalMinutes + 30).takeIf { it > 0 } ?: 30
                                viewModel.setIntegrityCheckIntervalMinutes(next)
                            }
                        },
                    ) {
                        Text("+30m")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setIntegrityCheckIntervalMinutes(
                                    (state.integrityCheckIntervalMinutes - 30).coerceAtLeast(0)
                                )
                            }
                        },
                    ) {
                        Text("-30m")
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.runIntegrityCheckAll()
                        }
                    },
                    enabled = !state.integrityChecking,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    if (state.integrityChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.st_integrity_check_all))
                    }
                }
                Button(
                    onClick = { /* toggle collapsed state handled by parent */ },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    Text("Expand All")
                }
            }

            if (state.integrityChecking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.st_integrity_checking))
                }
            }

            // Results
            if (projectsWithWorkspace.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.st_integrity_no_projects),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    "Project Results",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = projectsWithWorkspace,
                        key = { it.id },
                    ) { project ->
                        val result = state.integrityResults[project.id]
                        val isFirst = result == null || result.status == IntegrityScheduler.IntegrityResult.Status.FIRST_CHECK
                        val isOk = result?.status == IntegrityScheduler.IntegrityResult.Status.OK
                        val isChanged = result?.status == IntegrityScheduler.IntegrityResult.Status.CHANGED
                        val isError = result?.status == IntegrityScheduler.IntegrityResult.Status.ERROR

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            project.name,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        when {
                                            isFirst -> {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            isOk -> {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            isChanged -> {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            isError -> {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        when {
                                            isFirst -> stringResource(R.string.st_integrity_status_first)
                                            isOk -> stringResource(R.string.st_integrity_status_ok)
                                            isChanged -> "${stringResource(R.string.st_integrity_status_changed)} (${result!!.changedFiles.size})"
                                            isError -> stringResource(R.string.st_integrity_status_error)
                                            else -> "—"
                                        },
                                        fontSize = 11.sp,
                                        color = when {
                                            isFirst -> MaterialTheme.colorScheme.onSurfaceVariant
                                            isOk -> Color(0xFF10B981)
                                            isChanged -> Color(0xFFF59E0B)
                                            isError -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    if (result != null && result.checkedAtMillis > 0) {
                                        Text(
                                            stringResource(
                                                R.string.st_integrity_last_checked,
                                                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                                    .format(java.util.Date(result.checkedAtMillis)),
                                            ),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val workspace = File(context.filesDir, "workspaces/${project.id}")
                                                viewModel.runIntegrityCheck(project.id, project.name, workspace)
                                            }
                                        },
                                        enabled = !state.integrityChecking,
                                    ) {
                                        Text("Check")
                                    }
                                    if (result != null) {
                                        TextButton(
                                            onClick = {
                                                viewModel.resetIntegrityBaseline(project.id)
                                            },
                                        ) {
                                            Text(
                                                stringResource(R.string.st_integrity_reset_baseline),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            )
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
}
