package com.mindsparq.jarvis.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianBgTier0
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianElectricCyan
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianErrorRed
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianSurfaceTier1
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianTextMuted
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianTextOnSurface
import com.mindsparq.jarvis.core.designsystem.theme.ObsidianWarningAmber

@Composable
fun ApprovalCard(
    title: String,
    actionDetail: String,
    onApprove: () -> Unit,
    onDeny: (reason: String) -> Unit,
    onAlwaysAllow: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceTier1),
        border = BorderStroke(1.dp, ObsidianWarningAmber)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ $title",
                    color = ObsidianWarningAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "REQUIRES CONFIRMATION",
                    color = ObsidianTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code / Command preview box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBgTier0, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = actionDetail,
                    color = ObsidianTextOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianElectricCyan,
                        contentColor = Color(0xFF07090E)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = { onDeny("User rejected action") },
                    colors = ButtonDefaults.textButtonColors(contentColor = ObsidianErrorRed)
                ) {
                    Text("Deny")
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onAlwaysAllow != null) {
                    OutlinedButton(
                        onClick = onAlwaysAllow,
                        border = BorderStroke(1.dp, ObsidianTextMuted),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Always Allow", color = ObsidianTextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

