package com.vialo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vialo.app.ui.pairing.AcceptInviteScreen
import com.vialo.app.ui.pairing.EnterCodeScreen
import com.vialo.app.ui.pairing.GenerateCodeScreen
import com.vialo.app.ui.pairing.WhoHelpingMeScreen
import com.vialo.app.ui.pairing.CarerModeScreen
import com.vialo.app.ui.screens.add.AddShortcutScreen
import com.vialo.app.ui.screens.confirm.ConfirmAddScreen
import com.vialo.app.ui.screens.edit.EditShortcutScreen
import com.vialo.app.ui.screens.home.HomeScreen
import com.vialo.app.ui.screens.onboarding.OnboardingScreen
import com.vialo.app.ui.screens.settings.SettingsScreen

@Composable
fun VialoNavHost(
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
                onOpenCarerMode = { navController.navigate(Destinations.CARER_MODE) },
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
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenWhoHelpingMe = { navController.navigate(Destinations.WHO_HELPING_ME) },
            )
        }

        // ─── Pairing flow ──────────────────────────────────────────────
        composable(Destinations.WHO_HELPING_ME) {
            WhoHelpingMeScreen(
                onBack = { navController.popBackStack() },
                onAddHelper = { navController.navigate(Destinations.GENERATE_CODE) },
            )
        }
        composable(Destinations.GENERATE_CODE) {
            GenerateCodeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Destinations.ACCEPT_INVITE_ROUTE,
            arguments = listOf(navArgument("pendingId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val pendingId = backStackEntry.arguments?.getString("pendingId").orEmpty()
            AcceptInviteScreen(
                pendingId = pendingId,
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.popBackStack(Destinations.HOME, inclusive = false)
                },
            )
        }
        composable(Destinations.CARER_MODE) {
            CarerModeScreen(
                onBack = { navController.popBackStack() },
                onAddCaree = { navController.navigate(Destinations.ENTER_CODE) },
            )
        }
        composable(Destinations.ENTER_CODE) {
            EnterCodeScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
    }
}
