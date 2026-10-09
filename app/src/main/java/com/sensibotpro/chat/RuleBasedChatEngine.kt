package com.sensibotpro.chat

import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile

/**
 * RuleBasedChatEngine — Deterministic, on-device Free Fire coaching intelligence.
 *
 * Implements the [ChatEngine] interface using verified Free Fire physics heuristics,
 * 0-200 sensitivity formulas, DPI boundaries, and esports creator configurations.
 *
 * Characteristics:
 * - 100% offline, zero network calls, zero API keys, zero per-message cost.
 * - Authoritative source of truth for all sensitivity numbers.
 * - Supplies structured ground-truth context to Qwen / Local AI.
 */
open class RuleBasedChatEngine : NeuralChatEngine() {

    /**
     * Extracts structured sensitivity recommendations from a player's query and state.
     * This output is authoritative and serves as the strict reference for Qwen natural language generation.
     */
    fun extractStructuredRecommendation(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): StructuredSensiRecommendation {
        val input = userMessage.trim().lowercase()
        val gen = activeProfile?.general ?: 188
        val red = activeProfile?.redDot ?: 182
        val scope2x = activeProfile?.scope2x ?: 170
        val scope4x = activeProfile?.scope4x ?: 158
        val sniper = activeProfile?.sniper ?: 60
        val btn = activeProfile?.fireButtonSize ?: 46
        val stockDpi = if ((deviceSpecs?.displayDensityDpi ?: 411) in 250..700) deviceSpecs!!.displayDensityDpi else 411
        val recDpi = (stockDpi + 35).coerceIn(380, 500)

        // Detect weapon
        val weapon = when {
            input.contains("m1887") || input.contains("1887") -> "M1887"
            input.contains("mp40") -> "MP40"
            input.contains("ump") -> "UMP"
            input.contains("deagle") || input.contains("desert eagle") -> "Desert Eagle"
            input.contains("woodpecker") -> "Woodpecker"
            input.contains("shotgun") || input.contains("m1014") -> "M1014 Shotgun"
            input.contains("awm") || input.contains("sniper") -> "AWM Sniper"
            input.contains("ak47") || input.contains("ak ") -> "AK47"
            input.contains("scar") -> "SCAR"
            else -> null
        }

        return when {
            // 1. Creator sensitivities
            input.contains("white") || input.contains("444") -> {
                StructuredSensiRecommendation(
                    weapon = weapon ?: "M1887 & Desert Eagle",
                    range = "Close-to-Mid",
                    aimProblem = "One-Tap Headshot J-Drag",
                    recommendedGeneral = 195,
                    recommendedRedDot = 188,
                    recommendedScope2x = 180,
                    recommendedScope4x = 175,
                    recommendedSniper = 62,
                    recommendedFireButtonSize = 44,
                    recommendedDpi = 460,
                    generalDelta = 195 - gen,
                    redDotDelta = 188 - red,
                    dragTechnique = "Sharp waist-level J-drag flick with instant weapon swap",
                    trainingDrill = "15 practice shots on stationary dummy from 10m with Desert Eagle",
                    tacticalWhy = "White FF's signature Moroccan one-tap ratio locks instantly onto helmet level",
                    creatorReference = "White FF (White444) Official Settings"
                )
            }
            input.contains("raistar") -> {
                StructuredSensiRecommendation(
                    weapon = weapon ?: "Shotgun & SMG",
                    range = "Close Range Movement",
                    aimProblem = "360° Rotational Speed Drag",
                    recommendedGeneral = 198,
                    recommendedRedDot = 190,
                    recommendedScope2x = 185,
                    recommendedScope4x = 178,
                    recommendedSniper = 65,
                    recommendedFireButtonSize = 42,
                    recommendedDpi = 510,
                    generalDelta = 198 - gen,
                    redDotDelta = 190 - red,
                    dragTechnique = "Sprint -> Jump 90° rotation -> Wide upward drag arc -> Instant sit-up gloo wall",
                    trainingDrill = "10 quick-turn 180° drag shots followed by instant crouch gloo wall",
                    tacticalWhy = "Raistar's ultra-fast rotation sensitivity eliminates camera delay during 360-degree moves",
                    creatorReference = "Raistar Speed Movement Settings"
                )
            }
            input.contains("lyam") -> {
                StructuredSensiRecommendation(
                    weapon = weapon ?: "All Weapons (Lyam FF Setup)",
                    range = "All Ranges",
                    aimProblem = "Competitive One-Tap & Headshot Lock",
                    recommendedGeneral = 196,
                    recommendedRedDot = 188,
                    recommendedScope2x = 180,
                    recommendedScope4x = 172,
                    recommendedSniper = 60,
                    recommendedFireButtonSize = 44,
                    recommendedDpi = 450,
                    generalDelta = 196 - gen,
                    redDotDelta = 188 - red,
                    dragTechnique = "Custom J-drag curve: pull down 1cm and snap up diagonally with explosive acceleration",
                    trainingDrill = "15 minutes in Training Ground: 5m Desert Eagle, 10m M1887, 20m MP40",
                    tacticalWhy = "Lyam FF's balanced tournament profile delivers maximum flick runway on right thumb HUD",
                    creatorReference = "Lyam FF Official Signature Setup"
                )
            }

            // 2. Specific aim problems
            input.contains("above") || input.contains("overshoot") || input.contains("flies") || input.contains("sky") -> {
                val targetGen = (gen - 10).coerceAtLeast(140)
                val targetRed = (red - 8).coerceAtLeast(130)
                StructuredSensiRecommendation(
                    weapon = weapon ?: "Current Weapon",
                    range = "Close-to-Mid Range",
                    aimProblem = "Overshooting Head (Crosshair flies into the sky)",
                    recommendedGeneral = targetGen,
                    recommendedRedDot = targetRed,
                    recommendedScope2x = scope2x,
                    recommendedScope4x = (scope4x - 10).coerceAtLeast(80),
                    recommendedSniper = sniper,
                    recommendedFireButtonSize = btn,
                    recommendedDpi = stockDpi,
                    generalDelta = -10,
                    redDotDelta = -8,
                    dragTechnique = "Shorten upward swipe arc; stop thumb travel right at mid-screen height",
                    trainingDrill = "10 shots on stationary dummy from 8m; verify crosshair stops at helmet level",
                    tacticalWhy = "Lowering General by 10 points on the 200 scale dampens vertical drag acceleration so aim locks onto helmet hitbox without flying past"
                )
            }
            input.contains("body") || input.contains("chest") || input.contains("stuck") || input.contains("wont lift") || input.contains("won't lift") -> {
                val targetGen = (gen + 8).coerceAtMost(200)
                val targetRed = (red + 6).coerceAtMost(200)
                StructuredSensiRecommendation(
                    weapon = weapon ?: "Current Weapon",
                    range = "Close Range",
                    aimProblem = "Body Shot Lock (Aim stuck on chest, won't lift)",
                    recommendedGeneral = targetGen,
                    recommendedRedDot = targetRed,
                    recommendedScope2x = scope2x,
                    recommendedScope4x = scope4x,
                    recommendedSniper = sniper,
                    recommendedFireButtonSize = (btn - 2).coerceAtLeast(40),
                    recommendedDpi = recDpi,
                    generalDelta = +8,
                    redDotDelta = +6,
                    dragTechnique = "Position crosshair to the side of enemy shoulder before pulling upward in a J-curve to break chest auto-aim friction",
                    trainingDrill = "15 practice shots on Training Ground bots; ensure first bullet lands as a red headshot",
                    tacticalWhy = "Boosting General by 8 points and Red Dot by 6 points provides enough initial velocity to break Free Fire's chest magnetic assist"
                )
            }
            input.contains("m1887") || input.contains("shotgun") -> {
                StructuredSensiRecommendation(
                    weapon = "M1887 Shotgun",
                    range = "Close Range (3-8m)",
                    aimProblem = "Shotgun One-Tap Inconsistency",
                    recommendedGeneral = 196,
                    recommendedRedDot = 188,
                    recommendedScope2x = scope2x,
                    recommendedScope4x = scope4x,
                    recommendedSniper = sniper,
                    recommendedFireButtonSize = 44,
                    recommendedDpi = (stockDpi + 45).coerceIn(380, 510),
                    generalDelta = 196 - gen,
                    redDotDelta = 188 - red,
                    dragTechnique = "V-Drag / J-Drag: quick micro-dip down (1cm) followed by explosive upward snap",
                    trainingDrill = "20 drag shots from 5-10m in Training Room; aim for 15+ red numbers",
                    tacticalWhy = "Shotguns require high initial flick momentum and a smaller fire button (44%) to maximize vertical runway"
                )
            }
            input.contains("mp40") || input.contains("ump") || input.contains("smg") -> {
                StructuredSensiRecommendation(
                    weapon = weapon ?: "MP40 / UMP",
                    range = "Close to Mid Range",
                    aimProblem = "SMG Spray Tracking & Bullet Spread",
                    recommendedGeneral = (gen + 2).coerceAtMost(200),
                    recommendedRedDot = (red + 2).coerceAtMost(200),
                    recommendedScope2x = scope2x,
                    recommendedScope4x = scope4x,
                    recommendedSniper = sniper,
                    recommendedFireButtonSize = 48,
                    recommendedDpi = (stockDpi + 25).coerceIn(380, 460),
                    dragTechnique = "Smooth vertical tracking without over-flicking; fire in 6-8 bullet controlled bursts",
                    trainingDrill = "Moving bot tracking at 10m; keep entire magazine concentrated on upper torso and head",
                    tacticalWhy = "SMGs suffer from bullet spread jitter if sensitivity is too high; a steady 48% button ensures stable spray grouping"
                )
            }
            input.contains("dpi") || input.contains("smallest width") -> {
                StructuredSensiRecommendation(
                    weapon = "All Weapons",
                    range = "All Ranges",
                    aimProblem = "Display Density / Touch Sampling Calibration",
                    recommendedGeneral = gen,
                    recommendedRedDot = red,
                    recommendedScope2x = scope2x,
                    recommendedScope4x = scope4x,
                    recommendedSniper = sniper,
                    recommendedFireButtonSize = btn,
                    recommendedDpi = recDpi,
                    dragTechnique = "Maintain consistent thumb pressure across new swipe-to-virtual-pixel ratio",
                    trainingDrill = "180° quick-turn test in Training Ground after setting Smallest Width",
                    tacticalWhy = "Recommended DPI ($recDpi) expands virtual swipe distance smoothly. If you ever use 600+ on supported devices, remember to reset it after playing."
                )
            }
            else -> {
                // Device-calibrated baseline
                val baseGen = when {
                    (deviceSpecs?.refreshRate ?: 60) >= 120 -> 188
                    (deviceSpecs?.refreshRate ?: 60) >= 90 -> 192
                    else -> 196
                }
                StructuredSensiRecommendation(
                    weapon = weapon ?: "All Weapons (Balanced)",
                    range = "Balanced (All Ranges)",
                    aimProblem = "General Sensitivity Calibration",
                    recommendedGeneral = baseGen,
                    recommendedRedDot = (baseGen - 8),
                    recommendedScope2x = 175,
                    recommendedScope4x = 165,
                    recommendedSniper = 60,
                    recommendedFireButtonSize = 46,
                    recommendedDpi = recDpi,
                    generalDelta = baseGen - gen,
                    redDotDelta = (baseGen - 8) - red,
                    dragTechnique = "Standard J-Drag starting from chest level with smooth upward flick",
                    trainingDrill = "15-minute daily routine: 5 min warm-up, 5 min one-taps, 5 min moving tracking",
                    tacticalWhy = "Device-calibrated competitive baseline for ${deviceSpecs?.manufacturer ?: "device"} at ${deviceSpecs?.refreshRate ?: 60}Hz"
                )
            }
        }
    }
}
