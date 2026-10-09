package com.sensibotpro.ui.screens.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.OverlayViewModel

@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val hasPermission by viewModel.hasOverlayPermission.collectAsState()
    val isFloatingRunning by viewModel.isFloatingSensiRunning.collectAsState()
    val isCrosshairRunning by viewModel.isCrosshairRunning.collectAsState()

    val crosshairStyleVal by viewModel.crosshairStyle.collectAsState()
    val crosshairSizeVal by viewModel.crosshairSize.collectAsState()
    val crosshairColorVal by viewModel.crosshairColor.collectAsState()
    val crosshairThicknessVal by viewModel.crosshairThickness.collectAsState()
    val crosshairOpacityVal by viewModel.crosshairOpacity.collectAsState()

    var showPermissionExplanation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "PRO REFERENCE TOOLS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Visual overlays to reference sensitivity and center aim while in Training Ground.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Card
        if (!hasPermission) {
            GamingCard(borderColor = AccentAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Overlay Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Android requires explicit permission to draw floating reference cards above other applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlowButton(
                    text = "Grant Overlay Permission",
                    icon = Icons.Default.Settings,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tool 1: Floating In-Game Assistant
        SectionHeader(
            title = "1. Floating In-Game Assistant",
            subtitle = "Draggable overlay with Head, Body, & Leg shot optimization and interactive animation"
        )
        GamingCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFloatingRunning) "ASSISTANT HUD ACTIVE" else "ASSISTANT HUD STOPPED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFloatingRunning) AccentGreen else TextPrimary
                    )
                    Text(
                        text = "Opens a half-screen horizontal tactical HUD over your game with real-time animated targeting silhouettes and one-tap sensitivity calibration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isFloatingRunning,
                    onCheckedChange = {
                        if (!hasPermission) {
                            showPermissionExplanation = true
                        } else {
                            viewModel.toggleFloatingSensi()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = RubyRedPrimary,
                        uncheckedTrackColor = ElevatedSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tool 2: Practice Crosshair
        SectionHeader(
            title = "2. Practice Crosshair",
            subtitle = "Visual centering reference tool"
        )
        GamingCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCrosshairRunning) "CROSSHAIR OVERLAY ACTIVE" else "CROSSHAIR OVERLAY STOPPED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCrosshairRunning) AccentGreen else TextPrimary
                    )
                    Text(
                        text = "Fixed visual center reference for training your crosshair placement habits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isCrosshairRunning,
                    onCheckedChange = {
                        if (!hasPermission) {
                            showPermissionExplanation = true
                        } else {
                            viewModel.toggleCrosshair()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = RubyRedPrimary,
                        uncheckedTrackColor = ElevatedSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Crosshair Live Preview
            Text(
                text = "LIVE PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RubyRedPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VoidBlack),
                contentAlignment = Alignment.Center
            ) {
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(crosshairColorVal)).copy(alpha = crosshairOpacityVal)
                } catch (e: Exception) {
                    RubyRedPrimary.copy(alpha = crosshairOpacityVal)
                }

                Canvas(modifier = Modifier.size(100.dp)) {
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val half = (crosshairSizeVal.toFloat() * 1.2f).coerceIn(16f, 50f)
                    val gap = half * 0.35f
                    val thick = (crosshairThicknessVal.toFloat() * 1.2f).coerceIn(2f, 8f)

                    when (crosshairStyleVal) {
                        "Classic Cross" -> {
                            drawLine(previewColor, Offset(cx - half, cy), Offset(cx - gap, cy), strokeWidth = thick, cap = StrokeCap.Round)
                            drawLine(previewColor, Offset(cx + gap, cy), Offset(cx + half, cy), strokeWidth = thick, cap = StrokeCap.Round)
                            drawLine(previewColor, Offset(cx, cy - half), Offset(cx, cy - gap), strokeWidth = thick, cap = StrokeCap.Round)
                            drawLine(previewColor, Offset(cx, cy + gap), Offset(cx, cy + half), strokeWidth = thick, cap = StrokeCap.Round)
                            drawCircle(previewColor, radius = thick * 0.8f, center = Offset(cx, cy))
                        }
                        "Center Dot" -> {
                            drawCircle(previewColor, radius = half * 0.4f, center = Offset(cx, cy))
                        }
                        "Circle Dot" -> {
                            drawCircle(previewColor, radius = half * 0.75f, center = Offset(cx, cy), style = Stroke(width = thick))
                            drawCircle(previewColor, radius = thick * 0.9f, center = Offset(cx, cy))
                        }
                        "T-Style" -> {
                            drawLine(previewColor, Offset(cx - half, cy), Offset(cx - gap, cy), strokeWidth = thick, cap = StrokeCap.Round)
                            drawLine(previewColor, Offset(cx + gap, cy), Offset(cx + half, cy), strokeWidth = thick, cap = StrokeCap.Round)
                            drawLine(previewColor, Offset(cx, cy + gap), Offset(cx, cy + half), strokeWidth = thick, cap = StrokeCap.Round)
                            drawCircle(previewColor, radius = thick * 0.8f, center = Offset(cx, cy))
                        }
                        "Diamond" -> {
                            val path = Path().apply {
                                moveTo(cx, cy - half * 0.7f)
                                lineTo(cx + half * 0.7f, cy)
                                lineTo(cx, cy + half * 0.7f)
                                lineTo(cx - half * 0.7f, cy)
                                close()
                            }
                            drawPath(path, previewColor, style = Stroke(width = thick))
                            drawCircle(previewColor, radius = thick * 0.7f, center = Offset(cx, cy))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Crosshair Customization Controls
            Text(
                text = "CROSSHAIR STYLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            val styles = listOf("Classic Cross", "Center Dot", "Circle Dot", "T-Style", "Diamond")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                styles.take(3).forEach { s ->
                    val isSel = crosshairStyleVal == s
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) RubyRedDark else ElevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RubyRedPrimary else BorderStroke),
                        modifier = Modifier.weight(1f).clickable {
                            viewModel.updateCrosshairPreferences(style = s)
                        }
                    ) {
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                styles.drop(3).forEach { s ->
                    val isSel = crosshairStyleVal == s
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) RubyRedDark else ElevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RubyRedPrimary else BorderStroke),
                        modifier = Modifier.weight(1f).clickable {
                            viewModel.updateCrosshairPreferences(style = s)
                        }
                    ) {
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Crosshair Color
            Text(
                text = "COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            val colors = listOf(
                "#FF2A4D" to "Red",
                "#00E676" to "Green",
                "#00E5FF" to "Cyan",
                "#FFB300" to "Amber",
                "#FFFFFF" to "White"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { (hex, name) ->
                    val isSel = crosshairColorVal.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .then(
                                if (isSel) Modifier.border(2.dp, TextPrimary, CircleShape) else Modifier
                            )
                            .clickable {
                                viewModel.updateCrosshairPreferences(color = hex)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Size Slider
            Text(
                text = "Size: $crosshairSizeVal dp",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Slider(
                value = crosshairSizeVal.toFloat(),
                onValueChange = { viewModel.updateCrosshairPreferences(size = it.toInt()) },
                valueRange = 16f..48f,
                colors = SliderDefaults.colors(thumbColor = RubyRedPrimary, activeTrackColor = RubyRedPrimary)
            )

            // Opacity Slider
            Text(
                text = "Opacity: ${(crosshairOpacityVal * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Slider(
                value = crosshairOpacityVal,
                onValueChange = { viewModel.updateCrosshairPreferences(opacity = it) },
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(thumbColor = RubyRedPrimary, activeTrackColor = RubyRedPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Visual reference only. No automatic aiming or target assistance.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Overlay Permission Required", color = TextPrimary) },
            text = {
                Text(
                    "Floating Sensi BOT and Practice Crosshair need overlay permission to display visual reference panels above other apps. They do NOT inspect, read, or modify Free Fire in any way.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Open Settings", color = RubyRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanation = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CharcoalSurface
        )
    }
}
