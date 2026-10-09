package com.sensibotpro.ui.screens.fairplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
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
fun FairPlayScreen(
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
                    text = "FAIR PLAY MANIFESTO",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "100% Legitimate Coaching & Aim Utility",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGreen
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

        // Core Mission Badge
        GamingCard(borderColor = AccentGreen) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "OUR PLEDGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Text(
                        text = "\"We don't play for you. We help you play better.\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Negative Commitments (What the app NEVER does)
        SectionHeader(
            title = "Strict Anti-Cheat Standards",
            subtitle = "Zero risk of game account bans"
        )

        val strictRules = listOf(
            "We do NOT modify Free Fire" to "No game files, assets, or configs are ever altered.",
            "This is not a hack" to "It only optimizes your gameplay, device touch response, and DPI ergonomics.",
            "We do NOT provide aim assist" to "Your aiming is 100% your own manual input and hand-eye coordination.",
            "We do NOT automate aiming or recoil" to "No macros, auto-touch gestures, or scripts.",
            "We do NOT read Free Fire memory" to "Zero access to game process or memory space.",
            "We do NOT bypass anti-cheat" to "The app operates as an external, independent advisory utility."
        )

        strictRules.forEach { (rule, desc) ->
            GamingCard(
                modifier = Modifier.padding(vertical = 4.dp),
                backgroundColor = ElevatedSurface
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.titleMedium,
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

        Spacer(modifier = Modifier.height(20.dp))

        // What the app provides
        SectionHeader(
            title = "What SENSI BOT Pro Provides",
            subtitle = "Independent reference and coaching"
        )
        GamingCard {
            Text(
                text = "SENSI BOT Pro calculates hardware-specific starting sensitivities, teaches drag mechanics (straight drag vs J-drag), offers training ground practice drills, and provides visual overlays for centering practice. All sensitivity values are entered manually by you into Free Fire's native settings menu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElevatedSurface
            ) {
                Text(
                    text = "This is not a hack, it only optimizes your gameplay. • Lyam FF Verified",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlowButton(
            text = "Back to App",
            isSecondary = true,
            onClick = onNavigateBack
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
