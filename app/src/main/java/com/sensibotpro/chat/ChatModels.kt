package com.sensibotpro.chat

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val thinkingText: String? = null,
    val isStreaming: Boolean = false,
    val actionSuggestion: String? = null,
    val sensitivityAdjustment: SensitivityDelta? = null,
    val confidence: Float = 1.0f  // Intent confidence score (0.0 – 1.0)
)

data class SensitivityDelta(
    val generalChange: Int? = null,
    val redDotChange: Int? = null,
    val scope2xChange: Int? = null,
    val fireButtonChange: Int? = null,
    val recommendedDpiChange: Int? = null,
    val dpiStockValue: Int? = null,
    val dpiBoostAmount: Int? = null
)

data class QuickChip(
    val label: String,
    val prompt: String
)

// Conversation memory — tracks the last N turns for multi-turn context
data class ConversationTurn(
    val userMessage: String,
    val botIntent: String,
    val botResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatConversationState(
    var currentWeapon: String? = null,
    var currentProblem: String? = null,
    var currentRange: String? = null,
    var lastRecommendedGeneral: Int? = null,
    var lastRecommendedDpi: Int? = null,
    var awaitFollowUp: Boolean = false,
    var followUpType: String? = null,
    val recentTurns: ArrayDeque<ConversationTurn> = ArrayDeque(3),  // 3-turn memory window
    var sessionMessageCount: Int = 0
) {
    fun pushTurn(turn: ConversationTurn) {
        if (recentTurns.size >= 3) recentTurns.removeFirst()
        recentTurns.addLast(turn)
        sessionMessageCount++
    }

    fun lastBotIntent(): String? = recentTurns.lastOrNull()?.botIntent

    fun lastUserMessage(): String? = recentTurns.lastOrNull()?.userMessage
}
