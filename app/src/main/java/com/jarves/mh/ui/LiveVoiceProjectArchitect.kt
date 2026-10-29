package com.jarves.mh.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.jarves.mh.R
import com.jarves.mh.model.AndroidTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ArchitectMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "AI" or "USER"
    val text: String,
    val suggestedProjectName: String? = null,
    val suggestedTemplate: AndroidTemplate? = null,
    val readyToProceed: Boolean = false,
)

@Composable
fun LiveVoiceProjectArchitectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ArchitectMessage(
                sender = "AI",
                text = "Welcome to MindSparQ Live Voice Studio! Speak or type your project idea. I'll help you clarify the tech stack, architecture, and requirements. When you're happy, just say 'Proceed' to scaffold it immediately.",
            )
        )
    }

    // Voice dictation launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                // Automatically submit voice query
                handleUserMessage(
                    text = spokenText,
                    messages = messages,
                    setThinking = { isThinking = it },
                    onCreateProject = onCreateProject,
                )
                inputText = ""
            }
        }
    }

    fun triggerVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your project idea...")
        }
        try {
            isListening = true
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            isListening = false
        }
    }

    // Pulsing effect for microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.mindsparq_logo),
                                    contentDescription = "MindSparQ Logo",
                                    modifier = Modifier.size(30.dp).clip(CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Live Voice Idea Architect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                            Text(
                                "MindSparQ Autonomous Studio",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Chat History
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        if (msg.sender == "USER") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(18.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(2.dp)),
                                    modifier = Modifier.fillMaxWidth(0.85f),
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(18.dp).copy(bottomStart = androidx.compose.foundation.shape.CornerSize(2.dp)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth(0.92f),
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp,
                                        )

                                        if (msg.suggestedProjectName != null) {
                                            Spacer(Modifier.height(10.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            "Project: ${msg.suggestedProjectName}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                        )
                                                    }
                                                    if (msg.suggestedTemplate != null) {
                                                        Text(
                                                            "Stack: ${msg.suggestedTemplate.name}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(top = 2.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (msg.readyToProceed && msg.suggestedProjectName != null) {
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    onCreateProject(msg.suggestedProjectName, msg.suggestedTemplate)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Icon(Icons.Default.RocketLaunch, null, Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "🚀 Proceed & Create Project",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isThinking) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "MindSparQ Architect is reasoning...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Bottom Voice & Text Input Bar
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Glowing Voice Microphone Button
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(46.dp)
                                .scale(pulseScale)
                                .clickable { triggerVoiceInput() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speak idea",
                                    tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Speak or type project idea…", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    handleUserMessage(
                                        text = text,
                                        messages = messages,
                                        setThinking = { isThinking = it },
                                        onCreateProject = onCreateProject,
                                    )
                                }
                            }),
                        )

                        Spacer(Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    handleUserMessage(
                                        text = text,
                                        messages = messages,
                                        setThinking = { isThinking = it },
                                        onCreateProject = onCreateProject,
                                    )
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleUserMessage(
    text: String,
    messages: MutableList<ArchitectMessage>,
    setThinking: (Boolean) -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?) -> Unit,
) {
    messages.add(ArchitectMessage(sender = "USER", text = text))
    setThinking(true)

    val lower = text.lowercase().trim()

    // Check if user said "proceed"
    val lastAi = messages.findLast { it.sender == "AI" && it.suggestedProjectName != null }
    if (lastAi != null && (lower.contains("proceed") || lower.contains("yes") || lower.contains("create") || lower.contains("start"))) {
        setThinking(false)
        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = "⚡ Proceeding! Creating '${lastAi.suggestedProjectName}' right now. Welcome to your new workspace!",
                readyToProceed = false,
            )
        )
        onCreateProject(lastAi.suggestedProjectName!!, lastAi.suggestedTemplate)
        return
    }

    // Intelligent idea clarification
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        delay(600) // Brief natural pause

        val (slug, template, clarification) = generateClarification(text)

        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = clarification,
                suggestedProjectName = slug,
                suggestedTemplate = template,
                readyToProceed = true,
            )
        )
        setThinking(false)
    }
}

private fun generateClarification(userIdea: String): Triple<String, AndroidTemplate?, String> {
    val lower = userIdea.lowercase()

    val template = when {
        lower.contains("compose") || lower.contains("jetpack") -> AndroidTemplate.COMPOSE
        lower.contains("xml") || lower.contains("view") -> AndroidTemplate.XML
        lower.contains("android") -> AndroidTemplate.COMPOSE
        else -> null
    }

    // Generate clean slug from idea
    val words = userIdea.replace(Regex("[^a-zA-Z0-9 ]"), "")
        .split(" ")
        .filter { it.length > 2 && it.lowercase() !in listOf("the", "app", "want", "make", "create", "build", "with", "for") }
        .take(3)
        .joinToString("-") { it.lowercase() }
    val slug = if (words.isNotBlank()) words else "mindsparq-project"

    val clarification = buildString {
        append("Brilliant concept! I have structured the blueprint for **$slug**.\n\n")
        if (template != null) {
            append("• **Architecture**: Native Android with ${template.name}\n")
        } else {
            append("• **Architecture**: Full-stack modular workspace\n")
        }
        append("• **Recommended Modules**: UI components, SQLite local storage, and AI agent hooks.\n\n")
        append("Say **'Proceed'** or tap the button below to scaffold the folder structure immediately.")
    }

    return Triple(slug, template, clarification)
}

