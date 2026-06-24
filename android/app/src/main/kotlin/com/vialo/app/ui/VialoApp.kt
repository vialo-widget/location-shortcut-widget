package com.vialo.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vialo.app.data.Graph
import com.vialo.app.deeplink.DeepLinkBus
import com.vialo.app.service.deeplink.DeepLinkParser
import com.vialo.app.ui.components.VialoBackground
import com.vialo.app.ui.navigation.Destinations
import com.vialo.app.ui.navigation.VialoNavHost
import com.vialo.app.ui.pairing.PairingInviteOverlay
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
                            // Pairing notification taps → AcceptInviteScreen.
                            if (uri.scheme == "vialo" && uri.host == "pair" &&
                                uri.pathSegments.firstOrNull() == "accept"
                            ) {
                                val pendingId = uri.getQueryParameter("pendingId")
                                if (!pendingId.isNullOrBlank()) {
                                    navController.navigate(Destinations.acceptInvite(pendingId))
                                }
                                return@collect
                            }

                            // Shared shortcut link → ConfirmAddScreen.
                            val pending = DeepLinkParser.parse(uri) ?: return@collect
                            graph.pendingShortcutHolder.set(pending)
                            navController.navigate(Destinations.CONFIRM_ADD)
                        }
                    }

                    // In-memory only — "Skip for now" hides the overlay for
                    // the current process; the next cold start resurfaces
                    // any still-pending invites.
                    var skipped by remember { mutableStateOf<Set<String>>(emptySet()) }
                    val currentEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentEntry?.destination?.route
                    // Don't interrupt the welcome flow, and don't double up
                    // with the dedicated AcceptInviteScreen reached via FCM
                    // tap — that screen already shows the same prompt.
                    val overlayAllowed = seen &&
                        currentRoute != Destinations.ONBOARDING &&
                        currentRoute != Destinations.ACCEPT_INVITE_ROUTE

                    Box(modifier = Modifier.fillMaxSize()) {
                        VialoNavHost(
                            startDestination = if (seen) Destinations.HOME else Destinations.ONBOARDING,
                            navController = navController,
                        )
                        if (overlayAllowed) {
                            PairingInviteOverlay(
                                skippedPendingIds = skipped,
                                onSkip = { pendingId -> skipped = skipped + pendingId },
                            )
                        }
                    }
                }
            }
        }
    }
}
