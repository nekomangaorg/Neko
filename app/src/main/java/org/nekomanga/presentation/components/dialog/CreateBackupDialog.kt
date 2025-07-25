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
import eu.kanade.tachiyomi.data.backup.BackupConst
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.theme.Size

/** Dialog for creating a backup */
@Composable
fun CreateBackupDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var categoriesChecked by remember { mutableStateOf(true) }
    var chaptersChecked by remember { mutableStateOf(true) }
    var trackingChecked by remember { mutableStateOf(true) }
    var historyChecked by remember { mutableStateOf(true) }
    var allReadMangaChecked by remember { mutableStateOf(true) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.what_should_backup)) },
        text = {
            Column {
                CheckboxRow(
                    checkedState = true,
                    rowText = stringResource(R.string.manga),
                    checkedChange = {},
                    disabled = true,
                )
                CheckboxRow(
                    checkedState = categoriesChecked,
                    rowText = stringResource(R.string.categories),
                    checkedChange = { categoriesChecked = !categoriesChecked },
                )
                CheckboxRow(
                    checkedState = chaptersChecked,
                    rowText = stringResource(R.string.chapters),
                    checkedChange = { chaptersChecked = !chaptersChecked },
                )
                CheckboxRow(
                    checkedState = trackingChecked,
                    rowText = stringResource(R.string.tracking),
                    checkedChange = { trackingChecked = !trackingChecked },
                )
                CheckboxRow(
                    checkedState = historyChecked,
                    rowText = stringResource(R.string.history),
                    checkedChange = { historyChecked = !historyChecked },
                )
                CheckboxRow(
                    checkedState = allReadMangaChecked,
                    rowText = stringResource(R.string.all_read_manga),
                    checkedChange = { allReadMangaChecked = !allReadMangaChecked },
                )
                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    var backupFlag = 0

                    if (categoriesChecked) {
                        backupFlag = backupFlag or BackupConst.BACKUP_CATEGORY
                    }
                    if (chaptersChecked) {
                        backupFlag = backupFlag or BackupConst.BACKUP_CHAPTER
                    }
                    if (trackingChecked) {
                        backupFlag = backupFlag or BackupConst.BACKUP_TRACK
                    }
                    if (historyChecked) {
                        backupFlag = backupFlag or BackupConst.BACKUP_HISTORY
                    }
                    if (allReadMangaChecked) {
                        backupFlag = backupFlag or BackupConst.BACKUP_READ_MANGA
                    }

                    // figureOut flags
                    onConfirm(backupFlag)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
