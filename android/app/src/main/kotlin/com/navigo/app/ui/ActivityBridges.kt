package com.navigo.app.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lambdas backed by the Activity for things that must run with an Activity
 * context (intent launching, app-link verification, widget pinning).
 *
 * Provided by [NaviGoApp] and consumed where needed (e.g. Settings screen),
 * so the UI layer doesn't have to know about [com.navigo.app.MainActivity].
 */
data class ActivityBridges(
    val requestPinWidget: () -> Boolean,
    val isWidgetPinned: () -> Boolean,
    val isAppLinkVerified: () -> Boolean,
    val openAppLinkSettings: () -> Unit,
)

val LocalActivityBridges = staticCompositionLocalOf<ActivityBridges> {
    error("LocalActivityBridges not provided — wrap content in NaviGoApp { … }.")
}
