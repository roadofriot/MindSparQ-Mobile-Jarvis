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

/**
 * High-performance, natural live conversational client for MindSparQ Jarvis.
 * Directly streams responses from Google Gemini 2.5 Flash / OpenRouter.
 */
object GeminiLiveVoiceClient {

    private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

    suspend fun generateConversationalReply(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>, // sender ("USER" or "AI") to text
        languageInstruction: String = "Nepali and English",
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            if (cleanKey.startsWith("sk-or-")) {
                callOpenRouter(cleanKey, userPrompt, history)
            } else {
                callGoogleGemini(cleanKey, userPrompt, history, languageInstruction)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGoogleGemini(
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        languageInstruction: String,
    ): Result<String> {
        val url = URL("$GEMINI_API_URL?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 12_000
            readTimeout = 30_000
            doOutput = true
        }

        val systemPrompt = """
You are MindSparQ Jarvis, an elite autonomous software architect and warm, human AI engineering partner.
- You converse naturally, intelligently, and warmly in $languageInstruction.
- You respect the user's choices: user chooses the platform and languages (Flutter cross-platform, Native Android Kotlin/Compose, iOS/iPhone, Desktop Mac/Linux/Windows, Web HTML/JS/React, Go, Python, Rust, C++).
- Keep replies concise (2 to 4 sentences maximum) so spoken audio flows naturally like a real phone call.
- NEVER lecture, dump code blocks, or demand structured questionnaire forms during normal conversation.
- If the user discusses an idea, explore it warmly and ask a sharp, natural engineering question.
- If the user explicitly asks to generate or finalize the plan ('plan', 'proceed', 'banaideu', 'lock it in'), formulate the plan with PRD, TRD, Architecture, and Decisions Log.
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
            return Result.failure(Exception("Gemini API error ($code): $responseText"))
        }

        val json = JSONObject(responseText)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val reply = parts?.optJSONObject(0)?.optString("text")

        return if (!reply.isNullOrBlank()) {
            Result.success(reply.trim())
        } else {
            Result.failure(Exception("No reply generated from Gemini."))
        }
    }

    private fun callOpenRouter(
        apiKey: String,
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
                put("content", "You are MindSparQ Jarvis, a warm, elite software architect conversing live with a developer. Keep replies short (2-4 sentences max), human, and supportive in Nepali and English. The user chooses their target platform and language.")
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
            put("model", "google/gemini-2.5-flash")
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 600)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody.toString()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

        if (code !in 200..299) {
            return Result.failure(Exception("OpenRouter error ($code): $responseText"))
        }

        val json = JSONObject(responseText)
        val choices = json.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val reply = message?.optString("content")

        return if (!reply.isNullOrBlank()) {
            Result.success(reply.trim())
        } else {
            Result.failure(Exception("No content returned from OpenRouter."))
        }
    }
}

