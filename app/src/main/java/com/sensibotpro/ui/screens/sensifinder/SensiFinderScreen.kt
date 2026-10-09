package com.sensibotpro.ui.screens.sensifinder

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.domain.model.*
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.SensiFinderViewModel

@Composable
fun SensiFinderScreen(
    viewModel: SensiFinderViewModel,
    onNavigateToCalibration: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToDpi: () -> Unit = {},
    onNavigateToTouchLab: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val currentStep by viewModel.currentStep.collectAsState()
    val recommendation by viewModel.generatedRecommendation.collectAsState()

    val selectedPlaystyle by viewModel.selectedPlaystyle.collectAsState()
    val selectedFingerSetup by viewModel.selectedFingerSetup.collectAsState()
    val selectedWeapon by viewModel.selectedWeapon.collectAsState()
    val selectedProblem by viewModel.selectedProblem.collectAsState()
    val currentGeneralInput by viewModel.currentGeneralInput.collectAsState()
    val currentRedDotInput by viewModel.currentRedDotInput.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SENSI FINDER",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Build a sensitivity profile for your setup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (currentStep in 1..6) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Text(
                        text = "STEP $currentStep / 6",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RubyRedPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentStep == 7 && recommendation != null) {
            // Recommendation Result View
            val profile = recommendation!!.profile
            SectionHeader(title = "Your Tailored Profile", subtitle = "Starting Point — Test and Adjust")
            GamingCard(borderColor = RubyRedPrimary) {
                Text(
                    text = profile.name.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = RubyRedPrimary
                )
                Text(
                    text = profile.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                MetricBar(label = "General Sensitivity", value = profile.general)
                MetricBar(label = "Red Dot Sensitivity", value = profile.redDot)
                MetricBar(label = "2X Scope", value = profile.scope2x)
                MetricBar(label = "4X Scope", value = profile.scope4x)
                MetricBar(label = "Sniper Scope", value = profile.sniper)
                MetricBar(label = "Free Look", value = profile.freeLook)

                Spacer(modifier = Modifier.height(14.dp))

                SectionHeader(title = "HUD & Drag Mechanics")
                Text(
                    text = "• Fire Button Size: ${profile.fireButtonSize}%\n• Drag Technique: ${profile.dragStyle}\n• HUD Note: ${profile.hudNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                SectionHeader(title = "Display Density (DPI / Smallest Width)")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recommended: ${profile.recommendedDpi} DPI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RubyRedPrimary
                            )
                            Text(
                                text = "Stock Default: ${profile.defaultDpi} DPI",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile.dpiAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "How to adjust: Settings ➔ Developer Options ➔ Smallest width. If setting 600+, turn it back off/down after playing!",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionHeader(title = "Recommended Drill")
                Text(
                    text = recommendation!!.drillRoutine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentAmber
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlowButton(
                    text = "Save To My Profiles",
                    icon = Icons.Default.Bookmark,
                    onClick = {
                        viewModel.saveAsProfile {
                            onNavigateToProfiles()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDpi() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open DPI Calculator",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                            Text(
                                text = "Calculate exact Smallest Width with visual gauge & brand tips",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTouchLab() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calibrate With Finger Swipe Speed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            Text(
                                text = "Test actual drag velocity in px/s & screen friction",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    GlowButton(
                        text = "Calibrate Aim",
                        icon = Icons.Default.Tune,
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalibration
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlowButton(
                        text = "Restart",
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.resetWizard() }
                    )
                }
            }
        } else {
            // Wizard Steps
            when (currentStep) {
                1 -> {
                    // Step 1: Device
                    SectionHeader(title = "Step 1: Your Device", subtitle = "Detected from legitimate hardware APIs")
                    val specs = viewModel.deviceSpecs
                    GamingCard {
                        Text(
                            text = "${specs.manufacturer} ${specs.model}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Refresh Rate: ${specs.refreshRate} Hz • RAM: ${specs.totalRamGb} GB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Performance Tier: ${specs.performanceClass.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RubyRedPrimary
                        )
                        Text(
                            text = "Display Density: ${specs.displayDensityDpi} DPI (Screen Density)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                2 -> {
                    // Step 2: Playstyle
                    SectionHeader(title = "Step 2: Combat Playstyle", subtitle = "How do you engage opponents in gunfights?")
                    Playstyle.values().forEach { style ->
                        val isSelected = selectedPlaystyle == style
                        GamingCard(
                            modifier = Modifier.padding(vertical = 4.dp),
                            borderColor = if (isSelected) RubyRedPrimary else BorderStroke,
                            backgroundColor = if (isSelected) ElevatedSurface else CharcoalSurface,
                            onClick = { viewModel.selectedPlaystyle.value = style }
                        ) {
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                3 -> {
                    // Step 3: Finger Setup
                    SectionHeader(title = "Step 3: Finger Setup", subtitle = "How many fingers do you play with on your HUD?")
                    FingerSetup.values().forEach { setup ->
                        val isSelected = selectedFingerSetup == setup
                        GamingCard(
                            modifier = Modifier.padding(vertical = 4.dp),
                            borderColor = if (isSelected) RubyRedPrimary else BorderStroke,
                            backgroundColor = if (isSelected) ElevatedSurface else CharcoalSurface,
                            onClick = { viewModel.selectedFingerSetup.value = setup }
                        ) {
                            Text(
                                text = setup.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = "Recommended Fire Button: ${setup.fireButtonRecommendation}% scale",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                4 -> {
                    // Step 4: Weapon Style
                    SectionHeader(title = "Step 4: Primary Weapon Style", subtitle = "Which weapon category do you rely on most?")
                    WeaponType.values().forEach { weapon ->
                        val isSelected = selectedWeapon == weapon
                        GamingCard(
                            modifier = Modifier.padding(vertical = 4.dp),
                            borderColor = if (isSelected) RubyRedPrimary else BorderStroke,
                            backgroundColor = if (isSelected) ElevatedSurface else CharcoalSurface,
                            onClick = { viewModel.selectedWeapon.value = weapon }
                        ) {
                            Text(
                                text = weapon.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = "Sample: ${weapon.sampleWeapons}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                5 -> {
                    // Step 5: Current Problem
                    SectionHeader(title = "Step 5: Current Aim Problem", subtitle = "What consistently goes wrong during your drags?")
                    AimProblem.values().forEach { problem ->
                        val isSelected = selectedProblem == problem
                        GamingCard(
                            modifier = Modifier.padding(vertical = 4.dp),
                            borderColor = if (isSelected) RubyRedPrimary else BorderStroke,
                            backgroundColor = if (isSelected) ElevatedSurface else CharcoalSurface,
                            onClick = { viewModel.selectedProblem.value = problem }
                        ) {
                            Text(
                                text = problem.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) RubyRedPrimary else TextPrimary
                            )
                            Text(
                                text = problem.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                6 -> {
                    // Step 6: Current Sensitivity Benchmark
                    SectionHeader(title = "Step 6: Current In-Game Values", subtitle = "Enter what you currently have inside Free Fire settings")
                    GamingCard {
                        OutlinedTextField(
                            value = currentGeneralInput,
                            onValueChange = { viewModel.currentGeneralInput.value = it },
                            label = { Text("Current General (0 - 200)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RubyRedPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = currentRedDotInput,
                            onValueChange = { viewModel.currentRedDotInput.value = it },
                            label = { Text("Current Red Dot (0 - 200)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RubyRedPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                if (currentStep > 1) {
                    GlowButton(
                        text = "Back",
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.prevStep() }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                GlowButton(
                    text = if (currentStep == 6) "GENERATE PROFILE" else "Next Step",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.nextStep() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
