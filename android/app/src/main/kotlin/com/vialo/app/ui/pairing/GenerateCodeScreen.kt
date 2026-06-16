package com.vialo.app.ui.pairing

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.ui.LocalGraph
import kotlinx.coroutines.delay

/**
 * Caree generates a 6-digit code → reads it to the helper → helper enters
 * it on their phone, types in display names, submits. Caree then sees an
 * accept/reject screen elsewhere.
 *
 * The code displays big with a countdown bar. When expired, the user can
 * tap Refresh to mint a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateCodeScreen(onBack: () -> Unit) {
    val graph = LocalGraph.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Generate immediately on entry; clear on leave so a stale code doesn't
    // re-appear if the user returns to a different paired flow.
    LaunchedEffect(Unit) {
        if (state.activeCode == null) vm.generateCode()
    }

    // Live countdown — recomputed every second.
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.activeCode) {
        while (state.activeCode != null && nowTick < (state.activeCode?.expiresAt ?: 0L)) {
            delay(1_000)
            nowTick = System.currentTimeMillis()
        }
    }

    LaunchedEffect(status) {
        if (status is PairingViewModel.Status.Error) {
            snackbar.showSnackbar(
                (status as PairingViewModel.Status.Error).message.ifBlank { "Try again" },
            )
            vm.ackStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Helper invite") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Read this code to your helper",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Then they enter it in their Vialo app to ask permission to help you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            val code = state.activeCode
            val expiresAt = code?.expiresAt ?: 0L
            val msLeft by remember(code, nowTick) {
                derivedStateOf { (expiresAt - nowTick).coerceAtLeast(0L) }
            }
            val expired = code != null && msLeft <= 0L

            CodeCard(
                code = code?.code,
                msLeft = msLeft,
                expired = expired,
                isWorking = status is PairingViewModel.Status.Working,
            )

            Spacer(Modifier.height(28.dp))

            if (expired || code == null) {
                Button(
                    onClick = { vm.generateCode() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("  Generate a new code", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(code.code))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Text("  Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Open Vialo → Carer mode → Help someone → " +
                                        "and enter this code: ${code.code}",
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text("  Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeCard(
    code: String?,
    msLeft: Long,
    expired: Boolean,
    isWorking: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                isWorking && code == null -> {
                    Text(
                        "Generating…",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                code == null -> {
                    Text(
                        "—",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                else -> {
                    Text(
                        formatCode(code),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            if (code != null) {
                if (expired) {
                    Text(
                        "Code expired — generate a new one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    val totalMs = 10 * 60 * 1000L
                    val progress = (msLeft.toFloat() / totalMs).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Expires in ${formatRemaining(msLeft)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/** "123456" → "123 456". Easier to read aloud. */
private fun formatCode(code: String): String =
    if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code

private fun formatRemaining(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mm = totalSec / 60
    val ss = totalSec % 60
    return "%d:%02d".format(mm, ss)
}
