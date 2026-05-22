package com.navigo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.navigo.app.data.Graph
import com.navigo.app.deeplink.DeepLinkBus
import com.navigo.app.service.deeplink.DeepLinkParser
import com.navigo.app.ui.components.NaviGoBackground
import com.navigo.app.ui.navigation.Destinations
import com.navigo.app.ui.navigation.NaviGoNavHost
import com.navigo.app.ui.theme.NaviGoTheme

/**
 * Top-level Composable hosted by [com.navigo.app.MainActivity].
 *
 * Owns the navigation controller, exposes the [Graph] and [ActivityBridges]
 * via CompositionLocals, and routes incoming deep-link URIs to the Confirm
 * screen by parking the parsed payload on [Graph.pendingShortcutHolder].
 */
@Composable
fun NaviGoApp(
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
        NaviGoTheme {
            NaviGoBackground {
                val navController = rememberNavController()
                LaunchedEffect(navController) {
                    DeepLinkBus.uris.collect { uri ->
                        val pending = DeepLinkParser.parse(uri) ?: return@collect
                        graph.pendingShortcutHolder.set(pending)
                        navController.navigate(Destinations.CONFIRM_ADD)
                    }
                }
                NaviGoNavHost(navController = navController)
            }
        }
    }
}
