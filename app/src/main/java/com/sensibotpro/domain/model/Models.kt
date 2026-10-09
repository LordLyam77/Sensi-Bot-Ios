package com.sensibotpro.domain.model

data class SensitivityProfile(
    val id: String,
    val name: String,
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val sniper: Int,
    val freeLook: Int,
    val fireButtonSize: Int = 48,
    val recommendedDpi: Int = 440,
    val defaultDpi: Int = 411,
    val dpiAdvice: String = "Smallest Width: Increase by +30 to +50 over stock default. If 600+ is needed for your device, remember to turn it back down after playing to prevent daily System UI strain.",
    val playstyle: String = "Balanced",
    val weaponFocus: String = "Mixed",
    val dragStyle: String = "Smooth J-Drag",
    val hudNotes: String = "Position fire button slightly lower for longer vertical drag travel.",
    val explanation: String = "Balanced sensitivity optimized for steady control and responsive flicks.",
    val coachingTip: String = "Pull upward in a controlled arc. Do not swipe off screen.",
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Playstyle(val displayName: String, val description: String) {
    AGGRESSIVE("Aggressive", "High-mobility, fast camera turns, aggressive rushing"),
    BALANCED("Balanced", "All-round combat, mid-range duels and controlled sprays"),
    PRECISE("Precise", "Careful crosshair placement, headshot tracking and tap firing"),
    RUSH("Rush", "Instant close-quarters snap aiming and jumping shots"),
    LONG_RANGE("Long Range", "Scoped accuracy, distance stability and sniping control")
}

enum class FingerSetup(val displayName: String, val fireButtonRecommendation: Int) {
    TWO_FINGER("2 Finger", 46),
    THREE_FINGER("3 Finger", 48),
    FOUR_FINGER("4 Finger", 52),
    CUSTOM("Custom", 48)
}

enum class WeaponType(val displayName: String, val sampleWeapons: String) {
    SHOTGUN("Shotgun", "M1887, M1014, MAG-7"),
    SMG("SMG", "MP40, UMP, MP5, Thompson"),
    AR("AR", "Woodpecker, AK47, SCAR, M4A1"),
    SNIPER("Sniper", "AWM, M82B, Kar98k"),
    MIXED("Mixed", "All Weapon Classes")
}

enum class AimProblem(val displayName: String, val description: String) {
    AIM_TOO_SLOW("Aim feels too slow", "Crosshair struggles to reach the head in time"),
    OVERSHOOTING_HEAD("Aim goes above the head", "Crosshair flies past the enemy over the helmet"),
    BODY_SHOTS("Getting too many body shots", "Crosshair locks onto chest and won't lift to head"),
    INCONSISTENT_DRAG("Drag feels inconsistent", "Some shots connect, but drag distance varies wildly"),
    AIM_TOO_FAST("Aim feels too fast", "Over-swiping and losing camera stability in close quarters"),
    SCOPE_UNSTABLE("Scope feels unstable", "2x or 4x scope shakes too violently when firing"),
    CLOSE_RANGE_PROBLEM("Close-range aim problem", "Cannot track moving targets when rushing"),
    LONG_RANGE_PROBLEM("Long-range aim problem", "Micro-adjustments over distance are jerky"),
    ONE_TAP_PROBLEM("One-tap problem", "Struggling to land first-bullet headshots with M1887 or Desert Eagle")
}

enum class DevicePerformanceClass(val label: String) {
    LOW_END("Standard Range"),
    MID_RANGE("Mid Range"),
    HIGH_PERFORMANCE("High Performance"),
    FLAGSHIP("Flagship Tier")
}

data class DeviceSpecs(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val refreshRate: Int,
    val displayDensityDpi: Int,
    val resolution: String,
    val performanceClass: DevicePerformanceClass
)

data class SensitivityRecommendation(
    val profile: SensitivityProfile,
    val targetProblemAdjustment: String,
    val drillRoutine: String,
    val fairPlayNotice: String = "Starting Point — Test in Training Ground and adjust."
)
