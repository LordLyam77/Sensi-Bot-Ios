package com.sensibotpro.ui.screens.license

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sensibotpro.ui.theme.RubyRedPrimary
import com.sensibotpro.ui.theme.TextMuted
import com.sensibotpro.ui.theme.TextPrimary
import com.sensibotpro.ui.theme.VoidBlack

/**
 * GateSplashScreen displays a sleek verifying screen on app launch / cold start
 * while the server confirms the active session token.
 */
@Composable
fun GateSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = RubyRedPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SENSI BOT PRO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verifying license credentials...",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
