package com.vialo.app.ui.screens.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.vialo.app.ui.theme.ctaSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** When non-null, the carer is adding on this caree's behalf — writes
     *  go through a RemoteShortcutStore backed by the sync backend
     *  instead of the local DB. */
    careeDeviceId: String? = null,
) {
    val graph = LocalGraph.current
    val store = androidx.compose.runtime.remember(careeDeviceId) {
        if (careeDeviceId == null) {
            com.vialo.app.data.repo.LocalShortcutStore(graph.shortcutRepository, graph.expiryNotifier)
        } else {
            com.vialo.app.data.repo.RemoteShortcutStore(graph.vialoApi, careeDeviceId)
        }
    }
    val vm: AddShortcutViewModel = viewModel(
        factory = viewModelFactory { initializer { AddShortcutViewModel(graph, store) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedShortcutId) {
        if (state.savedShortcutId != null) onSaved()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { VialoTopBar(title = "Add shortcut", onBack = onBack) },
    ) { padding ->
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
                PlaceSearchField(
                    onPlaceSelected = vm::onPlaceSelected,
                    onSearch = { vm.search(it) },
                )
                Spacer(Modifier.height(VialoDimens.gapSm))
                OutlinedButton(
                    onClick = vm::useCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(VialoDimens.gapSm))
                    Text("Save where I am")
                }
                Spacer(Modifier.height(VialoDimens.gapMd))
                OutlinedTextField(
                    value = state.label,
                    onValueChange = vm::setLabel,
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.address.isNotBlank()) {
                    Spacer(Modifier.height(VialoDimens.gapSm))
                    Text(
                        state.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                IconPickerCompact(selectedKey = state.iconKey, onIconSelected = vm::setIcon)
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

            state.error?.let {
                Spacer(Modifier.height(VialoDimens.gapMd))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(VialoDimens.gapXl))
            Button(
                onClick = vm::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().ctaSize(),
            ) {
                Text(
                    if (state.isSaving) "Saving…" else "Save shortcut",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(VialoDimens.screenBottom))
        }
    }

    SaveBlockerDialog(
        blocker = state.blocker,
        candidateLabel = state.label.trim(),
        onDismiss = vm::dismissBlocker,
        onConfirmReplace = vm::confirmReplace,
    )
}
