package com.sensibotpro.chat

import android.content.Context
import android.content.SharedPreferences
import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class AiProvider(val displayName: String, val defaultModel: String) {
    GEMINI("Google Gemini (Free Tier)", "gemini-1.5-flash"),
    GROQ("Groq LLaMA 3.3 (Fast & Free)", "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter (Multi-Model)", "meta-llama/llama-3.2-3b-instruct:free")
}

data class AiConfig(
    val isOnlineEnabled: Boolean,
    val provider: AiProvider,
    val apiKey: String
)

class OnlineAiClient(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sensi_ai_config", Context.MODE_PRIVATE)

    fun getConfig(): AiConfig {
        val enabled = prefs.getBoolean("is_online_enabled", true)
        val providerStr = prefs.getString("ai_provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name
        val provider = try {
            AiProvider.valueOf(providerStr)
        } catch (_: Exception) {
            AiProvider.GEMINI
        }
        val apiKey = prefs.getString("api_key", "") ?: ""
        return AiConfig(enabled, provider, apiKey)
    }

    fun saveConfig(config: AiConfig) {
        prefs.edit()
            .putBoolean("is_online_enabled", config.isOnlineEnabled)
            .putString("ai_provider", config.provider.name)
            .putString("api_key", config.apiKey.trim())
            .apply()
    }

    suspend fun testConnection(provider: AiProvider, apiKey: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Confirm in 1 short sentence: Sensi Bot Pro online coaching connection verified."
                val response = when (provider) {
                    AiProvider.GEMINI -> callGemini(apiKey, prompt, null, null)
                    AiProvider.GROQ -> callGroq(apiKey, prompt, null, null)
                    AiProvider.OPENROUTER -> callOpenRouter(apiKey, prompt, null, null)
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun generateCoachingResponse(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): Result<String> {
        val config = getConfig()
        if (!config.isOnlineEnabled || config.apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Online AI not configured or API key empty"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = when (config.provider) {
                    AiProvider.GEMINI -> callGemini(config.apiKey, userMessage, activeProfile, deviceSpecs)
                    AiProvider.GROQ -> callGroq(config.apiKey, userMessage, activeProfile, deviceSpecs)
                    AiProvider.OPENROUTER -> callOpenRouter(config.apiKey, userMessage, activeProfile, deviceSpecs)
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildSystemPrompt(activeProfile: SensitivityProfile?, deviceSpecs: DeviceSpecs?): String {
        val deviceContext = if (deviceSpecs != null) {
            "Player Device: ${deviceSpecs.manufacturer} ${deviceSpecs.model} (${deviceSpecs.resolution}, ${deviceSpecs.totalRamGb}GB RAM, ${deviceSpecs.refreshRate}Hz refresh rate, ${deviceSpecs.displayDensityDpi} DPI)."
        } else ""

        val profileContext = if (activeProfile != null) {
            "Current Sensitivity: General=${activeProfile.general}, RedDot=${activeProfile.redDot}, 2x=${activeProfile.scope2x}, 4x=${activeProfile.scope4x}, Sniper=${activeProfile.sniper}, FreeLook=${activeProfile.freeLook}, FireButton=${activeProfile.fireButtonSize}%, DPI=${activeProfile.recommendedDpi}."
        } else ""

        return """
You are SENSI BOT Pro, an elite Free Fire esports aim and sensitivity coach.
You provide precise, authoritative, highly tactical advice for Free Fire players.

$deviceContext
$profileContext

COACHING GUIDELINES:
- CRITICAL: Free Fire updated its in-game sensitivity sliders to a 0–200 scale (maximum is 200, not 100). Always recommend sensitivities on the modern 0–200 scale (e.g. General 175–198, Red Dot 170–192, 2X 160–185, 4X 145–175).
- FAMOUS CREATOR SENSITIVITIES (0–200 SCALE):
  • White444 / White FF: General 195, Red Dot 188, 2X 180, 4X 175, Sniper 62, Button 44%, DPI 460. Signature: J-drag flick & fast weapon switch.
  • Raistar: General 198, Red Dot 190, 2X 185, 4X 178, Sniper 65, Button 42%, DPI 510. Signature: 360° rotation drag & sit-up gloo wall.
  • Ruok FF: General 192, Red Dot 185, 2X 178, 4X 168, Sniper 85, Button 45%, DPI 480.
  • Badge 99: General 194, Red Dot 186, 2X 175, 4X 170, Sniper 60, Button 48%, DPI 450.
  • Total Gaming (Ajjubhai): General 188, Red Dot 180, 2X 172, 4X 165, Sniper 55, Button 52%, DPI 420.
  • Nobru: General 196, Red Dot 188, 2X 182, 4X 174, Sniper 60, Button 43%, DPI 470.
  • B2K: General 190, Red Dot 182, 2X 178, 4X 172, Sniper 92, Button 46%, DPI 440.
  • Lyam FF: General 196, Red Dot 188, 2X 180, 4X 172, Sniper 60, Button 44%, DPI 450.
- When asked about weapons (M1887, Desert Eagle, MP40, Woodpecker, UMP, etc.), explain exact drag techniques (J-drag, rotation drag, snap-release timing) and recommend ideal fire button sizes (40% to 52%).
- When asked about DPI, give concrete numbers and recommended Smallest Width ranges (380–510). If a device or player requires 600+ DPI for intense flicking, explicitly remind them to turn it back down to default after their gaming session.
- Keep answers concise, clear, and structured with bullet points.
- Never mention other games. Focus 100% on Free Fire mechanics, HUD setup, and aim improvement.
""".trimIndent()
    }

    private fun callGemini(
        apiKey: String,
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): String {
        val systemPrompt = buildSystemPrompt(activeProfile, deviceSpecs)
        val combinedPrompt = "$systemPrompt\n\nPlayer Question:\n$userMessage"

        val endpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        )

        var lastException: Exception? = null

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 12000
                    readTimeout = 18000
                    doOutput = true
                }

                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val userObj = JSONObject().apply {
                            put("role", "user")
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", combinedPrompt))
                            }
                            put("parts", partsArray)
                        }
                        put(userObj)
                    }
                    put("contents", contentsArray)

                    val genConfig = JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 800)
                    }
                    put("generationConfig", genConfig)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(requestJson.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) return text.trim()
                        }
                    }
                } else {
                    val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    lastException = RuntimeException("Gemini API Error ($responseCode): $errorStr")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: RuntimeException("Failed to call Google Gemini API")
    }

    private fun callGroq(
        apiKey: String,
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): String {
        val systemPrompt = buildSystemPrompt(activeProfile, deviceSpecs)
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = 12000
            readTimeout = 18000
            doOutput = true
        }

        val requestJson = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            val messagesArray = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userMessage))
            }
            put("messages", messagesArray)
            put("max_tokens", 800)
            put("temperature", 0.7)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.getJSONObject(0).optJSONObject("message")
                val content = msg?.optString("content")
                if (!content.isNullOrBlank()) return content.trim()
            }
            throw RuntimeException("Groq returned empty response")
        } else {
            val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            throw RuntimeException("Groq API Error ($responseCode): $errorStr")
        }
    }

    private fun callOpenRouter(
        apiKey: String,
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): String {
        val systemPrompt = buildSystemPrompt(activeProfile, deviceSpecs)
        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("HTTP-Referer", "https://sensibotpro.app")
            setRequestProperty("X-Title", "SENSI BOT Pro")
            connectTimeout = 12000
            readTimeout = 18000
            doOutput = true
        }

        val requestJson = JSONObject().apply {
            put("model", "meta-llama/llama-3.2-3b-instruct:free")
            val messagesArray = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userMessage))
            }
            put("messages", messagesArray)
            put("max_tokens", 800)
            put("temperature", 0.7)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.getJSONObject(0).optJSONObject("message")
                val content = msg?.optString("content")
                if (!content.isNullOrBlank()) return content.trim()
            }
            throw RuntimeException("OpenRouter returned empty response")
        } else {
            val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            throw RuntimeException("OpenRouter API Error ($responseCode): $errorStr")
        }
    }
}
