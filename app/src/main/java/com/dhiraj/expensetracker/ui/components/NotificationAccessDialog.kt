package com.dhiraj.expensetracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * NotificationAccessDialog
 *
 * This dialog explains WHY notification access is required
 * and guides the user to enable it.
 *
 * Shown only when:
 * - Notification listener permission is missing
 * - User has not dismissed the dialog
 *
 * This dialog DOES NOT:
 * - Request permission itself
 * - Open system settings directly
 *
 * Those actions are delegated via callbacks.
 */
@Composable
fun NotificationAccessDialog(
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permission Required")
        },
        text = {
            Text(
                "To automatically track your expenses, " +
                        "this app needs Notification Access.\n\n" +
                        "Without it, transactions cannot be detected."
            )
        },
        confirmButton = {
            Button(onClick = onEnable) {
                Text("Enable Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
