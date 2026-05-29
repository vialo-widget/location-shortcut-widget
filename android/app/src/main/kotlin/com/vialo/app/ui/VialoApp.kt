package com.vialo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.vialo.app.data.Graph
import com.vialo.app.deeplink.DeepLinkBus
import com.vialo.app.service.deeplink.DeepLinkParser
import com.vialo.app.ui.components.VialoBackground
import com.vialo.app.ui.navigation.Destinations
import com.vialo.app.ui.navigation.VialoNavHost
import com.vialo.app.ui.theme.VialoTheme

/**
 * Top-level Composable hosted by [com.vialo.app.MainActivity].
 *
 * Owns the navigation controller, exposes the [Graph] and [ActivityBridges]
 * via CompositionLocals, and routes incoming deep-link URIs to the Confirm
 * screen by parking the parsed payload on [Graph.pendingShortcutHolder].
 *
 * Start destination depends on whether first-launch onboarding has been
 * completed (persisted via [Graph.appSettings]). While the flag is loading
 * we render only the background gradient — DataStore reads are typically
 * sub-100 ms so the user shouldn't perceive a delay.
 */
@Composable
fun VialoApp(
    graph: Graph,
    onRequestPinWidget: () -> Boolean,
    isWidgetPinned: () -> Boolean,
    isAppLinkVerified: () -> Boolean,
    openAppLinkSettings: () -> Unit,
) {
    val bridges = remember(
        onRequestPinWidget, isWidgetPinned, isAppLinkVerified, openAppLinkSettings,
    ) {
        ActivityBridges(
            requestPinWidget = onRequestPinWidget,
            isWidgetPinned = isWidgetPinned,
            isAppLinkVerified = isAppLinkVerified,
            openAppLinkSettings = openAppLinkSettings,
        )
    }

    CompositionLocalProvider(
        LocalGraph provides graph,
        LocalActivityBridges provides bridges,
    ) {
        VialoTheme {
            VialoBackground {
                val hasSeenOnboarding by graph.appSettings.hasSeenOnboarding
                    .collectAsStateWithLifecycle(initialValue = null)
                hasSeenOnboarding?.let { seen ->
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        DeepLinkBus.uris.collect { uri ->
                            val pending = DeepLinkParser.parse(uri) ?: return@collect
                            graph.pendingShortcutHolder.set(pending)
                            navController.navigate(Destinations.CONFIRM_ADD)
                        }
                    }
                    VialoNavHost(
                        startDestination = if (seen) Destinations.HOME else Destinations.ONBOARDING,
                        navController = navController,
                    )
                }
            }
        }
    }
}
