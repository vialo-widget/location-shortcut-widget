package com.vialo.app.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.pairing.Pair
import com.vialo.app.ui.LocalGraph

/**
 * Caree tab body — shows the helpers currently paired with this device and
 * an "Add a helper" CTA. Mirrors the standalone "Who's helping me" screen
 * minus the Scaffold/topbar, since it lives inside a Home tab.
 */
@Composable
fun CareePanel(
    onAddHelper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalGraph.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmRemove by remember { mutableStateOf<Pair?>(null) }

    LaunchedEffect(status) {
        when (val s = status) {
            is PairingViewModel.Status.Unpaired -> {
                snackbar.showSnackbar("Helper removed")
                vm.ackStatus()
            }
            is PairingViewModel.Status.Error -> {
                snackbar.showSnackbar(s.message.ifBlank { "Something went wrong" })
                vm.ackStatus()
            }
            else -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            if (state.asCaree.isEmpty()) {
                EmptyCareeBlurb(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.asCaree, key = { it.id }) { pair ->
                        HelperRow(
                            pair = pair,
                            onRemoveClick = { confirmRemove = pair },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddHelper,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add a helper", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(20.dp))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    confirmRemove?.let { pair ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove ${pair.otherDisplayName}?") },
            text = {
                Text(
                    "They will no longer be able to help manage your Vialo. " +
                        "Places they added stay on your phone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.unpair(pair.id)
                        confirmRemove = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Carer tab body — shows the carees this device is currently helping and a
 * "Help someone" CTA that opens the code-entry flow.
 */
@Composable
fun CarerPanel(
    onAddCaree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalGraph.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        if (state.asCarer.isEmpty()) {
            Text(
                "You're not helping anyone yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ask the person you want to help to open Vialo, turn on " +
                    "Caree mode in Settings, then tap \"Add a helper\" on " +
                    "their Caree tab. They will read you a 6-digit code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                "You're helping ${state.asCarer.size} " +
                    (if (state.asCarer.size == 1) "person" else "people"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.asCarer, key = { it.id }) { p ->
                    Text(
                        "• ${p.otherDisplayName}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAddCaree,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Help someone", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HelperRow(
    pair: Pair,
    onRemoveClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pair.otherDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove ${pair.otherDisplayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyCareeBlurb(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No one is helping you yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A family member or friend can help you add and update " +
                    "places on your Vialo. Tap \"Add a helper\" to start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
