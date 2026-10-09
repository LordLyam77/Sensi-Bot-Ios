package com.sensibotpro.domain.recommendation

import com.sensibotpro.domain.model.SensitivityProfile

enum class CalibrationFeedback(val title: String, val description: String) {
    OVERSHOOTING("Aim Goes Over Head", "Crosshair flies above the enemy's helmet"),
    BODY_SHOTS("Stuck on Body / Chest", "Crosshair drags to chest and fails to reach the head"),
    UNDERSHOOTING("Struggling to Lift Aim", "Swipe feels sluggish or thumb reaches top edge"),
    INCONSISTENT("Inconsistent Drag", "Landing some headshots, but spread is erratic"),
    PERFECT_HEADSHOTS("Headshots Connecting Well", "Landing consistent clean red numbers!")
}

data class CalibrationResult(
    val updatedProfile: SensitivityProfile,
    val adjustmentExplanation: String,
    val nextDrill: String,
    val isOptimal: Boolean
)

object CalibrationEngine {

    fun calibrate(profile: SensitivityProfile, feedback: CalibrationFeedback): CalibrationResult {
        return when (feedback) {
            CalibrationFeedback.OVERSHOOTING -> {
                val newGeneral = (profile.general - 4).coerceAtLeast(60)
                val newRedDot = (profile.redDot - 3).coerceAtLeast(55)
                CalibrationResult(
                    updatedProfile = profile.copy(general = newGeneral, redDot = newRedDot),
                    adjustmentExplanation = "Decreased General by 4 (to $newGeneral) and Red Dot by 3 (to $newRedDot).",
                    nextDrill = "Practice 10 shots with your primary weapon in the Training Ground. Shorten your upward thumb swipe distance slightly.",
                    isOptimal = false
                )
            }
            CalibrationFeedback.BODY_SHOTS -> {
                val newGeneral = (profile.general + 6).coerceAtMost(200)
                val newRedDot = (profile.redDot + 5).coerceAtMost(200)
                CalibrationResult(
                    updatedProfile = profile.copy(general = newGeneral, redDot = newRedDot),
                    adjustmentExplanation = "Increased General by 6 (to $newGeneral) and Red Dot by 5 (to $newRedDot).",
                    nextDrill = "Position crosshair at shoulder level before initiating your upward flick. Use a brisk J-shape drag rather than a slow pull.",
                    isOptimal = false
                )
            }
            CalibrationFeedback.UNDERSHOOTING -> {
                val newGeneral = (profile.general + 8).coerceAtMost(200)
                val newRedDot = (profile.redDot + 6).coerceAtMost(200)
                CalibrationResult(
                    updatedProfile = profile.copy(general = newGeneral, redDot = newRedDot),
                    adjustmentExplanation = "Increased General by 8 (to $newGeneral) and Red Dot by 6 (to $newRedDot).",
                    nextDrill = "Check your screen cleanliness. Try reducing your fire button size by 2-4% to grant more vertical swipe space.",
                    isOptimal = false
                )
            }
            CalibrationFeedback.INCONSISTENT -> {
                val newButtonSize = (profile.fireButtonSize - 2).coerceIn(42, 54)
                CalibrationResult(
                    updatedProfile = profile.copy(fireButtonSize = newButtonSize),
                    adjustmentExplanation = "Sensitivity values preserved. Adjusted Fire Button size to $newButtonSize% to stabilize your thumb landing point.",
                    nextDrill = "Focus on muscle memory: Maintain identical thumb flick speed across 15 consecutive dummy shots.",
                    isOptimal = false
                )
            }
            CalibrationFeedback.PERFECT_HEADSHOTS -> {
                CalibrationResult(
                    updatedProfile = profile,
                    adjustmentExplanation = "Sweet spot identified! Your current profile matches your drag rhythm and screen response.",
                    nextDrill = "Lock in this profile! Jump into a Clash Squad match to test under live combat pressure.",
                    isOptimal = true
                )
            }
        }
    }
}
