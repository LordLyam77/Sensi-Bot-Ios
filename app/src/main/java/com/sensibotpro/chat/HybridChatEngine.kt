package com.sensibotpro.chat

import android.content.Context
import android.content.SharedPreferences
import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile
import java.util.UUID

/**
 * AI Coaching Mode preference.
 */
enum class AiCoachingMode(val displayName: String, val badgeLabel: String) {
    SMART_COACH("Smart Coach (Hybrid / Local AI)", "SMART COACH"),
    RULE_BASED("Rule-Based Coach (Deterministic)", "SMART RULES")
}

/**
 * HybridChatEngine — Orchestrates the authoritative [RuleBasedChatEngine] and [LocalQwenChatEngine].
 *
 * ARCHITECTURAL FLOW:
 * 1. User sends a message.
 * 2. RuleBasedChatEngine extracts:
 *    - weapon, problem, range, playstyle, finger setup, device, current sensitivity.
 * 3. Authoritative recommendation engine computes structured recommendations.
 * 4. Passes structured recommendation + context to Qwen3-0.6B on-device model.
 * 5. Qwen turns it into natural, encouraging conversational coaching.
 * 6. If Qwen is not installed, device has insufficient RAM, or generation fails:
 *    Automatically falls back to [RuleBasedChatEngine] with zero interruption or crash!
 */
class HybridChatEngine(
    private val context: Context,
    val ruleBasedEngine: RuleBasedChatEngine = RuleBasedChatEngine()
) : ChatEngine {

    private val prefs: SharedPreferences = context.getSharedPreferences("sensi_ai_mode_prefs", Context.MODE_PRIVATE)

    val localModelManager = LocalModelManager(context)
    val localQwenEngine = LocalQwenChatEngine(context, localModelManager, ruleBasedEngine)
    val onlineClient = OnlineAiClient(context)

    companion object {
        const val KEY_AI_MODE = "selected_ai_coaching_mode"
    }

    fun getAiMode(): AiCoachingMode {
        val defaultMode = if (localModelManager.checkDeviceCapability().isSupported) {
            AiCoachingMode.SMART_COACH
        } else {
            AiCoachingMode.RULE_BASED
        }
        val saved = prefs.getString(KEY_AI_MODE, defaultMode.name) ?: defaultMode.name
        return try {
            AiCoachingMode.valueOf(saved)
        } catch (_: Exception) {
            defaultMode
        }
    }

    fun setAiMode(mode: AiCoachingMode) {
        prefs.edit().putString(KEY_AI_MODE, mode.name).apply()
    }

    override suspend fun processMessage(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): ChatMessage {
        val currentMode = getAiMode()

        // ── 1. RULE-BASED MODE SELECTED ──────────────────────────────────────
        if (currentMode == AiCoachingMode.RULE_BASED) {
            val response = ruleBasedEngine.processMessage(userMessage, activeProfile, deviceSpecs)
            return response.copy(
                thinkingText = response.thinkingText ?: "⚡ Smart Rules Coach — 100% Offline"
            )
        }

        // ── 2. SMART COACH (HYBRID / LOCAL QWEN) ─────────────────────────────
        // Try on-device Qwen first if model file exists and device is supported
        if (localModelManager.checkModelExists() && localModelManager.checkDeviceCapability().isSupported) {
            try {
                return localQwenEngine.processMessage(userMessage, activeProfile, deviceSpecs)
            } catch (e: Exception) {
                // If local inference fails, fall through safely to rule-based fallback
            }
        }

        // Optional Cloud AI fallback (only if user explicitly configured an API key)
        val cloudConfig = onlineClient.getConfig()
        if (cloudConfig.isOnlineEnabled && cloudConfig.apiKey.isNotBlank()) {
            val cloudResult = onlineClient.generateCoachingResponse(userMessage, activeProfile, deviceSpecs)
            if (cloudResult.isSuccess) {
                return ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = cloudResult.getOrThrow(),
                    isUser = false,
                    thinkingText = "⚡ Cloud AI (${cloudConfig.provider.displayName})",
                    confidence = 1.0f
                )
            }
        }

        // ── 3. SAFE SMART RULES FALLBACK ─────────────────────────────────────
        // If Qwen model is not installed or device is not capable, provide full coaching via Smart Rules
        val ruleResponse = ruleBasedEngine.processMessage(userMessage, activeProfile, deviceSpecs)

        val statusHint = when {
            !localModelManager.checkDeviceCapability().isSupported ->
                "⚡ Smart Rules Active — Local AI not recommended on this phone spec"
            !localModelManager.checkModelExists() ->
                "⚡ Smart Rules Active (Qwen3-0.6B model pending installation)"
            else ->
                "⚡ Local AI isn't available right now. Smart Rules are still active."
        }

        return ruleResponse.copy(
            thinkingText = ruleResponse.thinkingText ?: statusHint
        )
    }

    override fun getSuggestedChips(): List<QuickChip> {
        return ruleBasedEngine.getSuggestedChips()
    }

    override fun resetConversation() {
        localQwenEngine.resetConversation()
        ruleBasedEngine.resetConversation()
    }

    /**
     * Frees all on-device LLM memory when Sensi Bot screen is exited.
     */
    fun releaseResources() {
        localQwenEngine.unload()
    }
}
