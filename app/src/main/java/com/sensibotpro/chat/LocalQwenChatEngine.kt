package com.sensibotpro.chat

import android.content.Context
import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * LocalQwenChatEngine — On-device conversational AI powered by Qwen3-0.6B.
 *
 * ARCHITECTURAL PRINCIPLE:
 * Qwen is the LANGUAGE / CONVERSATION layer, NOT the source of truth for sensitivities.
 * It strictly receives authoritative recommendations from [RuleBasedChatEngine] and
 * renders them into fluid, natural, personalized esports coaching advice.
 *
 * SAFETY & FAIR PLAY:
 * Operates under [QwenPromptConfig.SYSTEM_PROMPT]. Strictly refuses cheats, aimbots,
 * macros, memory injection, or game modification.
 */
class LocalQwenChatEngine(
    private val context: Context,
    val localModelManager: LocalModelManager = LocalModelManager(context),
    private val ruleBasedEngine: RuleBasedChatEngine = RuleBasedChatEngine()
) : ChatEngine {

    private val runtime = LiteRtLmRuntime(context)
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    override suspend fun processMessage(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): ChatMessage = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()

        // 1. Strict Fair-Play Guardrail
        val lower = trimmed.lowercase()
        if (lower.contains("hack") || lower.contains("aimbot") || lower.contains("cheat") ||
            lower.contains("script") || lower.contains("anti ban") || lower.contains("antiban") ||
            lower.contains("auto headshot") || lower.contains("mod menu")
        ) {
            return@withContext ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "SENSI BOT Pro is an authentic aim-training and sensitivity coach. I do NOT provide aimbots, scripts, hacks, or game modifications.\n\nAll settings recommended here work within legitimate Free Fire controls to build your genuine muscle memory.",
                isUser = false,
                thinkingText = "🛡️ Fair-Play Safety Guardrail Activated",
                confidence = 1.0f
            )
        }

        // 2. Verify / Lazy-load the Local Model
        if (!localModelManager.isLoaded()) {
            val loadResult = localModelManager.loadModel()
            if (loadResult.isFailure) {
                // If model is missing or device is incapable, throw to trigger fallback
                throw loadResult.exceptionOrNull() ?: IllegalStateException("Failed to load local Qwen model")
            }
        }

        // 3. Ensure runtime engine is initialized with model artifact
        if (!runtime.isReady()) {
            val initResult = runtime.initialize(localModelManager.getModelFile())
            if (initResult.isFailure) {
                throw initResult.exceptionOrNull() ?: IllegalStateException("Failed to initialize LiteRT-LM runtime")
            }
        }

        // 4. Extract authoritative structured recommendation from Rule-Based engine
        val structuredRec = ruleBasedEngine.extractStructuredRecommendation(trimmed, activeProfile, deviceSpecs)
        val devSummary = "${deviceSpecs?.manufacturer ?: "Android"} ${deviceSpecs?.model ?: "Device"} (${deviceSpecs?.refreshRate ?: 60}Hz, ${deviceSpecs?.totalRamGb ?: 4}GB RAM)"
        val structuredContext = structuredRec.toContextString(devSummary)

        // 5. Build structured ChatML prompt with system instruction & context
        val prompt = QwenPromptConfig.buildChatPrompt(
            systemInstruction = QwenPromptConfig.SYSTEM_PROMPT,
            structuredContext = structuredContext,
            conversationHistory = conversationHistory,
            userMessage = trimmed
        )

        // 6. Execute inference through LiteRT-LM runtime
        val inferenceResult = runtime.generateResponse(prompt)
        if (inferenceResult.isFailure) {
            throw inferenceResult.exceptionOrNull() ?: IllegalStateException("Inference failed")
        }

        val generatedText = inferenceResult.getOrThrow()

        // 7. Store in lightweight conversation memory (max 3 turns)
        if (conversationHistory.size >= 3) {
            conversationHistory.removeAt(0)
        }
        conversationHistory.add(trimmed to generatedText)

        return@withContext ChatMessage(
            id = UUID.randomUUID().toString(),
            text = generatedText,
            isUser = false,
            thinkingText = "⚡ Qwen3-0.6B Local AI (On-Device Inference • 100% Offline)",
            confidence = 0.98f,
            actionSuggestion = if (structuredRec.generalDelta != 0) "Apply Recommended Settings" else null,
            sensitivityAdjustment = if (structuredRec.generalDelta != 0 || structuredRec.redDotDelta != 0) {
                SensitivityDelta(
                    generalChange = structuredRec.generalDelta,
                    redDotChange = structuredRec.redDotDelta,
                    recommendedDpiChange = structuredRec.recommendedDpi
                )
            } else null
        )
    }

    override fun getSuggestedChips(): List<QuickChip> {
        return ruleBasedEngine.getSuggestedChips()
    }

    override fun resetConversation() {
        conversationHistory.clear()
        ruleBasedEngine.resetConversation()
    }

    /**
     * Frees memory when chat is closed.
     */
    fun unload() {
        runtime.close()
        localModelManager.releaseResources()
        conversationHistory.clear()
    }
}
