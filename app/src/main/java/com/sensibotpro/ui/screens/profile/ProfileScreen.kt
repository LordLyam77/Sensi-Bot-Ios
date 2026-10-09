package com.sensibotpro.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.domain.model.SensitivityProfile
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.screens.support.OpenSourceLicensesDialog
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToLicense: () -> Unit,
    onNavigateToFairPlay: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val scrollState = rememberScrollState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val licenseInfo by viewModel.licenseInfo.collectAsState()
    val specs = viewModel.deviceSpecs
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Profile Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(RubyRedPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "SENSI PILOT",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${specs.manufacturer} ${specs.model} • ${specs.performanceClass.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // My Saved Profiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "My Profiles",
                subtitle = "${profiles.size} saved configurations"
            )
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "New Profile",
                    tint = RubyRedPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (profiles.isEmpty()) {
            GamingCard {
                Text(
                    text = "Your first profile is waiting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            profiles.forEach { p ->
                val isActive = p.id == activeProfile?.id
                GamingCard(
                    modifier = Modifier.padding(vertical = 4.dp),
                    borderColor = if (isActive) RubyRedPrimary else BorderStroke,
                    backgroundColor = if (isActive) ElevatedSurface else CharcoalSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) RubyRedPrimary else TextPrimary
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = RubyRedPrimary
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gen: ${p.general} • Red: ${p.redDot} • 2X: ${p.scope2x} • 4X: ${p.scope4x} • Btn: ${p.fireButtonSize}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row {
                            if (!isActive) {
                                IconButton(onClick = { viewModel.setActive(p.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = "Set Active",
                                        tint = TextSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.duplicate(p) }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate",
                                    tint = TextSecondary
                                )
                            }
                            if (p.isCustom) {
                                IconButton(onClick = { viewModel.delete(p.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Menu Sections
        SectionHeader(title = "Application & Standards")

        MenuRowItem(
            title = "Fair Play Guarantee",
            subtitle = "This is not a hack, it only optimizes your gameplay.",
            icon = Icons.Default.Shield,
            iconTint = AccentGreen,
            onClick = onNavigateToFairPlay
        )
        MenuRowItem(
            title = "Lyam FF on YouTube",
            subtitle = "Tutorials, sensitivity guides & gameplay highlights",
            icon = Icons.Default.SmartDisplay,
            iconTint = RubyRedPrimary,
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=Lyam+FF"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
        MenuRowItem(
            title = "License & Device Binding",
            subtitle = "Manage device activation and license binding",
            icon = Icons.Default.VpnKey,
            iconTint = RubyRedPrimary,
            onClick = onNavigateToLicense
        )
        MenuRowItem(
            title = "Support & Diagnostics",
            subtitle = "Sensitivity feedback, bug reports & license assistance",
            icon = Icons.Default.SupportAgent,
            iconTint = AccentAmber,
            onClick = onNavigateToSupport
        )
        MenuRowItem(
            title = "Privacy Policy",
            subtitle = "Local offline storage & transparency manifesto",
            icon = Icons.Default.PrivacyTip,
            iconTint = TextSecondary,
            onClick = onNavigateToPrivacy
        )
        MenuRowItem(
            title = "Open Source Licenses",
            subtitle = "Qwen3, Google LiteRT-LM & third-party notices",
            icon = Icons.Default.Description,
            iconTint = TextSecondary,
            onClick = { showLicensesDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App Version
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SENSI BOT Pro • Lyam FF Official Edition v1.0.1",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RubyRedPrimary
            )
            Text(
                text = "Installation ID: ${viewModel.licenseInfo.value.boundInstallationId}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog for creating a custom profile
    if (showCreateDialog) {
        var profileName by remember { mutableStateOf("") }
        var generalVal by remember { mutableStateOf("96") }
        var redDotVal by remember { mutableStateOf("90") }
        var btnVal by remember { mutableStateOf("48") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom Profile", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name (e.g. M1887 Rush)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyRedPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = generalVal,
                        onValueChange = { generalVal = it },
                        label = { Text("General Sensitivity (0-100)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyRedPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = redDotVal,
                        onValueChange = { redDotVal = it },
                        label = { Text("Red Dot (0-100)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyRedPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = btnVal,
                        onValueChange = { btnVal = it },
                        label = { Text("Fire Button Size (%)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyRedPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = if (profileName.isNotBlank()) profileName else "Custom Profile"
                        val g = generalVal.toIntOrNull() ?: 95
                        val r = redDotVal.toIntOrNull() ?: 90
                        val b = btnVal.toIntOrNull() ?: 48
                        viewModel.saveCustomProfile(
                            SensitivityProfile(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                general = g,
                                redDot = r,
                                scope2x = 84,
                                scope4x = 78,
                                sniper = 50,
                                freeLook = 75,
                                fireButtonSize = b,
                                isCustom = true
                            )
                        )
                        showCreateDialog = false
                    }
                ) {
                    Text("Save", color = RubyRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CharcoalSurface
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }
}

@Composable
fun MenuRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    GamingCard(
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
