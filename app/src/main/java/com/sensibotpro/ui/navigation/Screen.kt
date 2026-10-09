package com.sensibotpro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object SensiFinder : Screen("sensi_finder", "Sensi Finder", Icons.Default.Search)
    object SensiBot : Screen("sensi_bot", "Sensi Bot", Icons.Default.SmartToy)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    // Additional sub-screens
    object Calibration : Screen("calibration", "Calibrate")
    object OverlayControl : Screen("overlay_control", "Floating & Crosshair")
    object License : Screen("license", "Unlock Pro")
    object FairPlay : Screen("fair_play", "Fair Play")
    object Support : Screen("support", "Support")
    object Privacy : Screen("privacy", "Privacy")
    object DpiCalculator : Screen("dpi_calculator", "DPI Calculator")
    object FloatingAssistant : Screen("floating_assistant", "Assistant", Icons.Default.Layers)
    object TouchLab : Screen("touch_lab", "Touch Velocity Lab")
}
