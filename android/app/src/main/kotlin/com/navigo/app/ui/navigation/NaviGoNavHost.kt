package com.navigo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.navigo.app.ui.screens.add.AddShortcutScreen
import com.navigo.app.ui.screens.confirm.ConfirmAddScreen
import com.navigo.app.ui.screens.edit.EditShortcutScreen
import com.navigo.app.ui.screens.home.HomeScreen
import com.navigo.app.ui.screens.onboarding.OnboardingScreen
import com.navigo.app.ui.screens.settings.SettingsScreen

@Composable
fun NaviGoNavHost(
    startDestination: String = Destinations.HOME,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Destinations.HOME) {
                        // Pop the onboarding entry so back from Home exits
                        // the app instead of bringing the user back here.
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destinations.HOME) {
            HomeScreen(
                onAddShortcut = { navController.navigate(Destinations.ADD) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onEditShortcut = { id -> navController.navigate(Destinations.edit(id)) },
            )
        }
        composable(Destinations.ADD) {
            AddShortcutScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Destinations.CONFIRM_ADD) {
            ConfirmAddScreen(
                onBack = { navController.popBackStack() },
                onConfirmed = { navController.popBackStack() },
            )
        }
        composable(
            Destinations.EDIT_ROUTE,
            arguments = listOf(navArgument("shortcutId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("shortcutId").orEmpty()
            EditShortcutScreen(shortcutId = id, onClose = { navController.popBackStack() })
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
