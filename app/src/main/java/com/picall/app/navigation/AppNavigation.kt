package com.picall.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.picall.app.ui.screens.EditorScreen
import com.picall.app.ui.screens.PresetsScreen
import com.picall.app.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Editor : Screen("editor")
    data object Presets : Screen("presets")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Editor.route
    ) {
        composable(Screen.Editor.route) {
            EditorScreen(
                onNavigateToPresets = {
                    navController.navigate(Screen.Presets.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Presets.route) {
            PresetsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
