package com.sensibotpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sensibotpro.ui.navigation.AppNavigation
import com.sensibotpro.ui.theme.SensiBotProTheme
import com.sensibotpro.ui.viewmodel.AuthGatekeeperViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthGatekeeperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensiBotProTheme {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Periodic re-validation: Re-check validity whenever the app resumes from background
        // to catch revoked keys or manual server resets without requiring a full app restart.
        authViewModel.checkOnResume()
    }
}
