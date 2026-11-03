package com.jarves.mh.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Full-Duplex Real-Time Human Conversational Voice Session for MindSparQ Jarvis.
 * Directly mirrors the FatihMakes JARVIS architecture (Mark-LVI):
 * - Uses Google Gemini Multimodal Live API over Bidirectional WebSocket (BidiGenerateContent).
 * - Default voice: "Charon" (the authentic, refined, warm British Tony Stark JARVIS voice).
 * - 16kHz PCM streaming microphone input via [AudioRecord].
 * - 24kHz PCM ultra-low latency human audio synthesis via [AudioTrack].
 * - Hands-free continuous duplex conversation (no Google SpeechService dialogs or robotic TTS).
 * - Zero-latency user interruption (barge-in): instantly cuts off AI speech when you speak.
 * - Real-time audio waveform / RMS energy visualization.
 */
class GeminiLiveVoiceSession(
    private val context: Context,
    private val apiKey: String,
    val voiceName: String = "Charon", // Charon (default JARVIS), Puck, Kore, Fenrir, Aoede
    val initialModel: String = "models/gemini-2.5-flash-native-audio-latest",
    candidateModels: List<String> = emptyList(),
    private val onTranscript: (sender: String, text: String) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "GeminiLiveDuplex"
        private const val SAMPLE_RATE_IN = 16000
        private const val SAMPLE_RATE_OUT = 24000
        private const val LIVE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

        val DEFAULT_LIVE_LADDER = listOf(
            "models/gemini-2.5-flash-native-audio-latest",
            "models/gemini-2.0-flash-live-001",
            "models/gemini-3.1-flash-live-preview",
            "models/gemini-2.5-flash-native-audio-preview-12-2025",
            "models/gemini-3.8-live",
            "models/gemini-2.0-flash",
            "models/gemini-2.5-flash",
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

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private val sessionScope = CoroutineScope(Dispatchers.IO + Job())

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var isPlaying = false

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
                Log.d(TAG, "Gemini Live WebSocket opened successfully.")
                _state.value = SessionState.CONNECTED
                sendSetupMessage(webSocket)
                startAudioRecording(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text, webSocket)
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
                            - ADDRESS: Always address the user respectfully as 'sir' (or its everyday respectful equivalent in Nepali).
                            - REGISTER: One steady register for success, failure, and alarming news alike. Level tone under bad news is your whole character. Humour is dry understatement carried by content itself—at most one clause at the end of a thought.
                            - CONVERSATIONAL CADENCE: Keep spoken replies brief, crisp, and natural (1 to 2 sentences) for rapid human-to-human duplex rhythm.
                            - LANGUAGE: The language of your reply is strictly the language of the user's MOST RECENT utterance. If the user speaks Nepali (नेपाली), converse in fluent, natural, colloquial Nepali. If English, speak refined British English. Never mix two languages in one reply.
                            - ABSOLUTE PROHIBITIONS: Never use bullet points, asterisks, brackets, numbered lists, emoji, or code blocks in voice speech. Never evaluate the user's prompt ("good question", "great idea"). Never apologize for your nature.
                            """.trimIndent()
                        )
                    }))
                })
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
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(3200)

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        } catch (_: Exception) {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize. State: ${record.state}")
            onError("Microphone audio initialization failed. Please check app audio permissions.")
            return
        }

        audioRecord = record
        record.startRecording()
        _state.value = SessionState.LISTENING

        recordingJob = sessionScope.launch {
            val buffer = ByteArray(1600) // 50ms at 16kHz 16-bit mono
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    val energy = calculateRms(buffer, read)
                    _userAudioLevel.value = (energy / 3000f).coerceIn(0f, 1f)

                    // User barge-in detection: if user speaks while AI is playing audio, interrupt AI instantly!
                    if (isPlaying && energy > 1600f) {
                        Log.d(TAG, "User interrupted AI speech (energy: $energy). Flushing audio output!")
                        interruptAiSpeech(ws)
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
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    private fun handleServerMessage(jsonText: String, ws: WebSocket) {
        runCatching {
            Log.d(TAG, "Incoming Gemini Live server message: $jsonText")
            val root = JSONObject(jsonText)

            // Setup Confirmation: trigger the authentic Charon opening greeting!
            if (root.has("setupComplete") || root.has("setup_complete")) {
                Log.d(TAG, "Gemini Live setup complete confirmed by server.")
                _state.value = SessionState.LISTENING
                triggerInitialGreeting(ws)
                return
            }

            val serverContent = root.optJSONObject("serverContent") ?: root.optJSONObject("server_content") ?: return
            val modelTurn = serverContent.optJSONObject("modelTurn") ?: serverContent.optJSONObject("model_turn")
            val parts = modelTurn?.optJSONArray("parts")

            if (parts != null) {
                _state.value = SessionState.SPEAKING
                isPlaying = true
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                    if (inlineData != null) {
                        val base64 = inlineData.optString("data")
                        if (base64.isNotBlank()) {
                            val audioBytes = Base64.decode(base64, Base64.NO_WRAP)
                            val energy = calculateRms(audioBytes, audioBytes.size)
                            _aiAudioLevel.value = (energy / 3500f).coerceIn(0f, 1f)
                            audioTrack?.write(audioBytes, 0, audioBytes.size)
                        }
                    }
                    val textPart = part.optString("text")
                    if (textPart.isNotBlank()) {
                        onTranscript("JARVIS", textPart)
                    }
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

    private fun interruptAiSpeech(ws: WebSocket) {
        isPlaying = false
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        }
        _aiAudioLevel.value = 0f
        _state.value = SessionState.LISTENING

        // Notify Gemini server of turn interruption
        val interruptMsg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turnComplete", true)
                put("interrupted", true)
            })
        }
        ws.send(interruptMsg.toString())
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
