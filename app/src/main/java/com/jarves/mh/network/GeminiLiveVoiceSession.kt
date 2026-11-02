package com.jarves.mh.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * Uses Google Gemini Multimodal Live API over Bidirectional WebSocket (BidiGenerateContent).
 *
 * Capabilities:
 * - 16kHz PCM streaming microphone input via [AudioRecord]
 * - 24kHz PCM ultra-low latency human audio synthesis via [AudioTrack]
 * - Hands-free continuous duplex conversation (no Google SpeechService dialogs)
 * - Zero-latency user interruption (barge-in): instantly cuts off AI speech when you speak
 * - Real-time audio waveform / RMS energy visualization
 */
class GeminiLiveVoiceSession(
    private val context: Context,
    private val apiKey: String,
    private val voiceName: String = "Puck", // Puck, Charon, Kore, Fenrir, Aoede
    private val onTranscript: (sender: String, text: String) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "GeminiLiveDuplex"
        private const val SAMPLE_RATE_IN = 16000
        private const val SAMPLE_RATE_OUT = 24000
        private const val LIVE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

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
        .pingInterval(20, TimeUnit.SECONDS)
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

        val requestUrl = if (cleanKey.startsWith("AQ.") || cleanKey.startsWith("ya29.")) {
            LIVE_WS_URL
        } else {
            "$LIVE_WS_URL?key=$cleanKey"
        }

        val request = Request.Builder()
            .url(requestUrl)
            .apply {
                if (cleanKey.startsWith("AQ.") || cleanKey.startsWith("ya29.")) {
                    addHeader("Authorization", "Bearer $cleanKey")
                }
            }
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened successfully.")
                _state.value = SessionState.CONNECTED
                sendSetupMessage(ws)
                startAudioRecording(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Gemini Live WebSocket failure: ${t.message}", t)
                _state.value = SessionState.ERROR
                onError(t.message ?: "Voice connection failed")
                stop()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini Live WebSocket closed ($code): $reason")
                _state.value = SessionState.DISCONNECTED
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                put("generation_config", JSONObject().apply {
                    put("response_modalities", JSONArray().put("AUDIO"))
                    put("speech_config", JSONObject().apply {
                        put("voice_config", JSONObject().apply {
                            put("prebuilt_voice_config", JSONObject().apply {
                                put("voice_name", voiceName)
                            })
                        })
                    })
                })
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put(
                            "text",
                            "You are MindSparQ Jarvis, a warm, charismatic, and emotionally intelligent senior engineering partner conversing live with a developer. You speak naturally with expressive human cadence, contractions, natural pacing, and warmth. You are fluent in English and Nepali (नेपाली). Keep spoken answers concise (1 to 3 sentences maximum) for rapid conversational turn-taking. Never use robotic mannerisms, bullet points, markdown code blocks, or backticks during live voice conversation."
                        )
                    }))
                })
            })
        }
        ws.send(setupJson.toString())
        Log.d(TAG, "Sent Live setup message with voice '$voiceName'")
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecording(ws: WebSocket) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(3200)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf,
        )

        audioRecord?.startRecording()
        _state.value = SessionState.LISTENING

        recordingJob = sessionScope.launch {
            val buffer = ByteArray(3200) // 100ms at 16kHz 16-bit mono
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    val energy = calculateRms(buffer, read)
                    _userAudioLevel.value = (energy / 3000f).coerceIn(0f, 1f)

                    // User barge-in detection: if user speaks while AI is playing audio, interrupt AI instantly!
                    if (isPlaying && energy > 1800f) {
                        Log.d(TAG, "User interrupted AI speech (energy: $energy). Flushing audio output!")
                        interruptAiSpeech(ws)
                    }

                    val base64Data = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                    val chunkJson = JSONObject().apply {
                        put("realtime_input", JSONObject().apply {
                            put("media_chunks", JSONArray().put(JSONObject().apply {
                                put("mime_type", "audio/pcm")
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
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
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
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    private fun handleServerMessage(jsonText: String) {
        runCatching {
            val root = JSONObject(jsonText)
            val serverContent = root.optJSONObject("serverContent") ?: return
            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")

            if (parts != null) {
                _state.value = SessionState.SPEAKING
                isPlaying = true
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inlineData = part.optJSONObject("inlineData")
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

            if (serverContent.optBoolean("turnComplete", false)) {
                isPlaying = false
                _state.value = SessionState.LISTENING
                _aiAudioLevel.value = 0f
            }

            val interrupted = serverContent.optBoolean("interrupted", false)
            if (interrupted) {
                isPlaying = false
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.play()
                _state.value = SessionState.LISTENING
            }
        }.onFailure {
            Log.e(TAG, "Error handling server audio message", it)
        }
    }

    private fun interruptAiSpeech(ws: WebSocket) {
        isPlaying = false
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.play()
        _aiAudioLevel.value = 0f
        _state.value = SessionState.LISTENING

        // Notify Gemini server of turn interruption
        val interruptMsg = JSONObject().apply {
            put("client_content", JSONObject().apply {
                put("turn_complete", true)
                put("interrupted", true)
            })
        }
        ws.send(interruptMsg.toString())
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
