package com.vialo.app.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.ui.LocalGraph

/**
 * Carer enters the 6-digit code their caree gave them, plus the two display
 * names (what the caree will see for them, and how the carer will label the
 * caree in their own dropdown).
 *
 * On successful redeem the caree is pushed an "Accept / Reject" notification
 * and this screen pops back. The pair only goes live after caree accepts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterCodeScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val graph = LocalGraph.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val status by vm.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var code by remember { mutableStateOf("") }
    var carerName by remember { mutableStateOf("") }
    var careeName by remember { mutableStateOf("") }

    val canSubmit = code.length == 6 && code.all { it.isDigit() } &&
        carerName.isNotBlank() && careeName.isNotBlank() &&
        status !is PairingViewModel.Status.Working

    LaunchedEffect(status) {
        when (val s = status) {
            is PairingViewModel.Status.Redeemed -> {
                snackbar.showSnackbar("Sent. Waiting for them to accept.")
                vm.ackStatus()
                onDone()
            }
            is PairingViewModel.Status.Error -> {
                snackbar.showSnackbar(s.message.ifBlank { "Try again" })
                vm.ackStatus()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help someone") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Ask them to open Vialo → Settings → Who's helping me → " +
                    "Add a helper, and read you the 6-digit code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter(Char::isDigit).take(6) },
                label = { Text("6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = carerName,
                onValueChange = { carerName = it.take(40) },
                label = { Text("My name (they will see this)") },
                placeholder = { Text("Daughter") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = careeName,
                onValueChange = { careeName = it.take(40) },
                label = { Text("Their name (only you see this)") },
                placeholder = { Text("Dad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    vm.redeemCode(code, carerName.trim(), careeName.trim())
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (status is PairingViewModel.Status.Working) "Sending…" else "Send request",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
