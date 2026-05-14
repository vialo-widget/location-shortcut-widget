package com.navigo.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.duotone.Bank
import com.adamglin.phosphoricons.duotone.House
import com.adamglin.phosphoricons.duotone.MapPin
import com.navigo.app.data.model.Shortcut
import com.navigo.app.ui.LocalGraph

/**
 * Placeholder home screen — built during the Flutter→native rewrite to prove
 * the project compiles end-to-end with Phosphor icons and that the data layer
 * (Room + the legacy-prefs importer) is delivering shortcuts. Replaced in
 * Phase 4 with the real shortcut grid.
 */
@Composable
fun HomeScreen(
    onAddShortcut: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val repo = LocalGraph.current.shortcutRepository
    val shortcuts by repo.shortcuts.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NaviGo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Rewrite in progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Phosphor Duotone icons wired up:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconChip(PhosphorIcons.Duotone.House, "Home")
                IconChip(PhosphorIcons.Duotone.Bank, "Bank")
                IconChip(PhosphorIcons.Duotone.MapPin, "Place")
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Saved shortcuts (${shortcuts.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (shortcuts.isEmpty()) {
                Text(
                    text = "No shortcuts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shortcuts, key = { it.id }) { ShortcutRow(it) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddShortcut, modifier = Modifier.fillMaxWidth()) {
                Text("Add shortcut")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Settings")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: Shortcut) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(shortcut.label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "%.5f, %.5f · %s".format(
                    shortcut.latitude,
                    shortcut.longitude,
                    shortcut.iconName,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
