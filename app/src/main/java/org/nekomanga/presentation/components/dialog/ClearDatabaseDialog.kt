package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.theme.Size

/** Dialog for deleting downloads */
@Composable
fun ClearDatabaseDialog(onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var excludeReadChecked by remember { mutableStateOf(true) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.clear_database_confirmation_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.clear_database_confirmation))
                Gap(Size.small)
                CheckboxRow(
                    checkedState = excludeReadChecked,
                    rowText = stringResource(R.string.clear_db_exclude_read),
                    checkedChange = { excludeReadChecked = !excludeReadChecked },
                )

                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(excludeReadChecked)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
