package com.sensibotpro.headlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.headlock.*
import com.sensibotpro.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HeadlockWizard(
    initialConfig: HeadlockConfig,
    onCommit: (HeadlockConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(CalibrationStep.DEVICE_CLASS) }
    var workingConfig by remember { mutableStateOf(initialConfig.clamped()) }
    var sweepState by remember { mutableStateOf(BiasSweepState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "HEADLOCK CALIBRATION (${step.index + 1}/4)",
            color = RubyRedPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Text(text = step.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        when (step) {
            CalibrationStep.DEVICE_CLASS -> {
                Text(
                    text = "Select your device class to initialize deterministic drag distance and cadence.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                HeadlockDeviceClass.entries.forEach { dc ->
                    val selected = workingConfig.deviceClass == dc
                    Card(
                        onClick = {
                            val baseMetrics = HeadlockProfile.collectMetrics(context).copy(deviceClass = dc)
                            workingConfig = HeadlockCalibration.recommend(baseMetrics)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) RubyRedPrimary.copy(alpha = 0.22f) else CharcoalSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (selected) RubyRedPrimary else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = dc.label,
                            color = TextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            CalibrationStep.FIRE_ANCHOR -> {
                Text(
                    text = "Tap inside the preview pad where your Free Fire fire button sits.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(CharcoalSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, RubyRedPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val nx = (offset.x / size.width.toFloat()).toDouble().coerceIn(0.05, 0.95)
                                val ny = (offset.y / size.height.toFloat()).toDouble().coerceIn(0.05, 0.95)
                                workingConfig = workingConfig.copy(startX = nx, startY = ny).clamped()
                            }
                        }
                ) {
                    val dotX = maxWidth * workingConfig.startX.toFloat() - 14.dp
                    val dotY = maxHeight * workingConfig.startY.toFloat() - 14.dp
                    Box(
                        modifier = Modifier
                            .offset(x = dotX, y = dotY)
                            .size(28.dp)
                            .background(RubyRedPrimary, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
                Text(
                    text = "Anchor: (${(workingConfig.startX * 100).roundToInt()}%, ${(workingConfig.startY * 100).roundToInt()}%)",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            CalibrationStep.VERTICAL_BIAS_SWEEP -> {
                Text(
                    text = "Sweep vertical bias 0.6x → 1.6x (0.1 steps, 20 reps each). Keep the bias that lands ~6/10 (12/20) clean headshots.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Current Bias: ${sweepState.currentBias}x (Step ${sweepState.stepIndex + 1}/${sweepState.totalSteps})",
                            color = RubyRedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Reps: ${sweepState.repsCompleted}/${sweepState.targetRepsPerStep} • Hits: ${sweepState.hitsRecorded} • Best Bias: ${sweepState.bestBias}x",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { sweepState = HeadlockCalibration.recordSweepRep(sweepState, hit = true) },
                                enabled = sweepState.repsCompleted < sweepState.targetRepsPerStep,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F9D68))
                            ) { Text("Record Hit") }
                            OutlinedButton(
                                onClick = { sweepState = HeadlockCalibration.recordSweepRep(sweepState, hit = false) },
                                enabled = sweepState.repsCompleted < sweepState.targetRepsPerStep
                            ) { Text("Miss", color = TextPrimary) }
                        }
                        if (sweepState.currentBias < HeadlockConfig.MAX_VERTICAL_BIAS) {
                            TextButton(
                                onClick = {
                                    sweepState = HeadlockCalibration.advanceSweepBias(sweepState)
                                    workingConfig = workingConfig.copy(verticalBias = sweepState.bestBias).clamped()
                                }
                            ) {
                                Text("Next Bias Step (+0.1)", color = RubyRedPrimary)
                            }
                        }
                    }
                }
            }

            CalibrationStep.COMMIT -> {
                val finalCfg = workingConfig.copy(verticalBias = sweepState.bestBias).clamped()
                Text(
                    text = "Ready to commit calibrated Headlock profile:",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Device Class: ${finalCfg.deviceClass.label}", color = TextPrimary)
                        Text("Anchor: (${(finalCfg.startX * 100).roundToInt()}%, ${(finalCfg.startY * 100).roundToInt()}%)", color = TextPrimary)
                        Text("Distance: ${finalCfg.distancePx}px • Duration: ${finalCfg.durationMs}ms", color = TextPrimary)
                        Text("Vertical Bias: ${finalCfg.verticalBias}x", color = RubyRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    when (step) {
                        CalibrationStep.DEVICE_CLASS -> onCancel()
                        CalibrationStep.FIRE_ANCHOR -> step = CalibrationStep.DEVICE_CLASS
                        CalibrationStep.VERTICAL_BIAS_SWEEP -> step = CalibrationStep.FIRE_ANCHOR
                        CalibrationStep.COMMIT -> step = CalibrationStep.VERTICAL_BIAS_SWEEP
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (step == CalibrationStep.DEVICE_CLASS) "Cancel" else "Back", color = TextPrimary)
            }
            Button(
                onClick = {
                    when (step) {
                        CalibrationStep.DEVICE_CLASS -> step = CalibrationStep.FIRE_ANCHOR
                        CalibrationStep.FIRE_ANCHOR -> step = CalibrationStep.VERTICAL_BIAS_SWEEP
                        CalibrationStep.VERTICAL_BIAS_SWEEP -> {
                            workingConfig = workingConfig.copy(verticalBias = sweepState.bestBias).clamped()
                            step = CalibrationStep.COMMIT
                        }
                        CalibrationStep.COMMIT -> onCommit(workingConfig.copy(verticalBias = sweepState.bestBias).clamped())
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RubyRedPrimary)
            ) {
                Text(if (step == CalibrationStep.COMMIT) "Commit Profile" else "Next")
            }
        }
    }
}

