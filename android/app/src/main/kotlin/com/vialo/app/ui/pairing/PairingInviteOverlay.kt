package com.vialo.app.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vialo.app.data.pairing.InboundInvite
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.theme.VialoDimens
import com.vialo.app.ui.theme.ctaSize

/**
 * Full-page in-app prompt shown to the caree whenever a carer has a
 * pending invite waiting for them.
 *
 * Design intent:
 *   - **Full-bleed surface** that sits on top of whatever the caree was
 *     doing in the app — pairing is consequential enough to interrupt for.
 *   - **Three actions** — Accept and Reject act immediately; Skip dismisses
 *     this prompt for the current process only, so the overlay reappears
 *     on the next cold start (matching the requirement that a missed
 *     prompt resurfaces next time the app is opened).
 *   - **Multiple invites** are shown one at a time in inbox order. After
 *     each decision the next pending invite slides in automatically.
 *   - **Refresh on resume** so that a notification that landed while the
 *     app was backgrounded surfaces immediately on the next foreground.
 *
 * Caller hoists the in-memory skip set; this composable never persists
 * it. Suppression for routes that should NOT be interrupted (onboarding,
 * the standalone AcceptInviteScreen reached via FCM tap) is the caller's
 * responsibility — see [com.vialo.app.ui.VialoApp].
 */
@Composable
fun PairingInviteOverlay(
    skippedPendingIds: Set<String>,
    onSkip: (String) -> Unit,
) {
    val graph = LocalGraph.current
    val vm: PairingViewModel = viewModel(
        factory = viewModelFactory { initializer { PairingViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    // Refresh inbox on every foreground so an invite that arrived while
    // backgrounded is visible immediately.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val nextInvite: InboundInvite? = state.inbox.firstOrNull { it.pendingId !in skippedPendingIds }

    // Acknowledge transient status so a previous error doesn't linger across
    // invites; the Caree tab + EnterCodeScreen have their own snackbars.
    LaunchedEffect(status) {
        if (status is PairingViewModel.Status.Accepted ||
            status is PairingViewModel.Status.Rejected ||
            status is PairingViewModel.Status.Error
        ) {
            vm.ackStatus()
        }
    }

    if (nextInvite == null) return

    val working = status is PairingViewModel.Status.Working

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = VialoDimens.gapXl - 8.dp, vertical = VialoDimens.gapXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(VialoDimens.avatarLg)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(VialoDimens.iconXl),
                )
            }
            Spacer(Modifier.height(VialoDimens.gapLg))
            Text(
                "${nextInvite.carerDisplayName} wants to help you",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(VialoDimens.gapMd))
            Text(
                "If you accept, ${nextInvite.carerDisplayName} will be able to " +
                    "see and update the places saved on your Vialo. You can " +
                    "remove them any time from the Caree tab.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.acceptInvite(nextInvite.pendingId) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth().ctaSize(),
            ) {
                Text(
                    if (working) "Working…" else "Accept",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(VialoDimens.gapMd))
            OutlinedButton(
                onClick = { vm.rejectInvite(nextInvite.pendingId) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth().ctaSize(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Reject", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(VialoDimens.gapXs))
            // Skip is intentionally subtle — it's "decide later", not a
            // first-class action. Stays in-memory only, so the overlay
            // resurfaces on the next cold start.
            TextButton(
                onClick = { onSkip(nextInvite.pendingId) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Skip for now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(VialoDimens.gapSm))
        }
    }
}
