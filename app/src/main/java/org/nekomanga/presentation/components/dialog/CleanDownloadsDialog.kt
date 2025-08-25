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
fun CleanDownloadsDialog(onDismiss: () -> Unit, onConfirm: (Boolean, Boolean) -> Unit) {
    var readChecked by remember { mutableStateOf(true) }
    var readNotInLibraryChecked by remember { mutableStateOf(true) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.clean_up_downloaded_chapters)) },
        text = {
            Column {
                CheckboxRow(
                    checkedState = true,
                    rowText = stringResource(R.string.clean_orphaned_downloads),
                    checkedChange = {},
                    disabled = true,
                )
                CheckboxRow(
                    checkedState = readChecked,
                    rowText = stringResource(R.string.clean_read_downloads),
                    checkedChange = { readChecked = !readChecked },
                )
                CheckboxRow(
                    checkedState = readNotInLibraryChecked,
                    rowText = stringResource(R.string.clean_read_manga_not_in_library),
                    checkedChange = { readNotInLibraryChecked = !readNotInLibraryChecked },
                )

                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(readChecked, readNotInLibraryChecked)
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
