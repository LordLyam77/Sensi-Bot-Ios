package com.sensibotpro.domain.recommendation

import com.sensibotpro.domain.model.*
import java.util.UUID
import kotlin.math.roundToInt

// =========================================================================================
// SENSITIVITY CALCULATION TUNING CONSTANTS
// All variables below are named, documented values at the top of the file.
// Retune any value below directly without altering the underlying algorithmic structure.
// =========================================================================================

// -----------------------------------------------------------------------------------------
// STAGE 1: DEVICE-BASED BASELINE CONSTANTS
// -----------------------------------------------------------------------------------------

/**
 * Reference screen density (in PPI) against which all mobile displays are normalized.
 * A standard 6.5" 1080p Android display operates at ~400 - 415 PPI.
 * - Higher device PPI (dense pixels) -> lower ratio (less sensitivity required per physical inch).
 * - Lower device PPI (large budget screen) -> higher ratio (compensates for lower pixel density).
 * Reasonable tuning range: 380.0f - 440.0f (Default: 400.0f)
 */
const val REFERENCE_PPI: Float = 400.0f

/**
 * Base sensitivity constants per aim category on the modern 0 - 200 Free Fire sensitivity scale.
 * Formula: baseline_sensitivity = BASE_CONSTANT[category] * (REFERENCE_PPI / device_PPI)
 *
 * Aim Categories:
 * - BASE_CONSTANT_HIPFIRE:       General 360 camera movement & un-scoped hipfire drag.
 *                                Reasonable range: 175.0f - 198.0f (Default: 190.0f)
 * - BASE_CONSTANT_RED_DOT:       Red Dot & Holographic close-quarters tracking.
 *                                Reasonable range: 165.0f - 192.0f (Default: 182.0f)
 * - BASE_CONSTANT_SCOPE_2X:      2X Scope mid-range automatic sprays and tap-firing.
 *                                Reasonable range: 155.0f - 180.0f (Default: 172.0f)
 * - BASE_CONSTANT_SCOPE_4X:      4X Scope DMR single-taps & AR long-range recoil control.
 *                                Reasonable range: 140.0f - 170.0f (Default: 158.0f)
 * - BASE_CONSTANT_SCOPE_SNIPER:  6X - 8X Scope (AWM, Kar98k, M82B precision headshot flicks).
 *                                Reasonable range: 70.0f - 110.0f (Default: 95.0f)
 * - BASE_CONSTANT_GYROSCOPE:     Free Look camera & Gyroscope tilt responsiveness.
 *                                Reasonable range: 125.0f - 165.0f (Default: 145.0f)
 */
const val BASE_CONSTANT_HIPFIRE: Float = 190.0f
const val BASE_CONSTANT_RED_DOT: Float = 182.0f
const val BASE_CONSTANT_SCOPE_2X: Float = 172.0f
const val BASE_CONSTANT_SCOPE_4X: Float = 158.0f
const val BASE_CONSTANT_SCOPE_SNIPER: Float = 95.0f
const val BASE_CONSTANT_GYROSCOPE: Float = 145.0f

// -----------------------------------------------------------------------------------------
// STAGE 2: PERSONAL MULTIPLIER BOUNDS
// -----------------------------------------------------------------------------------------

/**
 * Minimum and maximum multiplier bounds for training ground personal adjustment.
 * Clamps the multiplier to prevent any single bad session or extreme input from pushing
 * a wildly unreasonable recommendation.
 * Formula: clamped_multiplier = raw_multiplier.coerceIn(MIN_MULTIPLIER_BOUND, MAX_MULTIPLIER_BOUND)
 *
 * Reasonable tuning ranges:
 * - MIN_MULTIPLIER_BOUND: 0.75f - 0.85f (Default: 0.80f -> max -20% sensitivity reduction)
 * - MAX_MULTIPLIER_BOUND: 1.15f - 1.25f (Default: 1.20f -> max +20% sensitivity boost)
 */
const val MIN_MULTIPLIER_BOUND: Float = 0.80f
const val MAX_MULTIPLIER_BOUND: Float = 1.20f

/**
 * Neutral baseline multiplier before training ground adjustments (1.0 = 100% of device baseline).
 */
const val NEUTRAL_MULTIPLIER: Float = 1.00f

// -----------------------------------------------------------------------------------------
// STAGE 2: TRAINING GROUND SIGNAL WEIGHTS
// -----------------------------------------------------------------------------------------

/**
 * Overshoot & Correction Weight:
 * Controls sensitivity reduction for overshoot (flying above the helmet) or
 * sensitivity increase for undershoot (stuck on chest / slow lift).
 * - More overshoot / corrections -> multiplier below 1.0 (lowers sensitivity).
 * - Fewer corrections / under-drag -> multiplier above 1.0 (lifts reticle).
 * Reasonable tuning range: 0.05f - 0.14f (Default: 0.09f)
 */
const val WEIGHT_OVERSHOOT_CORRECTION: Float = 0.09f

/**
 * Swipe Speed & Playstyle Pace Weight:
 * Rushing / aggressive playstyles with higher flick velocity receive higher multiplier (> 1.0).
 * Deliberate, cautious, or long-range playstyles receive lower multiplier (< 1.0).
 * Reasonable tuning range: 0.03f - 0.08f (Default: 0.05f)
 */
const val WEIGHT_SWIPE_SPEED: Float = 0.05f

/**
 * Consistency Weight:
 * Rewards consistent drag patterns with positive stability factor.
 * Penalizes erratic drag variance by pulling multiplier toward conservative control.
 * Reasonable tuning range: 0.02f - 0.06f (Default: 0.04f)
 */
const val WEIGHT_CONSISTENCY: Float = 0.04f

/**
 * Time-To-Target & Weapon Urgency Weight:
 * Accounts for required time-to-target: Shotgun close-quarters snap (<150ms) vs Sniper distance (>400ms).
 * Reasonable tuning range: 0.02f - 0.05f (Default: 0.03f)
 */
const val WEIGHT_TIME_TO_TARGET: Float = 0.03f

// -----------------------------------------------------------------------------------------
// RECOMMENDATION ENGINE
// -----------------------------------------------------------------------------------------

object RecommendationEngine {

    /**
     * Generates a fully optimized sensitivity recommendation using the two-stage algorithm:
     * Stage 1: Baseline sensitivity based on physical display PPI per aim category.
     * Stage 2: Personal multiplier calculated from training ground metrics & feedback.
     *
     * final_sensitivity = baseline_sensitivity * personal_multiplier
     */
    fun generateRecommendation(
        deviceSpecs: DeviceSpecs,
        playstyle: Playstyle,
        fingerSetup: FingerSetup,
        weaponType: WeaponType,
        problem: AimProblem,
        currentGeneral: Int? = null,
        currentRedDot: Int? = null
    ): SensitivityRecommendation {

        // =========================================================================
        // STAGE 1: Baseline Sensitivity (Device-Based)
        // baseline_sensitivity = BASE_CONSTANT[category] * (reference_PPI / device_PPI)
        // =========================================================================

        val devicePpi = if (deviceSpecs.displayDensityDpi in 160..800) {
            deviceSpecs.displayDensityDpi.toFloat()
        } else {
            REFERENCE_PPI
        }

        // Ratio of reference PPI to actual hardware screen PPI
        val ppiRatio = (REFERENCE_PPI / devicePpi).coerceIn(0.70f, 1.45f)

        // Compute baseline separately per aim category:
        // hipfire, red dot, 2x, 4x, 6-8x scope (sniper), gyroscope (free look)
        var baselineHipfire = BASE_CONSTANT_HIPFIRE * ppiRatio
        var baselineRedDot = BASE_CONSTANT_RED_DOT * ppiRatio
        var baselineScope2x = BASE_CONSTANT_SCOPE_2X * ppiRatio
        var baselineScope4x = BASE_CONSTANT_SCOPE_4X * ppiRatio
        var baselineSniper = BASE_CONSTANT_SCOPE_SNIPER * ppiRatio
        var baselineGyro = BASE_CONSTANT_GYROSCOPE * ppiRatio

        // Hardware touch polling rate compensation (120Hz+ vs 60Hz display refresh)
        if (deviceSpecs.refreshRate >= 120) {
            // High refresh rate touch sampling is ultra-dense; slight reduction prevents pixel skipping
            baselineHipfire -= 3.0f
            baselineRedDot -= 3.0f
        } else if (deviceSpecs.refreshRate <= 60) {
            // 60Hz displays benefit from a slight boost to reduce swipe friction feel
            baselineHipfire += 3.0f
            baselineRedDot += 2.0f
        }

        // =========================================================================
        // STAGE 2: Personal Multiplier (From Training Ground Metrics)
        // Computes multiplier from existing training ground data:
        // - More overshoot / corrections -> multiplier below 1.0 (lower sensitivity)
        // - Faster, more consistent, fewer corrections -> multiplier above 1.0 (higher sensitivity)
        // - Clamped between MIN_MULTIPLIER_BOUND (0.8) and MAX_MULTIPLIER_BOUND (1.2)
        // =========================================================================

        var personalMultiplierDelta = 0.0f

        // Signal 1: Overshoot & Correction
        // (Crosshair flies above head / over-flicking vs stuck on chest / undershooting)
        val overshootSignal = when (problem) {
            AimProblem.OVERSHOOTING_HEAD -> -1.0f  // Severe overshoot -> lower sensitivity
            AimProblem.AIM_TOO_FAST -> -1.2f       // Extreme over-rotation -> lower sensitivity
            AimProblem.LONG_RANGE_PROBLEM -> -0.6f // Distance jitter -> lower sensitivity
            AimProblem.BODY_SHOTS -> +1.0f         // Undershoot / chest lock -> raise sensitivity
            AimProblem.AIM_TOO_SLOW -> +1.2f       // Heavy drag / sluggish swipe -> raise sensitivity
            AimProblem.CLOSE_RANGE_PROBLEM -> +0.6f// Rushing target tracking -> raise sensitivity
            else -> 0.0f
        }
        personalMultiplierDelta += (overshootSignal * WEIGHT_OVERSHOOT_CORRECTION)

        // Signal 2: Swipe Speed & Playstyle Pace
        // (Faster rush tempo vs careful sniper placement)
        val speedSignal = when (playstyle) {
            Playstyle.RUSH -> +1.2f       // Rapid flick speed
            Playstyle.AGGRESSIVE -> +0.8f // High mobility
            Playstyle.BALANCED -> 0.0f    // Neutral
            Playstyle.PRECISE -> -0.8f    // Steady tracking
            Playstyle.LONG_RANGE -> -1.0f // Measured long-distance aim
        }
        personalMultiplierDelta += (speedSignal * WEIGHT_SWIPE_SPEED)

        // Signal 3: Consistency
        // (Inconsistent drag penalized to tighten variance; one-tap rewarded for muscle memory)
        val consistencySignal = when (problem) {
            AimProblem.INCONSISTENT_DRAG -> -1.0f // Erratic drag variance -> stabilize lower
            AimProblem.ONE_TAP_PROBLEM -> +0.4f   // Dedicated flick rhythm
            else -> +0.5f                         // Stable drag baseline
        }
        personalMultiplierDelta += (consistencySignal * WEIGHT_CONSISTENCY)

        // Signal 4: Time-To-Target & Weapon Urgency
        // (Instant reflex close range vs deliberate scoped align)
        val timeToTargetSignal = when (weaponType) {
            WeaponType.SHOTGUN -> +0.8f // Rapid snap required
            WeaponType.SMG -> +0.4f     // Continuous spray tracking
            WeaponType.AR -> 0.0f       // Balanced mid-range burst
            WeaponType.SNIPER -> -0.8f  // Precision micro-alignment
            WeaponType.MIXED -> 0.0f
        }
        personalMultiplierDelta += (timeToTargetSignal * WEIGHT_TIME_TO_TARGET)

        // Signal 5: Training Ground Current Sensitivity Drift (if provided by player)
        if (currentGeneral != null && currentGeneral in 50..200) {
            val driftRatio = (currentGeneral - 180).toFloat() / 200.0f
            personalMultiplierDelta += (driftRatio * 0.03f)
        }

        // Bounded multiplier clamped strictly between 0.80 and 1.20
        val personalMultiplier = (NEUTRAL_MULTIPLIER + personalMultiplierDelta)
            .coerceIn(MIN_MULTIPLIER_BOUND, MAX_MULTIPLIER_BOUND)

        // =========================================================================
        // FINAL SENSITIVITY CALCULATION
        // final_sensitivity = baseline_sensitivity * personal_multiplier
        // =========================================================================

        val finalGeneral = (baselineHipfire * personalMultiplier).roundToInt().coerceIn(100, 200)
        val finalRedDot = (baselineRedDot * personalMultiplier).roundToInt().coerceIn(100, 200)
        val final2x = (baselineScope2x * personalMultiplier).roundToInt().coerceIn(80, 200)
        val final4x = (baselineScope4x * personalMultiplier).roundToInt().coerceIn(70, 200)
        val finalSniper = (baselineSniper * personalMultiplier).roundToInt().coerceIn(50, 180)
        val finalFreeLook = (baselineGyro * personalMultiplier).roundToInt().coerceIn(80, 200)

        // ── Drag Style & HUD Ergonomics ──────────────────────────────────────────
        val dragStyle = when (weaponType) {
            WeaponType.SHOTGUN -> "Short explosive V-Drag / Curved J-Drag from below the chest"
            WeaponType.SMG -> "Linear upward straight drag with steady finger pressure"
            WeaponType.AR -> "Two-stage drag: Initial burst drag followed by micro recoil pull"
            WeaponType.SNIPER -> "Fast scope switch flick: Scope-in, micro align, instant shot"
            WeaponType.MIXED -> "Adaptive drag: Curved drag for close range, straight drag for mid range"
        }

        val fireButtonSize = fingerSetup.fireButtonRecommendation

        // ── Tactical Explanations & Training Drills ─────────────────────────────
        val (problemAdjustment, drillRoutine) = when (problem) {
            AimProblem.OVERSHOOTING_HEAD -> {
                "Multipliers tuned to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x (-${((1.0f - personalMultiplier) * 100).roundToInt()}%) to counter upward drag overshooting." to
                        "Spend 5 minutes in Training Ground with M1887 or Desert Eagle. Practice stopping your upward drag exactly when the fire button reaches mid-screen."
            }
            AimProblem.BODY_SHOTS -> {
                "Multipliers boosted to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x (+${((personalMultiplier - 1.0f) * 100).roundToInt()}%) to overcome chest drag lock and lift smoothly onto head level." to
                        "Position crosshair slightly to the side of the enemy's shoulder before firing, then pull in a fast J-curve toward the head."
            }
            AimProblem.AIM_TOO_SLOW -> {
                "Multipliers boosted to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x to eliminate screen friction and swipe resistance." to
                        "Clean your screen and practice fast 180-degree swipe turns to build muscle memory for the increased camera speed."
            }
            AimProblem.AIM_TOO_FAST -> {
                "Multipliers dialed back to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x to stabilize flick control and prevent overshooting." to
                        "Perform tracking drills on moving training bots without shooting for 3 minutes to reset your finger pace."
            }
            AimProblem.INCONSISTENT_DRAG -> {
                "Multipliers stabilized around golden baseline (${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x). Sizing fire button to $fireButtonSize% stabilizes your thumb runway." to
                        "Set fire button size to $fireButtonSize% and lower it slightly on the HUD so your thumb has a uniform drag path."
            }
            AimProblem.SCOPE_UNSTABLE -> {
                "Scoped sensitivities dampened to eliminate jitter and horizontal reticle bounce." to
                        "Equip an M4A1 with 4X scope and fire 3-bullet bursts at the 25m target, focusing on gentle downward thumb resistance."
            }
            AimProblem.CLOSE_RANGE_PROBLEM -> {
                "Optimized close quarters turning radius (${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x) so you can snap onto enemies executing slide/jump movements." to
                        "Practice jump-shots with SMG around training dummies, timing your drag at the peak of your jump."
            }
            AimProblem.LONG_RANGE_PROBLEM -> {
                "Fine-tuned distance optics for micro-flicks without unintended cursor over-travel." to
                        "Practice single-tap headshots on the 40m target with Woodpecker or AC80."
            }
            AimProblem.ONE_TAP_PROBLEM -> {
                "Calibrated for single-shot recoil recovery and instant crosshair reset on the modern 200 scale." to
                        "Use Desert Eagle in Training Room: Stand still, wait for bot alignment, flick upward swiftly, immediately un-scope/switch."
            }
        }

        // ── DPI / Display Density Calculation ───────────────────────────────────
        val stockDpi = if (deviceSpecs.displayDensityDpi in 320..560) deviceSpecs.displayDensityDpi else 411
        val dpiDelta = when {
            deviceSpecs.performanceClass == DevicePerformanceClass.LOW_END -> 0
            playstyle == Playstyle.RUSH || playstyle == Playstyle.AGGRESSIVE -> 45
            playstyle == Playstyle.PRECISE || playstyle == Playstyle.LONG_RANGE -> 20
            else -> 30
        }
        val safeRecommendedDpi = (stockDpi + dpiDelta).coerceIn(380, 500)
        val dpiAdvice = if (dpiDelta == 0) {
            "Stock DPI ($stockDpi) is recommended for your device tier to avoid System UI instability or memory strain."
        } else {
            "Recommended Smallest Width (DPI): $safeRecommendedDpi (Default: $stockDpi). Safe +$dpiDelta boost for swipe agility without causing UI text clipping."
        }

        val profileName = "${playstyle.displayName} ${weaponType.displayName}"

        val profile = SensitivityProfile(
            id = UUID.randomUUID().toString(),
            name = profileName,
            general = finalGeneral,
            redDot = finalRedDot,
            scope2x = final2x,
            scope4x = final4x,
            sniper = finalSniper,
            freeLook = finalFreeLook,
            fireButtonSize = fireButtonSize,
            recommendedDpi = safeRecommendedDpi,
            defaultDpi = stockDpi,
            dpiAdvice = dpiAdvice,
            playstyle = playstyle.displayName,
            weaponFocus = weaponType.displayName,
            dragStyle = dragStyle,
            hudNotes = "Fire button at ${fireButtonSize}%, placed in lower third of screen. Keep quick weapon switch button accessible.",
            explanation = "Calibrated for ${deviceSpecs.manufacturer} ${deviceSpecs.model} (${deviceSpecs.refreshRate}Hz, ${deviceSpecs.displayDensityDpi} PPI) with Stage 1 baseline & Stage 2 personal multiplier (${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x).",
            coachingTip = "Test these settings for 10-15 minutes in the Training Ground before competing in Ranked matches.",
            isCustom = false,
            isActive = true
        )

        return SensitivityRecommendation(
            profile = profile,
            targetProblemAdjustment = problemAdjustment,
            drillRoutine = drillRoutine
        )
    }
}
