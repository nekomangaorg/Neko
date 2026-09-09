package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.tachiyomi.data.backup.BackupConst
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

/** State representing selected items to backup in [CreateBackupDialog]. */
@Immutable
data class CreateBackupDialogState(
    val categories: Boolean = true,
    val chapters: Boolean = true,
    val tracking: Boolean = true,
    val history: Boolean = true,
    val allReadManga: Boolean = true,
) {
    /** Converts the selected options into a bitmask of [BackupConst] flags. */
    fun toBackupFlags(): Int {
        var flags = 0
        if (categories) flags = flags or BackupConst.BACKUP_CATEGORY
        if (chapters) flags = flags or BackupConst.BACKUP_CHAPTER
        if (tracking) flags = flags or BackupConst.BACKUP_TRACK
        if (history) flags = flags or BackupConst.BACKUP_HISTORY
        if (allReadManga) flags = flags or BackupConst.BACKUP_READ_MANGA
        return flags
    }
}

/** Stateless dialog for creating a backup. */
@Composable
fun CreateBackupDialog(
    state: CreateBackupDialogState,
    onToggleCategories: () -> Unit,
    onToggleChapters: () -> Unit,
    onToggleTracking: () -> Unit,
    onToggleHistory: () -> Unit,
    onToggleAllReadManga: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
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
                    checkedState = state.categories,
                    rowText = stringResource(R.string.categories),
                    checkedChange = { onToggleCategories() },
                )
                CheckboxRow(
                    checkedState = state.chapters,
                    rowText = stringResource(R.string.chapters),
                    checkedChange = { onToggleChapters() },
                )
                CheckboxRow(
                    checkedState = state.tracking,
                    rowText = stringResource(R.string.tracking),
                    checkedChange = { onToggleTracking() },
                )
                CheckboxRow(
                    checkedState = state.history,
                    rowText = stringResource(R.string.history),
                    checkedChange = { onToggleHistory() },
                )
                CheckboxRow(
                    checkedState = state.allReadManga,
                    rowText = stringResource(R.string.all_read_manga),
                    checkedChange = { onToggleAllReadManga() },
                )
                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = stringResource(id = R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

/** Stateful wrapper around [CreateBackupDialog] managing option selections. */
@Composable
fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    initialState: CreateBackupDialogState = CreateBackupDialogState(),
) {
    var state by remember { mutableStateOf(initialState) }

    CreateBackupDialog(
        state = state,
        onToggleCategories = { state = state.copy(categories = !state.categories) },
        onToggleChapters = { state = state.copy(chapters = !state.chapters) },
        onToggleTracking = { state = state.copy(tracking = !state.tracking) },
        onToggleHistory = { state = state.copy(history = !state.history) },
        onToggleAllReadManga = { state = state.copy(allReadManga = !state.allReadManga) },
        onDismissRequest = onDismiss,
        onConfirm = {
            onConfirm(state.toBackupFlags())
            onDismiss()
        },
    )
}

@Preview
@Composable
private fun CreateBackupDialogPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        CreateBackupDialog(
            state = CreateBackupDialogState(),
            onToggleCategories = {},
            onToggleChapters = {},
            onToggleTracking = {},
            onToggleHistory = {},
            onToggleAllReadManga = {},
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
