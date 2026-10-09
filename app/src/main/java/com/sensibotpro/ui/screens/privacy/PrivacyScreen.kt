package com.sensibotpro.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensibotpro.ui.components.GamingCard
import com.sensibotpro.ui.components.GlowButton
import com.sensibotpro.ui.components.SectionHeader
import com.sensibotpro.ui.theme.*

@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                    text = "PRIVACY POLICY",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Offline First • Transparent Data Handling",
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

        GamingCard(borderColor = BorderStroke) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RubyRedPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Local-Only Data Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SENSI BOT Pro v1.0.1 operates as an offline coaching utility. Your configurations, chat conversations, and custom profiles remain strictly on your local device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Key Commitments")

        val commitments = listOf(
            "Device Information" to "Collected purely via standard Android hardware APIs (Display refresh rate, model, RAM) to calculate sensitivity baseline formulas. Never uploaded to remote third-party trackers.",
            "Zero Game Access" to "We do not inspect, read, or harvest any data from Garena Free Fire. We do not access Free Fire files or memory.",
            "License Storage" to "License keys and generated installation IDs are saved in encrypted local SharedPreferences for persistent offline verification.",
            "No Ads or Data Brokering" to "SENSI BOT Pro is supported by a simple ₹299 one-time purchase. We do not show ads and never sell user data."
        )

        commitments.forEach { (title, desc) ->
            GamingCard(
                modifier = Modifier.padding(vertical = 4.dp),
                backgroundColor = ElevatedSurface
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RubyRedPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlowButton(
            text = "Back",
            isSecondary = true,
            onClick = onNavigateBack
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
