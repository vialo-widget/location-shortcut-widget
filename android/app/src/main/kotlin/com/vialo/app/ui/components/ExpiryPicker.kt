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

/** Single row of FilterChips, one per [ExpiryOption]. */
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
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.shortLabel) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
