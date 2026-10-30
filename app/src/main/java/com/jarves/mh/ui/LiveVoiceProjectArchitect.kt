package com.jarves.mh.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RocketLaunch
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.jarves.mh.network.GeminiLiveVoiceClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ArchitectWorkflowStage {
    CONVERSATION,      // Natural spoken dialogue first (exploring idea, platforms, features)
    PLAN_REVIEW,       // PRD, TRD, Architecture & Decisions Review
    PLAN_APPROVED,     // Stage 1 complete -> Ready for Stage 2 Implementation
    BUILDING,          // Scaffolding project and writing artifacts
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
    val decisions: String? = null,
    val workflowStage: ArchitectWorkflowStage = ArchitectWorkflowStage.CONVERSATION,
)

@Composable
fun LiveVoiceProjectArchitectDialog(
    geminiApiKey: String = "",
    onSaveGeminiApiKey: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String, decisions: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isVoiceMuted by rememberSaveable { mutableStateOf(false) }
    var selectedPlanTab by rememberSaveable { mutableIntStateOf(0) }
    var currentWorkflowStage by rememberSaveable { mutableStateOf(ArchitectWorkflowStage.CONVERSATION) }
    var showApiKeyInput by rememberSaveable { mutableStateOf(false) }
    var customApiKey by rememberSaveable { mutableStateOf(geminiApiKey) }

    val activeApiKey = customApiKey.ifBlank { geminiApiKey }

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
                // Configure natural speech
                val langResult = instance?.setLanguage(Locale.US)
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
                    instance?.setSpeechRate(1.02f)
                    instance?.setPitch(0.98f)
                }
                ttsEngine = instance
                if (!isVoiceMuted) {
                    val greeting = "नमस्ते! MindSparQ Jarvis online. What project or software idea are you thinking of building today?"
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
                text = "नमस्ते! MindSparQ Jarvis online. What project or application are we building today?\n\nTell me your idea naturally in Nepali or English. You choose the language and platform (Flutter, Android, iOS, Web, Go, Python, Desktop). Let's chat first, and whenever you're ready, we'll formulate the complete architecture plan!",
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
                    apiKey = activeApiKey,
                    messages = messages,
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ne-NP", "hi-IN"))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MindSparQ Live Voice Architect",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = Color(0xFFF0F6FC),
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (activeApiKey.isNotBlank()) Color(0xFF2EA043).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (activeApiKey.isNotBlank()) Color(0xFF2EA043).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        if (activeApiKey.isNotBlank()) "Gemini 2.5 Live" else "Local Voice",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeApiKey.isNotBlank()) Color(0xFF2EA043) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Live Conversational Partner · You Choose Tech Stack",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Key toggle
                        IconButton(onClick = { showApiKeyInput = !showApiKeyInput }) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "Set Gemini Key",
                                tint = if (activeApiKey.isNotBlank()) Color(0xFF2EA043) else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = {
                            isVoiceMuted = !isVoiceMuted
                            if (isVoiceMuted) ttsEngine?.stop()
                        }) {
                            Icon(
                                if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle Voice",
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

                // In-Dialog Gemini Key Bar (if toggled or missing)
                if (showApiKeyInput || activeApiKey.isBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF161E2E),
                        border = BorderStroke(1.dp, Color(0xFF222F46)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                placeholder = { Text("Paste Google Gemini API Key (AIza...)", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customApiKey.isNotBlank()) {
                                        onSaveGeminiApiKey(customApiKey.trim())
                                        showApiKeyInput = false
                                        Toast.makeText(context, "Gemini Key Saved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = customApiKey.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043))
                            ) {
                                Text("Save", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

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
                                                    apiKey = activeApiKey,
                                                    messages = messages,
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
                                                    apiKey = activeApiKey,
                                                    messages = messages,
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

                    // Conversational Phase Action: Quick button to formulate plan whenever user desires
                    if (currentWorkflowStage == ArchitectWorkflowStage.CONVERSATION && messages.count { it.sender == "USER" } >= 1) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    handleUserConversationalTurn(
                                        userInput = "Lock in my idea and synthesize the complete implementation plan",
                                        apiKey = activeApiKey,
                                        messages = messages,
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
                                Text("📋 Synthesize & Lock Architecture Plan", fontWeight = FontWeight.Bold, color = Color.Black)
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
                                    "Jarvis is thinking and formulating response...",
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
                            placeholder = { Text("Speak or type to Jarvis (Nepali / English)…", fontSize = 13.sp) },
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
                                        apiKey = activeApiKey,
                                        messages = messages,
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
                                        apiKey = activeApiKey,
                                        messages = messages,
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
                                Icons.AutoMirrored.Filled.Send,
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
        .replace(Regex("```[\\s\\S]*?```"), " I have detailed the code specifications in the plan document. ")
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
    apiKey: String,
    messages: MutableList<ArchitectMessage>,
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
    if (lastApprovedMessage != null && (lower.contains("proceed") || lower.contains("yes") || lower.contains("build") || lower.contains("start") || lower.contains("agree") || lower.contains("implement") || lower.contains("launch") || lower.contains("banaum") || lower.contains("thik cha"))) {
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
    if (lastReviewMessage != null && (lower.contains("approve") || lower.contains("proceed") || lower.contains("yes") || lower.contains("looks good") || lower.contains("confirm") || lower.contains("ok") || lower.contains("banaum") || lower.contains("thik cha"))) {
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

    // 3. User requested plan formulation
    val userWantsPlan = lower.contains("plan") || lower.contains("synthesize") || lower.contains("banaideu") || lower.contains("lock it in") || lower.contains("formulate")
    if (userWantsPlan || currentStage == ArchitectWorkflowStage.PLAN_REVIEW) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val slug = extractProjectSlug(userInput, messages)
            val allHistory = (messages.map { it.text } + userInput).joinToString(" ").lowercase()

            val detectedPlatform = when {
                allHistory.contains("flutter") || allHistory.contains("dart") -> "Flutter (Cross-Platform Mobile, Desktop, Web)"
                allHistory.contains("android") || allHistory.contains("compose") || allHistory.contains("kotlin") -> "Native Android (Jetpack Compose / Kotlin)"
                allHistory.contains("ios") || allHistory.contains("iphone") || allHistory.contains("mac") -> "iOS / macOS Desktop (Swift / Multiplatform)"
                allHistory.contains("desktop") || allHistory.contains("linux") || allHistory.contains("windows") -> "Cross-Platform Desktop (Linux, macOS, Windows)"
                allHistory.contains("golang") || allHistory.contains("go ") || allHistory.contains("go-") -> "Go (Golang High-Performance Microservices)"
                allHistory.contains("python") || allHistory.contains("fastapi") || allHistory.contains("django") -> "Python Backend & AI Services"
                allHistory.contains("web") || allHistory.contains("html") || allHistory.contains("react") || allHistory.contains("node") -> "Full-Stack Web (HTML5, CSS, React / Node.js)"
                else -> "User Selected Technology Stack"
            }

            val decisionsLog = """
# Architectural Decisions Log
| Category | Decision / Choice | Decided By | Engineering Rationale |
|---|---|---|---|
| Target Platform | $detectedPlatform | User Selection | Selected by developer based on product target requirements. |
| Architecture Pattern | Clean Architecture + Unidirectional StateFlow | AI Architect | Decoupled UI, robust business use cases, and 100% testable state transitions. |
| Data Persistence | Offline-First Local Storage & Caching | AI Architect | Resilient zero-latency startup and offline reliability. |
| Visual Aesthetic | Obsidian Dark OLED (Apple/macOS Minimal) | AI Architect | 2026 ergonomic dark mode with high contrast and subtle borders. |
""".trimIndent()

            val template = if (detectedPlatform.contains("Android")) AndroidTemplate.COMPOSE else null
            val (prd, trd, arch) = generateFullTechnicalPlan(slug, detectedPlatform, decisionsLog, messages.map { "${it.sender}: ${it.text}" }.joinToString("\n"))

            val planIntro = "I have synthesized the complete Product Requirements Document (PRD), Technical Requirements Document (TRD), System Architecture, and Decisions Log for **$slug** ($detectedPlatform).\n\nPlease review the tabs above. Once satisfied, click **'Stage 1: Approve Implementation Plan'** to proceed!"

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

    // 4. Natural Live AI Conversational Turn
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        val historyPairs = messages.map { it.sender to it.text }
        var aiReply: String? = null

        if (apiKey.isNotBlank()) {
            val result = GeminiLiveVoiceClient.generateConversationalReply(
                apiKey = apiKey,
                userPrompt = userInput,
                history = historyPairs,
            )
            aiReply = result.getOrNull()
        }

        if (aiReply.isNullOrBlank()) {
            // Intelligent natural conversational fallback (Nepali & English fluent)
            aiReply = generateLocalConversationalResponse(userInput, messages)
        }

        messages.add(
            ArchitectMessage(
                sender = "AI",
                text = aiReply,
                workflowStage = ArchitectWorkflowStage.CONVERSATION,
            )
        )
        setThinking(false)
        speak(aiReply)
    }
}

private fun generateLocalConversationalResponse(userInput: String, messages: List<ArchitectMessage>): String {
    val lower = userInput.lowercase()
    val slug = extractProjectSlug(userInput, messages)

    return when {
        lower.contains("flutter") -> {
            "बहुत राम्रो विचार! Flutter बाट तपाईंले Android, iOS, Desktop (Linux/Mac/Windows), र Web सबै एउटै कोडबेसबाट बनाउन सक्नुहुन्छ। तपाईंको $slug मा मुख्य मुख्य screen हरू के के राख्ने सोच छ?"
        }
        lower.contains("golang") || lower.contains("go") -> {
            "Excellent choice! Golang is ultra-fast, handles high concurrency, and compiles to clean static binaries. Will $slug provide REST APIs, gRPC, or microservice background workers?"
        }
        lower.contains("python") -> {
            "Great! Python provides rapid development with FastAPI, automation, and AI integrations. What will be the primary data flow or core features in $slug?"
        }
        lower.contains("web") || lower.contains("html") -> {
            "बबाल विचार! Web को लागि हामी responsive HTML5, modern CSS, र React/Vite प्रयोग गर्न सक्छौं। यसमा user authentication वा database sync चाहिन्छ?"
        }
        lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") -> {
            "नमस्ते! म तयार छु। तपाईं कुन project वा application बनाउन चाहनुहुन्छ, भन्नुहोस् — Flutter, Android, iOS, Desktop, वा Web जे पनि हामी बनाउन सक्छौं।"
        }
        else -> {
            "Understood! $slug sounds like a promising concept. You have full freedom to choose Flutter, Android, iOS, Desktop, or Web. What core features do you want users to experience first?"
        }
    }
}

private fun extractProjectSlug(currentInput: String, messages: List<ArchitectMessage>): String {
    val words = currentInput.replace(Regex("[^a-zA-Z0-9 ]"), "")
        .split(" ")
        .filter { it.length > 2 && it.lowercase() !in listOf("the", "app", "want", "make", "create", "build", "with", "for", "please", "can", "you", "like", "need", "that", "this", "also", "proceed", "plan", "formulate", "lock") }
        .take(3)
        .joinToString("-") { it.lowercase() }
    return if (words.isNotBlank()) words else "mindsparq-project"
}

private fun generateFullTechnicalPlan(
    projectName: String,
    platform: String,
    decisionsLog: String,
    contextHistory: String,
): Triple<String, String, String> {
    val prd = """
# Product Requirements Document (PRD)
## Project: $projectName
**Target Platform:** $platform  
**Author:** MindSparQ Jarvis Autonomous Architect  
**Status:** Approved for Implementation

### 1. Executive Summary & Vision
$projectName is engineered to deliver a seamless, high-performance user experience with zero runtime lag, resilient offline-first data persistence, and an intuitive modern interface.

### 2. Target Audience & Personas
- **Primary Users:** Power users seeking fast, responsive workflows across mobile, desktop, and web.
- **Key Pain Point Solved:** Eliminates slow startup latencies, memory bloating, and unreliable network dependency through local caching.

### 3. Core User Stories
1. **US-01:** As a user, I can launch the app instantly and access my local workspace within 200ms.
2. **US-02:** As a user, I can interact with rich controls, dynamic state updates, and view live results.
3. **US-03:** As a user, my data is securely encrypted at rest and automatically synchronized when network connectivity is established.

### 4. Non-Functional Requirements
- **Performance:** 60 FPS UI rendering, low memory footprint on all targets.
- **Reliability:** 100% crash-proof offline mode with graceful error boundaries.
- **Security:** Zero plaintext secret exposure; all sensitive tokens stored in secure hardware keychain.
""".trimIndent()

    val trd = """
# Technical Requirements Document (TRD)
## Project: $projectName
**Platform & Stack:** $platform

### 1. System Architecture
- **Pattern:** Unidirectional Data Flow (Clean Architecture / MVI)
- **Presentation Layer:** Modern declarative reactive components tailored to $platform.
- **Domain Layer:** Pure business logic and use cases.
- **Data Layer:** Repository pattern backed by local persistence and HTTP/WebSocket clients.

### 2. API & Service Contracts
- **Network Stack:** Resilient HTTP client with exponential backoff retries and JSON serialization.
- **State Management:** Asynchronous reactive streams and state flow.

### 3. Quality & Verification Gates
- Unit Test Coverage: >= 85% on core business use cases.
- Automated Linting and strict type checking.
""".trimIndent()

    val arch = """
# System Architecture Blueprint
## Project: $projectName
**Stack:** $platform

```mermaid
flowchart TD
    UI["Declarative UI Layer ($platform)"] --> VM["ViewModel / State Management"]
    VM --> Domain["Use Cases & Business Logic"]
    Domain --> Repo["Repository Pattern"]
    Repo --> Local["Offline-First Local Storage"]
    Repo --> Remote["Remote API & Cloud Services"]
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
│   ├── domain/
│   └── data/
```

### 4-Phase Implementation Roadmap
- **Phase 1:** Core Data Entities and Database Migrations
- **Phase 2:** Repository, Use Cases, and Reactive Pipeline
- **Phase 3:** Obsidian Kinetic Declarative UI Components
- **Phase 4:** End-to-End Testing, Polish, and Production Build Assembly
""".trimIndent()

    return Triple(prd, trd, arch)
}
