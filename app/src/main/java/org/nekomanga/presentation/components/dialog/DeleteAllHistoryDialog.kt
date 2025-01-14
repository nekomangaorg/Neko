package org.nekomanga.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.nekomanga.R

/** Simple Dialog to add a new category */
@Composable
fun DeleteAllHistoryDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.clear_history_confirmation_1)) },
        text = { Text(text = stringResource(R.string.clear_history_confirmation_2)) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
