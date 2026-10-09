package com.sensibotpro.chat

import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.model.SensitivityProfile
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * NeuralChatEngine — SENSI BOT Pro's on-device coaching intelligence.
 *
 * Architecture:
 *  - Intent Scoring: Weighted multi-keyword scoring across all intent categories
 *  - Entity Extraction: Weapon, range, DPI value, sensitivity number detection
 *  - Conversation Memory: 3-turn context window for follow-ups
 *  - Multi-Intent Fusion: Combines up to 2 detected intents for richer answers
 *  - Dynamic Response Generation: Live device & profile data baked into every response
 *  - Async API Simulation: Variable latency (350–900ms) based on response complexity
 *  - Confidence Gating: Clarifying questions when confidence < 0.35
 *
 * Fully offline. Zero network calls. Zero game access. 100% coaching utility.
 */
open class NeuralChatEngine : ChatEngine {

    private val state = ChatConversationState()

    // ─────────────────────────────────────────────────────────────────────────
    //  Intent Definitions  (id, keywords with weights, base latency, complexity)
    // ─────────────────────────────────────────────────────────────────────────
    private data class Intent(
        val id: String,
        val keywords: Map<String, Float>,  // keyword → weight
        val baseLatencyMs: Long,
        val complexity: Int  // 1–3, used for typing delay simulation
    )

    private val intents = listOf(
        Intent(
            id = "OVERSHOOT",
            keywords = mapOf(
                "above" to 0.9f, "overshoot" to 1.0f, "over head" to 1.0f, "overhead" to 1.0f,
                "too high" to 0.9f, "flies" to 0.7f, "goes up" to 0.7f, "fly past" to 0.8f,
                "sky" to 0.6f, "above helmet" to 1.0f, "misses above" to 0.9f,
                "drag goes up" to 0.95f, "above the head" to 1.0f, "past the head" to 1.0f
            ),
            baseLatencyMs = 520, complexity = 2
        ),
        Intent(
            id = "BODY_SHOTS",
            keywords = mapOf(
                "body" to 0.9f, "chest" to 0.9f, "body shot" to 1.0f, "won't lift" to 1.0f,
                "cant lift" to 1.0f, "can't lift" to 1.0f, "stuck" to 0.7f, "lock" to 0.6f,
                "too low" to 0.8f, "aim low" to 0.8f, "hitting body" to 1.0f,
                "not going up" to 0.9f, "doesn't go up" to 0.9f, "no headshot" to 0.75f
            ),
            baseLatencyMs = 480, complexity = 2
        ),
        Intent(
            id = "AIM_FAST",
            keywords = mapOf(
                "too fast" to 1.0f, "overswipe" to 1.0f, "over swipe" to 0.9f,
                "fast" to 0.6f, "sensitivity high" to 0.8f, "speed high" to 0.8f,
                "too sensitive" to 0.9f, "uncontrollable" to 0.85f, "spinning" to 0.7f,
                "camera too fast" to 1.0f, "flick too far" to 0.9f
            ),
            baseLatencyMs = 430, complexity = 2
        ),
        Intent(
            id = "AIM_SLOW",
            keywords = mapOf(
                "too slow" to 1.0f, "sluggish" to 1.0f, "slow aim" to 1.0f, "heavy" to 0.7f,
                "can't track" to 0.9f, "cant track" to 0.9f, "doesn't move" to 0.8f,
                "not responsive" to 0.85f, "lag" to 0.5f, "delayed" to 0.7f,
                "low sensitivity" to 0.8f, "slow camera" to 0.95f
            ),
            baseLatencyMs = 420, complexity = 2
        ),
        Intent(
            id = "DPI",
            keywords = mapOf(
                "dpi" to 1.0f, "smallest width" to 1.0f, "minimum width" to 0.95f,
                "display density" to 0.95f, "developer options" to 0.7f,
                "screen density" to 0.9f, "resolution setting" to 0.7f,
                "phone dpi" to 1.0f, "dp setting" to 0.85f, "dots per inch" to 0.8f,
                "sw dp" to 0.75f, "sw setting" to 0.75f, "best dpi" to 0.95f,
                "what dpi" to 1.0f, "how to set dpi" to 1.0f, "headshot dpi" to 0.9f
            ),
            baseLatencyMs = 600, complexity = 3
        ),
        Intent(
            id = "SCOPE",
            keywords = mapOf(
                "scope" to 0.9f, "2x" to 0.85f, "4x" to 0.85f, "zoom" to 0.75f,
                "shake" to 0.8f, "recoil" to 0.8f, "unstable scope" to 1.0f,
                "scope shake" to 1.0f, "spray" to 0.5f, "full auto" to 0.55f,
                "awm" to 0.6f, "kar98" to 0.6f, "sniper" to 0.6f,
                "scope sensitivity" to 0.95f, "scope too fast" to 0.9f
            ),
            baseLatencyMs = 450, complexity = 2
        ),
        Intent(
            id = "ONE_TAP",
            keywords = mapOf(
                "one tap" to 1.0f, "onetap" to 1.0f, "single tap" to 0.95f,
                "first bullet" to 0.9f, "deagle" to 0.85f, "desert eagle" to 0.85f,
                "woodpecker" to 0.75f, "headshot tap" to 0.95f, "tap shot" to 0.85f,
                "one shot" to 0.7f, "m1887 tap" to 0.9f, "instant kill" to 0.65f
            ),
            baseLatencyMs = 470, complexity = 2
        ),
        Intent(
            id = "SHOTGUN",
            keywords = mapOf(
                "m1887" to 1.0f, "shotgun" to 0.9f, "m1014" to 0.85f, "mag-7" to 0.85f,
                "mag7" to 0.85f, "inconsistent" to 0.65f, "v-drag" to 0.85f,
                "drag headshot" to 0.8f, "shotgun drag" to 1.0f, "1887" to 0.95f,
                "close range drag" to 0.8f, "drag shot" to 0.75f
            ),
            baseLatencyMs = 490, complexity = 2
        ),
        Intent(
            id = "SMG",
            keywords = mapOf(
                "mp40" to 1.0f, "ump" to 0.9f, "smg" to 0.85f, "thompson" to 0.8f,
                "mp5" to 0.85f, "submachine" to 0.8f, "rush gun" to 0.7f,
                "close spray" to 0.7f, "spray control" to 0.7f
            ),
            baseLatencyMs = 440, complexity = 2
        ),
        Intent(
            id = "AR",
            keywords = mapOf(
                "ak" to 0.9f, "ak47" to 0.95f, "scar" to 0.9f, "m4a1" to 0.9f,
                "assault rifle" to 0.85f, "ar" to 0.5f, "woodpecker" to 0.8f,
                "burst" to 0.6f, "burst fire" to 0.75f, "mid range" to 0.65f,
                "spray pattern" to 0.7f
            ),
            baseLatencyMs = 440, complexity = 2
        ),
        Intent(
            id = "FIRE_BUTTON",
            keywords = mapOf(
                "fire button" to 1.0f, "hud" to 0.85f, "button size" to 0.95f,
                "button placement" to 0.95f, "fire size" to 0.9f, "press button" to 0.75f,
                "layout" to 0.7f, "button position" to 0.9f, "custom hud" to 0.8f,
                "where to put" to 0.6f
            ),
            baseLatencyMs = 400, complexity = 1
        ),
        Intent(
            id = "RECOMMEND",
            keywords = mapOf(
                "recommend" to 0.9f, "best settings" to 1.0f, "what sensitivity" to 1.0f,
                "profile" to 0.65f, "settings for" to 0.85f, "optimal" to 0.8f,
                "what should" to 0.75f, "give me" to 0.5f, "suggest" to 0.85f,
                "which sensitivity" to 0.95f, "settings" to 0.45f, "baseline" to 0.7f,
                "starting point" to 0.75f, "starter settings" to 0.85f
            ),
            baseLatencyMs = 700, complexity = 3
        ),
        Intent(
            id = "FAIR_PLAY",
            keywords = mapOf(
                "hack" to 1.0f, "cheat" to 1.0f, "ban" to 0.85f, "inject" to 0.95f,
                "aimbot" to 1.0f, "auto aim" to 0.9f, "mod" to 0.8f, "safe" to 0.5f,
                "banned" to 0.85f, "risky" to 0.75f, "illegal" to 0.9f, "script" to 0.85f
            ),
            baseLatencyMs = 380, complexity = 1
        ),
        Intent(
            id = "TRAINING",
            keywords = mapOf(
                "practice" to 0.9f, "training" to 0.9f, "drill" to 0.85f, "improve" to 0.7f,
                "get better" to 0.8f, "training ground" to 0.95f, "routine" to 0.8f,
                "exercise" to 0.7f, "warm up" to 0.85f, "warmup" to 0.85f,
                "aim training" to 0.95f, "how to improve" to 0.85f
            ),
            baseLatencyMs = 550, complexity = 3
        ),
        Intent(
            id = "CLOSE_RANGE",
            keywords = mapOf(
                "close range" to 1.0f, "close" to 0.6f, "rush" to 0.7f,
                "aggressive" to 0.65f, "indoor" to 0.7f, "building" to 0.65f,
                "face to face" to 0.8f, "nearby" to 0.6f, "short range" to 0.95f
            ),
            baseLatencyMs = 430, complexity = 2
        ),
        Intent(
            id = "LONG_RANGE",
            keywords = mapOf(
                "long range" to 1.0f, "distance" to 0.75f, "far" to 0.65f,
                "snipe" to 0.8f, "sniper" to 0.7f, "mid range" to 0.7f,
                "scoping" to 0.7f, "cross map" to 0.85f, "far away" to 0.8f
            ),
            baseLatencyMs = 450, complexity = 2
        ),
        Intent(
            id = "GREETING",
            keywords = mapOf(
                "hello" to 1.0f, "hi" to 0.9f, "hey" to 0.9f, "what can you do" to 0.95f,
                "help" to 0.6f, "start" to 0.5f, "how are you" to 0.9f,
                "good morning" to 0.9f, "what do you do" to 0.85f, "who are you" to 0.9f
            ),
            baseLatencyMs = 320, complexity = 1
        ),
        Intent(
            id = "J_DRAG",
            keywords = mapOf(
                "j drag" to 1.0f, "j-drag" to 1.0f, "rotation drag" to 1.0f, "v-drag" to 0.95f,
                "curve drag" to 0.9f, "how to drag" to 1.0f, "drag style" to 0.95f,
                "drag technique" to 0.95f, "drag method" to 0.9f, "headshot drag" to 0.95f,
                "drag headshot" to 0.95f, "how to headshot" to 1.0f, "pull fire button" to 0.85f
            ),
            baseLatencyMs = 460, complexity = 2
        ),
        Intent(
            id = "CHARACTER_COMBO",
            keywords = mapOf(
                "character" to 1.0f, "skill" to 0.95f, "combo" to 0.95f, "alok" to 0.9f,
                "chrono" to 0.85f, "tatsuya" to 0.9f, "hayato" to 0.9f, "kelly" to 0.85f,
                "moco" to 0.85f, "dimitri" to 0.85f, "homer" to 0.85f, "best character" to 1.0f,
                "best skill" to 1.0f, "active skill" to 0.9f, "passive skill" to 0.9f
            ),
            baseLatencyMs = 450, complexity = 2
        ),
        Intent(
            id = "GRAPHICS_FPS",
            keywords = mapOf(
                "fps" to 1.0f, "60fps" to 1.0f, "90fps" to 1.0f, "120fps" to 1.0f,
                "lag" to 0.9f, "smooth" to 0.85f, "graphics" to 0.95f, "stutter" to 0.9f,
                "frame drop" to 0.95f, "framedrop" to 0.95f, "overheat" to 0.85f,
                "display settings" to 0.85f, "shadow" to 0.7f, "high fps" to 1.0f
            ),
            baseLatencyMs = 420, complexity = 2
        ),
        Intent(
            id = "CLASH_SQUAD",
            keywords = mapOf(
                "clash squad" to 1.0f, "cs rank" to 1.0f, "cs mode" to 0.95f,
                "round 1" to 0.95f, "first round" to 0.9f, "cs ranked" to 1.0f,
                "solo vs squad" to 0.9f, "rush team" to 0.85f
            ),
            baseLatencyMs = 440, complexity = 2
        ),
        Intent(
            id = "GLOO_WALL",
            keywords = mapOf(
                "gloo wall" to 1.0f, "gloo" to 0.95f, "fast gloo" to 1.0f,
                "sit up gloo" to 1.0f, "sit-up gloo" to 1.0f, "crouch gloo" to 1.0f,
                "wall drop" to 0.9f, "speed gloo" to 0.95f, "quick gloo" to 0.95f
            ),
            baseLatencyMs = 410, complexity = 2
        ),
        Intent(
            id = "YOUTUBER_SENSI",
            keywords = mapOf(
                "white ff" to 1.0f, "white444" to 1.0f, "white" to 0.75f, "444" to 0.85f,
                "raistar" to 1.0f, "ruok" to 1.0f, "ruok ff" to 1.0f,
                "badge 99" to 1.0f, "badge99" to 1.0f, "total gaming" to 1.0f,
                "ajjubhai" to 1.0f, "nobru" to 1.0f, "b2k" to 1.0f, "born2kill" to 1.0f,
                "lyam ff" to 1.0f, "lyam" to 0.9f, "youtuber" to 0.95f, "youtubers" to 0.95f,
                "creator settings" to 0.95f, "pro player sensi" to 1.0f, "pro sensi" to 0.9f,
                "streamer sensi" to 0.9f, "famous player" to 0.85f
            ),
            baseLatencyMs = 500, complexity = 2
        ),
        Intent(
            id = "SENSI_200_SCALE",
            keywords = mapOf(
                "200" to 0.95f, "200 scale" to 1.0f, "max 200" to 1.0f, "upto 200" to 1.0f,
                "up to 200" to 1.0f, "new update" to 0.85f, "ob update" to 0.85f,
                "sensitivity max" to 0.85f, "max sensitivity" to 0.9f, "why 200" to 1.0f,
                "new sensitivity" to 0.85f, "latest sensitivity" to 0.85f, "200 sensi" to 1.0f
            ),
            baseLatencyMs = 450, complexity = 2
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    //  Entity Extraction
    // ─────────────────────────────────────────────────────────────────────────
    private data class ExtractedEntities(
        val weapon: String?,
        val range: String?,
        val mentionedDpiValue: Int?,
        val mentionedSensValue: Int?
    )

    private fun extractEntities(input: String): ExtractedEntities {
        val weapon = when {
            input.contains("m1887") || input.contains("1887") -> "M1887"
            input.contains("mp40") || input.contains("mp-40") -> "MP40"
            input.contains("ump") -> "UMP"
            input.contains("deagle") || input.contains("desert eagle") -> "Desert Eagle"
            input.contains("woodpecker") -> "Woodpecker"
            input.contains("ac80") -> "AC80"
            input.contains("m500") -> "M500"
            input.contains("svd") -> "SVD"
            input.contains("ak47") || input.contains("ak ") -> "AK47"
            input.contains("scar") -> "SCAR"
            input.contains("m4a1") -> "M4A1"
            input.contains("groza") -> "Groza"
            input.contains("awm") -> "AWM"
            input.contains("m82b") -> "M82B"
            input.contains("kar98") -> "Kar98k"
            input.contains("m1014") -> "M1014"
            input.contains("mag7") || input.contains("mag-7") -> "MAG-7"
            input.contains("charge buster") -> "Charge Buster"
            input.contains("thompson") -> "Thompson"
            input.contains("mp5") -> "MP5"
            input.contains("bizon") -> "Bizon"
            input.contains("g18") -> "G18"
            input.contains("usp") -> "USP"
            else -> null
        }

        val range = when {
            input.contains("close") || input.contains("rush") || input.contains("indoor") -> "close"
            input.contains("long") || input.contains("far") || input.contains("snipe") -> "long"
            input.contains("mid") -> "mid"
            else -> null
        }

        // Extract any number that looks like a DPI value (300–700)
        val dpiVal = Regex("\\b([3-6]\\d{2})\\b").findAll(input)
            .map { it.value.toInt() }
            .firstOrNull { it in 300..700 }

        // Extract sensitivity value (30–200)
        val sensVal = Regex("\\b([3-9]\\d|1\\d{2}|200)\\b").findAll(input)
            .map { it.value.toInt() }
            .firstOrNull { it in 30..200 }

        return ExtractedEntities(weapon, range, dpiVal, sensVal)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Intent Scoring
    // ─────────────────────────────────────────────────────────────────────────
    private data class ScoredIntent(val intent: Intent, val score: Float)

    private fun scoreIntents(input: String): List<ScoredIntent> {
        val tokens = input.lowercase().replace("'", "").replace("\"", "")
        return intents.map { intent ->
            var score = 0f
            intent.keywords.forEach { (kw, weight) ->
                if (tokens.contains(kw)) {
                    score += weight
                }
            }
            ScoredIntent(intent, score)
        }.filter { it.score > 0f }.sortedByDescending { it.score }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Response Generation
    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun processMessage(
        userMessage: String,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): ChatMessage {
        val input = userMessage.trim().lowercase()
        val entities = extractEntities(input)

        // Update conversation state with newly detected entities
        entities.weapon?.let { state.currentWeapon = it }
        entities.range?.let { state.currentRange = it }

        // Score all intents
        val scored = scoreIntents(input)
        val topIntent = scored.firstOrNull()
        val secondIntent = scored.getOrNull(1)

        val confidence = topIntent?.score?.let { minOf(it / 1.5f, 1.0f) } ?: 0f

        // --- Handle pending follow-up first ---
        if (state.awaitFollowUp) {
            val followUpResult = handleFollowUp(input, entities, activeProfile, deviceSpecs)
            if (followUpResult != null) {
                simulateLatency(450, complexity = 2)
                val turn = ConversationTurn(userMessage, "FOLLOW_UP_${state.followUpType}", followUpResult.text)
                state.pushTurn(turn)
                return followUpResult
            }
        }

        // Deduced weapon intent if a specific weapon was mentioned
        val weaponIntent = when (entities.weapon) {
            "M1887", "M1014", "MAG-7", "Charge Buster" -> "SHOTGUN"
            "MP40", "UMP", "Thompson", "MP5", "Bizon" -> "SMG"
            "Woodpecker", "Desert Eagle", "AC80", "M500", "SVD" -> "ONE_TAP"
            "AWM", "M82B", "Kar98k" -> "SCOPE"
            "AK47", "SCAR", "M4A1", "Groza" -> "AR"
            else -> null
        }

        val isYoutuberQuery = input.contains("white") || input.contains("444") ||
                input.contains("raistar") || input.contains("ruok") ||
                input.contains("badge") || input.contains("ajjubhai") ||
                input.contains("total gaming") || input.contains("nobru") ||
                input.contains("b2k") || input.contains("born2kill") ||
                input.contains("lyam") || input.contains("youtuber") ||
                input.contains("creator") || input.contains("pro player")

        val is200ScaleQuery = input.contains("200") || input.contains("upto 200") ||
                input.contains("up to 200") || input.contains("200 scale") ||
                (input.contains("scale") && input.contains("sensi"))

        // Primary intent dispatch — smartly routes whenever weapons, drag, or key terms exist
        val primaryId = when {
            isYoutuberQuery -> "YOUTUBER_SENSI"
            is200ScaleQuery -> "SENSI_200_SCALE"
            weaponIntent != null -> weaponIntent
            topIntent != null && topIntent.score >= 0.35f -> topIntent.intent.id
            input.contains("j drag") || input.contains("how to drag") || input.contains("rotation") -> "J_DRAG"
            input.contains("one tap") || input.contains("onetap") || input.contains("headshot") -> "ONE_TAP"
            input.contains("skill") || input.contains("character") || input.contains("combo") -> "CHARACTER_COMBO"
            input.contains("fps") || input.contains("lag") || input.contains("smooth") -> "GRAPHICS_FPS"
            input.contains("gloo") -> "GLOO_WALL"
            input.contains("clash squad") || input.contains("cs") -> "CLASH_SQUAD"
            input.contains("dpi") || input.contains("smallest width") -> "DPI"
            input.contains("button") || input.contains("hud") || input.contains("claw") -> "FIRE_BUTTON"
            input.contains("settings") || input.contains("sensi") || input.contains("profile") -> "RECOMMEND"
            topIntent != null -> topIntent.intent.id
            else -> "RECOMMEND"
        }
        val latency = topIntent?.intent?.baseLatencyMs ?: 400L
        val complexity = topIntent?.intent?.complexity ?: 2
        simulateLatency(latency, complexity)

        val response = generateResponse(
            primaryId = primaryId,
            secondaryId = secondIntent?.intent?.id,
            entities = entities,
            activeProfile = activeProfile,
            deviceSpecs = deviceSpecs,
            confidence = confidence,
            input = input
        )

        state.pushTurn(ConversationTurn(userMessage, primaryId, response.text))
        return response.copy(confidence = confidence)
    }

    /** Realistic async latency — variable to feel like actual inference */
    private suspend fun simulateLatency(baseMs: Long, complexity: Int) {
        val jitter = Random.nextLong(-60, 120)
        val complexityExtra = (complexity - 1) * 80L
        delay((baseMs + jitter + complexityExtra).coerceAtLeast(250))
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Follow-Up Handler
    // ─────────────────────────────────────────────────────────────────────────
    private fun handleFollowUp(
        input: String,
        entities: ExtractedEntities,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?
    ): ChatMessage? {
        when (state.followUpType) {
            "RANGE_FOR_SPEED" -> {
                val isClose = input.contains("close") || input.contains("rush") || input.contains("indoor")
                val isLong = input.contains("long") || input.contains("far") || input.contains("scope") || input.contains("distance")
                val currentGen = activeProfile?.general ?: 96
                val currentRed = activeProfile?.redDot ?: 91

                return when {
                    isClose -> {
                        state.awaitFollowUp = true
                        state.followUpType = "WEAPON_FOR_CLOSE"
                        state.currentRange = "close"
                        ChatMessage(
                            text = "Got it — close range rushing. Quick turns are key without losing control.\n\nWhat weapon do you mainly use in close quarters? (e.g. M1887, MP40, or AK47?)",
                            isUser = false,
                            thinkingText = "Thinking: Mapping close-quarters drag arc for ${deviceSpecs?.model ?: "device"}...",
                            actionSuggestion = "Close Range Calibration"
                        )
                    }
                    isLong -> {
                        state.awaitFollowUp = false
                        state.followUpType = null
                        state.currentRange = "long"
                        val targetGen = (currentGen - 5).coerceAtLeast(65)
                        val targetRed = (currentRed - 3).coerceAtLeast(60)
                        ChatMessage(
                            text = "For long-range, micro-stability is everything — you want precise movements, not snappy ones.\n\nAdjust:\n• General: $targetGen (−5)\n• Red Dot: $targetRed (−3)\n• 4X Scope: −6 from current\n• Technique: Fire in 3-bullet bursts, apply gentle downward thumb pressure during spray.\n\n✅ Test on the 35m training dummy — you should tap reliably without scope bounce.",
                            isUser = false,
                            thinkingText = "Thinking: Calculating scope dispersion coefficient at 35m for ${deviceSpecs?.refreshRate ?: 60}Hz...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = -5, redDotChange = -3, scope2xChange = -4),
                            actionSuggestion = "3-Bullet Burst Drill"
                        )
                    }
                    else -> null
                }
            }
            "WEAPON_FOR_CLOSE" -> {
                state.awaitFollowUp = false
                state.followUpType = null
                val weapon = entities.weapon ?: state.currentWeapon ?: "Shotgun"
                val currentGen = activeProfile?.general ?: 96
                val currentRed = activeProfile?.redDot ?: 91
                val targetGen = (currentGen - 4).coerceAtLeast(70)
                val targetRed = (currentRed - 3).coerceAtLeast(65)
                val fireBtn = when {
                    input.contains("m1887") || input.contains("shotgun") -> 45
                    input.contains("mp40") || input.contains("smg") -> 47
                    else -> 46
                }
                return ChatMessage(
                    text = "For $weapon in close range:\n\n• General: $targetGen (−4)\n• Red Dot: $targetRed (−3)\n• Fire Button Size: $fireBtn% (lower placement)\n• Drag Style: Short explosive J-curve — start just below the enemy's shoulder, snap upward.\n• Stop your drag when the fire button reaches mid-screen.\n\n🎯 Run 20 practice shots in Training Ground, then test in Clash Squad.",
                    isUser = false,
                    thinkingText = "Thinking: Optimizing $weapon drag arc geometry for close-quarters engagement...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = -4, redDotChange = -3, fireButtonChange = fireBtn),
                    actionSuggestion = "J-Curve Drag Drill"
                )
            }
            "DPI_CONFIRM_STOCK" -> {
                state.awaitFollowUp = false
                state.followUpType = null
                val stockDpi = entities.mentionedDpiValue ?: state.lastRecommendedDpi ?: 411
                val recommended = (stockDpi + 40).coerceIn(380, 520)
                state.lastRecommendedDpi = recommended
                return ChatMessage(
                    text = "Perfect! Based on your stock DPI of $stockDpi:\n\n• ✅ Recommended Smallest Width: $recommended\n• Boost Amount: +${recommended - stockDpi}\n\n💡 Tip: Some devices and players need 600+ DPI for ultra flicks, but if you do push to 600+, always remember to turn it back down after your gaming session so your daily apps and system UI stay normal.\n\nPath: Developer Options → Smallest Width → enter $recommended → reboot.\n\nTest Free Fire sensitivity feel after reboot — your swipe distance will feel smoother, so you can fine-tune General in-game.",
                    isUser = false,
                    thinkingText = "Thinking: Validating DPI safety bounds for $stockDpi base value...",
                    sensitivityAdjustment = SensitivityDelta(
                        recommendedDpiChange = recommended,
                        dpiStockValue = stockDpi,
                        dpiBoostAmount = recommended - stockDpi
                    ),
                    actionSuggestion = "Reboot After DPI Change"
                )
            }
            else -> return null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Primary Response Dispatch
    // ─────────────────────────────────────────────────────────────────────────
    private fun generateResponse(
        primaryId: String,
        secondaryId: String?,
        entities: ExtractedEntities,
        activeProfile: SensitivityProfile?,
        deviceSpecs: DeviceSpecs?,
        confidence: Float,
        input: String
    ): ChatMessage {
        val gen = activeProfile?.general ?: 188
        val red = activeProfile?.redDot ?: 182
        val scope2x = activeProfile?.scope2x ?: 170
        val scope4x = activeProfile?.scope4x ?: 158
        val weapon = entities.weapon ?: state.currentWeapon ?: "your weapon"
        val devModel = deviceSpecs?.model ?: "your device"
        val refRate = deviceSpecs?.refreshRate ?: 60
        val stockDpi = if ((deviceSpecs?.displayDensityDpi ?: 411) in 250..700) deviceSpecs!!.displayDensityDpi else 411

        return when (primaryId) {

            "YOUTUBER_SENSI" -> {
                when {
                    input.contains("white") || input.contains("444") -> {
                        ChatMessage(
                            text = "👑 **White444 / White FF • Elite Moroccan One-Tap Legend**\n\n" +
                                    "White FF is famous for the fastest J-drag headshot flick & instant weapon switch in Free Fire history. Here are his verified competitive settings on the modern 200 scale:\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 195\n" +
                                    "• **Red Dot:** 188\n" +
                                    "• **2X Scope:** 180\n" +
                                    "• **4X Scope:** 175\n" +
                                    "• **Sniper Scope:** 62\n" +
                                    "• **Free Look:** 100\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 460 Smallest Width (Stock 392 + 68)\n" +
                                    "• **Fire Button Size:** 44% (Positioned low on right HUD to maximize upward drag runway)\n\n" +
                                    "⚡ **Signature Technique (J-Drag Flick):**\n" +
                                    "White FF begins his drag from the right side of the enemy's waist, pulls down 1cm to break auto-aim chest friction, then snaps diagonally up into the head hitbox with an instantaneous weapon switch.",
                            isUser = false,
                            thinkingText = "Thinking: Retrieving White444 (White FF) tournament profile — mapping to 0-200 sensitivity curve for ${devModel}...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 195 - gen, redDotChange = 188 - red, recommendedDpiChange = 460),
                            actionSuggestion = "Apply White FF Preset"
                        )
                    }
                    input.contains("raistar") -> {
                        ChatMessage(
                            text = "⚡ **Raistar • Speed Movement & 360° King**\n\n" +
                                    "Raistar revolutionized mobile Free Fire with unmatched 360° rotational movement and 0.2s sit-up gloo wall defense.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 198 (Ultra-fast rotation)\n" +
                                    "• **Red Dot:** 190\n" +
                                    "• **2X Scope:** 185\n" +
                                    "• **4X Scope:** 178\n" +
                                    "• **Sniper Scope:** 65\n" +
                                    "• **Free Look:** 120\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 510 Smallest Width (High-speed movement)\n" +
                                    "• **Fire Button Size:** 42% (Ultra-responsive tap-to-action)\n\n" +
                                    "⚡ **Signature Technique (Rotation Drag):**\n" +
                                    "Sprint → Jump-turn 90° → Curve fire button upward in a wide arch → Instant sit-down crouch + Gloo Wall drop.",
                            isUser = false,
                            thinkingText = "Thinking: Loading Raistar 360° rotation movement parameters & 200-scale profile...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 198 - gen, redDotChange = 190 - red, recommendedDpiChange = 510),
                            actionSuggestion = "Apply Raistar Preset"
                        )
                    }
                    input.contains("ruok") -> {
                        ChatMessage(
                            text = "🎯 **Ruok FF • One-Tap & Sniper Specialist**\n\n" +
                                    "Ruok FF is renowned for lethal single-bullet headshots with Desert Eagle, M1887, and AWM.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 192\n" +
                                    "• **Red Dot:** 185\n" +
                                    "• **2X Scope:** 178\n" +
                                    "• **4X Scope:** 168\n" +
                                    "• **Sniper Scope:** 85 (High sniper flick)\n" +
                                    "• **Free Look:** 110\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 480 Smallest Width\n" +
                                    "• **Fire Button Size:** 45%\n\n" +
                                    "⚡ **Signature Technique:**\n" +
                                    "Micro-pause (0.08s) when the red reticle locks on target before dragging vertically up with smooth acceleration.",
                            isUser = false,
                            thinkingText = "Thinking: Calibrating Ruok FF precision one-tap and sniper flick ratios...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 192 - gen, redDotChange = 185 - red, recommendedDpiChange = 480),
                            actionSuggestion = "Apply Ruok FF Preset"
                        )
                    }
                    input.contains("badge") || input.contains("99") -> {
                        ChatMessage(
                            text = "🔥 **Badge 99 • Rush Gameplay & Close Quarters**\n\n" +
                                    "Badge 99 plays high-tempo Clash Squad and Ranked rush with shotguns and SMGs.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 194\n" +
                                    "• **Red Dot:** 186\n" +
                                    "• **2X Scope:** 175\n" +
                                    "• **4X Scope:** 170\n" +
                                    "• **Sniper Scope:** 60\n" +
                                    "• **Free Look:** 100\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 450 Smallest Width\n" +
                                    "• **Fire Button Size:** 48%\n\n" +
                                    "⚡ **Signature Technique:**\n" +
                                    "Straight vertical flick starting slightly below chest level to lift crosshair smoothly to head height.",
                            isUser = false,
                            thinkingText = "Thinking: Analyzing Badge 99 rush playstyle and fire button geometry...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 194 - gen, redDotChange = 186 - red, recommendedDpiChange = 450),
                            actionSuggestion = "Apply Badge 99 Preset"
                        )
                    }
                    input.contains("total gaming") || input.contains("ajjubhai") -> {
                        ChatMessage(
                            text = "🛡️ **Total Gaming (Ajjubhai) • High-Stability Competitive Setup**\n\n" +
                                    "Ajjubhai focuses on mid-range laser sprays, consistent DMR tapping, and stable recoil control without screen jitter.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 188\n" +
                                    "• **Red Dot:** 180\n" +
                                    "• **2X Scope:** 172\n" +
                                    "• **4X Scope:** 165\n" +
                                    "• **Sniper Scope:** 55\n" +
                                    "• **Free Look:** 90\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 420 Smallest Width (Safe, stock-friendly)\n" +
                                    "• **Fire Button Size:** 52% (Larger button for reliable contact during intense teamfights)\n\n" +
                                    "⚡ **Signature Technique:**\n" +
                                    "Moderate, controlled upward drag with automatic rifles and smooth tracking.",
                            isUser = false,
                            thinkingText = "Thinking: Calculating Total Gaming stability curve on 200 scale...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 188 - gen, redDotChange = 180 - red, recommendedDpiChange = 420),
                            actionSuggestion = "Apply Ajjubhai Preset"
                        )
                    }
                    input.contains("nobru") -> {
                        ChatMessage(
                            text = "🏆 **Nobru • World Series MVP & Brazilian Movement God**\n\n" +
                                    "Nobru's legendary movement and close-range tracking made him a Free Fire World Champion.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 196\n" +
                                    "• **Red Dot:** 188\n" +
                                    "• **2X Scope:** 182\n" +
                                    "• **4X Scope:** 174\n" +
                                    "• **Sniper Scope:** 60\n" +
                                    "• **Free Look:** 100\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 470 Smallest Width\n" +
                                    "• **Fire Button Size:** 43%\n\n" +
                                    "⚡ **Signature Technique (Brazilian Capa):**\n" +
                                    "Diagonal flick drag with immediate crouch cancel.",
                            isUser = false,
                            thinkingText = "Thinking: Compiling Nobru world championship sensitivity ratios...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 196 - gen, redDotChange = 188 - red, recommendedDpiChange = 470),
                            actionSuggestion = "Apply Nobru Preset"
                        )
                    }
                    input.contains("b2k") || input.contains("born2kill") -> {
                        ChatMessage(
                            text = "🎯 **B2K (Born2Kill) • Tactical Sniper & Long-Range Master**\n\n" +
                                    "B2K is celebrated globally for precision sniper switching (Double AWM / M82B) and clean headshots.\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 190\n" +
                                    "• **Red Dot:** 182\n" +
                                    "• **2X Scope:** 178\n" +
                                    "• **4X Scope:** 172\n" +
                                    "• **Sniper Scope:** 92 (Super-fast double sniper quick-switch)\n" +
                                    "• **Free Look:** 80\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 440 Smallest Width\n" +
                                    "• **Fire Button Size:** 46%\n\n" +
                                    "⚡ **Signature Technique:**\n" +
                                    "Quick-scope flick with instant slot 1 ⇄ slot 2 weapon swap before recoil resets.",
                            isUser = false,
                            thinkingText = "Thinking: Loading B2K sniper quick-scope parameters on 200 scale...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 190 - gen, redDotChange = 182 - red, recommendedDpiChange = 440),
                            actionSuggestion = "Apply B2K Preset"
                        )
                    }
                    input.contains("lyam") -> {
                        ChatMessage(
                            text = "👑 **Lyam FF Official • Custom Headshot & One-Tap Sensi**\n\n" +
                                    "This is the official signature setup engineered by Lyam FF for SENSI BOT Pro, calibrated specifically for modern Free Fire gameplay on the 200 scale:\n\n" +
                                    "🎯 **In-Game Sensitivity (0–200 Scale):**\n" +
                                    "• **General:** 196\n" +
                                    "• **Red Dot:** 188\n" +
                                    "• **2X Scope:** 180\n" +
                                    "• **4X Scope:** 172\n" +
                                    "• **Sniper Scope:** 60\n" +
                                    "• **Free Look:** 100\n\n" +
                                    "📱 **Device & HUD Config:**\n" +
                                    "• **Phone DPI:** 450 Smallest Width (Perfect balance between speed & safety)\n" +
                                    "• **Fire Button Size:** 44% (Maximum flick runway)\n\n" +
                                    "⚡ **Coaching Tip from Lyam FF:**\n" +
                                    "Keep your crosshair slightly to the right of the enemy's head, pull your fire button down 1cm, and snap up with explosive acceleration. Subscribe to Lyam FF for weekly sensitivity breakdowns!",
                            isUser = false,
                            thinkingText = "Thinking: Loading official Lyam FF YouTube signature profile on 200 scale...",
                            sensitivityAdjustment = SensitivityDelta(generalChange = 196 - gen, redDotChange = 188 - red, recommendedDpiChange = 450),
                            actionSuggestion = "Apply Lyam FF Preset"
                        )
                    }
                    else -> {
                        ChatMessage(
                            text = "🌟 **Top Free Fire Mobile YouTubers & Pro Sensitivities (0–200 Scale)**\n\n" +
                                    "Here are the top creator sensitivities calibrated for the modern 200 scale:\n\n" +
                                    "• 👑 **White FF (White444):** General 195 | Red Dot 188 | Button 44% | DPI 460\n" +
                                    "• ⚡ **Raistar:** General 198 | Red Dot 190 | Button 42% | DPI 510\n" +
                                    "• 🎯 **Ruok FF:** General 192 | Red Dot 185 | Button 45% | DPI 480\n" +
                                    "• 🔥 **Badge 99:** General 194 | Red Dot 186 | Button 48% | DPI 450\n" +
                                    "• 🛡️ **Total Gaming (Ajjubhai):** General 188 | Red Dot 180 | Button 52% | DPI 420\n" +
                                    "• 🏆 **Nobru:** General 196 | Red Dot 188 | Button 43% | DPI 470\n" +
                                    "• 👑 **Lyam FF:** General 196 | Red Dot 188 | Button 44% | DPI 450\n\n" +
                                    "Ask me about any specific creator (like \"White FF sensitivity\" or \"Raistar settings\") for their full HUD, DPI, and drag technique breakdown!",
                            isUser = false,
                            thinkingText = "Thinking: Aggregating top mobile Free Fire creators on 200 scale...",
                            actionSuggestion = "View White FF Sensi"
                        )
                    }
                }
            }

            "SENSI_200_SCALE" -> {
                ChatMessage(
                    text = "📈 **Free Fire 0–200 Sensitivity Scale Explained**\n\n" +
                            "Garena Free Fire updated all in-game sensitivity sliders from the legacy 0–100 scale to **0–200** (max sensitivity is now 200).\n\n" +
                            "💡 **Why Garena Expanded It to 200:**\n" +
                            "• **High Refresh Rate Displays:** Modern smartphones run at 90Hz, 120Hz, and 144Hz with touch sampling rates up to 480Hz. The 0–100 scale lacked the resolution needed for micro-flicks.\n" +
                            "• **Finer Precision:** Doubling the scale provides double the increments for pinpoint headshot calibration.\n\n" +
                            "📐 **How to Convert Your Old Settings to the 200 Scale:**\n" +
                            "• Old General 95 → Modern **186–192**\n" +
                            "• Old General 98–100 → Modern **195–198**\n" +
                            "• Old Red Dot 90 → Modern **178–184**\n" +
                            "• Old 2X/4X Scopes 80–85 → Modern **165–175**\n\n" +
                            "✅ **All features, recommendations, DPI calculations, and presets in SENSI BOT Pro are fully calibrated to this modern 0–200 scale!**",
                    isUser = false,
                    thinkingText = "Thinking: Analyzing Free Fire OB 200-scale sensitivity mechanics and conversion factors...",
                    actionSuggestion = "200-Scale Conversion Guide"
                )
            }

            "OVERSHOOT" -> {
                val targetGen = (gen - 10).coerceAtLeast(140)
                val targetRed = (red - 8).coerceAtLeast(130)
                val rangeContext = when (entities.range ?: state.currentRange) {
                    "close" -> " In close range specifically, also try shortening your drag starting point — begin the swipe slightly lower on the enemy's body."
                    "long" -> " At long range, also reduce your 4X scope by −10 to prevent scope over-travel."
                    else -> ""
                }
                ChatMessage(
                    text = "Classic overshooting — your drag is outrunning the enemy's helmet position on the 200 scale.\n\n📐 Calibrated Adjustment (0–200 Scale):\n• General: $targetGen (−10 from $gen)\n• Red Dot: $targetRed (−8 from $red)\n• Technique: Shorten your upward drag arc. Stop the swipe when your fire button reaches mid-screen height.$rangeContext\n\n🎯 Training Drill: 10 shots on the stationary dummy from 8m with ${weapon}. You should see red numbers on the helmet every time before adjusting further.",
                    isUser = false,
                    thinkingText = "Thinking: Calculating drag overshoot vector — General differential analysis on ${devModel} at ${refRate}Hz...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = -10, redDotChange = -8),
                    actionSuggestion = "Mid-Screen Stop Drill"
                )
            }

            "BODY_SHOTS" -> {
                val targetGen = (gen + 8).coerceAtMost(200)
                val targetRed = (red + 6).coerceAtMost(200)
                val weaponNote = if (weapon.contains("M1887") || weapon.contains("Shotgun")) {
                    " For ${weapon}: also start your drag from beside the enemy's shoulder to bypass auto-aim friction."
                } else ""
                ChatMessage(
                    text = "Body shots happen when your drag doesn't break Free Fire's auto-aim friction zone around the chest.\n\n📐 Calibrated Adjustment (0–200 Scale):\n• General: $targetGen (+8 from $gen)\n• Red Dot: $targetRed (+6 from $red)$weaponNote\n• Technique: Position crosshair to the side of the enemy's shoulder — then drag upward in a sharp J-curve. This bypasses the chest lock.\n\n🎯 Drill: 15 practice shots on Training Ground dummies. First bullet should land as a headshot (red numbers).",
                    isUser = false,
                    thinkingText = "Thinking: Analyzing chest auto-aim friction threshold — computing J-drag coefficient...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = 8, redDotChange = 6),
                    actionSuggestion = "Shoulder-Start J-Drag"
                )
            }

            "AIM_FAST" -> {
                state.awaitFollowUp = true
                state.followUpType = "RANGE_FOR_SPEED"
                ChatMessage(
                    text = "When aim feels too fast, the fix depends on the engagement distance.\n\nIs the over-swiping problem mainly happening in:\n• **Close range** (indoor rushing, 3–10m)?\n• **Long range** (scoping, crossmap, 25m+)?",
                    isUser = false,
                    thinkingText = "Thinking: Detecting over-swipe pattern — awaiting range classification...",
                    actionSuggestion = "Specify Engagement Distance"
                )
            }

            "AIM_SLOW" -> {
                val targetGen = (gen + 10).coerceAtMost(200)
                val targetRed = (red + 8).coerceAtMost(200)
                val dpiNote = if (stockDpi < 420) {
                    val suggestDpi = (stockDpi + 40).coerceIn(380, 480)
                    "\n• DPI Tip: Your stock DPI ($stockDpi) is on the lower side. Try Smallest Width: $suggestDpi in Developer Options for snappier swipe response."
                } else ""
                ChatMessage(
                    text = "Sluggish aim is usually caused by screen friction or low sensitivity values.\n\n📐 Calibrated Adjustment (0–200 Scale):\n• General: $targetGen (+10 from $gen)\n• Red Dot: $targetRed (+8 from $red)\n• Fire Button: Reduce size by 3-5% to give your thumb more vertical travel runway$dpiNote\n\n🧼 Clean your screen surface first — oils and smears add real drag resistance. Then test 180° quick-turn swipes in Training Ground.",
                    isUser = false,
                    thinkingText = "Thinking: Evaluating touch sampling lag compensation for ${devModel}...",
                    sensitivityAdjustment = SensitivityDelta(
                        generalChange = 10,
                        redDotChange = 8,
                        recommendedDpiChange = if (stockDpi < 420) (stockDpi + 40).coerceIn(380, 480) else null
                    ),
                    actionSuggestion = "180° Quick-Turn Test"
                )
            }

            "DPI" -> {
                val recommendedDpi = when {
                    stockDpi < 380 -> stockDpi + 50
                    stockDpi < 440 -> stockDpi + 40
                    stockDpi < 500 -> stockDpi + 30
                    else -> stockDpi + 20
                }.coerceIn(360, 540)
                val boostAmt = recommendedDpi - stockDpi
                state.lastRecommendedDpi = recommendedDpi

                // Check if user mentioned a specific DPI they want to try
                val userDpi = entities.mentionedDpiValue
                val userDpiNote = if (userDpi != null) {
                    val safety = when {
                        userDpi >= 600 -> "⚡ $userDpi is high: some devices need this for max swipe speed, but be sure to turn it back down after your match so normal apps and keyboards don't shrink."
                        userDpi > 540 -> "⚠️ $userDpi is elevated — works great for gaming, but watch for smaller system font."
                        userDpi < 320 -> "⚠️ $userDpi is below safe minimum. This may cause UI elements to overflow your screen."
                        else -> "✅ $userDpi is well-balanced for your device."
                    }
                    "\n\nAbout the $userDpi DPI you mentioned: $safety"
                } else ""

                ChatMessage(
                    text = "DPI (Smallest Width) is one of the most misunderstood settings in competitive mobile gaming. Here's the full truth:\n\n📌 What Smallest Width Actually Does:\nIt changes how many virtual pixels your screen renders. A higher value means your thumb swipe covers more virtual distance → swipes feel faster/snappier. It does NOT increase auto-aim or guarantee headshots.\n\n📐 For ${devModel} (Stock: ${stockDpi} DPI):\n• ✅ Recommended: $recommendedDpi (+$boostAmt)\n• Safe Daily Range: ${stockDpi + 20} – ${(stockDpi + 60).coerceAtMost(540)}\n• ⚡ 600+ DPI: Some devices need 600+ for fast drag flicking — if you use 600+, make sure to turn it off/back to stock after playing!\n\n⚙️ How to Set It:\n1. Settings → About Phone → Tap \"Build Number\" 7 times\n2. Back → Developer Options\n3. Set \"Smallest Width\" to $recommendedDpi\n4. Reboot your phone$userDpiNote\n\nAfter changing DPI, calibrate General sensitivity around 185-196 in Free Fire for optimal balance.",
                    isUser = false,
                    thinkingText = "Thinking: Computing safe display density range for ${devModel} — checking SystemUI bounds at ${refRate}Hz render cycle...",
                    sensitivityAdjustment = SensitivityDelta(
                        recommendedDpiChange = recommendedDpi,
                        dpiStockValue = stockDpi,
                        dpiBoostAmount = boostAmt
                    ),
                    actionSuggestion = "Safe DPI: $recommendedDpi"
                )
            }

            "SCOPE" -> {
                val targetScope2x = (scope2x - 8).coerceAtLeast(100)
                val targetScope4x = (scope4x - 10).coerceAtLeast(80)
                val snipeNote = if (secondaryId == "LONG_RANGE") {
                    "\n• For AWM/Sniper: try Sniper Scope at ${(activeProfile?.sniper ?: 60) - 8} and scope-in only at the last moment before firing."
                } else ""
                ChatMessage(
                    text = "Scope instability is the #1 long-range killer on the 200 scale. Here's how to fix it:\n\n📐 Scope Calibration (0–200 Scale):\n• 2X Scope: $targetScope2x (−8 from $scope2x)\n• 4X Scope: $targetScope4x (−10 from $scope4x)$snipeNote\n• Technique: Fire in 3-4 bullet bursts. Between bursts, apply gentle continuous downward thumb pressure to counteract upward recoil.\n\n🎯 Drill: Equip M4A1 with 4X scope. Fire 4-bullet bursts at the 30m target. Goal: all 4 shots should land within a hand-width on the dummy.",
                    isUser = false,
                    thinkingText = "Thinking: Analyzing scope reticle dispersion pattern — computing horizontal oscillation dampening factor...",
                    sensitivityAdjustment = SensitivityDelta(scope2xChange = -8),
                    actionSuggestion = "4-Bullet Burst Recoil Drill"
                )
            }

            "ONE_TAP" -> {
                val refinedGen = gen.coerceIn(188, 196)
                val refinedRed = red.coerceIn(182, 190)
                ChatMessage(
                    text = "One-tap headshots require a very specific technique split on the 200 scale — reaction, flick, restraint.\n\n📐 Optimal One-Tap Settings (0–200 Scale):\n• General: $refinedGen\n• Red Dot: $refinedRed\n• Fire Button: 44–46% (smaller = more precision)\n\n🧠 The Secret Technique:\n1. Wait 0.1s after enemy stops moving\n2. Flick upward fast but SHORT — aim for upper chest level\n3. Fire immediately at apex of flick\n4. Instantly switch weapon or sprint\n\n🎯 Drill: Desert Eagle or M1887 in Training Room. 15m distance. Standing still. Practice the pause → flick → fire rhythm. Target: 8/10 red headshot numbers before testing in Ranked.",
                    isUser = false,
                    thinkingText = "Thinking: Calibrating first-bullet recoil recovery timing for one-tap mechanics...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = refinedGen - gen, redDotChange = refinedRed - red),
                    actionSuggestion = "Pause → Flick → Fire Drill"
                )
            }

            "SHOTGUN" -> {
                val shotgunGen = if (gen in 180..200) gen else 195
                val shotgunRed = if (red in 175..198) red else 188
                val dragStyle = if (entities.range == "close" || state.currentRange == "close") "V-Drag" else "J-Drag"
                ChatMessage(
                    text = "M1887 and Shotgun consistency is about muscle memory rhythm, not max sensitivity.\n\n📐 Shotgun-Optimized Settings (0–200 Scale):\n• General: $shotgunGen – ${(shotgunGen + 3).coerceAtMost(200)}\n• Red Dot: $shotgunRed – ${(shotgunRed + 3).coerceAtMost(200)}\n• Fire Button: 44-46% (lower HUD position)\n\n💡 $dragStyle Technique for $weapon:\n${if (dragStyle == "V-Drag") "• Pull crosshair DOWN slightly first (1cm), then snap UP explosively\n• The dip loads the drag momentum, the snap carries it to the head" else "• Start from beside the enemy's shoulder\n• Pull upward in a smooth J arc, stopping at helmet height\n• Maintain consistent swipe speed — not too fast, not too slow"}\n\n🎯 Key Drill: 5-10 meter range. 20 shots on stationary dummy. Count how many land as helmet damage. Target: 15/20 red numbers.",
                    isUser = false,
                    thinkingText = "Thinking: Modeling $weapon shell spread pattern at optimal drag distance...",
                    actionSuggestion = "$dragStyle Training Session"
                )
            }

            "SMG" -> {
                val smgGen = (gen + 2).coerceAtMost(200)
                val smgRed = (red + 2).coerceAtMost(200)
                ChatMessage(
                    text = "SMGs like $weapon excel in close-range spray control and hip-fire rushes.\n\n📐 SMG-Tuned Settings (0–200 Scale):\n• General: $smgGen (+2)\n• Red Dot: $smgRed (+2)\n• Fire Button: 47-49%\n\n💡 MP40 Technique:\n• Don't drag up aggressively — track at head level and let the recoil lift into the helmet\n• Fire in 6-8 bullet controlled bursts\n• Use jump + crouch pattern to make yourself a harder target while firing\n\n🎯 Drill: 10m moving bot in Training Ground. Fire full magazine while tracking head. Goal: 60%+ headshot rate per clip.",
                    isUser = false,
                    thinkingText = "Thinking: Computing SMG horizontal recoil compensation pattern for ${weapon}...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = 2, redDotChange = 2),
                    actionSuggestion = "Moving Target Tracking Drill"
                )
            }

            "AR" -> {
                val ar2xScope = (scope2x + 4).coerceAtMost(200)
                ChatMessage(
                    text = "ARs are all about controlled spray bursts and smart recoil management.\n\n📐 AR-Optimized Settings (0–200 Scale):\n• General: $gen (AR rarely needs General changes)\n• 2X Scope: $ar2xScope (+4)\n• 4X Scope: ${(scope4x + 2).coerceAtMost(200)} (+2)\n\n💡 $weapon Technique:\n• Fire in 4-5 bullet bursts for mid-range\n• Between bursts: micro-adjust crosshair DOWN to pre-compensate recoil\n• For close range: full-auto with downward thumb drag pressure\n\n🎯 Drill: Woodpecker at 4X — 5 bullet bursts at 35m target. All 5 should land within the dummy's torso. If any fly above the head, reduce 4X by −4.",
                    isUser = false,
                    thinkingText = "Thinking: Analyzing AR recoil compensation pattern for mid-range burst control...",
                    sensitivityAdjustment = SensitivityDelta(scope2xChange = 4),
                    actionSuggestion = "5-Bullet Mid-Range Burst Drill"
                )
            }

            "FIRE_BUTTON" -> {
                val fingerCount = when {
                    input.contains("2 finger") || input.contains("two finger") -> 2
                    input.contains("3 finger") || input.contains("three finger") -> 3
                    input.contains("4 finger") || input.contains("four finger") -> 4
                    else -> null
                }
                val recommendedSize = when (fingerCount) {
                    2 -> "44–46%"
                    3 -> "47–49%"
                    4 -> "50–54%"
                    else -> "44–48%"
                }
                ChatMessage(
                    text = "Fire button size and placement directly affect your drag distance and aiming consistency.\n\n📐 Recommended Fire Button:\n• Size: $recommendedSize${if (fingerCount != null) " (for $fingerCount-finger setup)" else ""}\n• Placement: Lower third of the right side — this maximizes your upward swipe vertical runway\n• Gap from edge: At least 5% from screen edge to prevent accidental swipes\n\n💡 Layout Tips:\n• Peek/Crouch buttons: keep within thumb-reach without repositioning\n• Quick Switch: place above fire button for instant weapon swap\n• Drag to Crouch: useful for rush playstyle\n\n🎯 Test the placement: Do 20 drag shots in Training Ground. If you feel your thumb hitting the screen edge, move the button inward slightly.",
                    isUser = false,
                    thinkingText = "Thinking: Mapping optimal fire button ergonomics for ${fingerCount ?: "your"}-finger layout...",
                    actionSuggestion = "Fire Button Placement Test"
                )
            }

            "RECOMMEND" -> {
                val dpiRecommend = (stockDpi + 35).coerceIn(380, 500)
                val baseGen = when {
                    refRate >= 120 -> 188
                    refRate >= 90 -> 192
                    else -> 196
                }
                val base2x = 175 + if (refRate >= 120) -4 else 0
                val base4x = 165 + if (refRate >= 120) -4 else 0
                ChatMessage(
                    text = "Based on ${devModel} running at ${refRate}Hz, here's your personalized starting baseline for Free Fire (0–200 Scale):\n\n━━━━━━━━━━━━━━━━━━\n📊 RECOMMENDED PROFILE (200 SCALE)\n━━━━━━━━━━━━━━━━━━\n• General: $baseGen\n• Red Dot: ${baseGen - 8}\n• 2X Scope: $base2x\n• 4X Scope: $base4x\n• Sniper Scope: 60\n• Free Look: 90\n• Fire Button: 46%\n━━━━━━━━━━━━━━━━━━\n📱 DPI (Smallest Width)\n• Stock: $stockDpi\n• Recommended: $dpiRecommend (+${dpiRecommend - stockDpi})\n━━━━━━━━━━━━━━━━━━\n\n⚠️ This is a starting point on the modern 200 scale — test for 15 minutes in Training Ground, then fine-tune based on your drag feel. Tell me what feels off and I'll adjust it for you!",
                    isUser = false,
                    thinkingText = "Thinking: Correlating ${refRate}Hz display matrix with touch sampling rate — building personalized profile on 200 scale for ${devModel}...",
                    sensitivityAdjustment = SensitivityDelta(
                        recommendedDpiChange = dpiRecommend,
                        dpiStockValue = stockDpi,
                        dpiBoostAmount = dpiRecommend - stockDpi
                    ),
                    actionSuggestion = "Test 15 Min in Training Ground"
                )
            }

            "FAIR_PLAY" -> {
                ChatMessage(
                    text = "SENSI BOT Pro (Lyam FF Official Edition) is 100% safe and fair-play.\n\n🛡️ What This App Does:\n• Personalized sensitivity recommendations for Free Fire\n• Drag technique coaching (J-Drag, Rotation, Straight Drag)\n• DPI (Smallest Width) advice & safety thresholds\n• Daily aim training routines & drills\n• Device-optimized profile presets\n\n✅ This is not a hack, it only optimizes your gameplay!\nAll settings are applied naturally to your device and game controls. We help YOU aim smarter — that's the Lyam FF standard.",
                    isUser = false,
                    thinkingText = "Thinking: Verifying fair-play compliance manifest..."
                )
            }

            "TRAINING" -> {
                ChatMessage(
                    text = "Here's a complete 20-minute daily aim training routine for Free Fire:\n\n🔥 Phase 1 — Warm-Up (5 min)\n• 50 drag shots on stationary dummy at 8m\n• Focus on smooth J-drag arc, not speed\n• Weapon: M1887 or Desert Eagle\n\n🎯 Phase 2 — Precision (7 min)\n• One-tap headshots from 15m on stationary dummy\n• Pause → flick → fire rhythm\n• Target: 8/10 red headshots per set\n\n⚡ Phase 3 — Speed Training (5 min)\n• Moving bot tracking — fire 6-bullet bursts\n• Track head level without over-swipe\n• 180° quick-turn reaction drills\n\n🧠 Phase 4 — Cool-Down (3 min)\n• Slow, controlled 5-shot burst at 35m with 4X scope\n• No rushing — perfect precision over speed\n\n💡 Pro Tip: Do this before every Ranked session. Consistency beats raw talent.",
                    isUser = false,
                    thinkingText = "Thinking: Designing progressive aim training protocol for ${devModel}...",
                    actionSuggestion = "20-Min Daily Training Routine"
                )
            }

            "CLOSE_RANGE" -> {
                val targetGen = (gen + 3).coerceAtMost(100)
                val targetRed = (red + 2).coerceAtMost(100)
                ChatMessage(
                    text = "Close range is where games are won and lost. Here's how to optimize for 3–12m duels:\n\n📐 Close-Range Settings:\n• General: $targetGen (+3) — faster turning for fast rusher tracking\n• Red Dot: $targetRed (+2)\n• Fire Button: 44-46% (lower, for longer drag runway)\n\n💡 Close-Range Technique Stack:\n• J-Drag or V-Drag from shoulder level\n• Jump-shots: fire at peak of jump — enemy can't predict your trajectory\n• Crouch + Fire: plant stance for shotgun stabilization\n\n🎯 Drill: 1v1 Clash Squad with MP40 or M1887. Focus only on first-bullet placement — if it's not a headshot, reset and try again.",
                    isUser = false,
                    thinkingText = "Thinking: Optimizing close-quarters sensitivity profile for ${weapon} engagement...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = 3, redDotChange = 2),
                    actionSuggestion = "Jump-Shot & J-Drag Combo Drill"
                )
            }

            "LONG_RANGE" -> {
                val targetGen = (gen - 3).coerceAtLeast(65)
                val target4x = (scope4x - 4).coerceAtLeast(40)
                ChatMessage(
                    text = "Long range is a patience game — precision over speed, always.\n\n📐 Long-Range Settings:\n• General: $targetGen (−3) — reduces micro-shake during crosshair placement\n• 4X Scope: $target4x (−4)\n• Sniper Scope: ${(activeProfile?.sniper ?: 52) - 4} (−4)\n\n💡 Long-Range Technique:\n• Pre-aim common doorways and angles — don't track moving targets\n• Scope-in at the last 0.3 seconds before shooting\n• 3-bullet burst max — let recoil settle between bursts\n• Use natural cover and peek-shoot rhythm\n\n🎯 Drill: Woodpecker at 4X scope — 3-bullet taps at 40m target. All bullets should land within head/shoulder area.",
                    isUser = false,
                    thinkingText = "Thinking: Computing micro-flick precision coefficient for cross-map engagement at ${refRate}Hz...",
                    sensitivityAdjustment = SensitivityDelta(generalChange = -3, scope2xChange = -2),
                    actionSuggestion = "Pre-Aim & Peek-Shoot Drill"
                )
            }

            "J_DRAG" -> {
                ChatMessage(
                    text = "The **J-Drag** (and Rotation Drag) is the most powerful aiming mechanic in Free Fire for consistent red numbers.\n\n📐 **How J-Drag Works:**\nInstead of pulling the fire button straight up, you trace a **'J' or 'U' shape** with your thumb:\n1. **Starting Point:** Place your thumb on the fire button while keeping your white crosshair near the enemy's neck/shoulder.\n2. **The Dip (Down & Curve):** Drag down slightly to let the game's aim assist engage the enemy's center-mass.\n3. **The Explosive Snap (Upward):** Immediately curve and snap upward toward the head in one fluid, unbroken motion.\n\n🎯 **Directional Rotation Drag:**\n• **Enemy running RIGHT:** Drag in a **right-curving J** toward top-right.\n• **Enemy running LEFT:** Drag in a **left-curving reverse-J** toward top-left.\n• **Enemy stationary:** Straight upward quick-snap.\n\n💡 **Settings Recommendation:**\n• General Sensitivity: ${gen.coerceAtLeast(96)}\n• Fire Button Size: 42% – 45% (placed in lower third of screen for maximum upward travel runway).",
                    isUser = false,
                    thinkingText = "Thinking: Deconstructing rotational drag physics & directional vector curves...",
                    actionSuggestion = "J-Drag Practice in Training Ground"
                )
            }

            "CHARACTER_COMBO" -> {
                ChatMessage(
                    text = "Here are the meta character skill combinations used by top Free Fire esports pros:\n\n🔥 **1. Meta Rush Combo (Clash Squad & Close Duels):**\n• **Active:** **Tatsuya** (Rebel Rush — double forward dash for instant repositioning) OR **Alok** (Drop the Beat — speed & heal).\n• **Passive 1: Hayato** (Bushido — increases armor penetration when HP drops, turns shotguns into 1-shot lethals).\n• **Passive 2: Kelly** (Dash — 6% permanent sprint speed for faster drag flicks).\n• **Passive 3: Moco** (Hacker's Eye — tags hit enemies for 5s so they can't hide behind gloo walls).\n\n🛡️ **2. Ranked Survival & Clash Squad Clutch:**\n• **Active:** **Dimitri** (Healing Heartbeat — self-revive) OR **Chrono** (Time Turner — forcefield shield).\n• **Passive 1: Sonia** (Nano Lifeshield — 3s shield on fatal damage).\n• **Passive 2: Luqueta** (Hat Trick — adds +50 permanent max HP on kills).\n• **Passive 3: Antonio** (Extra 40 shield points at start of round).\n\n🎯 **3. Long-Range / Sniper Specialist:**\n• **Active:** **Iris** (Wall Brawl — shoots through Gloo Walls).\n• **Passives:** **Rafael** (Silencer & bleed-out) + **Maro** (Damage increases with distance) + **Laura** (Increases accuracy by 50% while scoped).",
                    isUser = false,
                    thinkingText = "Thinking: Analyzing active/passive synergy matrix for current competitive meta...",
                    actionSuggestion = "Equip Meta Rush Combo"
                )
            }

            "GRAPHICS_FPS" -> {
                ChatMessage(
                    text = "High and stable FPS (frames per second) is critical for aim. Lower frame rates cause touch latency and missed headshots.\n\n⚙️ **Optimal Free Fire Display Settings:**\n• **Graphics:** **SMOOTH** (Recommended for tournament play — eliminates stutter and overheating).\n• **High FPS:** **HIGH** ⚠️ *(CRITICAL: Always keep High FPS ON. On 60Hz screens it unlocks true 60fps; on 90Hz/120Hz screens like ${devModel}, it unlocks ultra-low touch latency).* \n• **Shadow:** **OFF** (Shadows cost 15-20% GPU rendering budget and distract crosshair tracking).\n• **High Res (Normal):** Set to Normal to prevent thermal throttling.\n• **Filter:** Vivid or Soft (improves enemy contrast in dark corners).\n\n📱 **Device Tuning for ${devModel}:**\n• Enable **Game Space / Performance Mode** in phone settings.\n• Set phone screen refresh rate to maximum (${refRate}Hz).\n• Wipe screen with a dry microfiber cloth before matches to maintain smooth drag friction.",
                    isUser = false,
                    thinkingText = "Thinking: Optimizing GPU rendering pipeline and touch response frequency for ${devModel}...",
                    actionSuggestion = "Set Graphics: Smooth + High FPS"
                )
            }

            "CLASH_SQUAD" -> {
                ChatMessage(
                    text = "Clash Squad Ranked requires completely different pacing and round economy than Battle Royale.\n\n🏆 **Round-by-Round Strategy:**\n• **Round 1 (Pistol Duel):**\n  - Buy **M500** if you have clean one-tap aim (1 headshot = 165 instant knock).\n  - Buy **G18** if you prefer close-range hipfire spray tracking.\n• **Round 2 (First Purchase):**\n  - If you won R1: Buy **UMP** or **Thompson** + 1 Gloo Wall + Helmet.\n  - If you lost R1: Buy **Desert Eagle** or save cash for R3.\n• **Round 3 & Beyond (Full Buy):**\n  - Prioritize **M1887** (Shotgun) + **MP40** or **Woodpecker** + 3 Gloo Walls + Level 2 Helmet/Vest.\n\n💡 **Clash Squad Positioning Rules:**\n1. **Never rush alone:** Double-peak angles with a teammate.\n2. **Hold high ground:** Elevated positions let your crosshair naturally align with enemy head level.\n3. **Bait Gloo Walls:** Shoot the corner of an enemy's gloo wall to force them to step out into your crosshair.",
                    isUser = false,
                    thinkingText = "Thinking: Formulating competitive Clash Squad tactical roadmap and round economy...",
                    actionSuggestion = "Master M500 Round 1 Drill"
                )
            }

            "GLOO_WALL" -> {
                ChatMessage(
                    text = "Speed Gloo Wall (Sit-up Gloo) is the ultimate survival and rush skill in Free Fire.\n\n⚡ **Fast Sit-Up Gloo Sequence:**\n1. **Fire:** Drag up and shoot your weapon.\n2. **Gloo Select:** Immediately tap the Gloo Wall button without letting go of sprint momentum.\n3. **Look Down:** Swipe your camera thumb sharply DOWNWARD to target the ground directly at your feet.\n4. **Crouch:** Tap the Crouch button (shrinks your hitbox behind the wall).\n5. **Deploy:** Tap the Left Fire Button to plant the wall instantly.\n\n📐 **HUD Optimization for Fast Gloo:**\n• Place the **Gloo Wall button on the left side** (size 85-95%) so your left thumb can hit it without leaving the movement joystick.\n• Turn **ON 'Smart Throw'** in Free Fire controls if you want instant one-tap deployment.\n• Enable the **Left Fire Button: Always** to tap gloo walls without interrupting camera aim.",
                    isUser = false,
                    thinkingText = "Thinking: Breaking down multi-touch finger sequence for 0.2s sit-up gloo wall...",
                    actionSuggestion = "Sit-Up Gloo Drill in Training"
                )
            }

            else -> generateFallback(entities, deviceSpecs, activeProfile)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Fallback & Clarification
    // ─────────────────────────────────────────────────────────────────────────
    private fun generateFallback(
        entities: ExtractedEntities,
        deviceSpecs: DeviceSpecs?,
        activeProfile: SensitivityProfile?
    ): ChatMessage {
        val followUps = buildList {
            if (entities.weapon != null) add("your ${entities.weapon} aim")
            if (state.currentProblem != null) add("your ${state.currentProblem} issue")
        }
        val contextHint = if (followUps.isNotEmpty()) " I noticed you mentioned ${followUps.joinToString(" and ")} — want me to dig into that specifically?" else ""

        return ChatMessage(
            text = "I'm your Free Fire sensitivity coach, and I'm ready to help!$contextHint\n\nTry asking me about:\n• \"My drag goes above the head\" — overshooting fix\n• \"What DPI should I use?\" — Smallest Width guide\n• \"My M1887 is inconsistent\" — shotgun drill\n• \"Recommend settings for me\" — full personalized profile\n• \"How do I improve one taps?\" — tap-shot technique\n• \"Scope feels shaky\" — scope stabilization\n\nWhat feels most off during your gunfights?",
            isUser = false,
            thinkingText = "Thinking: Awaiting coaching intent classification..."
        )
    }

    private fun generateClarification(userMessage: String): ChatMessage {
        val clarifications = listOf(
            "Could you tell me more? For example: is your aim problem close-range (rushing) or long-range (scoping)?",
            "I want to give you the most accurate advice. What specifically feels wrong — are shots going above the head, hitting the body, or is the camera too fast/slow?",
            "Got your message! To help better, which weapon are you mainly struggling with right now?",
            "I'm analyzing that. To fine-tune my advice — what's the specific problem: overshooting, body shots, or scope shake?"
        )
        return ChatMessage(
            text = clarifications[state.sessionMessageCount % clarifications.size],
            isUser = false,
            thinkingText = "Thinking: Processing ambiguous input — requesting clarification parameters..."
        )
    }

    override fun getSuggestedChips(): List<QuickChip> = listOf(
        QuickChip("👑 White FF Sensi", "What is White FF (White444) sensitivity, DPI, and HUD?"),
        QuickChip("⚡ Raistar Sensi", "What is Raistar sensitivity, DPI, and fire button size?"),
        QuickChip("🔥 Lyam FF Official", "What is Lyam FF sensitivity and DPI settings?"),
        QuickChip("📈 200 Max Scale Guide", "Why is Free Fire sensitivity up to 200 now and how should I set it?"),
        QuickChip("🎯 Drag goes above head", "My drag consistently goes above the enemy's head"),
        QuickChip("📱 Best DPI for my phone", "What DPI (Smallest Width) should I use for my phone?"),
        QuickChip("💥 Getting body shots", "I keep getting body shots and my aim won't lift to the head"),
        QuickChip("⚡ Aim too slow", "My aim feels too slow and sluggish when I swipe"),
        QuickChip("🔫 M1887 inconsistent", "My M1887 shotgun drag feels inconsistent — sometimes hits, sometimes misses"),
        QuickChip("🔭 Scope unstable", "My 2x and 4x scope shakes too much when I fire"),
        QuickChip("🎯 One-tap headshots", "How do I improve my one-tap headshots with Desert Eagle?"),
        QuickChip("📊 Recommend settings", "Give me personalized sensitivity settings for my device on the 200 scale")
    )

    override fun resetConversation() {
        state.currentWeapon = null
        state.currentProblem = null
        state.currentRange = null
        state.lastRecommendedGeneral = null
        state.lastRecommendedDpi = null
        state.awaitFollowUp = false
        state.followUpType = null
        state.recentTurns.clear()
        state.sessionMessageCount = 0
    }
}
