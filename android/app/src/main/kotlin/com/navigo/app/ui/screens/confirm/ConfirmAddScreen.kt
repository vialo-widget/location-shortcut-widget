package com.navigo.app.ui.screens.confirm

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.navigo.app.ui.LocalGraph
import com.navigo.app.ui.components.ExpiryPicker
import com.navigo.app.ui.components.IconPicker
import com.navigo.app.ui.icons.ShortcutIcon
import com.navigo.app.ui.icons.ShortcutIconCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmAddScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val graph = LocalGraph.current
    val vm: ConfirmAddViewModel = viewModel(
        factory = viewModelFactory { initializer { ConfirmAddViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onConfirmed() }
    // Defend against a stale route — if there's no pending shortcut, just bounce out.
    LaunchedEffect(state.pending) { if (state.pending == null) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add shared shortcut?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        val pending = state.pending ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ShortcutIcon(
                            ShortcutIconCatalog.forKey(state.iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Column {
                        Text(state.label.ifBlank { pending.label },
                            style = MaterialTheme.typography.titleMedium)
                        if (pending.address.isNotBlank()) {
                            Text(
                                pending.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.label,
                onValueChange = vm::setLabel,
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text("Icon", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            IconPicker(selectedKey = state.iconKey, onIconSelected = vm::setIcon)

            Spacer(Modifier.height(20.dp))
            Text("Expires in", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExpiryPicker(selected = state.expiryOption, onSelect = vm::setExpiry)

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = vm::confirm,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSaving) "Saving…" else "Add to home")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
