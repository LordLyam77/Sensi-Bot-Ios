package com.sensibotpro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.sensibotpro.ui.screens.assistant.FloatingAssistantScreen
import com.sensibotpro.ui.screens.fairplay.FairPlayScreen
import com.sensibotpro.ui.screens.home.HomeScreen
import com.sensibotpro.ui.screens.license.LicenseScreen
import com.sensibotpro.ui.screens.overlay.OverlayScreen
import com.sensibotpro.ui.screens.privacy.PrivacyScreen
import com.sensibotpro.ui.screens.profile.ProfileScreen
import com.sensibotpro.ui.screens.sensibot.SensiBotScreen
import com.sensibotpro.ui.screens.sensifinder.CalibrationScreen
import com.sensibotpro.ui.screens.sensifinder.DpiCalculatorScreen
import com.sensibotpro.ui.screens.sensifinder.SensiFinderScreen
import com.sensibotpro.ui.screens.support.SupportScreen
import com.sensibotpro.ui.theme.*
import com.sensibotpro.ui.viewmodel.*

import com.sensibotpro.ui.screens.license.GateSplashScreen
import com.sensibotpro.ui.screens.license.LicenseGateScreen
import com.sensibotpro.ui.viewmodel.AuthGateState
import com.sensibotpro.ui.viewmodel.AuthGatekeeperViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthGatekeeperViewModel = viewModel()
) {
    val authState by authViewModel.gateState.collectAsState()

    // STRICT GATEKEEPER:
    // If not authenticated, NO route or screen can be rendered.
    when (authState) {
        is AuthGateState.Checking -> {
            GateSplashScreen()
            return
        }
        is AuthGateState.Unauthenticated -> {
            LicenseGateScreen(viewModel = authViewModel)
            return
        }
        is AuthGateState.Authenticated -> {
            // Unlocks complete application navigation graph
        }
    }

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.SensiFinder,
        Screen.FloatingAssistant,
        Screen.SensiBot,
        Screen.Profile
    )

    @OptIn(ExperimentalLayoutApi::class)
    val isImeVisible = WindowInsets.isImeVisible
    val showBottomBar = bottomNavScreens.any { it.route == currentDestination?.route } &&
        !(currentDestination?.route == Screen.SensiBot.route && isImeVisible)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CharcoalSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentDestination?.route == screen.route
                        NavigationBarItem(
                            icon = {
                                if (screen.icon != null) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextPrimary,
                                selectedTextColor = RubyRedPrimary,
                                indicatorColor = RubyRedPrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToSensiFinder = { navController.navigate(Screen.SensiFinder.route) },
                    onNavigateToSensiBot = { navController.navigate(Screen.SensiBot.route) },
                    onNavigateToOverlay = { navController.navigate(Screen.FloatingAssistant.route) },
                    onNavigateToFairPlay = { navController.navigate(Screen.FairPlay.route) },
                    onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) },
                    onNavigateToDpi = { navController.navigate(Screen.DpiCalculator.route) },
                    onNavigateToTouchLab = { navController.navigate(Screen.TouchLab.route) }
                )
            }
            composable(Screen.SensiFinder.route) {
                val finderVm: SensiFinderViewModel = viewModel()
                SensiFinderScreen(
                    viewModel = finderVm,
                    onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) },
                    onNavigateToProfiles = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDpi = { navController.navigate(Screen.DpiCalculator.route) },
                    onNavigateToTouchLab = { navController.navigate(Screen.TouchLab.route) }
                )
            }
            composable(Screen.SensiBot.route) {
                val botVm: SensiBotViewModel = viewModel()
                SensiBotScreen(viewModel = botVm)
            }
            composable(Screen.Profile.route) {
                val profileVm: ProfileViewModel = viewModel()
                ProfileScreen(
                    viewModel = profileVm,
                    onNavigateToLicense = { navController.navigate(Screen.License.route) },
                    onNavigateToFairPlay = { navController.navigate(Screen.FairPlay.route) },
                    onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
                )
            }
            composable(Screen.Calibration.route) {
                val calibVm: CalibrationViewModel = viewModel()
                CalibrationScreen(
                    viewModel = calibVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.OverlayControl.route) {
                val overlayVm: OverlayViewModel = viewModel()
                OverlayScreen(viewModel = overlayVm)
            }
            composable(Screen.License.route) {
                val licenseVm: LicenseViewModel = viewModel()
                LicenseScreen(
                    viewModel = licenseVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FairPlay.route) {
                FairPlayScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Support.route) {
                SupportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DpiCalculator.route) {
                val finderVm: SensiFinderViewModel = viewModel()
                val activeProfile by finderVm.profileRepository.activeProfile.collectAsState()
                DpiCalculatorScreen(
                    deviceSpecs = finderVm.deviceSpecs,
                    initialGeneralSensi = activeProfile?.general
                )
            }
            composable(Screen.FloatingAssistant.route) {
                val overlayVm: OverlayViewModel = viewModel()
                FloatingAssistantScreen(
                    viewModel = overlayVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.TouchLab.route) {
                val labVm: com.sensibotpro.ui.screens.touchlab.TouchLabViewModel = viewModel()
                com.sensibotpro.ui.screens.touchlab.TouchLabScreen(
                    viewModel = labVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
