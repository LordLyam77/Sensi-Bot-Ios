package com.sensibotpro.chat

import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile

interface ChatEngine {
    /**
     * Process a message asynchronously — suspend function makes this feel exactly like
     * a real API call (network or LLM) while running entirely on-device.
     */
    suspend fun processMessage(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): ChatMessage

    fun getSuggestedChips(): List<QuickChip>

    fun resetConversation()
}
