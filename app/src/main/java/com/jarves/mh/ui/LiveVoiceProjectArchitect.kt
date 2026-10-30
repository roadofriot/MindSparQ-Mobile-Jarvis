package com.jarves.mh.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jarves.mh.R
import com.jarves.mh.model.AndroidTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ArchitectWorkflowStage {
    DISCOVERY,         // Requirements interview & interactive MCQs
    PLAN_REVIEW,       // PRD, TRD, Architecture & Decisions Review
    PLAN_APPROVED,     // Stage 1 complete -> Ready for Stage 2 Build
    BUILDING,          // Scaffolding project & writing artifacts
}

data class McqOption(
    val id: String,
    val title: String,
    val isRecommended: Boolean = false,
    val aiRationale: String = "",
)

data class McqQuestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val options: List<McqOption>,
    var selectedOptionId: String? = null,
    var customAnswer: String? = null,
    var isAiDecided: Boolean = false,
    var aiReasoning: String? = null,
)

data class ArchitectMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "AI" or "USER"
    val text: String,
    val suggestedProjectName: String? = null,
    val suggestedTemplate: AndroidTemplate? = null,
    val prd: String? = null,
    val trd: String? = null,
    val architecture: String? = null,
    val decisions: String? = null,
    val workflowStage: ArchitectWorkflowStage = ArchitectWorkflowStage.DISCOVERY,
)

@Composable
fun LiveVoiceProjectArchitectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String, decisions: String) -> Unit,
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isVoiceMuted by rememberSaveable { mutableStateOf(false) }
    var selectedPlanTab by rememberSaveable { mutableIntStateOf(0) }
    var currentWorkflowStage by rememberSaveable { mutableStateOf(ArchitectWorkflowStage.DISCOVERY) }

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
                    instance?.setSpeechRate(1.04f)
                    instance?.setPitch(0.97f)
                }
                ttsEngine = instance
                if (!isVoiceMuted) {
                    val greeting = "MindSparQ Jarvis online. What project idea are we architecting today?"
                    instance?.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "jarvis_welcome")
                }
            }
        }
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    // Adaptive Structured Requirements Discovery Questions
    val questions = remember {
        mutableStateListOf(
            McqQuestion(
                id = "platform",
                title = "1. Target Platform & Runtime",
                subtitle = "Where will your application primarily run?",
                options = listOf(
                    McqOption("android_native", "Native Android (Compose / Kotlin)", isRecommended = true, aiRationale = "Optimal performance, low RAM footprint, and full hardware camera & sensor access."),
                    McqOption("kmp", "Kotlin Multiplatform (Mobile + Desktop)", aiRationale = "Shares domain logic seamlessly across Android, iOS, and Desktop JVM."),
                    McqOption("web", "Full-Stack Web (Node.js / React)", aiRationale = "Instant cross-platform browser availability with low barrier to entry."),
                    McqOption("cli", "Linux Terminal CLI Daemon", aiRationale = "Ultra-lightweight headless service with minimal CPU and memory overhead.")
                )
            ),
            McqQuestion(
                id = "architecture",
                title = "2. Architecture & State Management",
                subtitle = "Choose your code structure pattern",
                options = listOf(
                    McqOption("mvi", "Clean MVI + StateFlow (Unidirectional)", isRecommended = true, aiRationale = "Predictable state transitions, 100% testable reducers, and immutable UI state."),
                    McqOption("mvvm", "Decoupled MVVM + Kotlin Coroutines", aiRationale = "Standard Android lifecycle-aware ViewModels and simple reactive streams."),
                    McqOption("event_driven", "Event-Driven Actor Pipeline", aiRationale = "High throughput asynchronous event streaming for real-time applications."),
                    McqOption("modular", "Minimal Modular Rapid Prototype", aiRationale = "Zero boilerplate, flat package structure for rapid concept validation.")
                )
            ),
            McqQuestion(
                id = "persistence",
                title = "3. Data Persistence & Storage",
                subtitle = "How should local and cloud data be stored?",
                options = listOf(
                    McqOption("offline_room", "Offline-First Room SQLite Database", isRecommended = true, aiRationale = "Zero internet dependency with immediate startup response and ACID safety."),
                    McqOption("encrypted_kv", "Hardware-Backed KeyStore & Encrypted Storage", aiRationale = "Military-grade AES-256 GCM encryption for authentication tokens and credentials."),
                    McqOption("cloud_sync", "Ktor HTTP / WebSockets Cloud Sync", aiRationale = "Real-time background bidirectional synchronization with remote APIs."),
                    McqOption("in_memory", "Pure In-Memory / Ephemeral", aiRationale = "Transient in-RAM processing without disk I/O latency.")
                )
            ),
            McqQuestion(
                id = "theme",
                title = "4. UI Design & Theme Aesthetic",
                subtitle = "Visual design system and typography",
                options = listOf(
                    McqOption("obsidian_dark", "Obsidian Dark OLED (Apple / Mac Minimal)", isRecommended = true, aiRationale = "High contrast, battery-efficient, sleek 2026 dark mode with subtle borders."),
                    McqOption("clean_light", "Clean Light Neutral", aiRationale = "Airy, high readability layout tailored for daylight reading."),
                    McqOption("cyber_neon", "Cyber Neon Hacker Monospace", aiRationale = "Developer-centric vibrant accent palette with monospace typography.")
                )
            )
        )
    }

    val messages = remember {
        mutableStateListOf(
            ArchitectMessage(
                sender = "AI",
                text = "MindSparQ Jarvis online. What project idea are we architecting today?\n\nTell me your concept or select requirements below. I'll ask adaptive clarification questions, form an implementation plan, and wait for your explicit approval before building.",
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
                    questions = questions,
                    currentStage = currentWorkflowStage,
                    setStage = { currentWorkflowStage = it },
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

    // Microphone pulse animation
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

    val resolvedCount = questions.count { it.selectedOptionId != null || !it.customAnswer.isNullOrBlank() }
    val totalCount = questions.size

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
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F141C),
            border = BorderStroke(1.dp, Color(0xFF1E2638)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .imePadding()
            ) {
                // Header Bar
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
                                "Adaptive Discovery · 2-Stage Gated Engineering",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Requirements Progress Bar
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, Color(0xFF222F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Requirements Discovery Progress",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                "$resolvedCount / $totalCount Resolved",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (resolvedCount == totalCount) Color(0xFF2EA043) else Color(0xFF38BDF8)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { resolvedCount.toFloat() / totalCount },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (resolvedCount == totalCount) Color(0xFF2EA043) else Color(0xFF38BDF8),
                            trackColor = Color(0xFF0F141C),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Chat History & Interactive Cards
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
                                modifier = Modifier.fillMaxWidth(0.94f),
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

                                    // Interactive PRD / TRD / Architecture / Decisions Plan Viewer
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
                                                    Tab(
                                                        selected = selectedPlanTab == 3,
                                                        onClick = { selectedPlanTab = 3 },
                                                        text = { Text("Decisions Log", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                    )
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                val displayDoc = when (selectedPlanTab) {
                                                    0 -> msg.prd
                                                    1 -> msg.trd
                                                    2 -> msg.architecture
                                                    else -> msg.decisions.orEmpty()
                                                }

                                                Text(
                                                    text = displayDoc,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Stage 1 Gating Button: Approve Implementation Plan
                                    if (msg.workflowStage == ArchitectWorkflowStage.PLAN_REVIEW) {
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                handleUserConversationalTurn(
                                                    userInput = "Approve implementation plan",
                                                    messages = messages,
                                                    questions = questions,
                                                    currentStage = currentWorkflowStage,
                                                    setStage = { currentWorkflowStage = it },
                                                    setThinking = { isThinking = it },
                                                    speak = { speakAloud(it) },
                                                    onCreateProject = onCreateProject,
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color.White)
                                            Spacer(Modifier.width(8.dp))
                                            Text("✅ Stage 1: Approve Implementation Plan", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    // Stage 2 Gating Button: OK, Proceed with Implementation
                                    if (msg.workflowStage == ArchitectWorkflowStage.PLAN_APPROVED) {
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                handleUserConversationalTurn(
                                                    userInput = "Proceed with implementation and build",
                                                    messages = messages,
                                                    questions = questions,
                                                    currentStage = currentWorkflowStage,
                                                    setStage = { currentWorkflowStage = it },
                                                    setThinking = { isThinking = it },
                                                    speak = { speakAloud(it) },
                                                    onCreateProject = onCreateProject,
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                                            Spacer(Modifier.width(8.dp))
                                            Text("🚀 Stage 2: OK, Proceed with Implementation", fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive MCQ Discovery Question Cards
                    if (currentWorkflowStage == ArchitectWorkflowStage.DISCOVERY) {
                        item {
                            Text(
                                "Select your requirements or let AI decide:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(questions, key = { it.id }) { q ->
                            val isResolved = q.selectedOptionId != null || !q.customAnswer.isNullOrBlank()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF131A26),
                                border = BorderStroke(1.dp, if (isResolved) Color(0xFF2EA043).copy(alpha = 0.5f) else Color(0xFF1E2638)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                                            Text(q.subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }
                                        if (isResolved) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF2EA043).copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, Color(0xFF2EA043))
                                            ) {
                                                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Check, null, tint = Color(0xFF2EA043), modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Resolved", fontSize = 10.sp, color = Color(0xFF2EA043), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Option Chips
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        q.options.forEach { option ->
                                            val isSelected = q.selectedOptionId == option.id && q.customAnswer == null
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F141C),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2638)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        q.selectedOptionId = option.id
                                                        q.customAnswer = null
                                                        q.isAiDecided = false
                                                        q.aiReasoning = null
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .background(if (isSelected) Color(0xFF38BDF8) else Color.Transparent, CircleShape)
                                                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569), CircleShape)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(option.title, fontSize = 12.sp, color = if (isSelected) Color.White else Color(0xFFCBD5E1))
                                                    }
                                                    if (option.isRecommended) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                                        ) {
                                                            Text(
                                                                "Recommended",
                                                                fontSize = 9.5.sp,
                                                                color = Color(0xFFF59E0B),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Action bar: AI Decide & Custom Answer
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // AI Decide Button
                                        OutlinedButton(
                                            onClick = {
                                                val rec = q.options.firstOrNull { it.isRecommended } ?: q.options.first()
                                                q.selectedOptionId = rec.id
                                                q.customAnswer = null
                                                q.isAiDecided = true
                                                q.aiReasoning = rec.aiRationale
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f))
                                        ) {
                                            Icon(Icons.Default.Psychology, null, modifier = Modifier.size(15.dp), tint = Color(0xFFA78BFA))
                                            Spacer(Modifier.width(6.dp))
                                            Text("🤖 AI Decide", fontSize = 11.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.SemiBold)
                                        }

                                        // Custom Answer Indicator / Input Toggle
                                        var showCustomInput by remember { mutableStateOf(q.customAnswer != null) }
                                        TextButton(
                                            onClick = { showCustomInput = !showCustomInput },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(13.dp), tint = Color(0xFF94A3B8))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (showCustomInput) "Hide Custom" else "Custom Answer", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }
                                    }

                                    // Transparent AI Reasoning Display
                                    if (q.isAiDecided && q.aiReasoning != null) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "💡 Jarvis Rationale: ${q.aiReasoning}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD8B4FE),
                                                modifier = Modifier.padding(8.dp),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    // Custom Answer Input Field
                                    if (q.customAnswer != null || q.selectedOptionId == null) {
                                        var customText by remember { mutableStateOf(q.customAnswer.orEmpty()) }
                                        OutlinedTextField(
                                            value = customText,
                                            onValueChange = {
                                                customText = it
                                                q.customAnswer = if (it.isNotBlank()) it else null
                                                if (it.isNotBlank()) {
                                                    q.selectedOptionId = null
                                                    q.isAiDecided = false
                                                    q.aiReasoning = null
                                                }
                                            },
                                            placeholder = { Text("Or specify custom requirement...", fontSize = 11.5.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Unlock Implementation Plan button once requirements are sufficiently clear
                        item {
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    handleUserConversationalTurn(
                                        userInput = "Formulate implementation plan based on selected requirements",
                                        messages = messages,
                                        questions = questions,
                                        currentStage = currentWorkflowStage,
                                        setStage = { currentWorkflowStage = it },
                                        setThinking = { isThinking = it },
                                        speak = { speakAloud(it) },
                                        onCreateProject = onCreateProject,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("📋 Formulate Implementation Plan (PRD, TRD & Architecture)", fontWeight = FontWeight.Bold, color = Color.Black)
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
                                        questions = questions,
                                        currentStage = currentWorkflowStage,
                                        setStage = { currentWorkflowStage = it },
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
                                        questions = questions,
                                        currentStage = currentWorkflowStage,
                                        setStage = { currentWorkflowStage = it },
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
        .replace("MVI", "M V I")
        .replace("MVVM", "M V V M")
        .replace(Regex("[\\r\\n]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(300)
}

private fun handleUserConversationalTurn(
    userInput: String,
    messages: MutableList<ArchitectMessage>,
    questions: List<McqQuestion>,
    currentStage: ArchitectWorkflowStage,
    setStage: (ArchitectWorkflowStage) -> Unit,
    setThinking: (Boolean) -> Unit,
    speak: (String) -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String, decisions: String) -> Unit,
) {
    messages.add(ArchitectMessage(sender = "USER", text = userInput))
    setThinking(true)

    val lower = userInput.lowercase().trim()

    // 1. Check if user is approving Stage 2: Proceed with Implementation & Build
    val lastApprovedMessage = messages.findLast { it.sender == "AI" && it.workflowStage == ArchitectWorkflowStage.PLAN_APPROVED }
    if (lastApprovedMessage != null && (lower.contains("proceed") || lower.contains("yes") || lower.contains("build") || lower.contains("start") || lower.contains("agree") || lower.contains("implement") || lower.contains("launch"))) {
        setThinking(false)
        setStage(ArchitectWorkflowStage.BUILDING)
        val confirmation = "⚡ Launching build for '${lastApprovedMessage.suggestedProjectName}'. Writing PRD, TRD, Architecture, and Decisions artifacts to Documents/MindSparQ-Mobile-Jarvis/Plans and scaffolding project workspace."
        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = confirmation,
                workflowStage = ArchitectWorkflowStage.BUILDING,
            )
        )
        speak("Building your application now. Scaffolding project and writing specifications.")
        onCreateProject(
            lastApprovedMessage.suggestedProjectName ?: "mindsparq-app",
            lastApprovedMessage.suggestedTemplate,
            lastApprovedMessage.prd.orEmpty(),
            lastApprovedMessage.trd.orEmpty(),
            lastApprovedMessage.architecture.orEmpty(),
            lastApprovedMessage.decisions.orEmpty(),
        )
        return
    }

    // 2. Check if user is approving Stage 1: Approve Implementation Plan
    val lastReviewMessage = messages.findLast { it.sender == "AI" && it.workflowStage == ArchitectWorkflowStage.PLAN_REVIEW }
    if (lastReviewMessage != null && (lower.contains("approve") || lower.contains("proceed") || lower.contains("yes") || lower.contains("looks good") || lower.contains("confirm") || lower.contains("ok"))) {
        setThinking(false)
        setStage(ArchitectWorkflowStage.PLAN_APPROVED)
        val approvedNotice = "✅ **Stage 1 Complete: Implementation Plan Approved!**\n\nThe specifications for **${lastReviewMessage.suggestedProjectName}** are locked. Ready to execute code scaffolding. Say **'Proceed'** or tap the build button below to start building!"
        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = approvedNotice,
                suggestedProjectName = lastReviewMessage.suggestedProjectName,
                suggestedTemplate = lastReviewMessage.suggestedTemplate,
                prd = lastReviewMessage.prd,
                trd = lastReviewMessage.trd,
                architecture = lastReviewMessage.architecture,
                decisions = lastReviewMessage.decisions,
                workflowStage = ArchitectWorkflowStage.PLAN_APPROVED,
            )
        )
        speak("Plan approved. Whenever you are ready to assemble the codebase, say proceed or tap the button.")
        return
    }

    // 3. User requested plan formulation (or answered enough MCQs)
    val userWantsPlan = lower.contains("plan") || lower.contains("formulate") || lower.contains("generate") || lower.contains("draft") || lower.contains("proceed")
    if (userWantsPlan || currentStage == ArchitectWorkflowStage.PLAN_REVIEW) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            delay(500)
            val slug = extractProjectSlug(userInput, messages)
            val template = if (questions.find { it.id == "platform" }?.selectedOptionId == "android_native") AndroidTemplate.COMPOSE else null

            val decisionsLog = buildDecisionsLog(questions)
            val (prd, trd, arch) = generateFullTechnicalPlan(slug, decisionsLog, messages.map { "${it.sender}: ${it.text}" }.joinToString("\n"))

            val planIntro = "I have synthesized the complete Product Requirements Document (PRD), Technical Requirements Document (TRD), System Architecture, and Decisions Log for **$slug**.\n\nPlease review the tabs above. Once satisfied, click **'Stage 1: Approve Implementation Plan'** to proceed!"

            setStage(ArchitectWorkflowStage.PLAN_REVIEW)
            messages.add(
                ArchitectMessage(
                    sender = "AI",
                    text = planIntro,
                    suggestedProjectName = slug,
                    suggestedTemplate = template,
                    prd = prd,
                    trd = trd,
                    architecture = arch,
                    decisions = decisionsLog,
                    workflowStage = ArchitectWorkflowStage.PLAN_REVIEW,
                )
            )
            setThinking(false)
            speak("I have formulated the complete implementation plan for $slug. Review the specifications above and approve the plan when ready.")
        }
        return
    }

    // 4. Adaptive Requirements Discovery Turn
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        delay(500)
        val userTurnCount = messages.count { it.sender == "USER" }
        val slug = extractProjectSlug(userInput, messages)
        val (aiResponse, aiSpeech) = generateAdaptiveClarification(userInput, slug, questions)

        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = aiResponse,
                suggestedProjectName = slug,
                workflowStage = ArchitectWorkflowStage.DISCOVERY,
            )
        )
        setThinking(false)
        speak(aiSpeech)
    }
}

private fun extractProjectSlug(currentInput: String, messages: List<ArchitectMessage>): String {
    val allText = (messages.map { it.text } + currentInput).joinToString(" ")
    val words = currentInput.replace(Regex("[^a-zA-Z0-9 ]"), "")
        .split(" ")
        .filter { it.length > 2 && it.lowercase() !in listOf("the", "app", "want", "make", "create", "build", "with", "for", "please", "can", "you", "like", "need", "that", "this", "also", "proceed", "plan", "formulate") }
        .take(3)
        .joinToString("-") { it.lowercase() }
    return if (words.isNotBlank()) words else "mindsparq-project"
}

private fun buildDecisionsLog(questions: List<McqQuestion>): String {
    val sb = StringBuilder()
    sb.append("# Architectural Decisions Log\n\n")
    sb.append("| Category | Decision / Choice | Decided By | Engineering Rationale |\n")
    sb.append("|---|---|---|---|\n")
    questions.forEach { q ->
        val chosenText = when {
            !q.customAnswer.isNullOrBlank() -> q.customAnswer!!
            q.selectedOptionId != null -> q.options.firstOrNull { it.id == q.selectedOptionId }?.title ?: "Default"
            else -> q.options.firstOrNull { it.isRecommended }?.title ?: "Default"
        }
        val decidedBy = if (q.isAiDecided) "AI Architect" else if (q.customAnswer != null || q.selectedOptionId != null) "User" else "AI Default"
        val rationale = q.aiReasoning ?: q.options.firstOrNull { it.isRecommended }?.aiRationale ?: "Standard best practice"
        sb.append("| ${q.title.replace(Regex("^\\d+\\.\\s*"), "")} | $chosenText | $decidedBy | $rationale |\n")
    }
    return sb.toString()
}

private fun generateAdaptiveClarification(userInput: String, slug: String, questions: List<McqQuestion>): Pair<String, String> {
    val lower = userInput.lowercase()
    val uncompleted = questions.filter { it.selectedOptionId == null && it.customAnswer.isNullOrBlank() }

    val nextTopic = uncompleted.firstOrNull()?.title?.replace(Regex("^\\d+\\.\\s*"), "") ?: "Core Features"

    val (uiText, speechText) = when {
        lower.contains("crypto") || lower.contains("wallet") -> {
            "Understood. For **$slug**, I recommend hardware-backed KeyStore encryption and offline-first Room SQLite caching for resilient balance tracking. Would you like public price feeds via CoinGecko?" to
            "Understood. For $slug, I recommend hardware encryption and offline caching. What are the key screens you want to build first?"
        }
        lower.contains("shop") || lower.contains("commerce") || lower.contains("store") -> {
            "Noted. For **$slug**, we will configure an offline-first cart with Room database and snappy checkout state flow. Shall we proceed with Native Jetpack Compose or Kotlin Multiplatform?" to
            "Noted. For $slug, we will configure an offline cart with Room database and snappy checkout state flow. Shall we proceed with Native Jetpack Compose or Kotlin Multiplatform?"
        }
        lower.contains("ai") || lower.contains("agent") || lower.contains("llm") -> {
            "Excellent. For **$slug**, we'll architect streaming token support with Gemini 2.5 and local model fallbacks. Next, let's nail down **$nextTopic** in the options above." to
            "Excellent. For $slug, we will architect streaming token support with Gemini and local model fallbacks. Next, let's nail down $nextTopic in the options above."
        }
        else -> {
            "Great concept for **$slug**. To ensure production-grade architecture, I've highlighted recommendations for **$nextTopic** above. You can tap an option, choose **'🤖 AI Decide'**, or enter a custom requirement." to
            "Great concept for $slug. To ensure production grade architecture, please review the requirements above, or tap AI Decide to have me choose the optimal path."
        }
    }
    return uiText to speechText
}

private fun generateFullTechnicalPlan(projectName: String, decisionsLog: String, contextHistory: String): Triple<String, String, String> {
    val prd = """
# Product Requirements Document (PRD)
## Project: $projectName
**Author:** MindSparQ Jarvis Autonomous Architect  
**Status:** Approved for Implementation

### 1. Executive Summary & Vision
$projectName is engineered to deliver a seamless, high-performance user experience with zero runtime lag, resilient offline-first data persistence, and an intuitive modern interface.

### 2. Target Audience & Personas
- **Primary Users:** Power users seeking fast, responsive workflows on modern mobile and cross-platform devices.
- **Key Pain Point Solved:** Eliminates slow startup latencies, memory bloating, and unreliable network dependency through local caching.

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
├── DECISIONS.md
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
