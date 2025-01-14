package org.nekomanga.presentation.components.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

/** Simple Dialog to add a new category */
@Composable
fun DeleteHistoryDialog(
    onDismiss: () -> Unit,
    name: String,
    @StringRes description: Int,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = {
            Column {
                Text(text = stringResource(description), style = MaterialTheme.typography.bodyLarge)
                Gap(Size.small)
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
