package com.sensibotpro.domain.touch

import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile
import com.sensibotpro.domain.recommendation.*
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SwipeSample(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val distancePx: Float,
    val durationMs: Long,
    val velocityPxPerSec: Float,
    val angleDegrees: Float,
    val isCurvedJDrag: Boolean
)

data class TouchLabAnalysisResult(
    val swipeCount: Int,
    val averageVelocityPxPerSec: Float,
    val maxVelocityPxPerSec: Float,
    val dragPaceLabel: String,
    val detectedFrictionTier: String,
    val dragCurvatureType: String,
    val recommendedGeneral: Int,
    val recommendedRedDot: Int,
    val recommendedScope2x: Int,
    val recommendedScope4x: Int,
    val recommendedButtonSize: Int,
    val recommendedDpiBoost: Int,
    val diagnosisHeadline: String,
    val tacticalBreakdown: String,
    val generatedProfile: SensitivityProfile
)

object TouchAnalysisEngine {

    /**
     * Analyzes recorded touch swipes against display refresh rate and screen metrics
     * using the two-stage sensitivity formula:
     * Stage 1: Device-based baseline = BASE_CONSTANT[category] * (REFERENCE_PPI / device_PPI)
     * Stage 2: Personal multiplier derived from swipe velocity, consistency, and over-drag.
     */
    fun analyzeSwipes(
        samples: List<SwipeSample>,
        deviceSpecs: DeviceSpecs
    ): TouchLabAnalysisResult {
        if (samples.isEmpty()) {
            return fallbackResult(deviceSpecs)
        }

        val count = samples.size
        val avgVelocity = samples.map { it.velocityPxPerSec }.average().toFloat()
        val maxVelocity = samples.maxOf { it.velocityPxPerSec }
        val curvedDragCount = samples.count { it.isCurvedJDrag }
        val isDominantJDrag = curvedDragCount >= (count / 2)

        // ── STAGE 1: Baseline Sensitivity (Device-Based) ──────────────────────────
        val devicePpi = if (deviceSpecs.displayDensityDpi in 160..800) {
            deviceSpecs.displayDensityDpi.toFloat()
        } else {
            REFERENCE_PPI
        }
        val ppiRatio = (REFERENCE_PPI / devicePpi).coerceIn(0.70f, 1.45f)

        var baselineHipfire = BASE_CONSTANT_HIPFIRE * ppiRatio
        var baselineRedDot = BASE_CONSTANT_RED_DOT * ppiRatio
        var baselineScope2x = BASE_CONSTANT_SCOPE_2X * ppiRatio
        var baselineScope4x = BASE_CONSTANT_SCOPE_4X * ppiRatio
        val baselineSniper = BASE_CONSTANT_SCOPE_SNIPER * ppiRatio
        val baselineGyro = BASE_CONSTANT_GYROSCOPE * ppiRatio

        if (deviceSpecs.refreshRate >= 120) {
            baselineHipfire -= 3.0f
            baselineRedDot -= 3.0f
        } else if (deviceSpecs.refreshRate <= 60) {
            baselineHipfire += 3.0f
            baselineRedDot += 2.0f
        }

        // ── STAGE 2: Personal Multiplier (From Touch Lab Training Metrics) ────────
        // Compute consistency (velocity standard deviation vs mean)
        val variance = samples.map { (it.velocityPxPerSec - avgVelocity) * (it.velocityPxPerSec - avgVelocity) }.average()
        val stdDev = sqrt(variance).toFloat()
        val coefOfVariation = if (avgVelocity > 0f) (stdDev / avgVelocity).coerceIn(0f, 1f) else 0.5f

        // Consistency signal: low variance -> higher consistency (>0), erratic variance -> penalty (<0)
        val consistencySignal = ((0.35f - coefOfVariation) / 0.35f).coerceIn(-1.0f, 1.0f)

        // Swipe speed signal: compared to standard 1100 px/s reference flick speed
        val speedSignal = ((avgVelocity - 1100f) / 600f).coerceIn(-1.0f, 1.0f)

        // Overshoot / correction signal:
        // Extreme velocities (>1450 px/s) indicate over-flicking (aim flying above head) -> negative multiplier
        // Very low velocities (<850 px/s) indicate heavy friction resistance -> positive multiplier
        val overshootSignal = when {
            avgVelocity > 1450f -> -1.0f // Excessive velocity overshoots helmet -> pull sensitivity down
            avgVelocity > 1150f -> -0.4f
            avgVelocity < 850f -> +1.0f  // Drag resistance needs higher lift
            else -> 0.0f
        }

        val multiplierDelta = (overshootSignal * WEIGHT_OVERSHOOT_CORRECTION) +
                (speedSignal * WEIGHT_SWIPE_SPEED) +
                (consistencySignal * WEIGHT_CONSISTENCY)

        val personalMultiplier = (NEUTRAL_MULTIPLIER + multiplierDelta)
            .coerceIn(MIN_MULTIPLIER_BOUND, MAX_MULTIPLIER_BOUND)

        // ── FINAL SENSITIVITIES: baseline * personalMultiplier ─────────────────────
        val finalGeneral = (baselineHipfire * personalMultiplier).roundToInt().coerceIn(100, 200)
        val finalRedDot = (baselineRedDot * personalMultiplier).roundToInt().coerceIn(100, 200)
        val final2x = (baselineScope2x * personalMultiplier).roundToInt().coerceIn(80, 200)
        val final4x = (baselineScope4x * personalMultiplier).roundToInt().coerceIn(70, 200)
        val finalSniper = (baselineSniper * personalMultiplier).roundToInt().coerceIn(50, 180)
        val finalFreeLook = (baselineGyro * personalMultiplier).roundToInt().coerceIn(80, 200)

        // Determine Drag Pace & Screen Friction Tier
        val (paceLabel, frictionTier, dpiBoost, buttonSize) = when {
            avgVelocity > 1450f -> {
                Tuple4("Explosive Flick (Ultra Rapid)", "Ultra-Low Screen Friction (Smooth Glide)", 15, 42)
            }
            avgVelocity > 950f -> {
                Tuple4("Balanced Competitive (Snappy)", "Optimal Glass Friction", 35, 44)
            }
            else -> {
                Tuple4("Controlled Drag (Heavy Feel)", "High Screen Drag Resistance", 50, 48)
            }
        }

        val curvatureLabel = if (isDominantJDrag) "Curved J-Drag (One-Tap Technique)" else "Linear Straight Drag (SMG & Tracking)"

        val headline = when {
            avgVelocity > 1450f -> "⚡ HIGH FLICK VELOCITY (${avgVelocity.toInt()} PX/S)"
            avgVelocity > 950f -> "🎯 BALANCED TOURNAMENT PACE (${avgVelocity.toInt()} PX/S)"
            else -> "🛡️ HEAVY DRAG RESISTANCE (${avgVelocity.toInt()} PX/S)"
        }

        val breakdown = when {
            avgVelocity > 1450f -> {
                "Your upward finger whip travels at an explosive ${avgVelocity.toInt()} px/s with minimal screen friction. Personal multiplier calibrated to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x to prevent flying past helmet height. Sized fire button to ${buttonSize}% for sharp stop-travel."
            }
            avgVelocity > 950f -> {
                "Your swipe velocity averages ${avgVelocity.toInt()} px/s with clean vertical stability. Stage 1 baseline tuned to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x multiplier. General $finalGeneral and Red Dot $finalRedDot on modern 200 scale."
            }
            else -> {
                "Your finger drag velocity averages ${avgVelocity.toInt()} px/s due to screen drag friction. Multiplier boosted to ${String.format(java.util.Locale.US, "%.2f", personalMultiplier)}x (General $finalGeneral) with a +$dpiBoost Smallest Width (DPI) recommendation so drags reach head level effortlessly."
            }
        }

        val stockDpi = if (deviceSpecs.displayDensityDpi in 250..700) deviceSpecs.displayDensityDpi else 411
        val targetDpi = (stockDpi + dpiBoost).coerceIn(380, 520)

        val profile = SensitivityProfile(
            id = UUID.randomUUID().toString(),
            name = "Touch Calibrated ($paceLabel)",
            general = finalGeneral,
            redDot = finalRedDot,
            scope2x = final2x,
            scope4x = final4x,
            sniper = finalSniper,
            freeLook = finalFreeLook,
            fireButtonSize = buttonSize,
            recommendedDpi = targetDpi,
            defaultDpi = stockDpi,
            dpiAdvice = "Smallest Width calibrated for your ${avgVelocity.toInt()} px/s thumb drag velocity.",
            playstyle = if (avgVelocity > 1200f) "Aggressive" else "Balanced",
            weaponFocus = if (isDominantJDrag) "Shotgun & One-Tap" else "SMG & Tracking",
            dragStyle = curvatureLabel,
            hudNotes = "Fire button sized at $buttonSize% placed in lower right quadrant for optimal drag travel.",
            explanation = "Calibrated specifically to your finger swipe physics (${avgVelocity.toInt()} px/s) and ${deviceSpecs.refreshRate}Hz screen response.",
            coachingTip = "Test your new calibrated setup in Free Fire Training Ground for 5 minutes.",
            isCustom = true,
            isActive = true
        )

        return TouchLabAnalysisResult(
            swipeCount = count,
            averageVelocityPxPerSec = avgVelocity,
            maxVelocityPxPerSec = maxVelocity,
            dragPaceLabel = paceLabel,
            detectedFrictionTier = frictionTier,
            dragCurvatureType = curvatureLabel,
            recommendedGeneral = finalGeneral,
            recommendedRedDot = finalRedDot,
            recommendedScope2x = final2x,
            recommendedScope4x = final4x,
            recommendedButtonSize = buttonSize,
            recommendedDpiBoost = dpiBoost,
            diagnosisHeadline = headline,
            tacticalBreakdown = breakdown,
            generatedProfile = profile
        )
    }

    private fun fallbackResult(deviceSpecs: DeviceSpecs): TouchLabAnalysisResult {
        val sample = SwipeSample(0f, 500f, 0f, 100f, 400f, 350L, 1140f, -90f, false)
        return analyzeSwipes(listOf(sample), deviceSpecs)
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
