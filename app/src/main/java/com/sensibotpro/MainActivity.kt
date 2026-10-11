package com.sensibotpro

import android.os.Bundle
import android.util.Log
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
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "enableEdgeToEdge ignored", e)
        }
        setContent {
            SensiBotProTheme {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            authViewModel.checkOnResume()
        } catch (e: Throwable) {
            Log.e("MainActivity", "checkOnResume ignored", e)
        }
    }
}
