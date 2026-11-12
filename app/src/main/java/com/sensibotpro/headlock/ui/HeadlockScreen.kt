package com.sensibotpro.headlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.headlock.*
import com.sensibotpro.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HeadlockScreen(onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val bridge = remember(context) { HeadlockChannel(context.applicationContext) }
    val controller = remember(bridge) { HeadlockController(bridge) }

    var snapshot by remember { mutableStateOf(controller.snapshot) }
    var showWizard by remember { mutableStateOf(false) }

    if (showWizard) {
        HeadlockWizard(
            initialConfig = snapshot.config,
            onCommit = { calibrated ->
                controller.updateConfig(calibrated)
                snapshot = controller.snapshot
                showWizard = false
            },
            onCancel = { showWizard = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "HEADLOCK MACRO",
            color = RubyRedPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Isolated calibrated drag-stroke macro & overlay toggle. Zero game injection.",
            color = TextMuted,
            fontSize = 13.sp
        )

        // Status + Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RubyRedPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (snapshot.state) {
                                HeadlockState.ENABLED -> "BUBBLE ACTIVE"
                                HeadlockState.NEEDS_PERMISSION -> "PERMISSION REQUIRED"
                                HeadlockState.DISABLED -> "HEADLOCK IDLE"
                            },
                            color = when (snapshot.state) {
                                HeadlockState.ENABLED -> Color(0xFF2EE59D)
                                HeadlockState.NEEDS_PERMISSION -> Color(0xFFFFB020)
                                HeadlockState.DISABLED -> TextPrimary
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = snapshot.statusMessage,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = snapshot.state == HeadlockState.ENABLED,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable) {
                                controller.enable()
                            } else {
                                controller.disable()
                            }
                            snapshot = controller.snapshot
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RubyRedPrimary
                        )
                    )
                }

                if (snapshot.state == HeadlockState.NEEDS_PERMISSION) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33FFB020), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB020)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Sensi Bot Max Headlock in Accessibility Settings and allow Display Over Other Apps.",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { controller.openSystemSettings() },
                                colors = ButtonDefaults.buttonColors(containerColor = RubyRedPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Accessibility Settings", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Current Config Card
        val cfg = snapshot.config
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("MACRO PARAMETERS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeadlockMode.entries.forEach { mode ->
                        FilterChip(
                            selected = cfg.mode == mode,
                            onClick = {
                                controller.updateConfig(cfg.copy(mode = mode))
                                snapshot = controller.snapshot
                            },
                            label = { Text(mode.wireValue.uppercase()) }
                        )
                    }
                    HeadlockCurve.entries.forEach { curve ->
                        FilterChip(
                            selected = cfg.curve == curve,
                            onClick = {
                                controller.updateConfig(cfg.copy(curve = curve))
                                snapshot = controller.snapshot
                            },
                            label = { Text(curve.wireValue) }
                        )
                    }
                }

                Text(
                    text = "Anchor: (${(cfg.startX * 100).roundToInt()}%, ${(cfg.startY * 100).roundToInt()}%) • " +
                        "Distance: ${cfg.distancePx}px • Duration: ${cfg.durationMs}ms • Bias: ${cfg.verticalBias}x",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Text("Distance: ${cfg.distancePx} px", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = cfg.distancePx.toFloat(),
                    onValueChange = {
                        controller.updateConfig(cfg.copy(distancePx = it.roundToInt()))
                        snapshot = controller.snapshot
                    },
                    valueRange = 15f..180f,
                    colors = SliderDefaults.colors(thumbColor = RubyRedPrimary, activeTrackColor = RubyRedPrimary)
                )

                Text("Duration: ${cfg.durationMs} ms", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = cfg.durationMs.toFloat(),
                    onValueChange = {
                        controller.updateConfig(cfg.copy(durationMs = it.roundToInt()))
                        snapshot = controller.snapshot
                    },
                    valueRange = 30f..220f,
                    colors = SliderDefaults.colors(thumbColor = RubyRedPrimary, activeTrackColor = RubyRedPrimary)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showWizard = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyRedPrimary)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calibration Wizard")
                    }
                    OutlinedButton(
                        onClick = {
                            controller.applyRecommendation(HeadlockProfile.collectMetrics(context))
                            snapshot = controller.snapshot
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Auto Recommend", color = TextPrimary)
                    }
                }
            }
        }
    }
}

