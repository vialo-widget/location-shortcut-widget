package com.vialo.app.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.model.ExpiryStatus
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.computeExpiryStatus
import com.vialo.app.data.model.expiryBadgeText
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.icons.ShortcutIcon
import com.vialo.app.ui.icons.ShortcutIconCatalog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddShortcut: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditShortcut: (String) -> Unit,
) {
    val graph = LocalGraph.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(graph) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var actionTarget by remember { mutableStateOf<Shortcut?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vialo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddShortcut,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add shortcut") },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Loading…",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            state.shortcuts.isEmpty() -> EmptyHomeState(
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.shortcuts, key = { it.id }) { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onTap = { vm.launchNavigation(context, shortcut) },
                        onLongPress = { actionTarget = shortcut },
                    )
                }
            }
        }
    }

    actionTarget?.let { target ->
        ShortcutActionsSheet(
            shortcut = target,
            onDismiss = { actionTarget = null },
            onNavigate = { vm.launchNavigation(context, target); actionTarget = null },
            onShare = { vm.share(context, target); actionTarget = null },
            onEdit = { actionTarget = null; onEditShortcut(target.id) },
            onDelete = { vm.delete(target); actionTarget = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val status = computeExpiryStatus(shortcut.expiresAt, shortcut.createdAt)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = tileBackground(status),
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ShortcutIcon(
                    imageVector = ShortcutIconCatalog.forKey(shortcut.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                shortcut.expiresAt?.let { ExpiryBadge(status, expiryBadgeText(it)) }
            }
            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpiryBadge(status: ExpiryStatus, text: String) {
    val (bg, fg) = badgeColors(status)
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun tileBackground(status: ExpiryStatus): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return when (status) {
        ExpiryStatus.URGENT -> if (isDark) Color(0xFF3D0A0A) else Color(0xFFFDE8E8)
        ExpiryStatus.WARNING -> if (isDark) Color(0xFF2E1A00) else Color(0xFFFFF3E0)
        else -> scheme.surfaceVariant
    }
}

private fun badgeColors(status: ExpiryStatus): Pair<Color, Color> = when (status) {
    ExpiryStatus.URGENT -> Color(0xFFC62828) to Color.White
    ExpiryStatus.WARNING -> Color(0xFFFF8F00) to Color.White
    ExpiryStatus.SUBTLE -> Color(0xFF9E9E9E) to Color.White
    ExpiryStatus.NONE -> Color.Transparent to Color.Transparent
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutActionsSheet(
    shortcut: Shortcut,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                shortcut.label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SheetAction("Navigate") { onNavigate() }
            SheetAction("Share") { onShare() }
            SheetAction("Edit") { onEdit() }
            SheetAction("Delete", destructive = true) { onDelete() }
        }
    }
}

@Composable
private fun SheetAction(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No shortcuts yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tap “Add shortcut” to save your first place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
