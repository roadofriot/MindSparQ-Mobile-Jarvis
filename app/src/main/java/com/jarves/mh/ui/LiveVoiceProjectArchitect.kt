package com.jarves.mh.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import com.jarves.mh.util.ConversationExportHelper
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
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
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.network.GeminiLiveVoiceClient
import com.jarves.mh.network.VoiceIntent
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class JarvisVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
}

data class JarvisVoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "JARVIS" or "USER"
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * FatihMakes-inspired JARVIS Live Voice Assistant for MindSparQ.
 * Replaces bureaucratic questionnaire forms with an ultra-responsive,
 * human-like conversational AI partner and Arc Reactor Neural Core.
 */
@Composable
fun LiveVoiceProjectArchitectDialog(
    geminiApiKey: String = "",
    onSaveGeminiApiKey: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onCreateProject: (name: String, template: AndroidTemplate?, prd: String, trd: String, architecture: String, decisions: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var voiceState by remember { mutableStateOf(JarvisVoiceState.IDLE) }
    var isVoiceMuted by rememberSaveable { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val initialKey = geminiApiKey.trim()
    var activeApiKey by rememberSaveable { mutableStateOf(initialKey) }
    var showApiKeyInput by rememberSaveable { mutableStateOf(initialKey.isBlank()) }
    var tempApiKey by rememberSaveable { mutableStateOf(initialKey) }
    var isValidatingKey by remember { mutableStateOf(false) }
    var validationStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val messages = remember {
        mutableStateListOf(
            JarvisVoiceMessage(
                sender = "JARVIS",
                text = "Online and at your service, sir. What are we building or exploring today?",
            )
        )
    }

    // High-performance Text-to-Speech Engine with Multilingual Audio Synthesis
    var ttsEngine: TextToSpeech? by remember { mutableStateOf(null) }

    fun speak(text: String) {
        if (isVoiceMuted || ttsEngine == null) {
            voiceState = JarvisVoiceState.IDLE
            return
        }
        voiceState = JarvisVoiceState.SPEAKING
        val sanitized = sanitizeForSpeech(text)

        // Multi-language auto-detection: Nepali, Hindi, English
        val hasDevanagari = sanitized.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            val nepali = Locale("ne", "NP")
            val hindi = Locale("hi", "IN")
            val nepAvail = ttsEngine?.isLanguageAvailable(nepali) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (nepAvail >= TextToSpeech.LANG_AVAILABLE) {
                ttsEngine?.language = nepali
            } else {
                val hinAvail = ttsEngine?.isLanguageAvailable(hindi) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (hinAvail >= TextToSpeech.LANG_AVAILABLE) {
                    ttsEngine?.language = hindi
                }
            }
        } else {
            ttsEngine?.language = Locale.US
        }

        ttsEngine?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech")
    }

    fun stopSpeaking() {
        ttsEngine?.stop()
        if (voiceState == JarvisVoiceState.SPEAKING) {
            voiceState = JarvisVoiceState.IDLE
        }
    }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                runCatching {
                    val voices = instance?.voices?.toList() ?: emptyList()
                    val naturalVoice = voices.firstOrNull { voice ->
                        voice.locale.language == "en" &&
                            (voice.name.contains("natural", ignoreCase = true) ||
                             voice.name.contains("neural", ignoreCase = true) ||
                             voice.name.contains("wavenet", ignoreCase = true) ||
                             voice.quality >= android.speech.tts.Voice.QUALITY_HIGH) &&
                            !voice.isNetworkConnectionRequired
                    } ?: voices.firstOrNull { it.locale.language == "en" }

                    if (naturalVoice != null) instance?.voice = naturalVoice
                    instance?.setSpeechRate(1.0f) // Natural human rate
                    instance?.setPitch(1.0f) // Warm conversational pitch
                }
                ttsEngine = instance
                if (!isVoiceMuted) {
                    val welcome = "Online and at your service, sir. What are we building or exploring today?"
                    instance?.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, "jarvis_init")
                }
            }
        }
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun handleUserInput(rawText: String) {
        val input = rawText.trim()
        if (input.isBlank()) return

        stopSpeaking()
        messages.add(JarvisVoiceMessage(sender = "USER", text = input))
        voiceState = JarvisVoiceState.THINKING

        val intent = GeminiLiveVoiceClient.parseVoiceIntent(input)
        when (intent) {
            is VoiceIntent.SwitchOffline -> {
                val reply = "Switching active intelligence to local offline Ollama model, sir."
                messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                speak(reply)
            }
            is VoiceIntent.SwitchAgent -> {
                val reply = "Handing control over to ${intent.agentId} coding engine, sir."
                messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                speak(reply)
            }
            is VoiceIntent.RunBuild -> {
                val reply = "Initiating Android compilation and build sequence now, sir."
                messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                speak(reply)
            }
            is VoiceIntent.SwitchModel -> {
                val reply = "Switching to verified free open-source coding model on OpenRouter, sir."
                messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                speak(reply)
            }
            is VoiceIntent.Conversational -> {
                // Check if user wants to scaffold a new project
                val lower = input.lowercase()
                if (lower.startsWith("create project") || lower.startsWith("new project") || lower.startsWith("make project")) {
                    val nameCandidate = input.substringAfter("project").trim().removePrefix("called").removePrefix("named").trim().take(30).ifBlank { "mindsparq-app" }
                    val reply = "Scaffolding new project workspace for '$nameCandidate' immediately, sir."
                    messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                    speak(reply)
                    onCreateProject(nameCandidate, AndroidTemplate.COMPOSE, "# $nameCandidate PRD", "# $nameCandidate TRD", "# Architecture", "# Decisions")
                    return
                }

                scope.launch {
                    val history = messages.takeLast(10).map { it.sender to it.text }
                    val keyToUse = activeApiKey.ifBlank { tempApiKey }.trim()
                    val result = withContext(Dispatchers.IO) {
                        GeminiLiveVoiceClient.generateConversationalReply(
                            apiKey = keyToUse,
                            userPrompt = input,
                            history = history,
                            languageInstruction = "Nepali and English naturally",
                        )
                    }

                    result.onSuccess { reply ->
                        messages.add(JarvisVoiceMessage(sender = "JARVIS", text = reply))
                        speak(reply)
                    }.onFailure { err ->
                        val cleanErrMsg = err.localizedMessage?.replace("java.lang.Exception: ", "")?.replace("java.lang.IllegalStateException: ", "") ?: "Connection issue"
                        val fallback = "I heard you, sir, but encountered an issue: $cleanErrMsg"
                        messages.add(JarvisVoiceMessage(sender = "JARVIS", text = fallback))
                        speak(fallback)
                    }
                }
            }
        }
    }

    // Android Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                handleUserInput(spoken)
            } else {
                voiceState = JarvisVoiceState.IDLE
            }
        } else {
            voiceState = JarvisVoiceState.IDLE
        }
    }

    fun triggerListening() {
        stopSpeaking()
        voiceState = JarvisVoiceState.LISTENING
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ne-NP", "hi-IN"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Speech recognition unavailable on this device", Toast.LENGTH_SHORT).show()
            voiceState = JarvisVoiceState.IDLE
        }
    }

    // Infinite animations for the Arc Reactor Neural Core
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (voiceState) {
            JarvisVoiceState.LISTENING -> 1.35f
            JarvisVoiceState.SPEAKING -> 1.25f
            JarvisVoiceState.THINKING -> 1.15f
            JarvisVoiceState.IDLE -> 1.05f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (voiceState == JarvisVoiceState.LISTENING) 600 else 1200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (voiceState == JarvisVoiceState.THINKING) 1400 else 6000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin_angle",
    )

    val haloColor by animateColorAsState(
        targetValue = when (voiceState) {
            JarvisVoiceState.LISTENING -> Color(0xFFEF4444) // Energetic red/coral
            JarvisVoiceState.THINKING -> Color(0xFFF59E0B)  // Golden amber
            JarvisVoiceState.SPEAKING -> Color(0xFF06B6D4)  // Electric Cyan
            JarvisVoiceState.IDLE -> Color(0xFF3B82F6)      // Sleek Cobalt Blue
        },
        animationSpec = tween(400),
        label = "halo_color",
    )

    Dialog(
        onDismissRequest = {
            stopSpeaking()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D14), // Premium deep space cockpit black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(haloColor),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "JARVIS LIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showApiKeyInput = !showApiKeyInput },
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "Configure API Key",
                                tint = if (activeApiKey.isBlank()) PocketOrange else Color.Gray,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                isVoiceMuted = !isVoiceMuted
                                if (isVoiceMuted) stopSpeaking()
                            },
                        ) {
                            Icon(
                                if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle Mute",
                                tint = if (isVoiceMuted) Color.Gray else haloColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                stopSpeaking()
                                messages.clear()
                                messages.add(JarvisVoiceMessage(sender = "JARVIS", text = "Ready, sir. What's on your mind?"))
                            },
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                stopSpeaking()
                                onDismiss()
                            },
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AnimatedVisibility(showApiKeyInput) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.dp, if (activeApiKey.isBlank()) PocketOrange else Color(0xFF22304F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Google Gemini API Key for Live Voice",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeApiKey.isBlank()) PocketOrange else Color(0xFF93C5FD),
                                )
                                TextButton(
                                    onClick = {
                                        runCatching {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                            context.startActivity(browserIntent)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    Text("Get Free Key ↗", fontSize = 11.sp, color = PocketOrange)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Google Gemini keys start with 'AQ.' or 'AIzaSy'. OpenRouter keys start with 'sk-or-'.",
                                fontSize = 10.5.sp,
                                color = Color.Gray,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = tempApiKey,
                                    onValueChange = { tempApiKey = it },
                                    placeholder = { Text("Paste Gemini (AQ. / AIzaSy...) or OpenRouter (sk-or-...)", fontSize = 11.5.sp, color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PocketOrange,
                                        unfocusedBorderColor = Color(0xFF22304F),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                        if (!clipText.isNullOrBlank()) {
                                            tempApiKey = clipText
                                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Text("Paste", fontSize = 11.5.sp, color = Color.White)
                                }
                            }

                            // Key format live validation badge & actions
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isValidatingKey) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = PocketOrange,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Testing connection...",
                                            fontSize = 10.5.sp,
                                            color = PocketOrange,
                                        )
                                    }
                                } else {
                                    val (isSuccess, resultMsg) = validationStatus ?: (null to null)
                                    if (resultMsg != null) {
                                        Text(
                                            if (isSuccess == true) "✅ $resultMsg" else "❌ $resultMsg",
                                            fontSize = 10.5.sp,
                                            color = if (isSuccess == true) Color(0xFF4ADE80) else Color(0xFFF87171),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                        )
                                    } else {
                                        val trimmed = tempApiKey.trim()
                                        val statusText = when {
                                            trimmed.startsWith("AQ.") -> "🟢 Google Antigravity / Gemini Key (AQ...)"
                                            trimmed.startsWith("AIzaSy") -> "🟢 Google Gemini API Key (AIzaSy...)"
                                            trimmed.startsWith("sk-or-") -> "🟢 OpenRouter API Key (sk-or-...)"
                                            trimmed.isNotBlank() -> "🟢 Key Recognized (${trimmed.take(8)}...)"
                                            else -> "Enter your Google Gemini (AQ. / AIzaSy...) or OpenRouter key"
                                        }
                                        val statusColor = when {
                                            trimmed.isNotBlank() -> Color(0xFF4ADE80)
                                            else -> Color.Gray
                                        }
                                        Text(
                                            statusText,
                                            fontSize = 10.5.sp,
                                            color = statusColor,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (activeApiKey.isNotBlank() || tempApiKey.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                tempApiKey = ""
                                                activeApiKey = ""
                                                validationStatus = null
                                                onSaveGeminiApiKey("")
                                                Toast.makeText(context, "Key cleared", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                        ) {
                                            Text("Clear", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    // Validate Key Button
                                    Button(
                                        onClick = {
                                            val candidate = tempApiKey.trim()
                                            if (candidate.isBlank()) {
                                                Toast.makeText(context, "Please enter an API key to test", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isValidatingKey = true
                                            validationStatus = null
                                            scope.launch {
                                                val res = GeminiLiveVoiceClient.validateApiKey(candidate)
                                                isValidatingKey = false
                                                if (res.isSuccess) {
                                                    validationStatus = true to (res.getOrNull() ?: "Key valid & connected!")
                                                } else {
                                                    val err = res.exceptionOrNull()?.message ?: "Validation failed"
                                                    validationStatus = false to err
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        enabled = !isValidatingKey && tempApiKey.isNotBlank(),
                                    ) {
                                        Text("Validate", fontSize = 11.5.sp, color = Color(0xFF93C5FD))
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    Button(
                                        onClick = {
                                            val key = tempApiKey.trim()
                                            if (key.isNotBlank()) {
                                                activeApiKey = key
                                                onSaveGeminiApiKey(key)
                                                showApiKeyInput = false
                                                Toast.makeText(context, "API Key saved! Ready to talk.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // FatihMakes-inspired Arc Reactor Neural Core
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clickable {
                            if (voiceState == JarvisVoiceState.SPEAKING) {
                                stopSpeaking()
                            } else {
                                triggerListening()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // Outer Pulsing Glow Wave
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.radialGradient(listOf(haloColor.copy(alpha = 0.6f), Color.Transparent)),
                                ),
                                CircleShape,
                            ),
                    )

                    // Rotating Quantum Orbit Ring
                    Box(
                        modifier = Modifier
                            .size(125.dp)
                            .rotate(spinAngle)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(1.5.dp, haloColor.copy(alpha = 0.5f)),
                                CircleShape,
                            ),
                    )

                    // Inner Core
                    Surface(
                        modifier = Modifier.size(85.dp),
                        shape = CircleShape,
                        color = Color(0xFF131B2E),
                        border = BorderStroke(2.dp, haloColor),
                        shadowElevation = 8.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (voiceState == JarvisVoiceState.THINKING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = haloColor,
                                )
                            } else {
                                Icon(
                                    if (voiceState == JarvisVoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Core State",
                                    tint = haloColor,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (voiceState) {
                        JarvisVoiceState.LISTENING -> "Listening to you, sir..."
                        JarvisVoiceState.THINKING -> "Consulting neural intelligence..."
                        JarvisVoiceState.SPEAKING -> "Speaking (Tap core to interrupt)..."
                        JarvisVoiceState.IDLE -> "Tap core or mic to speak"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = haloColor,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )

                // Export & Copy Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LIVE CONVERSATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF64748B),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Copy All Messages Icon Button
                        IconButton(
                            onClick = {
                                val fullText = messages.joinToString("\n\n") { "${if (it.sender == "USER") "You" else "JARVIS"}: ${it.text}" }
                                ConversationExportHelper.copyToClipboard(context, fullText, "Conversation")
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy all conversation",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp),
                            )
                        }

                        // Export to Markdown (.md)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(6.dp))
                                .clickable {
                                    val pairs = messages.map { it.sender to it.text }
                                    Toast.makeText(context, "Exporting Markdown...", Toast.LENGTH_SHORT).show()
                                    ConversationExportHelper.exportToMarkdown(context, "Voice Session", pairs)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("MD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                        }

                        // Export to Doc (.doc)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(6.dp))
                                .clickable {
                                    val pairs = messages.map { it.sender to it.text }
                                    Toast.makeText(context, "Exporting Doc...", Toast.LENGTH_SHORT).show()
                                    ConversationExportHelper.exportToDoc(context, "Voice Session", pairs)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("DOC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
                        }

                        // Export to PDF (.pdf)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(6.dp))
                                .clickable {
                                    val pairs = messages.map { it.sender to it.text }
                                    Toast.makeText(context, "Generating PDF...", Toast.LENGTH_SHORT).show()
                                    ConversationExportHelper.exportToPdf(context, "Voice Session", pairs)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PocketOrange)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Live Transcription Feed
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isJarvis = msg.sender == "JARVIS"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isJarvis) Arrangement.Start else Arrangement.End,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isJarvis) 4.dp else 16.dp,
                                    bottomEnd = if (isJarvis) 16.dp else 4.dp,
                                ),
                                color = if (isJarvis) Color(0xFF161F33) else PocketOrange.copy(alpha = 0.20f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isJarvis) Color(0xFF22304F) else PocketOrange.copy(alpha = 0.45f),
                                ),
                                modifier = Modifier.fillMaxWidth(0.88f),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (isJarvis) "JARVIS" else "YOU",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isJarvis) Color(0xFF06B6D4) else PocketOrange,
                                            letterSpacing = 0.8.sp,
                                        )
                                        IconButton(
                                            onClick = { ConversationExportHelper.copyToClipboard(context, msg.text, "Message") },
                                            modifier = Modifier.size(20.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy message",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quick Action Suggestion Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val chips = listOf(
                        "Switch to Claude" to "switch to claude",
                        "Offline Model" to "switch to offline",
                        "Run Build" to "run build",
                    )
                    chips.forEach { (label, command) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF161F33),
                            border = BorderStroke(1.dp, Color(0xFF2A395C)),
                            modifier = Modifier.clickable { handleUserInput(command) },
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Bottom Input & Mic Dock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Speak or type to JARVIS...", fontSize = 13.sp, color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF22304F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(8.dp))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = textInput
                                textInput = ""
                                handleUserInput(text)
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF06B6D4),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { triggerListening() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(haloColor),
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Speak",
                                tint = Color.Black,
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
        .replace(Regex("```[\\s\\S]*?```"), " I have prepared the code. ")
        .replace(Regex("https?://\\S+"), " web link ")
        .replace(Regex("[*#`_~<>{}\\[\\]]"), "")
        .replace(Regex("(?m)^\\s*[-*•]\\s*"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s*"), "")
        .replace(Regex("([a-zA-Z0-9]):\\s"), "$1, ")
        .replace("API", "A P I")
        .replace("UI", "U I")
        .replace("SQLite", "S Q L lite")
        .replace("SDK", "S D K")
        .replace(Regex("[\\r\\n]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(500)
}
