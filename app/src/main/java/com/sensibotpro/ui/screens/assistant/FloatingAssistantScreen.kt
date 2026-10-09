package com.sensibotpro.ui.screens.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.R
import com.sensibotpro.service.FloatingSensiService
import com.sensibotpro.service.TargetZone
import com.sensibotpro.ui.components.GamingCard
import com.sensibotpro.ui.components.GlowButton
import com.sensibotpro.ui.components.SectionHeader
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.OverlayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Floating Assistant Screen
 *
 * Provides complete control, configuration, and explanation
 * for the in-game floating overlay assistant.
 */
@Composable
fun FloatingAssistantScreen(
    viewModel: OverlayViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val hasPermission by viewModel.hasOverlayPermission.collectAsState()
    val isRunning by viewModel.isFloatingSensiRunning.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }

    // Pre-launch config preferences
    val prefs = remember { context.getSharedPreferences(FloatingSensiService.PREFS_NAME, Context.MODE_PRIVATE) }
    var defaultEdge by remember { mutableStateOf(prefs.getString("default_edge", "RIGHT") ?: "RIGHT") }
    var optHeadEnabled by remember { mutableStateOf(prefs.getBoolean("opt_head_enabled", true)) }
    var optBodyEnabled by remember { mutableStateOf(prefs.getBoolean("opt_body_enabled", true)) }
    var optLegsEnabled by remember { mutableStateOf(prefs.getBoolean("opt_legs_enabled", true)) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermission()
    }

    // Refresh overlay running state on lifecycle resume
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Header Row ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
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
            }
            Column {
                Text(
                    text = "FLOATING ASSISTANT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Real-Time In-Game Tactical Gaming HUD",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  1. MASTER TOGGLE & STATUS CARD
        // ═════════════════════════════════════════════════════════════════════
        GamingCard(
            borderColor = if (isRunning) AccentGreen else BorderStroke
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) AccentGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "ASSISTANT ACTIVE OVER GAME" else "ASSISTANT DISABLED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isRunning) AccentGreen else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning)
                            "Floating launcher is visible over your game. Tap it anytime to open the half-screen menu. To completely stop it, turn this switch OFF."
                        else
                            "Enable to display a small draggable floating launcher on top of Free Fire.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = isRunning,
                    onCheckedChange = {
                        if (!hasPermission) {
                            showPermissionDialog = true
                        } else {
                            viewModel.toggleFloatingSensi()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = RubyRedPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ElevatedSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  OVERLAY PERMISSION WARNING (If not granted)
        // ═════════════════════════════════════════════════════════════════════
        if (!hasPermission) {
            GamingCard(borderColor = AccentAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Android Overlay Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Android requires explicit permission to display the floating assistant launcher on top of Free Fire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlowButton(
                    text = "GRANT OVERLAY PERMISSION",
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

        // ═════════════════════════════════════════════════════════════════════
        //  2. PRE-LAUNCH CONFIGURATION
        // ═════════════════════════════════════════════════════════════════════
        SectionHeader(
            title = "Floating Button Configuration",
            subtitle = "Customize the floating launcher and menu options"
        )

        GamingCard {
            // A. Preferred Initial Screen Edge
            Text(
                text = "INITIAL BUTTON EDGE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Where the floating button docks when first launched (you can drag it anywhere later).",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("LEFT" to "Left Screen Edge", "RIGHT" to "Right Screen Edge").forEach { (edge, label) ->
                    val isSelected = defaultEdge == edge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) RubyRedPrimary.copy(alpha = 0.2f) else ElevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) RubyRedPrimary else BorderStroke),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                defaultEdge = edge
                                prefs.edit().putString("default_edge", edge).apply()
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) RubyRedPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderStroke, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // B. Menu Options to Include
            Text(
                text = "VISIBLE MENU OPTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select which optimization tabs appear inside the in-game floating menu.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            ConfigCheckboxRow(
                title = "Optimize Head Shots (Critical Flick)",
                checked = optHeadEnabled,
                onCheckedChange = {
                    optHeadEnabled = it
                    prefs.edit().putBoolean("opt_head_enabled", it).apply()
                }
            )
            ConfigCheckboxRow(
                title = "Optimize Body Shots (Chest Spray)",
                checked = optBodyEnabled,
                onCheckedChange = {
                    optBodyEnabled = it
                    prefs.edit().putBoolean("opt_body_enabled", it).apply()
                }
            )
            ConfigCheckboxRow(
                title = "Optimize Leg Shots (Crouch Recovery)",
                checked = optLegsEnabled,
                onCheckedChange = {
                    optLegsEnabled = it
                    prefs.edit().putBoolean("opt_legs_enabled", it).apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════
        //  4. HOW THE FLOATING ASSISTANT WORKS
        // ═════════════════════════════════════════════════════════════════════
        SectionHeader(
            title = "How the Assistant Works",
            subtitle = "Quick guide to in-game overlay operation"
        )

        GamingCard {
            val steps = listOf(
                "Launch Assistant" to "Flip the switch at the top of this page. The floating button appears immediately.",
                "Play Free Fire" to "Open Free Fire. Drag the floating button to your preferred side edge so it stays out of your crosshair.",
                "Tap to Expand" to "Tap the button to open the half-screen tactical menu. Your game remains visible on the other half.",
                "Select Optimization" to "Tap Head, Body, or Leg shots. Watch the character calculate and lock the hitbox, then tap Apply.",
                "Close or Disable" to "Tap ✕ to collapse the menu while playing. To completely remove the floating button, return to this page and disable it."
            )

            steps.forEachIndexed { index, (title, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(RubyRedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ConfigCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = RubyRedPrimary,
                uncheckedColor = TextMuted,
                checkmarkColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
