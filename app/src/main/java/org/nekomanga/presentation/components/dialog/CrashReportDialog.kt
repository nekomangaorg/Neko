package org.nekomanga.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import org.nekomanga.R

/**
 * Offers the log of the last crash. An outside tap does not close it, because closing marks the
 * report seen.
 */
@Composable
fun CrashReportDialog(onDismissRequest: () -> Unit, onShareClick: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                text =
                    stringResource(
                        id = R.string.crash_report_dialog_title,
                        stringResource(id = R.string.app_name),
                    )
            )
        },
        text = { Text(text = stringResource(id = R.string.crash_report_dialog_body)) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onShareClick) { Text(text = stringResource(id = R.string.share)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
    )
}
