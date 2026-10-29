package com.mindsparq.jarvis.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DiffLineType {
    ADDITION,
    DELETION,
    CONTEXT,
    HEADER
}

data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNum: Int? = null,
    val newLineNum: Int? = null
)

object DiffParser {
    fun parse(diffText: String): List<DiffLine> {
        val lines = diffText.lines()
        var oldCounter = 1
        var newCounter = 1
        return lines.map { line ->
            when {
                line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@") -> {
                    DiffLine(DiffLineType.HEADER, line)
                }
                line.startsWith("+") -> {
                    DiffLine(DiffLineType.ADDITION, line, newLineNum = newCounter++)
                }
                line.startsWith("-") -> {
                    DiffLine(DiffLineType.DELETION, line, oldLineNum = oldCounter++)
                }
                else -> {
                    DiffLine(DiffLineType.CONTEXT, line, oldLineNum = oldCounter++, newLineNum = newCounter++)
                }
            }
        }
    }
}

@Composable
fun InteractiveDiffViewer(
    modifier: Modifier = Modifier,
    filePath: String,
    diffContent: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val diffLines = DiffParser.parse(diffContent)

    val addColor = Color(0x2210B981)
    val delColor = Color(0x22EF4444)
    val addText = Color(0xFF10B981)
    val delText = Color(0xFFEF4444)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // File header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = filePath,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "${diffLines.count { it.type == DiffLineType.ADDITION }} additions, ${diffLines.count { it.type == DiffLineType.DELETION }} deletions",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Diff lines viewer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(diffLines) { item ->
                    val bg = when (item.type) {
                        DiffLineType.ADDITION -> addColor
                        DiffLineType.DELETION -> delColor
                        else -> Color.Transparent
                    }
                    val textColor = when (item.type) {
                        DiffLineType.ADDITION -> addText
                        DiffLineType.DELETION -> delText
                        DiffLineType.HEADER -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (item.newLineNum ?: item.oldLineNum)?.toString() ?: " ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Floating approval actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReject()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApprove()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Apply Diff")
            }
        }
    }
}

