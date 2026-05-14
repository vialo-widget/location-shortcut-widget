package com.navigo.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.navigo.app.data.Graph

/**
 * CompositionLocal that exposes the app's [Graph] to any Composable below
 * [NaviGoApp]. Screens read it to obtain repositories, settings, etc.
 */
val LocalGraph = staticCompositionLocalOf<Graph> {
    error("LocalGraph not provided — wrap content in NaviGoApp { … }.")
}
