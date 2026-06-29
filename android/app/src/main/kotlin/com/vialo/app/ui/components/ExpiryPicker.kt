package com.vialo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vialo.app.data.model.ExpiryOption

/**
 * Single row of FilterChips, one per [ExpiryOption].
 *
 * The first chip ([ExpiryOption.NEVER]) gets a wider slot than the rest —
 * "Never" is the only label that's a real word, so equal-weight chips
 * forced it to wrap to two lines on narrow screens. The duration chips
 * ("3d" / "1w" / "1m" / "1y") are short enough to share the remaining
 * space equally without crowding.
 */
@Composable
fun ExpiryPicker(
    selected: ExpiryOption,
    onSelect: (ExpiryOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ExpiryOption.entries.forEach { option ->
            val weight = if (option == ExpiryOption.NEVER) NEVER_WEIGHT else DURATION_WEIGHT
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.shortLabel, maxLines = 1) },
                modifier = Modifier.weight(weight),
            )
        }
    }
}

// Picked by eye on a 360-dp screen: NEVER claims ~24% of the row vs the
// ~19% each duration chip gets, enough to keep "Never" on one line under
// FilterChip's internal padding without making the duration chips feel
// pinched.
private const val NEVER_WEIGHT = 1.4f
private const val DURATION_WEIGHT = 1.0f
