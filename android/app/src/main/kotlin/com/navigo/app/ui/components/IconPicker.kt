package com.navigo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navigo.app.ui.icons.ShortcutIcon
import com.navigo.app.ui.icons.ShortcutIconCatalog

/**
 * Grid of the 24 shortcut icons. Tap to select.
 *
 * Visual selection state: a primary-coloured ring + a tinted background.
 */
@Composable
fun IconPicker(
    selectedKey: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 5,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.heightIn(max = 320.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
    ) {
        items(ShortcutIconCatalog.entries, key = { it.key }) { entry ->
            val isSelected = entry.key == selectedKey
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null,
                onClick = { onIconSelected(entry.key) },
                modifier = Modifier.aspectRatio(1f),
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ShortcutIcon(
                        imageVector = entry.image,
                        contentDescription = entry.label,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
