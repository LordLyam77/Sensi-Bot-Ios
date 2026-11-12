package com.sensibotpro.headlock.ui

import androidx.compose.runtime.Composable
import com.sensibotpro.headlock.HeadlockModule

@Composable
fun HeadlockGate(onNavigateBack: () -> Unit = {}) {
    if (!HeadlockModule.isFeatureEnabled) return
    HeadlockScreen(onNavigateBack = onNavigateBack)
}

