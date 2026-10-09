package com.sensibotpro.ui.screens.sensifinder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sensibotpro.domain.recommendation.CalibrationFeedback
import com.sensibotpro.ui.components.*
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.CalibrationViewModel

@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val result by viewModel.calibrationResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "CALIBRATE AIM",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Test your profile in Training Ground and tell us the result.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (activeProfile != null) {
            val p = activeProfile!!
            SectionHeader(title = "Current Profile Being Tuned", subtitle = p.name)
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "General: ${p.general}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RubyRedPrimary
                    )
                    Text(
                        text = "Red Dot: ${p.redDot}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RubyRedPrimary
                    )
                    Text(
                        text = "Button: ${p.fireButtonSize}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Result Card if feedback applied
        if (result != null) {
            val res = result!!
            SectionHeader(title = "Coaching Adjustment")
            GamingCard(
                borderColor = if (res.isOptimal) AccentGreen else RubyRedPrimary,
                backgroundColor = ElevatedSurface
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (res.isOptimal) Icons.Default.CheckCircle else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (res.isOptimal) AccentGreen else RubyRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (res.isOptimal) "SWEET SPOT REACHED!" else "ADJUSTMENT APPLIED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (res.isOptimal) AccentGreen else RubyRedPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = res.adjustmentExplanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Next Drill: ${res.nextDrill}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentAmber
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Test Outcome Options
        SectionHeader(
            title = "What happened during your training?",
            subtitle = "Choose what best describes your shot trajectory"
        )

        CalibrationFeedback.values().forEach { feedback ->
            GamingCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { viewModel.applyFeedback(feedback) }
            ) {
                Text(
                    text = feedback.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (feedback == CalibrationFeedback.PERFECT_HEADSHOTS) AccentGreen else TextPrimary
                )
                Text(
                    text = feedback.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        GlowButton(
            text = "Done Calibrating",
            isSecondary = true,
            onClick = onNavigateBack
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
