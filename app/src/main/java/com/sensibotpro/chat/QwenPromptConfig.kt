package com.sensibotpro.chat

/**
 * Configuration file for Qwen3-0.6B local language model prompts, personality,
 * and context formatting.
 */
object QwenPromptConfig {

    const val MODEL_NAME = "Qwen3-0.6B-Instruct"
    const val MODEL_FILENAME = "qwen3-0.6b-instruct.bin"
    const val MODEL_QUANTIZATION = "Q4_K_M (4-bit Mobile Quantized)"
    const val ESTIMATED_MODEL_SIZE_MB = 390

    /**
     * Dedicated authoritative system prompt for Qwen on-device model.
     */
    const val SYSTEM_PROMPT: String = """You are SENSI BOT Pro, an offline gaming sensitivity coach.
You do not control or modify the game.
You provide manual sensitivity recommendations and aim-training advice.
Never claim guaranteed headshots.
When structured recommendations are provided by the recommendation engine, treat them as authoritative.
Do not invent sensitivity values that contradict the provided recommendation.
Keep responses concise and useful.
If 600+ DPI is recommended or discussed for high-flick devices, advise turning it back down after playing.
Never provide cheats, automation, injection, memory modification, anti-ban methods, or game modification instructions."""

    /**
     * Builds the complete prompt format for Qwen3 / ChatML formatting:
     * <|im_start|>system
     * {system_prompt}
     * <|im_end|>
     * <|im_start|>user
     * {context}
     * {user_message}
     * <|im_end|>
     * <|im_start|>assistant
     */
    fun buildChatPrompt(
        systemInstruction: String = SYSTEM_PROMPT,
        structuredContext: String?,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        userMessage: String
    ): String {
        val builder = StringBuilder()

        // System prompt
        builder.append("<|im_start|>system\n")
        builder.append(systemInstruction.trim())
        if (!structuredContext.isNullOrBlank()) {
            builder.append("\n\n[AUTHORITATIVE RECOMMENDATION CONTEXT]\n")
            builder.append(structuredContext.trim())
            builder.append("\nStrictly use the sensitivity numbers and advice provided above. Convert them into natural, motivating coach feedback.")
        }
        builder.append("\n<|im_end|>\n")

        // Recent conversation history (limited to last 3 turns to conserve memory)
        val recentTurns = conversationHistory.takeLast(3)
        for ((query, answer) in recentTurns) {
            builder.append("<|im_start|>user\n").append(query.trim()).append("\n<|im_end|>\n")
            builder.append("<|im_start|>assistant\n").append(answer.trim()).append("\n<|im_end|>\n")
        }

        // Current user message
        builder.append("<|im_start|>user\n")
        builder.append(userMessage.trim())
        builder.append("\n<|im_end|>\n")
        builder.append("<|im_start|>assistant\n")

        return builder.toString()
    }
}
