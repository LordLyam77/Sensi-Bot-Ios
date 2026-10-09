package com.sensibotpro.ui.screens.sensibot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.chat.AiCoachingMode
import com.sensibotpro.chat.AiConfig
import com.sensibotpro.chat.AiProvider
import com.sensibotpro.chat.ChatMessage
import com.sensibotpro.chat.DeviceCapabilityResult
import com.sensibotpro.chat.ModelStatus
import com.sensibotpro.ui.components.GamingCard
import com.sensibotpro.ui.screens.support.OpenSourceLicensesDialog
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.SensiBotViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensiBotScreen(
    viewModel: SensiBotViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val deviceCapability = viewModel.deviceCapability
    val aiCoachingMode by viewModel.aiCoachingMode.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Scroll to bottom when keyboard opens so input field and last message are never hidden
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Mode indicator calculation
    val (modeBadgeText, modeBadgeColor) = when {
        !deviceCapability.isSupported || aiCoachingMode == AiCoachingMode.RULE_BASED ->
            "[ SMART RULES ]" to AccentAmber
        modelStatus == ModelStatus.Loaded ->
            "[ LOCAL AI ]" to AccentGreen
        aiCoachingMode == AiCoachingMode.SMART_COACH && modelStatus == ModelStatus.Ready ->
            "[ LOCAL AI ]" to AccentGreen
        aiCoachingMode == AiCoachingMode.SMART_COACH ->
            "[ SMART COACH ]" to RubyRedPrimary
        else ->
            "[ SMART RULES ]" to AccentAmber
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .imePadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = RubyRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SENSI BOT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // SENSI BOT MODE Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SENSI BOT MODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = modeBadgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, modeBadgeColor)
                    ) {
                        Text(
                            text = modeBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = modeBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Top Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AI Settings Button
                IconButton(
                    onClick = { showAiConfigDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "AI Brain Settings",
                        tint = RubyRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Chat",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested Query Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.chips) { chip ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    modifier = Modifier.clickable {
                        viewModel.sendMessage(chip.prompt)
                    }
                ) {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
        }

        // Input Field Area (Guaranteed visible above keyboard with imePadding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Sensi Bot (e.g. M1887 headshot drag)...", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RubyRedPrimary,
                    unfocusedBorderColor = BorderStroke,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val text = inputText
                            inputText = ""
                            viewModel.sendMessage(text)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(text)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RubyRedPrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // AI Configuration Dialog
    if (showAiConfigDialog) {
        AiConfigDialog(
            aiCoachingMode = aiCoachingMode,
            modelStatus = modelStatus,
            deviceCapability = deviceCapability,
            onModeChange = { newMode ->
                viewModel.setAiCoachingMode(newMode)
            },
            onOpenLicenses = {
                showLicensesDialog = true
            },
            onDismiss = { showAiConfigDialog = false }
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }
}

@Composable
fun AiConfigDialog(
    aiCoachingMode: AiCoachingMode,
    modelStatus: ModelStatus,
    deviceCapability: DeviceCapabilityResult,
    onModeChange: (AiCoachingMode) -> Unit,
    onOpenLicenses: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(aiCoachingMode) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = RubyRedPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Coaching & Mode Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Section 1: AI MODE
                Text(
                    text = "AI MODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = RubyRedPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Option: Smart Coach
                val isSmartSelected = selectedMode == AiCoachingMode.SMART_COACH
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSmartSelected) RubyRedPrimary.copy(alpha = 0.15f) else ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSmartSelected) RubyRedPrimary else BorderStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (deviceCapability.isSupported) {
                                selectedMode = AiCoachingMode.SMART_COACH
                            } else {
                                selectedMode = AiCoachingMode.RULE_BASED
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSmartSelected,
                            onClick = {
                                if (deviceCapability.isSupported) {
                                    selectedMode = AiCoachingMode.SMART_COACH
                                } else {
                                    selectedMode = AiCoachingMode.RULE_BASED
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = RubyRedPrimary,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Smart Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSmartSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = "Qwen3-0.6B On-Device AI + Authoritative Sensitivity Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option: Rule-Based Coach
                val isRuleSelected = selectedMode == AiCoachingMode.RULE_BASED
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRuleSelected) RubyRedPrimary.copy(alpha = 0.15f) else ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRuleSelected) RubyRedPrimary else BorderStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = AiCoachingMode.RULE_BASED }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isRuleSelected,
                            onClick = { selectedMode = AiCoachingMode.RULE_BASED },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = RubyRedPrimary,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Rule-Based Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRuleSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = "Deterministic mathematical coaching. 100% offline & zero RAM impact.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Device Capability Alert
                if (!deviceCapability.isSupported) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF261C14),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Local AI isn't recommended on this device.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${deviceCapability.reason}. The system automatically routes to Rule-Based Coach to prevent in-game lag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF14241B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Device suitable for on-device AI (${String.format("%.1f", deviceCapability.totalRamGb)}GB RAM • API ${deviceCapability.apiLevel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Section 2: Local Model Status
                Text(
                    text = "ON-DEVICE MODEL (QWEN3-0.6B)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Runtime Status:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (modelStatus == ModelStatus.Ready || modelStatus == ModelStatus.Loaded)
                                    AccentGreen.copy(alpha = 0.2f)
                                else
                                    RubyRedPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = modelStatus.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (modelStatus == ModelStatus.Ready || modelStatus == ModelStatus.Loaded)
                                        AccentGreen
                                    else
                                        RubyRedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Model target: models/qwen3-0.6b-instruct.bin",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RAM Safe: Unloads instantly when exiting Sensi Bot screen. Never runs in background or overlay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Open Source Licenses Button
                OutlinedButton(
                    onClick = onOpenLicenses,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = RubyRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Source Licenses (Qwen & LiteRT)",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onModeChange(selectedMode)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RubyRedPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save & Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) RubyRedDark else CharcoalSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) RubyRedPrimary else BorderStroke
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RubyRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SENSI COACH (ON-DEVICE NEURAL)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RubyRedPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (message.thinkingText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedSurface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderStroke),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "🧠 ${message.thinkingText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (message.text.isEmpty() && message.isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = RubyRedPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synthesizing coaching advice...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = if (message.isStreaming) "${message.text} ▌" else message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                if (message.sensitivityAdjustment?.recommendedDpiChange != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A1A24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📱 DPI TARGET",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "${message.sensitivityAdjustment.recommendedDpiChange}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = AccentAmber
                                    )
                                    Text(
                                        text = "Smallest Width",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (message.sensitivityAdjustment.dpiStockValue != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "BOOST",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RubyRedGlow
                                        ) {
                                            Text(
                                                text = "+${message.sensitivityAdjustment.dpiBoostAmount ?: (message.sensitivityAdjustment.recommendedDpiChange - message.sensitivityAdjustment.dpiStockValue)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = RubyRedPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "from ${message.sensitivityAdjustment.dpiStockValue}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.actionSuggestion != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElevatedSurface
                    ) {
                        Text(
                            text = "DRILL: ${message.actionSuggestion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
