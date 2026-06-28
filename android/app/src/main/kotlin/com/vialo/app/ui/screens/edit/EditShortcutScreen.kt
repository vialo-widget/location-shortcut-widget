package com.vialo.app.ui.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.service.search.PlaceResult
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.components.ExpiryPicker
import com.vialo.app.ui.components.IconPickerCompact
import com.vialo.app.ui.components.PlaceSearchField
import com.vialo.app.ui.components.SaveBlockerDialog
import com.vialo.app.ui.components.SectionCard
import com.vialo.app.ui.components.SectionLabel
import com.vialo.app.ui.components.SectionLabelGap
import com.vialo.app.ui.components.SectionSpacer
import com.vialo.app.ui.components.TransportModePicker
import com.vialo.app.ui.components.VialoTopBar
import com.vialo.app.ui.theme.VialoDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShortcutScreen(
    shortcutId: String,
    onClose: () -> Unit,
    /** When non-null, the carer is editing on this caree's behalf — reads
     *  and writes go through the sync backend rather than the local DB. */
    careeDeviceId: String? = null,
) {
    val graph = LocalGraph.current
    val store = remember(careeDeviceId) {
        if (careeDeviceId == null) {
            com.vialo.app.data.repo.LocalShortcutStore(graph.shortcutRepository, graph.expiryNotifier)
        } else {
            com.vialo.app.data.repo.RemoteShortcutStore(graph.vialoApi, careeDeviceId)
        }
    }
    val vmKey = careeDeviceId?.let { "$it/$shortcutId" } ?: shortcutId
    val vm: EditShortcutViewModel = viewModel(
        key = vmKey,
        factory = viewModelFactory { initializer { EditShortcutViewModel(graph, store, shortcutId) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    LaunchedEffect(state.notFound) { if (state.notFound) onClose() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { VialoTopBar(title = "Edit shortcut", onBack = onClose) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }
        // Discard the loaded snapshot only after we've used it — we don't
        // reference `original` directly in this body, but the load gate
        // above protects against rendering a half-loaded form.
        state.original ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = VialoDimens.screenH)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(VialoDimens.screenTop))

            SectionLabel("Where")
            SectionLabelGap()
            SectionCard {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = vm::setLabel,
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(VialoDimens.gapMd))
                Text(
                    "Location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(VialoDimens.gapSm))
                LocationRow(
                    address = state.address,
                    onChangeClick = { showLocationPicker = true },
                )
            }

            SectionSpacer()
            SectionLabel("Style")
            SectionLabelGap()
            SectionCard {
                Text(
                    "Icon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(VialoDimens.gapSm))
                IconPickerCompact(
                    selectedKey = state.iconKey,
                    onIconSelected = vm::setIcon,
                )
            }

            SectionSpacer()
            SectionLabel("Behaviour")
            SectionLabelGap()
            SectionCard {
                Text(
                    "Open with",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(VialoDimens.gapSm))
                TransportModePicker(
                    selected = state.transportMode,
                    onSelect = vm::setTransportMode,
                )
                Spacer(Modifier.height(VialoDimens.gapMd))
                Text(
                    "Expires in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(VialoDimens.gapSm))
                ExpiryPicker(selected = state.expiryOption, onSelect = vm::setExpiry)
            }

            Spacer(Modifier.height(VialoDimens.gapXl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VialoDimens.gapMd),
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text("Delete") }
                Button(
                    onClick = vm::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
            Spacer(Modifier.height(VialoDimens.screenBottom))
        }
    }

    if (showLocationPicker) {
        LocationPickerSheet(
            onDismiss = { showLocationPicker = false },
            onSearch = { vm.searchPlaces(it) },
            onSelected = { place ->
                vm.setLocation(place)
                showLocationPicker = false
            },
        )
    }

    SaveBlockerDialog(
        blocker = state.blocker,
        candidateLabel = state.label.trim(),
        onDismiss = vm::dismissBlocker,
        onConfirmReplace = vm::confirmReplace,
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this shortcut?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LocationRow(
    address: String,
    onChangeClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(VialoDimens.cornerMd),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = VialoDimens.gapMd + 2.dp,
                vertical = VialoDimens.gapSm + 2.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    address.ifBlank { "(no address)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = onChangeClick) { Text("Change") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerSheet(
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<PlaceResult>,
    onSelected: (PlaceResult) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = VialoDimens.screenH, vertical = VialoDimens.gapSm)) {
            Text(
                "Change location",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = VialoDimens.gapMd),
            )
            PlaceSearchField(
                onPlaceSelected = onSelected,
                onSearch = onSearch,
            )
            Spacer(Modifier.height(VialoDimens.gapLg))
        }
    }
}
