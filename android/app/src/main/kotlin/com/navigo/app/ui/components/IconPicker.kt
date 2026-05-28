package com.navigo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * Collapsed icon picker — shows the currently selected icon + a "Change"
 * action; the full 24-icon grid opens in a modal bottom sheet on tap.
 *
 * Used on the Edit screen where the full grid would dominate the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerCompact(
    selectedKey: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ShortcutIcon(
                    imageVector = ShortcutIconCatalog.forKey(selectedKey),
                    contentDescription = ShortcutIconCatalog.labelFor(selectedKey),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                ShortcutIconCatalog.labelFor(selectedKey),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Tap “Show more” to pick a different icon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { showSheet = true }) { Text("Show more") }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Choose an icon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                IconPicker(
                    selectedKey = selectedKey,
                    onIconSelected = { key ->
                        onIconSelected(key)
                        showSheet = false
                    },
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 16.dp))
            }
        }
    }
}
