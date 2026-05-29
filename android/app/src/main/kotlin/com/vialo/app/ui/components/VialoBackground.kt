package com.vialo.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

/**
 * App-wide background — a radial gradient with a slight green tint anchored
 * at the top-left corner, fading into a slight grey across the rest of the
 * screen. Applied once in [com.vialo.app.ui.VialoApp] so every screen
 * inherits it without each Scaffold redeclaring its own colour.
 *
 * Tile and card backgrounds (surfaceVariant, expiry tints, etc.) remain
 * opaque on top, so the gradient is purely atmospheric.
 *
 * Each Scaffold using this needs `containerColor = Color.Transparent`, and
 * its TopAppBar `containerColor = Color.Transparent` too — otherwise the
 * Scaffold's default surface fill paints over the gradient.
 */
@Composable
fun VialoBackground(content: @Composable BoxScope.() -> Unit) {
    val isDark = isSystemInDarkTheme()
    val (centerColor, edgeColor) = if (isDark) {
        Color(0xFF0F1611) to Color(0xFF14151A)
    } else {
        Color(0xFFE2EDE3) to Color(0xFFEDEDED)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Anchor the gradient at the top-left corner and let its
                // radius reach the far (bottom-right) corner so the falloff
                // covers the whole screen no matter the aspect ratio.
                val diagonal = sqrt(size.width * size.width + size.height * size.height)
                    .coerceAtLeast(1f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(centerColor, edgeColor),
                        center = Offset.Zero,
                        radius = diagonal,
                    ),
                )
            },
        content = content,
    )
}
