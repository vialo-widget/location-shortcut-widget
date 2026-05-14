package com.navigo.app.ui

import androidx.compose.runtime.Composable
import com.navigo.app.ui.navigation.NaviGoNavHost
import com.navigo.app.ui.theme.NaviGoTheme

/**
 * Top-level Composable, hosted by [com.navigo.app.MainActivity].
 *
 * Native bridges (widget pinning, app-link verification) flow in as lambdas
 * so the UI layer never touches platform-specific Activity APIs directly.
 */
@Composable
fun NaviGoApp(
    onRequestPinWidget: () -> Boolean,
    isWidgetPinned: () -> Boolean,
    isAppLinkVerified: () -> Boolean,
    openAppLinkSettings: () -> Unit,
) {
    NaviGoTheme {
        // TODO(Phase 4): plumb the lambdas through to Settings and Add screens.
        NaviGoNavHost()
    }
}
