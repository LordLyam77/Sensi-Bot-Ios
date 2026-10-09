package com.sensibotpro.ui.screens.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.theme.*

@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedTopic by remember { mutableStateOf("License problem") }
    var userMessage by remember { mutableStateOf("") }
    var ticketSubmitted by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val topics = listOf(
        "License problem",
        "Device activation problem",
        "Sensitivity feedback",
        "Report a bug",
        "General support"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SUPPORT & FEEDBACK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "We are here to assist with licenses, devices & coaching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ticketSubmitted) {
            GamingCard(borderColor = AccentGreen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ticket Submitted!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Thank you! Our support team reviews all inquiries promptly. For license transfer requests, please allow up to 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlowButton(
                    text = "Submit Another Inquiry",
                    isSecondary = true,
                    onClick = {
                        ticketSubmitted = false
                        userMessage = ""
                    }
                )
            }
        } else {
            SectionHeader(title = "Select Topic")
            topics.forEach { topic ->
                val isSel = selectedTopic == topic
                GamingCard(
                    modifier = Modifier.padding(vertical = 3.dp),
                    borderColor = if (isSel) RubyRedPrimary else BorderStroke,
                    backgroundColor = if (isSel) ElevatedSurface else CharcoalSurface,
                    onClick = { selectedTopic = topic }
                ) {
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) RubyRedPrimary else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Message Details")
            GamingCard {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    placeholder = { Text("Describe what happened or share your feedback...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyRedPrimary,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlowButton(
                    text = "Submit Ticket",
                    icon = Icons.Default.Send,
                    onClick = {
                        if (userMessage.isNotBlank()) {
                            ticketSubmitted = true
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diagnostic Tools
        SectionHeader(title = "Hardware Diagnostics", subtitle = "Copy system specs for support assistance")
        GamingCard(backgroundColor = ElevatedSurface) {
            Text(
                text = "Having issues with device activation? Copy your diagnostic summary to share directly with customer service.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlowButton(
                text = "Copy Diagnostics to Clipboard",
                icon = Icons.Default.ContentCopy,
                isSecondary = true,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        "Sensi Bot Diagnostics",
                        "App: SENSI BOT Pro v1.0.1\nDevice: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
                    )
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Source & Legal Notices
        SectionHeader(title = "Legal & Open Source", subtitle = "Third-party libraries, Qwen3 & Google LiteRT licenses")
        GamingCard(backgroundColor = ElevatedSurface) {
            Text(
                text = "SENSI BOT Pro utilizes open-source on-device AI runtimes and models including Qwen3-0.6B and Google LiteRT-LM under permissive commercial distribution licenses.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlowButton(
                text = "View Open Source Licenses",
                icon = Icons.Default.Description,
                isSecondary = true,
                onClick = { showLicensesDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }
}
