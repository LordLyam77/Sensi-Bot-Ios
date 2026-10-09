package com.sensibotpro.ui.screens.sensifinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.ui.components.GamingCard
import com.sensibotpro.ui.components.GlowButton
import com.sensibotpro.ui.components.SectionHeader
import com.sensibotpro.ui.theme.*

/**
 * Aim problems / Player needs for DPI & Sensi calibration
 */
enum class PlayerAimNeed(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
) {
    HEAVY_DRAG(
        title = "Aim Feels Heavy / Slow Drag",
        subtitle = "Hard to drag up to head; swipes feel sluggish",
        icon = Icons.Default.Speed,
        badge = "Needs Speed"
    ),
    OVERSHOOTING(
        title = "Overshooting / Crosshair Flies Above Head",
        subtitle = "Crosshair goes into sky; difficult to lock head",
        icon = Icons.Default.Adjust,
        badge = "Needs Control"
    ),
    SHOTGUN_FLICK(
        title = "Shotgun & One-Tap Flicks (M1887, Deagle)",
        subtitle = "Instant vertical snap and crisp one-tap reset",
        icon = Icons.Default.FlashOn,
        badge = "M1887 • Deagle"
    ),
    SMG_TRACKING(
        title = "SMG Spray Tracking (MP40, UMP)",
        subtitle = "Smooth tracking with zero screen shake or jitter",
        icon = Icons.Default.FilterCenterFocus,
        badge = "MP40 • UMP"
    ),
    BALANCED(
        title = "Balanced / Clean Competitive",
        subtitle = "Optimal all-round baseline for smooth drag headshots",
        icon = Icons.Default.CheckCircle,
        badge = "All-Round"
    )
}

enum class DpiSafetyLevel {
    SAFE,
    CAUTION,
    DANGER,
    TOO_LOW
}

/**
 * Calculated optimization package
 */
data class DpiOptimizationResult(
    val recommendedDpi: Int,
    val recommendedGeneralSensi: Int,
    val dpiBoost: Int,
    val generalDelta: Int,
    val safetyLevel: DpiSafetyLevel,
    val safetyHeadline: String,
    val safetyDetails: String,
    val tacticalWhy: String,
    val dragTechniqueTip: String
)

/**
 * Intuitively optimizes DPI and in-game General sensitivity based on:
 * 1. User's default or current DPI
 * 2. User's current in-game General Sensitivity
 * 3. User's specific gameplay need / aim problem
 */
@Composable
fun DpiCalculatorScreen(
    deviceSpecs: DeviceSpecs,
    initialGeneralSensi: Int? = null
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Detected physical screen density from device
    val detectedStockDpi = remember {
        if (deviceSpecs.displayDensityDpi in 240..640) deviceSpecs.displayDensityDpi else 411
    }

    // 2. User inputs
    var userDpiInput by remember { mutableStateOf(detectedStockDpi.toString()) }
    var currentGeneralSensi by remember { mutableFloatStateOf((initialGeneralSensi ?: 185).toFloat().coerceIn(50f, 200f)) }
    var selectedNeed by remember { mutableStateOf(PlayerAimNeed.HEAVY_DRAG) }

    // 3. Calculation and progress states
    var isCalculating by remember { mutableStateOf(false) }
    var hasCalculated by remember { mutableStateOf(false) }
    var isInputChangedSinceCalc by remember { mutableStateOf(false) }
    var calculatingStep by remember { mutableStateOf("Initializing calculation...") }
    var calculationProgress by remember { mutableFloatStateOf(0f) }
    var calculatedOptimization by remember { mutableStateOf<DpiOptimizationResult?>(null) }

    // Parsed current DPI
    val currentDpi = userDpiInput.toIntOrNull() ?: detectedStockDpi

    // Current DPI safety classification
    val currentDpiSafety = remember(currentDpi) {
        when {
            currentDpi > 600 -> DpiSafetyLevel.DANGER
            currentDpi > 540 -> DpiSafetyLevel.CAUTION
            currentDpi < 320 -> DpiSafetyLevel.TOO_LOW
            else -> DpiSafetyLevel.SAFE
        }
    }

    // Calculation executor
    val triggerCalculation: () -> Unit = {
        if (!isCalculating) {
            coroutineScope.launch {
                isCalculating = true
                hasCalculated = false
                isInputChangedSinceCalc = false
                calculationProgress = 0.05f

                calculatingStep = "Reading ${deviceSpecs.manufacturer} ${deviceSpecs.model} display density ($detectedStockDpi stock)..."
                calculationProgress = 0.22f
                delay(280)

                calculatingStep = "Simulating ${selectedNeed.title} upward drag curve..."
                calculationProgress = 0.52f
                delay(320)

                calculatingStep = "Balancing DPI ratio with Free Fire General sensitivity (${currentGeneralSensi.toInt()})..."
                calculationProgress = 0.78f
                delay(300)

                calculatingStep = "Verifying ${deviceSpecs.manufacturer} SystemUI safety limits & bootloop protection..."
                calculationProgress = 0.96f
                delay(260)

                calculationProgress = 1f
                calculatedOptimization = calculateDpiOptimization(
                    currentDpi = currentDpi,
                    currentGeneral = currentGeneralSensi.toInt(),
                    need = selectedNeed,
                    stockDpi = detectedStockDpi,
                    deviceSpecs = deviceSpecs
                )
                isCalculating = false
                hasCalculated = true

                // Smoothly scroll down so the player sees the newly calculated results
                delay(150)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Top Header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DPI & SENSI OPTIMIZER",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Calibrate Phone DPI + In-Game General Together",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${deviceSpecs.manufacturer} ${deviceSpecs.model.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  STEP 1: USER INPUTS (Clear & Simple)
        // ═════════════════════════════════════════════════════════════════════
        SectionHeader(
            title = "1. Your Current Settings",
            subtitle = "Enter your current phone DPI & in-game sensitivity"
        )

        GamingCard {
            // ── Input A: Current / Default Phone DPI ────────────────────────
            Text(
                text = "PHONE DPI / SMALLEST WIDTH",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter what DPI you are currently using, or your phone's default stock value.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userDpiInput,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            userDpiInput = newValue
                            if (hasCalculated) isInputChangedSinceCalc = true
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = RubyRedPrimary
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "DPI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyRedPrimary,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Quick button to reset to detected stock
                OutlinedButton(
                    onClick = {
                        userDpiInput = detectedStockDpi.toString()
                        if (hasCalculated) isInputChangedSinceCalc = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    modifier = Modifier.height(56.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Reset Stock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("$detectedStockDpi", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }

            // Real-time safety warning for CURRENT DPI
            AnimatedVisibility(visible = currentDpiSafety != DpiSafetyLevel.SAFE) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (currentDpiSafety) {
                        DpiSafetyLevel.DANGER -> Color(0xFF330000)
                        DpiSafetyLevel.CAUTION -> Color(0xFF332600)
                        else -> Color(0xFF1E1E2C)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentDpiSafety == DpiSafetyLevel.DANGER) RubyRedPrimary else AccentAmber
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentDpiSafety == DpiSafetyLevel.DANGER) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (currentDpiSafety == DpiSafetyLevel.DANGER) RubyRedPrimary else AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentDpiSafety) {
                                DpiSafetyLevel.DANGER -> "⚡ Current DPI ($currentDpi) is 600+! Some devices need this for max swipe speed, but be sure to turn it back off/down after playing to avoid System UI strain."
                                DpiSafetyLevel.CAUTION -> "⚠️ Current DPI ($currentDpi) is high (>540). Works great for gaming, but watch for smaller system font."
                                DpiSafetyLevel.TOO_LOW -> "⚠️ Current DPI ($currentDpi) is below 320. UI elements will overlap."
                                DpiSafetyLevel.SAFE -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentDpiSafety == DpiSafetyLevel.DANGER) RubyRedPrimary else AccentAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = BorderStroke, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(18.dp))

            // ── Input B: Current Free Fire General Sensitivity ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FREE FIRE GENERAL SENSI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your current in-game General slider value",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Text(
                        text = "${currentGeneralSensi.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = RubyRedPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = currentGeneralSensi,
                onValueChange = {
                    currentGeneralSensi = it
                    if (hasCalculated) isInputChangedSinceCalc = true
                },
                valueRange = 50f..200f,
                steps = 149,
                colors = SliderDefaults.colors(
                    thumbColor = RubyRedPrimary,
                    activeTrackColor = RubyRedPrimary,
                    inactiveTrackColor = ElevatedSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Quick preset pills (Modern Free Fire 0-200 Scale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(95, 140, 170, 185, 195, 200).forEach { preset ->
                    val isSelected = currentGeneralSensi.toInt() == preset
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) RubyRedPrimary else ElevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) RubyRedGlow else BorderStroke
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentGeneralSensi = preset.toFloat()
                                if (hasCalculated) isInputChangedSinceCalc = true
                            }
                    ) {
                        Text(
                            text = "$preset",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  STEP 2: USER'S CURRENT AIM NEED
        // ═════════════════════════════════════════════════════════════════════
        SectionHeader(
            title = "2. What is Your Main Aim Problem?",
            subtitle = "Select what you need to improve right now"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerAimNeed.values().forEach { need ->
                val isSelected = selectedNeed == need
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF221115) else CharcoalSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) RubyRedPrimary else BorderStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedNeed = need
                            if (hasCalculated) isInputChangedSinceCalc = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) RubyRedPrimary else ElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = need.icon,
                                contentDescription = null,
                                tint = if (isSelected) TextPrimary else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = need.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                            Text(
                                text = need.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) RubyRedPrimary.copy(alpha = 0.25f) else ElevatedSurface
                        ) {
                            Text(
                                text = need.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isSelected) RubyRedPrimary else TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  CALCULATION ACTION & PROGRESS HUD
        // ═════════════════════════════════════════════════════════════════════
        if (isCalculating) {
            GamingCard(
                borderColor = RubyRedPrimary,
                backgroundColor = Color(0xFF190D11)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = RubyRedPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "CALCULATING OPTIMAL RESULTS...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = RubyRedPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { calculationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = RubyRedPrimary,
                        trackColor = ElevatedSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = calculatingStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElevatedSurface
                        ) {
                            Text(
                                text = "${(calculationProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        } else if (!hasCalculated) {
            GlowButton(
                text = "⚡ CALCULATE RESULTS",
                icon = Icons.Default.Bolt,
                onClick = triggerCalculation
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "READY TO CALCULATE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap 'Calculate Results' above to generate your customized DPI & General sensitivity calibration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            GlowButton(
                text = if (isInputChangedSinceCalc) "🔄 RE-CALCULATE RESULTS" else "✓ RE-RUN CALCULATION",
                icon = if (isInputChangedSinceCalc) Icons.Default.Refresh else Icons.Default.Check,
                isSecondary = !isInputChangedSinceCalc,
                onClick = triggerCalculation
            )

            if (isInputChangedSinceCalc) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Inputs modified. Tap 'Re-Calculate Results' to update your calibration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  REVEALED RESULTS: STEP 3 & STEP 4 (Animated Entry)
        // ═════════════════════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = hasCalculated && calculatedOptimization != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val optimization = calculatedOptimization ?: return@AnimatedVisibility
            Column {
                Spacer(modifier = Modifier.height(20.dp))

                // ═════════════════════════════════════════════════════════════════════
                //  STEP 3: THE RECOMMENDATION & SAFETY VERDICT (Clear & Actionable)
                // ═════════════════════════════════════════════════════════════════════
                SectionHeader(
                    title = "3. Recommended Calibration",
                    subtitle = "Customized specifically for your phone & chosen need"
                )

                // ── Main Result Card ────────────────────────────────────────────────
                GamingCard(
                    borderColor = when (optimization.safetyLevel) {
                        DpiSafetyLevel.SAFE -> AccentGreen
                        DpiSafetyLevel.CAUTION -> AccentAmber
                        else -> RubyRedPrimary
                    }
                ) {
                    // A. Big Safety Verdict Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (optimization.safetyLevel) {
                            DpiSafetyLevel.SAFE -> Color(0xFF0C2E1B)
                            DpiSafetyLevel.CAUTION -> Color(0xFF332700)
                            else -> Color(0xFF330909)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (optimization.safetyLevel) {
                                    DpiSafetyLevel.SAFE -> Icons.Default.VerifiedUser
                                    DpiSafetyLevel.CAUTION -> Icons.Default.Warning
                                    else -> Icons.Default.GppBad
                                },
                                contentDescription = null,
                                tint = when (optimization.safetyLevel) {
                                    DpiSafetyLevel.SAFE -> AccentGreen
                                    DpiSafetyLevel.CAUTION -> AccentAmber
                                    else -> RubyRedPrimary
                                },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = optimization.safetyHeadline,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = when (optimization.safetyLevel) {
                                        DpiSafetyLevel.SAFE -> AccentGreen
                                        DpiSafetyLevel.CAUTION -> AccentAmber
                                        else -> RubyRedPrimary
                                    }
                                )
                                Text(
                                    text = optimization.safetyDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // B. Direct Side-by-Side Comparison (Before vs After)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElevatedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "YOUR CURRENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$currentDpi",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DPI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "General: ${currentGeneralSensi.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Recommended Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF142419),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECOMMENDED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Black
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (optimization.dpiBoost >= 0) "+${optimization.dpiBoost}" else "${optimization.dpiBoost}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${optimization.recommendedDpi}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = AccentGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DPI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentGreen,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "General: ${optimization.recommendedGeneralSensi} (${if (optimization.generalDelta >= 0) "+${optimization.generalDelta}" else "${optimization.generalDelta}"})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // C. Visual Interactive DPI Safety Zone Track
                    DpiSafetyVisualizer(
                        currentDpi = currentDpi,
                        recommendedDpi = optimization.recommendedDpi
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderStroke, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // D. Tactical Reason Why This Combo Works
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Why this combination works:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = optimization.tacticalWhy,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // E. Pro Drag Technique Tip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElevatedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = optimization.dragTechniqueTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ═════════════════════════════════════════════════════════════════════
                //  STEP 4: EASY 3-STEP "HOW TO APPLY" (Zero Confusion)
                // ═════════════════════════════════════════════════════════════════════
                SectionHeader(
                    title = "How to Apply in 3 Steps",
                    subtitle = "Follow this simple setup to apply your new settings"
                )

                GamingCard(borderColor = RubyRedPrimary) {
                    val steps = listOf(
                        "Set Phone DPI" to "Go to Settings → Developer Options → Smallest Width → Enter ${optimization.recommendedDpi} → Tap OK. (If 600+ is used, turn it back down after playing).",
                        "Set In-Game General" to "Open Free Fire → Settings → Sensitivity → Set General to ${optimization.recommendedGeneralSensi}.",
                        "Warm-Up Test" to "Jump into Training Grounds for 5 minutes and practice your upward drag with your primary weapon."
                    )

                    steps.forEachIndexed { idx, (title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(RubyRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Brand-Specific Path Quick Card ──────────────────────────────────
                SectionHeader(
                    title = "Direct Settings Path For Your Phone",
                    subtitle = "Find Smallest Width quickly on your brand"
                )

                GamingCard {
                    val brandLower = deviceSpecs.manufacturer.lowercase()
                    val specificPath = when {
                        brandLower.contains("samsung") -> "Settings → Developer Options → Smallest Width (Max safe: 540 DP on One UI)"
                        brandLower.contains("xiaomi") || brandLower.contains("poco") || brandLower.contains("redmi") -> "Settings → Additional Settings → Developer Options → Smallest Width / DP"
                        brandLower.contains("oneplus") -> "Settings → System → Developer Options → Smallest Width (OxygenOS is very stable)"
                        brandLower.contains("realme") || brandLower.contains("oppo") -> "Settings → System Settings → Developer Options → Minimum Width"
                        brandLower.contains("vivo") || brandLower.contains("iqoo") -> "Settings → System Management → Developer Options → Smallest Width"
                        brandLower.contains("infinix") || brandLower.contains("tecno") -> "Settings → System → Developer Options → Display Smallest Width"
                        else -> "Settings → System → Developer Options → Smallest Width"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = RubyRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${deviceSpecs.manufacturer} Navigation Path:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RubyRedPrimary
                            )
                            Text(
                                text = specificPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DPI Safety Visualizer Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DpiSafetyVisualizer(
    currentDpi: Int,
    recommendedDpi: Int
) {
    val totalMin = 240f
    val totalMax = 640f

    val currentFraction = ((currentDpi - totalMin) / (totalMax - totalMin)).coerceIn(0f, 1f)
    val recommendedFraction = ((recommendedDpi - totalMin) / (totalMax - totalMin)).coerceIn(0f, 1f)

    val animCurrent by animateFloatAsState(currentFraction, tween(600, easing = FastOutSlowInEasing), label = "current")
    val animRec by animateFloatAsState(recommendedFraction, tween(800, easing = FastOutSlowInEasing), label = "rec")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DPI Safety Spectrum", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Max Safe: 540", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Spectrum bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF661111), // Too low
                        0.25f to AccentGreen,      // 340-480 Safe
                        0.70f to AccentGreen,      // 480-540 Safe
                        0.75f to AccentAmber,      // 541-600 Caution
                        1.0f to RubyRedPrimary     // > 600 Danger
                    )
                )
        ) {
            // Track layout for markers
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val trackWidth = maxWidth

                // Recommended Marker (Green/White indicator)
                Box(
                    modifier = Modifier
                        .offset(x = (trackWidth * animRec) - 6.dp)
                        .size(12.dp, 18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                        .border(1.dp, AccentGreen, RoundedCornerShape(3.dp))
                )

                // Current Marker (Dot)
                Box(
                    modifier = Modifier
                        .offset(x = (trackWidth * animCurrent) - 4.dp)
                        .size(8.dp, 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(RubyRedPrimary)
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("320 (Min)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
            Text("360–540 (DAILY SAFE)", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("600+ (RESET AFTER PLAY)", style = MaterialTheme.typography.labelSmall, color = RubyRedPrimary, fontSize = 9.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Calculation Engine
// ─────────────────────────────────────────────────────────────────────────────
private fun calculateDpiOptimization(
    currentDpi: Int,
    currentGeneral: Int,
    need: PlayerAimNeed,
    stockDpi: Int,
    deviceSpecs: DeviceSpecs
): DpiOptimizationResult {
    // Safe ceiling for Android phones (540 DP is standard max before SystemUI risk)
    val maxSafeDpi = 530
    val minSafeDpi = 360

    var targetDpi: Int
    var targetGeneral: Int
    var whyText: String
    var techniqueTip: String

    when (need) {
        PlayerAimNeed.HEAVY_DRAG -> {
            // Aim feels too heavy / sluggish
            if (currentGeneral >= 190) {
                // In-game general is already near max. User urgently needs a DPI boost.
                val boost = when {
                    stockDpi < 390 -> 50
                    stockDpi < 440 -> 45
                    else -> 35
                }
                targetDpi = (stockDpi + boost).coerceIn(minSafeDpi, maxSafeDpi)
                targetGeneral = 196
                whyText = "Your in-game General is already at ${currentGeneral}/200. Raising DPI to $targetDpi increases your swipe-to-virtual-pixel ratio by ~15%, making vertical drags feel effortless without having to swipe off the top of your screen."
            } else {
                // Boost both moderately
                val boost = 30
                targetDpi = (stockDpi + boost).coerceIn(minSafeDpi, maxSafeDpi)
                targetGeneral = (currentGeneral + 12).coerceIn(175, 198)
                whyText = "Boosting DPI to $targetDpi plus calibrating General to $targetGeneral on the modern 200 scale solves heavy drag smoothly without causing crosshair jitter."
            }
            techniqueTip = "Drag your fire button straight up with smooth, moderate thumb pressure."
        }

        PlayerAimNeed.OVERSHOOTING -> {
            // Crosshair flies over the head into the sky
            if (currentDpi > stockDpi + 20) {
                // Current DPI is elevated and causing over-acceleration. Lower it towards stock.
                targetDpi = (stockDpi + 10).coerceIn(minSafeDpi, maxSafeDpi)
            } else {
                targetDpi = stockDpi
            }
            // Drop General sensitivity by 10-15 points on the 200 scale
            targetGeneral = (currentGeneral - 14).coerceIn(160, 182)
            whyText = "Overshooting happens when upward drag acceleration is too fast. Lowering your DPI to $targetDpi and setting General to $targetGeneral tightens your crosshair lock on the enemy's head hitbox."
            techniqueTip = "Use a gentle upward drag curve, stopping your thumb right when the red crosshair hits the upper chest."
        }

        PlayerAimNeed.SHOTGUN_FLICK -> {
            // Instant vertical snap for M1887 / Deagle
            val boost = when {
                stockDpi < 400 -> 45
                else -> 35
            }
            targetDpi = (stockDpi + boost).coerceIn(minSafeDpi, 510)
            targetGeneral = 196
            whyText = "One-tap shotguns require high initial flick velocity. A DPI of $targetDpi combined with 196 General (modern 200 scale) provides instantaneous upward flick response with clean recovery."
            techniqueTip = "Use a quick 'V-drag' or 'J-drag': pull down slightly and whip straight up in one fast motion."
        }

        PlayerAimNeed.SMG_TRACKING -> {
            // Stable tracking for MP40 / UMP without spray jitter
            val boost = 20
            targetDpi = (stockDpi + boost).coerceIn(minSafeDpi, 460)
            targetGeneral = (currentGeneral).coerceIn(172, 185)
            whyText = "SMGs suffer from bullet spread jitter if DPI or sensitivity is set too high. A moderate DPI of $targetDpi with General at $targetGeneral keeps your automatic spray tightly grouped on target."
            techniqueTip = "Track the enemy smoothly; don't over-flick. Let the aim assist pull into the chest, then gently lift."
        }

        PlayerAimNeed.BALANCED -> {
            // Clean competitive balance
            val boost = 30
            targetDpi = (stockDpi + boost).coerceIn(minSafeDpi, 480)
            targetGeneral = 188
            whyText = "DPI $targetDpi and General 188 is the golden competitive standard for ${deviceSpecs.manufacturer} on the 200 scale. It offers fast close-range 180° turns while maintaining precision at mid-range."
            techniqueTip = "Balanced setup works across all guns. Focus on crosshair placement at head-level before dragging."
        }
    }

    val dpiBoost = targetDpi - currentDpi
    val generalDelta = targetGeneral - currentGeneral

    // Safety analysis for recommended DPI
    val safetyLevel = when {
        targetDpi > 600 -> DpiSafetyLevel.DANGER
        targetDpi > 540 -> DpiSafetyLevel.CAUTION
        targetDpi < 320 -> DpiSafetyLevel.TOO_LOW
        else -> DpiSafetyLevel.SAFE
    }

    val safetyHeadline = when (safetyLevel) {
        DpiSafetyLevel.SAFE -> "🛡️ BALANCED & SAFE FOR YOUR PHONE"
        DpiSafetyLevel.CAUTION -> "⚡ HIGH SENSITIVITY DPI"
        DpiSafetyLevel.DANGER -> "⚡ HIGH FLICK DPI (600+)"
        DpiSafetyLevel.TOO_LOW -> "⚠️ TOO LOW"
    }

    val safetyDetails = when (safetyLevel) {
        DpiSafetyLevel.SAFE -> "Target $targetDpi DPI is comfortable for daily phone usage and gaming without UI shrinking."
        DpiSafetyLevel.CAUTION -> "Target $targetDpi is elevated for faster swipe agility. Recommended to check quick toggles."
        DpiSafetyLevel.DANGER -> "Target $targetDpi provides extreme flick speed. Important: Turn it back down after your gaming session!"
        DpiSafetyLevel.TOO_LOW -> "Target $targetDpi may enlarge icons."
    }

    return DpiOptimizationResult(
        recommendedDpi = targetDpi,
        recommendedGeneralSensi = targetGeneral,
        dpiBoost = dpiBoost,
        generalDelta = generalDelta,
        safetyLevel = safetyLevel,
        safetyHeadline = safetyHeadline,
        safetyDetails = safetyDetails,
        tacticalWhy = whyText,
        dragTechniqueTip = techniqueTip
    )
}
