package com.vialo.app.ui.screens.carer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.model.Shortcut
import com.vialo.app.service.navigation.NavigationLauncher
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.components.EmptyState
import com.vialo.app.ui.components.ShortcutTile
import com.vialo.app.ui.components.VialoTopBar
import com.vialo.app.ui.theme.VialoDimens

/**
 * Carer-side per-caree screen: "Helping <name>".
 *
 * Read-only in chunk 3 — we fetch the snapshot, render the tile grid, and
 * tapping a tile launches navigation to that place (carer en route to the
 * caree's saved spot). Long-press is a no-op for now; chunk 4 wires it to
 * an actions sheet (edit / delete) and adds a FAB to create on the caree's
 * behalf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpingCareeScreen(
    careeDeviceId: String,
    onBack: () -> Unit,
    onAddShortcut: () -> Unit,
    onEditShortcut: (String) -> Unit,
) {
    val graph = LocalGraph.current
    val context = LocalContext.current
    val vm: HelpingCareeViewModel = viewModel(
        key = "helping/$careeDeviceId",
        factory = viewModelFactory {
            initializer { HelpingCareeViewModel(graph, careeDeviceId) }
        },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var actionTarget by remember { mutableStateOf<Shortcut?>(null) }

    // Re-fetch when the user comes back to this screen so chunk 4's FCM
    // shortcut_changed handler isn't the only path that pulls fresh state.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refresh()
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    val title = state.careeDisplayName?.let { "Helping $it" } ?: "Helping…"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { VialoTopBar(title = title, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = VialoDimens.screenH),
        ) {
            when {
                state.isLoading && state.shortcuts.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.shortcuts.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Place,
                    title = "No places yet",
                    body = "Tap \"Add shortcut\" to save a place for " +
                        "${state.careeDisplayName ?: "them"}. " +
                        "It will appear on their Vialo right away.",
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentPadding = PaddingValues(vertical = VialoDimens.gapMd),
                    horizontalArrangement = Arrangement.spacedBy(VialoDimens.gapMd),
                    verticalArrangement = Arrangement.spacedBy(VialoDimens.gapMd),
                ) {
                    items(state.shortcuts, key = { it.id }) { shortcut ->
                        ShortcutTile(
                            shortcut = shortcut,
                            onTap = { NavigationLauncher.launch(context, shortcut) },
                            onLongPress = { actionTarget = shortcut },
                        )
                    }
                }
            }
            Spacer(Modifier.height(VialoDimens.gapMd))
            Button(
                onClick = onAddShortcut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.padding(start = VialoDimens.gapSm))
                Text("Add shortcut", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(VialoDimens.screenBottom))
        }
    }

    actionTarget?.let { target ->
        ShortcutActionsSheet(
            shortcut = target,
            onDismiss = { actionTarget = null },
            onNavigate = {
                NavigationLauncher.launch(context, target)
                actionTarget = null
            },
            onEdit = {
                actionTarget = null
                onEditShortcut(target.id)
            },
            onDelete = {
                vm.deleteShortcut(target.id)
                actionTarget = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutActionsSheet(
    shortcut: Shortcut,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(VialoDimens.gapLg)) {
            Text(
                shortcut.label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = VialoDimens.gapMd),
            )
            SheetAction("Navigate") { onNavigate() }
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
            .padding(vertical = VialoDimens.gapMd + 2.dp, horizontal = VialoDimens.gapXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
