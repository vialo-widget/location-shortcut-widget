package com.navigo.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.navigo.app.ui.screens.home.HomeScreen

@Composable
fun NaviGoNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                onAddShortcut = { navController.navigate(Destinations.ADD) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }
        composable(Destinations.ADD) { StubScreen("Add shortcut") }
        composable(Destinations.SETTINGS) { StubScreen("Settings") }
    }
}

@Composable
private fun StubScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$name — coming in Phase 4")
    }
}
