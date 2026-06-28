package com.vialo.app.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.model.Shortcut
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.components.EmptyState
import com.vialo.app.ui.components.ShortcutTile
import com.vialo.app.ui.components.VialoTopBar
import com.vialo.app.ui.pairing.CareePanel
import com.vialo.app.ui.pairing.CarerPanel
import com.vialo.app.ui.theme.VialoDimens
import kotlinx.coroutines.launch

private enum class HomeTab(val title: String, val icon: ImageVector) {
    MyLocations("My locations", Icons.Outlined.Place),
    Caree("Caree", Icons.Outlined.Favorite),
    Carer("Carer", Icons.Outlined.SupervisorAccount),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddShortcut: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditShortcut: (String) -> Unit,
    onAddHelper: () -> Unit = {},
    onOpenInvite: (String) -> Unit = {},
    onAddCaree: () -> Unit = {},
    onOpenCaree: (String) -> Unit = {},
) {
    val graph = LocalGraph.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(graph) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var actionTarget by remember { mutableStateOf<Shortcut?>(null) }

    val tabs = remember(state.careeModeEnabled, state.carerModeEnabled) {
        buildList {
            add(HomeTab.MyLocations)
            if (state.careeModeEnabled) add(HomeTab.Caree)
            if (state.carerModeEnabled) add(HomeTab.Carer)
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // When the tab list shrinks because a mode toggled off, keep the pager
    // pinned to a valid index instead of leaving it past the end.
    LaunchedEffect(tabs.size) {
        if (pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(tabs.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                VialoTopBar(
                    title = "Vialo",
                    titleStyle = MaterialTheme.typography.headlineSmall,
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                )
                if (tabs.size > 1) {
                    HomeTabRow(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            when (tabs[page]) {
                HomeTab.MyLocations -> MyLocationsTab(
                    state = state,
                    onTap = { vm.launchNavigation(context, it) },
                    onLongPress = { actionTarget = it },
                    onAddShortcut = onAddShortcut,
                )
                HomeTab.Caree -> CareePanel(
                    onAddHelper = onAddHelper,
                    onOpenInvite = onOpenInvite,
                )
                HomeTab.Carer -> CarerPanel(
                    onAddCaree = onAddCaree,
                    onOpenCaree = onOpenCaree,
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTabRow(
    tabs: List<HomeTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val safeIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
    TabRow(
        selectedTabIndex = safeIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { positions ->
            if (safeIndex < positions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[safeIndex]),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == safeIndex,
                onClick = { onTabSelected(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                },
                text = {
                    Text(tab.title, style = MaterialTheme.typography.labelLarge)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyLocationsTab(
    state: HomeUiState,
    onTap: (Shortcut) -> Unit,
    onLongPress: (Shortcut) -> Unit,
    onAddShortcut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VialoDimens.screenH),
    ) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurface)
            }

            state.shortcuts.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Place,
                title = "No places yet",
                body = "Tap \"Add shortcut\" below to save your first place. " +
                    "One tap from your home screen, you're on your way.",
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
                        onTap = { onTap(shortcut) },
                        onLongPress = { onLongPress(shortcut) },
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
        Column(modifier = Modifier.padding(VialoDimens.gapLg)) {
            Text(
                shortcut.label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = VialoDimens.gapMd),
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
