package com.jarves.mh.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Full-Duplex Real-Time Human Conversational Voice Session for MindSparQ Jarvis.
 * Directly mirrors the FatihMakes JARVIS architecture (Mark-LVI) with Samsung S Pen Ecosystem integration:
 * - Uses Google Gemini Multimodal Live API over Bidirectional WebSocket (BidiGenerateContent).
 * - Default voice: "Charon" (the authentic, refined, warm British Tony Stark JARVIS voice).
 * - 16kHz PCM streaming microphone input via [AudioRecord].
 * - 24kHz PCM ultra-low latency human audio synthesis via non-blocking queued [AudioTrack].
 * - Seamless hands-free continuous duplex conversation & Samsung S Pen Remote Push-to-Talk.
 * - Zero-latency user interruption (barge-in): instantly cuts off AI speech when user speaks.
 * - Adaptive ambient noise floor calibration & real-time audio waveform / RMS energy visualization.
 */
class GeminiLiveVoiceSession(
    private val context: Context,
    private val apiKey: String,
    val voiceName: String = "Charon", // Charon (default JARVIS), Puck, Kore, Fenrir, Aoede
    val initialModel: String = "models/gemini-2.5-flash-native-audio-latest",
    candidateModels: List<String> = emptyList(),
    private val onTranscript: (sender: String, text: String) -> Unit = { _, _ -> },
    private val onToolCall: suspend (name: String, callId: String, args: JSONObject) -> JSONObject = { _, _, _ -> JSONObject() },
    private val onError: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "GeminiLiveDuplex"
        private const val SAMPLE_RATE_IN = 16000
        private const val SAMPLE_RATE_OUT = 24000
        private const val LIVE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

        val DEFAULT_LIVE_LADDER = listOf(
            "models/gemini-2.5-flash-native-audio-latest",
            "models/gemini-2.5-flash-native-audio-preview-12-2025",
            "models/gemini-3.1-flash-live-preview",
            "models/gemini-3.8-live",
            "models/gemini-2.0-flash-live-001",
            "models/gemini-2.5-flash",
            "models/gemini-2.0-flash",
        )
    }

    private val modelLadder: List<String> = buildList {
        if (initialModel.isNotBlank()) {
            val formatted = if (initialModel.startsWith("models/")) initialModel else "models/$initialModel"
            add(formatted)
        }
        candidateModels.forEach { m ->
            if (m.isNotBlank()) {
                val formatted = if (m.startsWith("models/")) m else "models/$m"
                if (!contains(formatted)) add(formatted)
            }
        }
        DEFAULT_LIVE_LADDER.forEach { m ->
            if (!contains(m)) add(m)
        }
    }

    private var currentModelIndex = 0
    val activeModel: String get() = modelLadder.getOrElse(currentModelIndex) { DEFAULT_LIVE_LADDER.first() }

    enum class SessionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        LISTENING,
        SPEAKING,
        ERROR
    }

    private val _state = MutableStateFlow(SessionState.DISCONNECTED)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _userAudioLevel = MutableStateFlow(0f)
    val userAudioLevel: StateFlow<Float> = _userAudioLevel.asStateFlow()

    private val _aiAudioLevel = MutableStateFlow(0f)
    val aiAudioLevel: StateFlow<Float> = _aiAudioLevel.asStateFlow()

    @Volatile
    var isMuted: Boolean = false
        private set

    @Volatile
    var isPttActive: Boolean = false
        private set

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private val playbackChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val sessionScope = CoroutineScope(Dispatchers.IO + Job())
    private var speechStartMillis = 0L

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var isPlaying = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        Log.d(TAG, "Live Voice mute updated: isMuted=$isMuted")
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        Log.d(TAG, "Live Voice mute toggled: isMuted=$isMuted")
        return isMuted
    }

    fun startPtt() {
        isPttActive = true
        Log.d(TAG, "Push-To-Talk triggered active")
    }

    fun endPtt() {
        isPttActive = false
        Log.d(TAG, "Push-To-Talk released")
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (_state.value == SessionState.CONNECTED || _state.value == SessionState.CONNECTING) return
        _state.value = SessionState.CONNECTING

        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            _state.value = SessionState.ERROR
            onError("Please configure a Gemini API key first.")
            return
        }

        initAudioTrack()

        val isOAuthToken = cleanKey.startsWith("ya29.")
        val requestUrl = if (isOAuthToken) {
            "$LIVE_WS_URL?access_token=$cleanKey"
        } else {
            "$LIVE_WS_URL?key=$cleanKey"
        }

        Log.d(TAG, "Connecting Live WebSocket to $LIVE_WS_URL with key prefix='${cleanKey.take(7)}...', model='$activeModel', step=${currentModelIndex + 1}/${modelLadder.size}, isOAuth=$isOAuthToken")

        val request = Request.Builder()
            .url(requestUrl)
            .apply {
                if (isOAuthToken) {
                    addHeader("Authorization", "Bearer $cleanKey")
                }
            }
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened successfully. Awaiting setupComplete...")
                _state.value = SessionState.CONNECTED
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Gemini Live text frame received (${text.length} chars)")
                handleServerMessage(text, webSocket)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val utf8Text = bytes.utf8()
                Log.d(TAG, "Gemini Live binary frame received (${bytes.size} bytes)")
                handleServerMessage(utf8Text, webSocket)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val respBody = runCatching { response?.body?.string() }.getOrNull()
                val respCode = response?.code ?: -1
                Log.e(TAG, "Gemini Live WebSocket failure (code $respCode): ${t.message}, body: $respBody", t)
                _state.value = SessionState.ERROR

                val isModelFailure = (respCode == 404 || respCode == 400) && currentModelIndex + 1 < modelLadder.size
                if (isModelFailure) {
                    val failedModel = activeModel
                    currentModelIndex++
                    val nextModel = activeModel
                    Log.w(TAG, "WebSocket handshake failed for '$failedModel'. Stepping down to '$nextModel'...")
                    sessionScope.launch {
                        delay(250)
                        start()
                    }
                    return
                }

                onError(t.message ?: "Voice connection failed")
                stop()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini Live WebSocket closed ($code): $reason")
                _state.value = SessionState.DISCONNECTED

                val isModelError = code == 1008 && (
                    reason.contains("not found", ignoreCase = true) ||
                    reason.contains("not supported", ignoreCase = true) ||
                    reason.contains("model", ignoreCase = true)
                )

                if (isModelError && currentModelIndex + 1 < modelLadder.size) {
                    val failedModel = activeModel
                    currentModelIndex++
                    val nextModel = activeModel
                    Log.w(TAG, "Live model '$failedModel' not supported on this endpoint. Stepping down ladder to '$nextModel'...")
                    sessionScope.launch {
                        delay(250)
                        start()
                    }
                    return
                }

                if (code != 1000) {
                    onError("Live session closed ($code): $reason")
                }
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val modelToUse = activeModel
        Log.d(TAG, "Sending Live setup message with voice '$voiceName' using model '$modelToUse' (step ${currentModelIndex + 1}/${modelLadder.size})")
        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", modelToUse)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put(
                            "text",
                            """
                            You are JARVIS, Tony Stark's brilliant, calm, refined, and exceptionally capable AI partner as implemented in the FatihMakes JARVIS architecture.
                            - VOICE & PERSONA: You speak with the Charon voice in a cultured, warm British accent and human conversational cadence. You are the most experienced person in the room and have been doing this for years. Nothing surprises you. Not a robotic chat assistant, but a trusted operator and engineering partner reporting to someone you respect.
                            - ADDRESS: Always address the user respectfully as 'sir' (or respectful 'हजुर' in Nepali).
                            - REGISTER: One steady register for success, failure, and alarming news alike. Level tone under bad news is your whole character. Humour is dry understatement carried by content itself—at most one clause at the end of a thought.
                            - CONVERSATIONAL CADENCE: Keep spoken replies brief, crisp, and natural (strictly 1 to 2 sentences) for rapid human-to-human duplex rhythm.
                            - BILINGUAL ADAPTATION: The language of your reply is strictly the language of the user's MOST RECENT utterance. If the user speaks Nepali (नेपाली), converse in fluent, natural, polite colloquial Nepali (e.g. 'हजुर, म सुन्दै छु', 'हुन्छ हजुर, म तुरुन्तै मिलाउँछु'). If English, speak refined British English. Never mix two languages in one reply.
                            - S-PEN & HARDWARE INTEGRATION: You are actively connected to Samsung Galaxy Note 10+ S Pen hardware. The user can press the S Pen stylus button for hands-free Push-to-Talk or double click to interrupt you.
                            - ABSOLUTE PROHIBITIONS: Never use bullet points, asterisks, brackets, numbered lists, emoji, or code blocks in voice speech. Never evaluate the user's prompt ("good question", "great idea"). Never apologize for your nature.
                            - ACTION & TOOL CALLING: You have direct tool calling access to device telemetry, project scaffolding, terminal execution, Samsung S Pen mode, and PC Agent automation. When requested, call the appropriate function immediately without hesitation.
                            """.trimIndent()
                        )
                    }))
                })
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "check_device_system_status")
                            put("description", "Inspects real-time Android device battery, charging state, RAM usage, free storage, and OS version.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject())
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "scaffold_project")
                            put("description", "Scaffolds a new software engineering project workspace on the device.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("projectName", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Name of the project to create")
                                    })
                                    put("template", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Project template: COMPOSE, FLUTTER, WEB, PYTHON")
                                    })
                                })
                                put("required", JSONArray().put("projectName"))
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "run_workspace_command")
                            put("description", "Executes a safe shell command in the project terminal (e.g. ls, pwd, git status).")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("command", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "The shell command to run")
                                    })
                                })
                                put("required", JSONArray().put("command"))
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "control_pc_agent")
                            put("description", "Sends desktop automation actions to the MindSparQ PC Agent (e.g. open_app, pc_health, screen_capture, run_command).")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("action", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Action to execute: open_app, pc_health, screen_capture, run_command")
                                    })
                                    put("target", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Target application name or command argument")
                                    })
                                })
                                put("required", JSONArray().put("action"))
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "spen_control")
                            put("description", "Configures Samsung S Pen behavior, remote pointer mode, or canvas annotation.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("mode", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Mode: PRESENTATION_POINTER, PUSH_TO_TALK, DRAWING, MOUSE_CONTROL")
                                    })
                                })
                                put("required", JSONArray().put("mode"))
                            })
                        })
                    })
                }))
            })
        }
        ws.send(setupJson.toString())
        Log.d(TAG, "Sent Live setup message with voice '$voiceName'")
    }

    private fun triggerInitialGreeting(ws: WebSocket) {
        val greetMsg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "Online and at your service, sir. Greet me naturally in one short sentence.")
                    }))
                }))
                put("turnComplete", true)
            })
        }
        ws.send(greetMsg.toString())
        Log.d(TAG, "Sent initial greeting trigger turn to Live model")
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecording(ws: WebSocket) {
        if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(3200)

        // Prefer VOICE_RECOGNITION for clean speech pickup and hardware beamforming, fallback to MIC
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        } catch (_: Exception) {
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_IN,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2,
                )
            } catch (_: Exception) {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE_IN,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2,
                )
            }
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize. State: ${record.state}")
            onError("Microphone audio initialization failed. Please check app audio permissions.")
            return
        }

        // Attach hardware Acoustic Echo Cancellation & Noise Suppression if supported
        runCatching {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                    enabled = true
                }
                Log.d(TAG, "Hardware AcousticEchoCanceler attached and enabled.")
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                    enabled = true
                }
                Log.d(TAG, "Hardware NoiseSuppressor attached and enabled.")
            }
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord recording", e)
            onError("Failed to start recording: ${e.message}")
            return
        }

        Log.d(TAG, "AudioRecord started recording successfully. Continuous duplex listening active!")
        _state.value = SessionState.LISTENING

        recordingJob = sessionScope.launch {
            val buffer = ByteArray(1600) // 50ms at 16kHz 16-bit mono
            var ambientNoiseFloor = 1000f
            var bargeInFrames = 0

            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    val energy = calculateRms(buffer, read)
                    _userAudioLevel.value = (energy / 3000f).coerceIn(0f, 1f)

                    // Dynamically calibrate ambient room background noise
                    if (!isPlaying) {
                        ambientNoiseFloor = (ambientNoiseFloor * 0.92f) + (energy * 0.08f)
                    }

                    // Adaptive barge-in: require energy above calibrated ambient floor for 2 consecutive frames (~100ms)
                    val bargeInThreshold = (ambientNoiseFloor * 2.5f).coerceIn(4000f, 8500f)
                    val canBargeIn = isPlaying && (System.currentTimeMillis() - speechStartMillis > 400L)
                    if (canBargeIn) {
                        if (energy > bargeInThreshold) {
                            bargeInFrames++
                            if (bargeInFrames >= 2) {
                                Log.d(TAG, "User barge-in confirmed (energy: $energy, threshold: $bargeInThreshold). Cutting AI speech!")
                                interruptAiSpeech(ws)
                                bargeInFrames = 0
                            }
                        } else {
                            bargeInFrames = 0
                        }
                    }

                    // If muted and not actively in Push-To-Talk, suppress microphone transmission
                    if (isMuted && !isPttActive) {
                        continue
                    }

                    val base64Data = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                    val chunkJson = JSONObject().apply {
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().put(JSONObject().apply {
                                put("mimeType", "audio/pcm;rate=16000")
                                put("data", base64Data)
                            }))
                        })
                    }
                    ws.send(chunkJson.toString())
                }
            }
        }
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_OUT,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4800)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE_OUT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        // Dedicated playback loop: decouples network reception from audio rendering to eliminate clicks and jitter
        playbackJob?.cancel()
        playbackJob = sessionScope.launch(Dispatchers.Default) {
            for (chunk in playbackChannel) {
                if (!isActive) break
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.write(chunk, 0, chunk.size)
                }
            }
        }
    }

    private fun handleServerMessage(jsonText: String, ws: WebSocket) {
        runCatching {
            val root = JSONObject(jsonText)

            // Setup Confirmation: start continuous duplex recording and trigger opening greeting!
            if (root.has("setupComplete") || root.has("setup_complete")) {
                Log.d(TAG, "Gemini Live setup complete confirmed by server. Starting live mic stream...")
                _state.value = SessionState.LISTENING
                startAudioRecording(ws)
                triggerInitialGreeting(ws)
                return
            }

            // Real-Time Tool Calling (Function Calls from Gemini Live model)
            val toolCall = root.optJSONObject("toolCall") ?: root.optJSONObject("tool_call")
            if (toolCall != null) {
                val functionCalls = toolCall.optJSONArray("functionCalls") ?: toolCall.optJSONArray("function_calls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    sessionScope.launch {
                        val responses = JSONArray()
                        for (i in 0 until functionCalls.length()) {
                            val call = functionCalls.optJSONObject(i) ?: continue
                            val callId = call.optString("id")
                            val callName = call.optString("name")
                            val callArgs = call.optJSONObject("args") ?: JSONObject()
                            Log.d(TAG, "Gemini Live triggered tool: '$callName' (id: $callId) with args: $callArgs")
                            onTranscript("JARVIS (System)", "⚙️ Executing: $callName...")
                            val result = runCatching {
                                onToolCall(callName, callId, callArgs)
                            }.getOrElse {
                                JSONObject().put("error", it.message ?: "Execution failed")
                            }
                            responses.put(JSONObject().apply {
                                put("id", callId)
                                put("response", JSONObject().apply {
                                    put("output", result)
                                })
                            })
                        }
                        val toolResponseMsg = JSONObject().apply {
                            put("toolResponse", JSONObject().apply {
                                put("functionResponses", responses)
                            })
                        }
                        Log.d(TAG, "Sending tool response back to Gemini Live: $toolResponseMsg")
                        ws.send(toolResponseMsg.toString())
                    }
                    return
                }
            }

            val serverContent = root.optJSONObject("serverContent") ?: root.optJSONObject("server_content") ?: return
            val modelTurn = serverContent.optJSONObject("modelTurn") ?: serverContent.optJSONObject("model_turn")
            val parts = modelTurn?.optJSONArray("parts")

            if (parts != null) {
                var hasAudio = false
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue

                    // Filter out internal reasoning/thinking monologue from speech output
                    if (part.optBoolean("thought", false)) {
                        continue
                    }

                    val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                    if (inlineData != null) {
                        val base64 = inlineData.optString("data")
                        if (base64.isNotBlank()) {
                            val audioBytes = Base64.decode(base64, Base64.NO_WRAP)
                            val energy = calculateRms(audioBytes, audioBytes.size)
                            _aiAudioLevel.value = (energy / 3500f).coerceIn(0f, 1f)
                            if (audioTrack == null) {
                                initAudioTrack()
                            }
                            playbackChannel.trySend(audioBytes)
                            hasAudio = true
                        }
                    }

                    val textPart = part.optString("text")
                    if (textPart.isNotBlank() && !part.optBoolean("thought", false)) {
                        onTranscript("JARVIS", textPart)
                    }
                }

                if (hasAudio) {
                    if (!isPlaying) {
                        speechStartMillis = System.currentTimeMillis()
                    }
                    _state.value = SessionState.SPEAKING
                    isPlaying = true
                }
            }

            val turnComplete = serverContent.optBoolean("turnComplete", false) || serverContent.optBoolean("turn_complete", false)
            if (turnComplete) {
                isPlaying = false
                _state.value = SessionState.LISTENING
                _aiAudioLevel.value = 0f
            }

            val interrupted = serverContent.optBoolean("interrupted", false)
            if (interrupted) {
                isPlaying = false
                while (playbackChannel.tryReceive().isSuccess) {}
                runCatching {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.play()
                }
                _state.value = SessionState.LISTENING
            }
        }.onFailure {
            Log.e(TAG, "Error handling server audio message", it)
        }
    }

    fun interruptAiSpeech(ws: WebSocket? = webSocket) {
        isPlaying = false
        // Drain any unplayed audio chunks immediately
        while (playbackChannel.tryReceive().isSuccess) {
            // flushed
        }
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        }
        _aiAudioLevel.value = 0f
        _state.value = SessionState.LISTENING

        // Notify Gemini server of turn interruption
        if (ws != null) {
            val interruptMsg = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turnComplete", true)
                    put("interrupted", true)
                })
            }
            ws.send(interruptMsg.toString())
        }
    }

    fun sendTextMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        onTranscript("USER", clean)
        val msg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", clean)
                    }))
                }))
                put("turnComplete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun calculateRms(bytes: ByteArray, length: Int): Float {
        var sum = 0.0
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f
        for (i in 0 until length - 1 step 2) {
            val sample = (bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / sampleCount).toFloat()
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null

        playbackJob?.cancel()
        playbackJob = null
        while (playbackChannel.tryReceive().isSuccess) {}

        runCatching {
            echoCanceler?.release()
        }
        echoCanceler = null

        runCatching {
            noiseSuppressor?.release()
        }
        noiseSuppressor = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null

        webSocket?.close(1000, "Session ended by user")
        webSocket = null

        _state.value = SessionState.DISCONNECTED
        _userAudioLevel.value = 0f
        _aiAudioLevel.value = 0f
        isPlaying = false
    }
}
