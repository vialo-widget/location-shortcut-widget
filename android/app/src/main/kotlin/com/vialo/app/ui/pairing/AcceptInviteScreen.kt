package com.vialo.app.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.pairing.InboundInvite
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.components.VialoTopBar
import com.vialo.app.ui.theme.VialoDimens

/**
 * Caree's accept/reject screen, addressed by a specific [pendingId].
 *
 * Typical entry point is a notification tap; we look up the invite in
 * the repository's inbox and show a big "Daughter is ready to help you"
 * with two clear actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptInviteScreen(
    pendingId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val graph = LocalGraph.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // The invite may not be in state yet on first composition — refresh once
    // to be sure (the VM kicks one in init but we don't want a race).
    LaunchedEffect(pendingId) { vm.refresh() }

    LaunchedEffect(status) {
        if (status is PairingViewModel.Status.Error) {
            snackbar.showSnackbar(
                (status as PairingViewModel.Status.Error).message.ifBlank { "Try again" },
            )
            vm.ackStatus()
        }
    }

    val invite: InboundInvite? = state.inbox.firstOrNull { it.pendingId == pendingId }

    Scaffold(
        topBar = { VialoTopBar(title = "Helper request", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = VialoDimens.screenH),
        ) {
            if (invite == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "This request is no longer available",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(VialoDimens.gapSm))
                    Text(
                        "It may have expired or been cancelled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(VialoDimens.gapXl - 8.dp))
                    Button(onClick = onBack) { Text("OK") }
                }
                return@Box
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(VialoDimens.gapXl + 8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VialoDimens.cornerLg + 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VialoDimens.gapXl - 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            invite.carerDisplayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(VialoDimens.gapSm))
                        Text(
                            "is ready to help you with Vialo",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(VialoDimens.gapXl - 4.dp))
                Text(
                    "They will be able to see and change your saved places. " +
                        "They cannot see your location or anything else.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { vm.acceptInvite(invite.pendingId, onAccepted = { onDone() }) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Accept", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(VialoDimens.gapSm))
                OutlinedButton(
                    onClick = { vm.rejectInvite(invite.pendingId, onRejected = { onDone() }) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reject", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(VialoDimens.screenBottom))
            }
        }
    }
}
