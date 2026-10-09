package com.sensibotpro.ui.screens.touchlab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.ui.components.GamingCard
import com.sensibotpro.ui.components.GlowButton
import com.sensibotpro.ui.components.SectionHeader
import com.sensibotpro.ui.theme.*

/**
 * TouchLabScreen — Screen Friction & Drag Velocity Lab
 *
 * An interactive touch physics laboratory for Free Fire players:
 * - Real-time gesture velocity tracking (px/s)
 * - Trajectory glow visualization
 * - Touch friction diagnosis
 * - 1-tap calibrated sensitivity application on the modern 200 scale
 */
@Composable
fun TouchLabScreen(
    viewModel: TouchLabViewModel,
    onNavigateBack: () -> Unit
) {
    val phase by viewModel.phase.collectAsState()
    val swipes by viewModel.swipes.collectAsState()
    val liveVelocity by viewModel.currentSwipeVelocity.collectAsState()
    val result by viewModel.analysisResult.collectAsState()
    val isApplied by viewModel.isApplied.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElevatedSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TOUCH VELOCITY LAB",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RubyRedPrimary
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Hardware Screen Friction & Drag Physics Diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Metrics Summary
        GamingCard(borderColor = RubyRedGlow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${viewModel.deviceSpecs.manufacturer} ${viewModel.deviceSpecs.model}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sampling Rate: ${viewModel.deviceSpecs.refreshRate}Hz Display • ${viewModel.deviceSpecs.displayDensityDpi} DPI",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Text(
                        text = if (phase == TouchLabPhase.SWIPING) "TEST ACTIVE" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (phase == TouchLabPhase.SWIPING) AccentGreen else AccentAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  TEST INTERACTION BOX
        // ═════════════════════════════════════════════════════════════════════
        if (phase == TouchLabPhase.IDLE) {
            GamingCard(borderColor = RubyRedPrimary) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(RubyRedPrimary.copy(alpha = 0.15f))
                            .border(1.5.dp, RubyRedPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = RubyRedPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Calibrate With Your Finger Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Perform 5 rapid upward drag swipes (like pulling a headshot with an M1887 or MP40). Our engine calculates your exact velocity in pixels/second to determine if you need faster or slower sensitivity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowButton(
                        text = "START 5-SWIPE TEST",
                        icon = Icons.Default.PlayArrow,
                        onClick = { viewModel.startSession() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (phase == TouchLabPhase.SWIPING) {
            // Interactive Swipe Canvas Box
            GamingCard(borderColor = RubyRedPrimary) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SWIPE UPWARD IN BOX",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = RubyRedPrimary
                        )
                        Text(
                            text = "${swipes.size} / 5 SWIPES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Drag Pad
                    var touchStart by remember { mutableStateOf(Offset.Zero) }
                    var touchCurrent by remember { mutableStateOf(Offset.Zero) }
                    var startTime by remember { mutableLongStateOf(0L) }
                    var isTouching by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface)
                            .border(2.dp, Brush.verticalGradient(listOf(RubyRedPrimary, BorderStroke)), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        touchStart = offset
                                        touchCurrent = offset
                                        startTime = System.currentTimeMillis()
                                        isTouching = true
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        touchCurrent += dragAmount
                                    },
                                    onDragEnd = {
                                        val duration = System.currentTimeMillis() - startTime
                                        viewModel.recordSwipe(
                                            startX = touchStart.x,
                                            startY = touchStart.y,
                                            endX = touchCurrent.x,
                                            endY = touchCurrent.y,
                                            durationMs = duration
                                        )
                                        isTouching = false
                                    },
                                    onDragCancel = {
                                        isTouching = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Center target guideline
                            drawLine(
                                color = BorderStroke,
                                start = Offset(size.width / 2, size.height * 0.2f),
                                end = Offset(size.width / 2, size.height * 0.85f),
                                strokeWidth = 2f
                            )

                            // Live gesture trace
                            if (isTouching) {
                                drawLine(
                                    color = RubyRedPrimary,
                                    start = touchStart,
                                    end = touchCurrent,
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = AccentCyan,
                                    radius = 16f,
                                    center = touchCurrent
                                )
                            }
                        }

                        // Instructions inside box
                        if (!isTouching) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardDoubleArrowUp,
                                    contentDescription = null,
                                    tint = RubyRedPrimary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "DRAG UPWARD SWIFTLY",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pull like an in-game headshot flick",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Live velocity badge
                        if (liveVelocity > 0f) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VoidBlack.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RubyRedPrimary),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "LIVE VELOCITY: ${liveVelocity.toInt()} PX/S",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (phase == TouchLabPhase.CALCULATING) {
            // Calculating Sci-Fi Scanner
            GamingCard(borderColor = RubyRedPrimary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = RubyRedPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "COMPUTING DRAG PHYSICS...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Analyzing drag curves • screen friction • touch velocity vectors",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else if (phase == TouchLabPhase.COMPLETED && result != null) {
            val res = result!!

            // Results Headline Card
            GamingCard(borderColor = RubyRedPrimary) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = RubyRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = res.diagnosisHeadline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = res.tacticalBreakdown,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("AVG SPEED", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${res.averageVelocityPxPerSec.toInt()} px/s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("PEAK FLICK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${res.maxVelocityPxPerSec.toInt()} px/s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RubyRedPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElevatedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("DRAG STYLE DETECTED", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(res.dragCurvatureType, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calibrated 200 Scale Numbers
            SectionHeader(
                title = "Calibrated Sensitivity Setup",
                subtitle = "Optimized for your physical finger swipe velocity"
            )

            GamingCard(borderColor = AccentGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GENERAL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${res.recommendedGeneral}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = AccentGreen)
                        Text("200 Scale", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RED DOT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${res.recommendedRedDot}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text("200 Scale", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUTTON", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${res.recommendedButtonSize}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = AccentAmber)
                        Text("HUD Size", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DPI", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${res.generatedProfile.recommendedDpi}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = AccentCyan)
                        Text("Width", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isApplied) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF003314),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CALIBRATED PROFILE ACTIVE IN PROFILES!", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 12.sp)
                        }
                    }
                } else {
                    GlowButton(
                        text = "APPLY CALIBRATED PROFILE",
                        icon = Icons.Default.DoneAll,
                        onClick = { viewModel.applyCalibratedProfile() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.startSession() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RE-TEST FINGER SPEED")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
