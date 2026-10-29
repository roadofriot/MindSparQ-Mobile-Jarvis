package com.mindsparq.jarvis.core.designsystem.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianBgTier0
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianContainerTier2
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianElectricCyan
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianSurfaceTier1
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianSyntheticEmerald
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianTextMuted
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianTextOnSurface

enum class WindowWidthClass {
    COMPACT,   // Phone portrait (< 600 dp)
    MEDIUM,    // Tablet / Foldable (600 - 1024 dp)
    EXPANDED   // PC Desktop / Ultra-wide (> 1024 dp)
}

object ResponsiveLayoutHelper {
    fun resolveWidthClass(widthDp: Int): WindowWidthClass {
        return when {
            widthDp < 600 -> WindowWidthClass.COMPACT
            widthDp <= 1024 -> WindowWidthClass.MEDIUM
            else -> WindowWidthClass.EXPANDED
        }
    }
}

@Composable
fun HubTopBar(
    projectName: String,
    modelName: String,
    modeLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianSurfaceTier1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Project Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(ObsidianContainerTier2, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "●",
                color = ObsidianSyntheticEmerald,
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = projectName,
                color = ObsidianTextOnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Model & Mode Capsule
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = modelName,
                color = ObsidianElectricCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(ObsidianBgTier0, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = modeLabel,
                color = ObsidianTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

