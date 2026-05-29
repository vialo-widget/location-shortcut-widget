package com.vialo.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.vialo.app.data.Graph

/**
 * CompositionLocal that exposes the app's [Graph] to any Composable below
 * [VialoApp]. Screens read it to obtain repositories, settings, etc.
 */
val LocalGraph = staticCompositionLocalOf<Graph> {
    error("LocalGraph not provided — wrap content in VialoApp { … }.")
}
