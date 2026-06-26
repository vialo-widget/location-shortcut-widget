package com.vialo.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vialo.app.data.model.TransportMode

/**
 * Three-way picker that decides which app handles a shortcut tap.
 *
 * Segmented row (Drive / Transit / Uber) — matches the widget-style
 * picker in Settings so the visual language is consistent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportModePicker(
    selected: TransportMode,
    onSelect: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = TransportMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(mode.label())
                },
            )
        }
    }
}

private fun TransportMode.label(): String = when (this) {
    TransportMode.DRIVE -> "Drive"
    TransportMode.TRANSIT -> "Transit"
    TransportMode.UBER -> "Uber"
}

private fun TransportMode.icon(): ImageVector = when (this) {
    TransportMode.DRIVE -> Icons.Outlined.DirectionsCar
    TransportMode.TRANSIT -> Icons.Outlined.DirectionsBus
    TransportMode.UBER -> Icons.Outlined.LocalTaxi
}

/** Used by Add/Edit between the chip and the search field — exported so
 *  both screens stay visually aligned without each defining its own gap. */
@Suppress("unused")
@Composable
fun TransportModePickerSpacer() {
    Spacer(modifier = Modifier.width(8.dp))
}
