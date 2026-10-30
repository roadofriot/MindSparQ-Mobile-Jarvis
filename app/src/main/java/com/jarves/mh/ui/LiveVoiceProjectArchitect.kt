package com.jarves.mh.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jarves.mh.R
import com.jarves.mh.model.AndroidTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ArchitectActionType {
    NONE,
    PROPOSE_PLAN, // Button: Generate PRD, TRD & Implementation Plan
    READY_TO_BUILD, // Button: Proceed & Build Application
}

data class ArchitectMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "AI" or "USER"
    val text: String,
    val suggestedProjectName: String? = null,
    val suggestedTemplate: AndroidTemplate? = null,
    val prd: String? = null,
    val trd: String? = null,
    val architecture: String? = null,
    val actionType: ArchitectActionType = ArchitectActionType.NONE,
)

@Composable
fun LiveVoiceProjectArchitectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String) -> Unit,
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isVoiceMuted by rememberSaveable { mutableStateOf(false) }
    var selectedPlanTab by rememberSaveable { mutableStateOf(0) }

    // Natural Human Voice Speech Engine
    var ttsEngine: TextToSpeech? by remember { mutableStateOf(null) }

    fun speakAloud(text: String) {
        if (!isVoiceMuted && ttsEngine != null) {
            val spoken = sanitizeForSpeech(text)
            ttsEngine?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        }
    }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                runCatching {
                    // Select highest quality human/neural English voice available
                    val voiceCandidates = instance?.voices?.filter { it.locale.language == "en" }
                    val highQualityVoice = voiceCandidates?.firstOrNull { voice ->
                        !voice.isNetworkConnectionRequired &&
                        (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH ||
                         voice.name.contains("neural", ignoreCase = true) ||
                         voice.name.contains("en-us-x", ignoreCase = true))
                    } ?: voiceCandidates?.firstOrNull()
                    if (highQualityVoice != null) {
                        instance?.voice = highQualityVoice
                    }
                    // Natural human speech tempo & warm pitch (Mark-LVI Jarvis cadence)
                    instance?.setSpeechRate(1.04f)
                    instance?.setPitch(0.97f)
                }
                ttsEngine = instance
                if (!isVoiceMuted) {
                    val greeting = "MindSparQ Jarvis online. What are we building today?"
                    instance?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "jarvis_welcome")
                }
            }
        }
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    val messages = remember {
        mutableStateListOf(
            ArchitectMessage(
                sender = "AI",
                text = "MindSparQ Jarvis online. What are we building today?",
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
                handleUserConversationalTurn(
                    userInput = spokenText,
                    messages = messages,
                    setThinking = { isThinking = it },
                    speak = { speakAloud(it) },
                    onCreateProject = onCreateProject,
                )
                inputText = ""
            }
        }
    }

    fun triggerVoiceInput() {
        ttsEngine?.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your project idea to Jarvis...")
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
        onDismissRequest = {
            ttsEngine?.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F141C),
            border = BorderStroke(1.dp, Color(0xFF1E2638)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .imePadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.mindsparq_logo),
                                    contentDescription = "MindSparQ Logo",
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "MindSparQ Live Voice Architect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF0F6FC),
                            )
                            Text(
                                "Autonomous Multi-Turn Engineering Partner",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice Mute Toggle
                        IconButton(onClick = {
                            isVoiceMuted = !isVoiceMuted
                            if (isVoiceMuted) ttsEngine?.stop()
                        }) {
                            Icon(
                                if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle Voice Reading",
                                tint = if (isVoiceMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            ttsEngine?.stop()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Chat History & Blueprint View
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isAi = msg.sender == "AI"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isAi) 4.dp else 16.dp,
                                    bottomEnd = if (isAi) 16.dp else 4.dp,
                                ),
                                color = if (isAi) Color(0xFF161E2E) else MaterialTheme.colorScheme.primaryContainer,
                                border = if (isAi) BorderStroke(1.dp, Color(0xFF222F46)) else null,
                                modifier = Modifier.fillMaxWidth(0.92f),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isAi) Icons.Default.SmartToy else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isAi) "MindSparQ Jarvis" else "You",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = msg.text,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = if (isAi) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )

                                    // Interactive PRD / TRD / Architecture Plan Viewer
                                    if (msg.prd != null && msg.trd != null && msg.architecture != null) {
                                        Spacer(Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF0B0E14),
                                            border = BorderStroke(1.dp, Color(0xFF1E2638)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                TabRow(
                                                    selectedTabIndex = selectedPlanTab,
                                                    containerColor = Color.Transparent,
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                ) {
                                                    Tab(
                                                        selected = selectedPlanTab == 0,
                                                        onClick = { selectedPlanTab = 0 },
                                                        text = { Text("PRD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                    Tab(
                                                        selected = selectedPlanTab == 1,
                                                        onClick = { selectedPlanTab = 1 },
                                                        text = { Text("TRD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                    Tab(
                                                        selected = selectedPlanTab == 2,
                                                        onClick = { selectedPlanTab = 2 },
                                                        text = { Text("Architecture", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                val displayDoc = when (selectedPlanTab) {
                                                    0 -> msg.prd
                                                    1 -> msg.trd
                                                    else -> msg.architecture
                                                }

                                                Text(
                                                    text = displayDoc,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Action Triggers
                                    if (msg.actionType == ArchitectActionType.PROPOSE_PLAN) {
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                handleUserConversationalTurn(
                                                    userInput = "Proceed and generate implementation plan",
                                                    messages = messages,
                                                    setThinking = { isThinking = it },
                                                    speak = { speakAloud(it) },
                                                    onCreateProject = onCreateProject,
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("📋 Generate PRD, TRD & Implementation Plan", fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    } else if (msg.actionType == ArchitectActionType.READY_TO_BUILD) {
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                handleUserConversationalTurn(
                                                    userInput = "Proceed and build application",
                                                    messages = messages,
                                                    setThinking = { isThinking = it },
                                                    speak = { speakAloud(it) },
                                                    onCreateProject = onCreateProject,
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("🚀 Proceed & Build Application Now", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
                                    "Jarvis Architect is formulating engineering specifications...",
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
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Speak or type to Jarvis…", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    handleUserConversationalTurn(
                                        userInput = text,
                                        messages = messages,
                                        setThinking = { isThinking = it },
                                        speak = { speakAloud(it) },
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
                                    handleUserConversationalTurn(
                                        userInput = text,
                                        messages = messages,
                                        setThinking = { isThinking = it },
                                        speak = { speakAloud(it) },
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

private fun sanitizeForSpeech(text: String): String {
    return text
        .replace(Regex("```[\\s\\S]*?```"), " I have detailed the code specifications in the document. ")
        .replace(Regex("[*#`_~]"), "")
        .replace(Regex("[•·-]\\s+"), "")
        .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
        .replace("PRD", "Product Requirements")
        .replace("TRD", "Technical Requirements")
        .replace("API", "A P I")
        .replace("UI", "U I")
        .replace("SQLite", "S Q L lite")
        .replace("SDK", "S D K")
        .replace(Regex("[\\r\\n]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(450)
}

private fun handleUserConversationalTurn(
    userInput: String,
    messages: MutableList<ArchitectMessage>,
    setThinking: (Boolean) -> Unit,
    speak: (String) -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String) -> Unit,
) {
    messages.add(ArchitectMessage(sender = "USER", text = userInput))
    setThinking(true)

    val lower = userInput.lowercase().trim()

    // 1. Check if user confirmed building after plan is generated
    val lastPlanMessage = messages.findLast { it.sender == "AI" && it.actionType == ArchitectActionType.READY_TO_BUILD }
    if (lastPlanMessage != null && (lower.contains("proceed") || lower.contains("yes") || lower.contains("build") || lower.contains("start") || lower.contains("agree") || lower.contains("implement") || lower.contains("launch"))) {
        setThinking(false)
        val confirmation = "⚡ Launching build for '${lastPlanMessage.suggestedProjectName}'. Scaffolding project into Documents/MindSparQ-Mobile-Jarvis, generating PRD, TRD, and architecture files, and opening workspace."
        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = confirmation,
                actionType = ArchitectActionType.NONE,
            )
        )
        speak("Initiating build. Creating project files, writing specifications, and launching your workspace now.")
        onCreateProject(
            lastPlanMessage.suggestedProjectName ?: "mindsparq-app",
            lastPlanMessage.suggestedTemplate,
            lastPlanMessage.prd.orEmpty(),
            lastPlanMessage.trd.orEmpty(),
            lastPlanMessage.architecture.orEmpty(),
        )
        return
    }

    // 2. Check if user confirmed generating implementation plan
    val lastProposeMessage = messages.findLast { it.sender == "AI" && it.actionType == ArchitectActionType.PROPOSE_PLAN }
    if (lastProposeMessage != null && (lower.contains("proceed") || lower.contains("yes") || lower.contains("plan") || lower.contains("generate") || lower.contains("create") || lower.contains("sure") || lower.contains("ok") || lower.contains("draft"))) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            delay(600)
            val projectName = lastProposeMessage.suggestedProjectName ?: "mindsparq-app"
            val template = lastProposeMessage.suggestedTemplate
            val (prd, trd, arch) = generateFullTechnicalPlan(projectName, messages.map { "${it.sender}: ${it.text}" }.joinToString("\n"))

            val planIntro = "I have formulated the complete Product Requirements Document (PRD), Technical Requirements Document (TRD), and System Architecture for **$projectName**.\n\nReview the tabs above. When you are ready, say **'Proceed'** or tap the build button below to start implementing the application!"

            messages.add(
                ArchitectMessage(
                    sender = "AI",
                    text = planIntro,
                    suggestedProjectName = projectName,
                    suggestedTemplate = template,
                    prd = prd,
                    trd = trd,
                    architecture = arch,
                    actionType = ArchitectActionType.READY_TO_BUILD,
                )
            )
            setThinking(false)
            speak("Blueprint synthesized for $projectName. Review the specifications above. When you are ready to start building the code, say proceed.")
        }
        return
    }

    // 3. Multi-turn Conversational Discovery & Architecture Interview
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        delay(600)
        val userTurnCount = messages.count { it.sender == "USER" }
        val (aiResponse, aiSpeech, slug, template, shouldProposePlan) = generateConversationalInterviewTurn(
            currentInput = userInput,
            turnCount = userTurnCount,
            fullConversation = messages.toList()
        )

        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = aiResponse,
                suggestedProjectName = slug,
                suggestedTemplate = template,
                actionType = if (shouldProposePlan) ArchitectActionType.PROPOSE_PLAN else ArchitectActionType.NONE,
            )
        )
        setThinking(false)
        speak(aiSpeech)
    }
}

private fun generateConversationalInterviewTurn(
    currentInput: String,
    turnCount: Int,
    fullConversation: List<ArchitectMessage>,
): Quintuple<String, String, String, AndroidTemplate?, Boolean> {
    val lower = currentInput.lowercase()

    // Determine template from context
    val allText = (fullConversation.map { it.text } + currentInput).joinToString(" ").lowercase()
    val template = when {
        allText.contains("android") || allText.contains("compose") || allText.contains("mobile") -> AndroidTemplate.COMPOSE
        allText.contains("xml") || allText.contains("view") -> AndroidTemplate.XML
        else -> null
    }

    // Extract smart project slug
    val words = currentInput.replace(Regex("[^a-zA-Z0-9 ]"), "")
        .split(" ")
        .filter { it.length > 2 && it.lowercase() !in listOf("the", "app", "want", "make", "create", "build", "with", "for", "please", "can", "you", "like", "need", "that", "this", "also") }
        .take(3)
        .joinToString("-") { it.lowercase() }
    val slug = if (words.isNotBlank()) words else "mindsparq-project"

    val userExplicitlyWantsPlan = lower.contains("plan") || lower.contains("proceed") || lower.contains("done") || lower.contains("create") || lower.contains("start") || lower.contains("draft")

    if (turnCount >= 2 || userExplicitlyWantsPlan) {
        val proposalText = buildString {
            append("Architecture locked in for **$slug**:\n\n")
            append("• **Platform**: ${if (template != null) "Native Android (Jetpack Compose / Kotlin)" else "Full-Stack Modular Application"}\n")
            append("• **Data Layer**: Offline-first Room persistence & encrypted storage\n")
            append("• **Architecture**: Unidirectional Data Flow with MVI & StateFlow\n\n")
            append("I have enough context to generate the complete **PRD**, **TRD**, and **Architecture Blueprint**. Shall I draft the implementation plan?")
        }
        val proposalSpeech = "Architecture locked in for $slug. I have enough context to synthesize the complete product requirements, technical specifications, and architecture blueprint. Shall I draft the implementation plan?"
        return Quintuple(proposalText, proposalSpeech, slug, template, true)
    }

    // Natural human engineer interview questions (Mark-LVI cadence)
    val (uiText, speechText) = when {
        lower.contains("store") || lower.contains("shop") || lower.contains("commerce") || lower.contains("delivery") || lower.contains("food") -> {
            "Understood. For **$slug**, will this require real-time order tracking with WebSockets, or local cart persistence with offline-first Room database? Also, what's your preferred theme — Obsidian Dark OLED or clean light?" to
            "Understood. For $slug, will this require real-time order tracking with web sockets, or local cart persistence and offline database? And what is your preferred theme?"
        }
        lower.contains("crypto") || lower.contains("finance") || lower.contains("money") || lower.contains("wallet") -> {
            "Understood. For **$slug**, should we connect public market price feeds (CoinGecko) with local caching, or track private balances with encrypted SQLite? And do you want interactive candlestick charts?" to
            "Understood. For $slug, should we connect public market price feeds with local caching, or track private balances with encrypted storage? And do you want interactive charts?"
        }
        lower.contains("ai") || lower.contains("llm") || lower.contains("bot") || lower.contains("chatgpt") || lower.contains("model") -> {
            "Understood. For **$slug**, will we integrate local device models via Ollama, or stream tokens from Gemini and OpenRouter with markdown rendering?" to
            "Understood. For $slug, will we integrate local device models, or stream tokens from Gemini and OpenRouter with markdown rendering?"
        }
        lower.contains("game") || lower.contains("play") -> {
            "Understood. For **$slug**, are you aiming for a 60 FPS canvas game loop, or turn-based interactive state machine logic?" to
            "Understood. For $slug, are you aiming for a 60 frames per second canvas game loop, or turn-based state machine logic?"
        }
        lower.contains("chat") || lower.contains("social") || lower.contains("message") -> {
            "Understood. For **$slug**, should we architect local peer-to-peer WebSocket messaging, or cloud-backed synchronization with end-to-end message encryption?" to
            "Understood. For $slug, should we architect local peer to peer web socket messaging, or cloud synchronization with message encryption?"
        }
        lower.contains("task") || lower.contains("todo") || lower.contains("note") || lower.contains("productivity") -> {
            "Understood. For **$slug**, should we include recurring background alarms and offline-first Room database sync so it works completely without internet?" to
            "Understood. For $slug, should we include recurring background alarms and offline database sync so it works completely without internet?"
        }
        else -> {
            "Understood. To get the architecture right for **$slug**, what are the top 2 features users will interact with first, and do you prefer offline-first local storage or external cloud services?" to
            "Understood. To get the architecture right for $slug, what are the top 2 features users will interact with first, and do you prefer offline local storage or external cloud services?"
        }
    }

    return Quintuple(uiText, speechText, slug, template, false)
}

private fun generateFullTechnicalPlan(projectName: String, contextHistory: String): Triple<String, String, String> {
    val prd = """
# Product Requirements Document (PRD)
## Project: $projectName
**Author:** MindSparQ Jarvis Autonomous Architect  
**Status:** Approved for Implementation

### 1. Executive Summary & Vision
$projectName is engineered to deliver a seamless, high-performance user experience with zero runtime lag, resilient offline-first data persistence, and an intuitive modern interface.

### 2. Target Audience & Personas
- **Primary Users:** Power users seeking fast, responsive workflows on modern mobile and cross-platform devices.
- **Key Pain Point Solved:** Eliminates fragmentation, slow startup latencies, and unreliable network dependency through local caching.

### 3. Core User Stories
1. **US-01:** As a user, I can launch the app instantly and access my local workspace within 200ms.
2. **US-02:** As a user, I can interact with rich controls, dynamic state updates, and view live results with haptic feedback.
3. **US-03:** As a user, my data is securely encrypted at rest and automatically synchronized when network connectivity is established.

### 4. Non-Functional Requirements
- **Performance:** 60 FPS UI rendering, < 50MB RAM footprint on low-spec hardware.
- **Reliability:** 100% crash-proof offline mode with graceful error boundaries.
- **Security:** Zero plaintext secret exposure; all sensitive tokens stored in secure hardware keychain.
""".trimIndent()

    val trd = """
# Technical Requirements Document (TRD)
## Project: $projectName

### 1. System Architecture
- **Pattern:** Unidirectional Data Flow (MVI / Clean Architecture)
- **Presentation Layer:** Jetpack Compose / Declarative UI components with Material 3 Obsidian tokens.
- **Domain Layer:** Pure Kotlin business logic and use cases.
- **Data Layer:** Repository pattern backed by SQLite / Room local storage and Ktor HTTP client.

### 2. Data Models & Schema
```sql
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### 3. API & Service Contracts
- **Network Stack:** Ktor 3.x with exponential backoff retries and JSON serialization.
- **State Management:** Kotlin Coroutines `StateFlow` and `SharedFlow`.

### 4. Quality & Verification Gates
- Unit Test Coverage: >= 85% on core business use cases.
- Automated Linting and strict type checking.
""".trimIndent()

    val arch = """
# System Architecture Blueprint
## Project: $projectName

```mermaid
flowchart TD
    UI["Jetpack Compose UI Layer"] --> VM["ViewModel / StateFlow"]
    VM --> Domain["Use Cases & Business Logic"]
    Domain --> Repo["Repository Pattern"]
    Repo --> Local["SQLite / Room Database"]
    Repo --> Remote["Ktor HTTP Client"]
```

### Directory Structure Map
```text
$projectName/
├── PRD.md
├── TRD.md
├── ARCHITECTURE.md
├── README.md
├── src/
│   ├── ui/
│   │   ├── components/
│   │   └── screens/
│   ├── domain/
│   │   ├── models/
│   │   └── usecases/
│   └── data/
│       ├── local/
│       └── remote/
└── build.gradle.kts
```

### 4-Phase Implementation Roadmap
- **Phase 1:** Core Data Entities and Database Migrations
- **Phase 2:** Repository, Use Cases, and StateFlow Pipeline
- **Phase 3:** Obsidian Kinetic Declarative UI Components
- **Phase 4:** End-to-End Testing, Polish, and Production APK Assembly
""".trimIndent()

    return Triple(prd, trd, arch)
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
