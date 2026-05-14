package com.navigo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.navigo.app.data.Graph
import com.navigo.app.ui.navigation.NaviGoNavHost
import com.navigo.app.ui.theme.NaviGoTheme

/**
 * Top-level Composable, hosted by [com.navigo.app.MainActivity].
 *
 * Native bridges (widget pinning, app-link verification) flow in as lambdas
 * so the UI layer never touches platform-specific Activity APIs directly.
 * The [Graph] is exposed via [LocalGraph] so any screen can grab the repo it
 * needs without prop-drilling.
 */
@Composable
fun NaviGoApp(
    graph: Graph,
    onRequestPinWidget: () -> Boolean,
    isWidgetPinned: () -> Boolean,
    isAppLinkVerified: () -> Boolean,
    openAppLinkSettings: () -> Unit,
) {
    CompositionLocalProvider(LocalGraph provides graph) {
        NaviGoTheme {
            // TODO(Phase 4): plumb the lambdas through to Settings and Add screens.
            NaviGoNavHost()
        }
    }
}
