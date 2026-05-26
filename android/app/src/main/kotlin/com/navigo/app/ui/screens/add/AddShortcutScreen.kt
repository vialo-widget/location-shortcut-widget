package com.navigo.app.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.navigo.app.ui.LocalGraph
import com.navigo.app.ui.components.ExpiryPicker
import com.navigo.app.ui.components.IconPicker
import com.navigo.app.ui.components.PlaceSearchField
import com.navigo.app.ui.components.SaveBlockerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val graph = LocalGraph.current
    val vm: AddShortcutViewModel = viewModel(
        factory = viewModelFactory { initializer { AddShortcutViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedShortcutId) {
        if (state.savedShortcutId != null) onSaved()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Add shortcut") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            PlaceSearchField(
                onPlaceSelected = vm::onPlaceSelected,
                onSearch = { vm.search(it) },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = vm::useCurrentLocation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save where I am")
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.label,
                onValueChange = vm::setLabel,
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.address.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    state.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Icon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            IconPicker(selectedKey = state.iconKey, onIconSelected = vm::setIcon)

            Spacer(Modifier.height(20.dp))
            Text(
                "Expires in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ExpiryPicker(selected = state.expiryOption, onSelect = vm::setExpiry)

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = vm::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving…" else "Save shortcut")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    SaveBlockerDialog(
        blocker = state.blocker,
        candidateLabel = state.label.trim(),
        onDismiss = vm::dismissBlocker,
        onConfirmReplace = vm::confirmReplace,
    )
}