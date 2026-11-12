package com.sensibotpro.headlock

import kotlin.math.roundToInt

enum class CalibrationStep(val index: Int, val title: String) {
    DEVICE_CLASS(0, "Device Class"),
    FIRE_ANCHOR(1, "Fire-Button Anchor"),
    VERTICAL_BIAS_SWEEP(2, "Vertical Bias Sweep"),
    COMMIT(3, "Review & Commit")
}

data class BiasSweepState(
    val currentBias: Double = 0.6,
    val repsCompleted: Int = 0,
    val hitsRecorded: Int = 0,
    val bestBias: Double = 1.0,
    val bestHitScore: Int = -1
) {
    val stepIndex: Int
        get() = ((currentBias - 0.6) * 10.0).roundToInt().coerceIn(0, 10)

    val totalSteps: Int get() = 11
    val targetRepsPerStep: Int get() = 20
    val targetSweetSpotTenScale: Int get() = 6 // 6/10 == 12/20
}

/**
 * Deterministic recommendation math & shared calibration wizard state.
 * Zero network calls, zero side effects.
 */
object HeadlockCalibration {

    fun recommend(metrics: HeadlockDeviceMetrics): HeadlockConfig {
        val height = metrics.screenHeightPx.coerceIn(1000, 4000)
        val hz = metrics.refreshRateHz.coerceIn(60, 240)

        val baseDistance = when (metrics.deviceClass) {
            HeadlockDeviceClass.COMPACT -> 34
            HeadlockDeviceClass.PHONE -> 42
            HeadlockDeviceClass.LARGE -> 50
            HeadlockDeviceClass.TABLET -> 64
        }

        val heightFactor = (height / 2400.0).coerceIn(0.8, 1.35)
        val distancePx = (baseDistance * heightFactor).roundToInt().coerceIn(10, 400)

        val durationMs = when {
            hz >= 120 -> 78
            hz >= 90 -> 86
            else -> 96
        }

        val repeatIntervalMs = when {
            hz >= 120 -> 48
            hz >= 90 -> 54
            else -> 62
        }

        val verticalBias = when (metrics.deviceClass) {
            HeadlockDeviceClass.COMPACT -> 0.9
            HeadlockDeviceClass.PHONE -> 1.0
            HeadlockDeviceClass.LARGE -> 1.1
            HeadlockDeviceClass.TABLET -> 1.2
        }

        val (startX, startY) = when (metrics.deviceClass) {
            HeadlockDeviceClass.TABLET -> 0.85 to 0.80
            else -> 0.82 to 0.78
        }

        return HeadlockConfig(
            v = HeadlockConfig.CURRENT_VERSION,
            mode = HeadlockMode.HOLD,
            startX = startX,
            startY = startY,
            distancePx = distancePx,
            durationMs = durationMs,
            repeatIntervalMs = repeatIntervalMs,
            burstCount = 3,
            curve = if (hz >= 120) HeadlockCurve.EASE_OUT else HeadlockCurve.LINEAR,
            verticalBias = verticalBias,
            deviceClass = metrics.deviceClass
        ).clamped()
    }

    fun recordSweepRep(state: BiasSweepState, hit: Boolean): BiasSweepState {
        if (state.repsCompleted >= state.targetRepsPerStep) return state
        val nextReps = state.repsCompleted + 1
        val nextHits = state.hitsRecorded + if (hit) 1 else 0
        // Track bias closest to 6/10 (12/20) sweet spot or highest score
        val score = nextHits
        val updatedBestBias = if (score > state.bestHitScore) state.currentBias else state.bestBias
        val updatedBestScore = if (score > state.bestHitScore) score else state.bestHitScore
        return state.copy(
            repsCompleted = nextReps,
            hitsRecorded = nextHits,
            bestBias = updatedBestBias,
            bestHitScore = updatedBestScore
        )
    }

    fun advanceSweepBias(state: BiasSweepState): BiasSweepState {
        val nextBias = (((state.currentBias + 0.1) * 10.0).roundToInt() / 10.0)
            .coerceAtMost(HeadlockConfig.MAX_VERTICAL_BIAS)
        return state.copy(
            currentBias = nextBias,
            repsCompleted = 0,
            hitsRecorded = 0
        )
    }
}

