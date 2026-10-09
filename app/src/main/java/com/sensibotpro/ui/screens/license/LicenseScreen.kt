package com.sensibotpro.ui.screens.license

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.LicenseViewModel

@Composable
fun LicenseScreen(
    viewModel: LicenseViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val licenseState by viewModel.licenseState.collectAsState()
    val statusMessage by viewModel.activationStatusMessage.collectAsState()
    val isError by viewModel.isError.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var showTransferDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "UNLOCK SENSI BOT Pro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "One-time purchase. No monthly subscription.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Price Card
        GamingCard(borderColor = RubyRedPrimary) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIFETIME PRO LICENSE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RubyRedPrimary
                    )
                    Text(
                        text = "₹299",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface
                ) {
                    Text(
                        text = if (licenseState.isLicensed) "STATUS: ACTIVE" else "STATUS: INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (licenseState.isLicensed) AccentGreen else AccentAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val features = listOf(
                "Personalized sensitivity profiles",
                "Offline Sensi Bot aim coaching",
                "Device-aware hardware recommendations",
                "Floating Sensi Bot overlay HUD",
                "Practice Crosshair reference overlay",
                "Unlimited saved custom configurations",
                "Future engine & drill updates"
            )

            features.forEach { feat ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Activation Card
        SectionHeader(
            title = "License Activation",
            subtitle = "Enter your unique product key"
        )
        GamingCard {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it.uppercase() },
                placeholder = { Text("ENTER LICENSE KEY (e.g. DEV-TEST-299)", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RubyRedPrimary,
                    unfocusedBorderColor = BorderStroke,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) RubyRedPrimary else AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlowButton(
                text = "ACTIVATE LICENSE",
                icon = Icons.Default.VpnKey,
                onClick = { viewModel.activate(keyInput) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Developer Testing: Enter key 'DEV-TEST-299' to simulate instant activation.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Binding Info
        SectionHeader(
            title = "Hardware Binding",
            subtitle = "License binds securely to your application installation ID"
        )
        GamingCard(backgroundColor = ElevatedSurface) {
            Text(
                text = "Hardware Device Identity:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = licenseState.boundInstallationId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Each license key is permanently locked to this device. If you switch devices or reinstall the application, contact Lyam FF with your Device ID for manual reset.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = { showTransferDialog = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Request Device Reset Instructions →",
                    color = RubyRedPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showTransferDialog) {
        var transferReason by remember { mutableStateOf("") }
        var resultMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Device Transfer Request", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Submit a request to detach this license from installation ID ${licenseState.boundInstallationId} and transfer to a new device.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = transferReason,
                        onValueChange = { transferReason = it },
                        label = { Text("Reason for transfer") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyRedPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resultMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(resultMessage!!, color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDeviceTransfer(transferReason) { msg ->
                            resultMessage = msg
                        }
                    }
                ) {
                    Text("Submit", color = RubyRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = CharcoalSurface
        )
    }
}
