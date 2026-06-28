package com.vialo.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * One TopAppBar used by every Scaffold in the app.
 *
 * Settles the small inconsistencies that built up over a few iterations:
 * title style (some screens used `titleLarge`, some `headlineSmall`, some
 * raw `Text`), the always-transparent container (so the gradient
 * background reads), and the AutoMirrored back arrow.
 *
 * Caller picks whether to show a back button by passing or omitting
 * [onBack]. [actions] receives a [RowScope] so the call site can stack
 * IconButtons just like the platform [TopAppBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VialoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
    ),
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title, style = titleStyle) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
    )
}
