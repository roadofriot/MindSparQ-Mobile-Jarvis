package com.jarves.mh.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed interface VoiceIntent {
    data class SwitchModel(val query: String) : VoiceIntent
    data class SwitchAgent(val agentId: String) : VoiceIntent
    data object SwitchOffline : VoiceIntent
    data object RunBuild : VoiceIntent
    data class Conversational(val text: String) : VoiceIntent
}

/**
 * High-performance, natural live conversational client for MindSparQ Jarvis.
 * Directly streams responses from Google Gemini 2.5 Flash / OpenRouter.
 */
object GeminiLiveVoiceClient {

    fun parseVoiceIntent(spokenText: String): VoiceIntent {
        val lower = spokenText.trim().lowercase()
        return when {
            lower.contains("switch to offline") || lower.contains("switch to ollama") || lower.contains("offline model") ->
                VoiceIntent.SwitchOffline
            lower.contains("switch to free") || lower.contains("free model") || lower.contains("free coding") ->
                VoiceIntent.SwitchModel("free")
            lower.contains("switch to claude") || lower.contains("use claude") ->
                VoiceIntent.SwitchAgent("claude-code")
            lower.contains("switch to deepseek") || lower.contains("use deepseek") ->
                VoiceIntent.SwitchAgent("deepseek-harness")
            lower.contains("switch to antigravity") || lower.contains("use antigravity") ->
                VoiceIntent.SwitchAgent("antigravity")
            lower.contains("switch to opencode") || lower.contains("use opencode") ->
                VoiceIntent.SwitchAgent("opencode")
            lower.contains("run build") || lower.contains("build app") || lower.contains("build android") ->
                VoiceIntent.RunBuild
            else -> VoiceIntent.Conversational(spokenText)
        }
    }

    private val GEMINI_MODELS = listOf(
        "gemini-flash-lite-latest",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3.8-flash",
    )
    private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

    suspend fun validateApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please paste or type an API key first."))
        }

        if (cleanKey.startsWith("sk-or-")) {
            return@withContext runCatching {
                val url = URL("https://openrouter.ai/api/v1/auth/key")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $cleanKey")
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                    val label = JSONObject(body).optJSONObject("data")?.optString("label") ?: "Active"
                    "OpenRouter API Key valid ($label)!"
                } else {
                    val errStream = conn.errorStream ?: conn.inputStream
                    val errText = BufferedReader(InputStreamReader(errStream, "UTF-8")).use { it.readText() }
                    val msg = runCatching { JSONObject(errText).optJSONObject("error")?.optString("message") }.getOrNull() ?: errText
                    throw Exception("OpenRouter auth failed (HTTP $code): $msg")
                }
            }
        } else {
            // Google Gemini API keys can start with 'AQ.' or 'AIzaSy'
            return@withContext runCatching {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    "Google Gemini API Key valid & connected (gemini-flash-lite)!"
                } else {
                    val errStream = conn.errorStream ?: conn.inputStream
                    val errText = BufferedReader(InputStreamReader(errStream, "UTF-8")).use { it.readText() }
                    val msg = runCatching { JSONObject(errText).optJSONObject("error")?.optString("message") }.getOrNull() ?: errText
                    throw Exception("Gemini auth failed (HTTP $code): $msg")
                }
            }
        }
    }

    suspend fun generateConversationalReply(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>, // sender ("USER" or "AI") to text
        languageInstruction: String = "Nepali and English",
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()

        // 1. If key is OpenRouter
        if (cleanKey.startsWith("sk-or-")) {
            val orResult = callOpenRouter(cleanKey, userPrompt, history)
            if (orResult.isSuccess) return@withContext orResult
            // If OpenRouter failed, try local Ollama before reporting failure
            val localOllama = callLocalOllama(userPrompt, history)
            if (localOllama.isSuccess) return@withContext localOllama
            return@withContext orResult
        }

        // 2. If key is Google Gemini / Antigravity (starts with AQ. or AIzaSy or other non-OpenRouter key)
        if (cleanKey.isNotBlank()) {
            val geminiResult = callGoogleGemini(cleanKey, userPrompt, history, languageInstruction)
            if (geminiResult.isSuccess) return@withContext geminiResult
        }

        // 3. Try local Ollama if running
        val localOllama = callLocalOllama(userPrompt, history)
        if (localOllama.isSuccess) return@withContext localOllama

        return@withContext Result.failure(
            IllegalStateException("Please tap [🔑] at top to configure your Google Gemini (AQ. / AIzaSy...) or OpenRouter (sk-or-...) API key.")
        )
    }

    private fun callGoogleGemini(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        languageInstruction: String,
    ): Result<String> {
        var lastError: Exception? = null
        for (model in GEMINI_MODELS) {
            val targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val res = executeGeminiPost(targetUrl, userPrompt, history, languageInstruction)
            if (res.isSuccess) return res
            lastError = res.exceptionOrNull() as? Exception ?: Exception(res.toString())
        }
        return Result.failure(lastError ?: Exception("Failed to connect to Google Gemini"))
    }

    private fun executeGeminiPost(
        targetUrl: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        languageInstruction: String,
    ): Result<String> {
        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 12_000
            readTimeout = 30_000
            doOutput = true
        }

        val systemPrompt = """
You are MindSparQ Jarvis, a warm, charismatic, and emotionally intelligent senior software architect conversing live with an engineer.
- Conversational Cadence: You speak naturally like a real human partner—warm, expressive, supportive, and direct. Use everyday conversational contractions ("I'm", "We'll", "Sure thing!", "Sounds awesome!").
- NO ROBOTIC MANNERISMS: Never speak in a stiff, monotone robotic style. Never read out bullet lists, numbered points, markdown asterisks, backticks, or code blocks during voice conversation.
- MULTILINGUAL MASTERY: You are fluent in English, Nepali (नेपाली), Hindi (हिन्दी), and any language the user requests. Always answer in the exact language, dialect, or script the user speaks or asks for (e.g., if user speaks in Nepali, reply naturally in Nepali; if Hindi, in Hindi; if English, in English; if mixed, match naturally).
- Keep spoken replies concise (2 to 4 sentences maximum) so live voice conversation feels rapid, responsive, and natural like a phone call.
- The user decides their target platform and tech stack (Flutter cross-platform, Native Android Kotlin/Compose, iOS/Swift, Web React/HTML, Go, Python, Rust, C++). Back their choices wholeheartedly.
- If the user explicitly asks to finalize or generate a project plan ('proceed', 'banaideu', 'plan generate gara'), summarize the architecture decisions concisely and offer to scaffold it.
""".trimIndent()

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))

            val contentsArray = JSONArray()
            val recentHistory = history.takeLast(8)
            for ((sender, text) in recentHistory) {
                val role = if (sender.equals("USER", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            })

            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 600)
            })
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody.toString()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

        if (code !in 200..299) {
            val errorMsg = runCatching {
                JSONObject(responseText).optJSONObject("error")?.optString("message")
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: responseText

            return Result.failure(Exception("Gemini error ($code): $errorMsg"))
        }

        val json = JSONObject(responseText)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        val sb = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val partObj = parts.optJSONObject(i)
                val text = partObj?.optString("text")
                if (!text.isNullOrBlank() && !text.equals("null", ignoreCase = true)) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(text.trim())
                }
            }
        }
        val reply = sb.toString().trim()

        return if (reply.isNotBlank()) {
            Result.success(reply)
        } else {
            Result.failure(Exception("No reply generated from Gemini."))
        }
    }

    private val FREE_MODELS_CASCADE = listOf(
        "nvidia/nemotron-3.5-lightning:free",
        "google/gemma-4-26b-a4b-it:free",
        "google/gemma-4-31b-it:free",
        "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
        "cohere/north-mini-code:free",
        "poolside/laguna-s-2.1:free",
    )

    private fun callOpenRouter(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
    ): Result<String> {
        var lastError: Exception? = null
        for (modelId in FREE_MODELS_CASCADE) {
            val res = executeOpenRouterPost(apiKey, modelId, userPrompt, history)
            if (res.isSuccess) return res
            lastError = res.exceptionOrNull() as? Exception ?: Exception(res.toString())
        }
        return Result.failure(lastError ?: Exception("OpenRouter free models unavailable."))
    }

    private fun executeOpenRouterPost(
        apiKey: String,
        modelId: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
    ): Result<String> {
        val url = URL(OPENROUTER_API_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("HTTP-Referer", "https://mindsparq.com")
            setRequestProperty("X-Title", "MindSparQ Jarvis")
            connectTimeout = 12_000
            readTimeout = 30_000
            doOutput = true
        }

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are MindSparQ Jarvis, a warm, charismatic, and emotionally intelligent senior software architect speaking live with an engineer. Speak like a real human—warm, engaging, and conversational. Fluent in English, Nepali (नेपाली), Hindi (हिन्दी), and any language user chooses. Always reply in user's language. Keep answers concise (2-4 sentences max) without asterisks, bullet points, or robotic phrasing.")
            })
            for ((sender, text) in history.takeLast(8)) {
                put(JSONObject().apply {
                    put("role", if (sender.equals("USER", ignoreCase = true)) "user" else "assistant")
                    put("content", text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArray)
            put("temperature", 0.6)
            put("max_tokens", 350)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody.toString()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

        if (code !in 200..299) {
            return Result.failure(Exception("OpenRouter error ($code) on $modelId: $responseText"))
        }

        val json = JSONObject(responseText)
        val choices = json.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")

        val rawContent = message?.opt("content")
        var reply = if (rawContent != null && rawContent != JSONObject.NULL) rawContent.toString().trim() else ""
        if (reply.isBlank() || reply.equals("null", ignoreCase = true)) {
            val rawText = message?.opt("text")
            if (rawText != null && rawText != JSONObject.NULL) reply = rawText.toString().trim()
        }
        if (reply.isBlank() || reply.equals("null", ignoreCase = true)) {
            val rawReasoning = message?.opt("reasoning")
            if (rawReasoning != null && rawReasoning != JSONObject.NULL) reply = rawReasoning.toString().trim()
        }

        return if (reply.isNotBlank() && !reply.equals("null", ignoreCase = true)) {
            Result.success(reply)
        } else {
            Result.failure(Exception("No content returned from OpenRouter on $modelId."))
        }
    }

    private fun callLocalOllama(
        userPrompt: String,
        history: List<Pair<String, String>>,
    ): Result<String> = runCatching {
        val url = URL("http://127.0.0.1:11434/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 3_000
            readTimeout = 15_000
            doOutput = true
        }
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are MindSparQ Jarvis, a warm, elite software architect and conversational partner. Keep replies short (1-3 sentences max) in English or Nepali.")
            })
            for ((sender, text) in history.takeLast(6)) {
                put(JSONObject().apply {
                    put("role", if (sender.equals("USER", ignoreCase = true)) "user" else "assistant")
                    put("content", text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }
        val jsonBody = JSONObject().apply {
            put("model", "qwen2.5-coder:1.5b")
            put("messages", messagesArray)
            put("max_tokens", 300)
        }
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody.toString()) }
        val code = conn.responseCode
        if (code in 200..299) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val choices = JSONObject(responseText).optJSONArray("choices")
            val reply = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            if (!reply.isNullOrBlank()) return@runCatching reply.trim()
        }
        throw IllegalStateException("Ollama offline")
    }
}

