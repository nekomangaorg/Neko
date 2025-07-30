package org.nekomanga.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

/** Dialog for logout confirmation */
@Composable
fun LogoutDialog(sourceName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.log_out_from_, sourceName)) },
        text = {},
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.log_out))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
