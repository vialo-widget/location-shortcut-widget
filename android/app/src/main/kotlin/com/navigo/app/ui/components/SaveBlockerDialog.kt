package com.navigo.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.navigo.app.data.validation.SaveBlocker

/**
 * Renders the appropriate AlertDialog for a non-null [SaveBlocker]. Used
 * from Add / Edit / Confirm-Add so all three screens reach the same copy
 * and button layout for the same conflict.
 *
 * Pass [candidateLabel] so the "Replace" prompt can quote both names —
 * the existing label comes from the matched shortcut, the new one from
 * what the user typed.
 */
@Composable
fun SaveBlockerDialog(
    blocker: SaveBlocker?,
    candidateLabel: String,
    onDismiss: () -> Unit,
    onConfirmReplace: () -> Unit,
) {
    when (blocker) {
        null -> Unit
        is SaveBlocker.ExactDuplicate -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Already saved") },
            text = {
                Text("“${blocker.matched.label}” is already in your shortcuts.")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            },
        )
        is SaveBlocker.ReplacePrompt -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Same location") },
            text = {
                Text(
                    "You already have “${blocker.matched.label}” saved at this " +
                        "location. Replace it with “$candidateLabel”?",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmReplace) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
        )
        SaveBlocker.LabelTaken -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Label already exists") },
            text = {
                Text("“$candidateLabel” is already used. Pick a different name.")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            },
        )
    }
}
