package com.picall.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        composable(Screen.Editor.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val loadPresetId = savedStateHandle.get<Long>("load_preset_id")

            EditorScreen(
                loadPresetId = loadPresetId,
                onConsumeLoadPreset = { savedStateHandle.remove<Long>("load_preset_id") },
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
                onNavigateBack = { navController.popBackStack() },
                onLoadPreset = { presetId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("load_preset_id", presetId)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
