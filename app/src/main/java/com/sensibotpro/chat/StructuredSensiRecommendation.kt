package com.sensibotpro.chat

/**
 * Authoritative structured recommendation output produced by the Rule-Based
 * recommendation engine. Qwen / Local AI consumes this structured context to
 * formulate natural conversational coaching, but is never allowed to invent or
 * alter these sensitivity numbers.
 */
data class StructuredSensiRecommendation(
    val weapon: String? = null,
    val range: String? = null,
    val aimProblem: String? = null,
    val recommendedGeneral: Int,
    val recommendedRedDot: Int,
    val recommendedScope2x: Int,
    val recommendedScope4x: Int,
    val recommendedSniper: Int,
    val recommendedFireButtonSize: Int,
    val recommendedDpi: Int,
    val generalDelta: Int = 0,
    val redDotDelta: Int = 0,
    val dragTechnique: String,
    val trainingDrill: String,
    val tacticalWhy: String,
    val creatorReference: String? = null
) {
    /**
     * Formats this structured recommendation as clean context for the on-device LLM (Qwen).
     */
    fun toContextString(deviceSummary: String): String {
        val weaponStr = weapon ?: "All Weapons (Balanced)"
        val problemStr = aimProblem ?: "General Aim Calibration"
        val creatorStr = if (creatorReference != null) "\nCreator Reference: $creatorReference" else ""
        return """
Device Context: $deviceSummary
Weapon: $weaponStr
Aim Focus / Issue: $problemStr
Authoritative Calibrated Values (Modern 0-200 Scale):
- General Sensitivity: $recommendedGeneral ${if (generalDelta != 0) "(${if (generalDelta > 0) "+$generalDelta" else "$generalDelta"})" else ""}
- Red Dot Sensitivity: $recommendedRedDot ${if (redDotDelta != 0) "(${if (redDotDelta > 0) "+$redDotDelta" else "$redDotDelta"})" else ""}
- 2X Scope: $recommendedScope2x
- 4X Scope: $recommendedScope4x
- Sniper Scope: $recommendedSniper
- Fire Button Size: $recommendedFireButtonSize%
- Safe Phone DPI (Smallest Width): $recommendedDpi
Drag Technique: $dragTechnique
Recommended Drill: $trainingDrill
Tactical Explanation: $tacticalWhy$creatorStr
""".trimIndent()
    }
}
