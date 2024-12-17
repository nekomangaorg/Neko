package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.theme.Size

/** Simple Dialog to add a new category */
@Composable
fun ClearDownloadQueueDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {

    CompositionLocalProvider(
        LocalRippleConfiguration provides
            nekoRippleConfiguration(MaterialTheme.colorScheme.primary),
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        AlertDialog(
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.clear_download_queue),
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                MaterialTheme.colorScheme.onSurface
                            ),
                    )
                    Gap(Size.medium)
                    Text(
                        text = stringResource(R.string.clear_download_queue_confirmation),
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                MaterialTheme.colorScheme.onSurface
                            ),
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Text(text = stringResource(id = R.string.clear))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
