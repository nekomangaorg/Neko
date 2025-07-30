package org.nekomanga.presentation.components.dialog

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

/** Dialog for restoring backup */
@Composable
fun RestoreDialog(uri: Uri, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val results = BackupFileValidator().validate(context, uri)

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.restore_backup)) },
        text = {
            Column {
                Text(text = stringResource(id = R.string.restore_neko))
                if (results.missingMangaDexEntries) {
                    Gap(Size.small)
                    Text(text = stringResource(id = R.string.restore_missing_mangadex))
                }
                if (results.missingTrackers.isNotEmpty()) {
                    Gap(Size.small)
                    Text(text = stringResource(id = R.string.restore_missing_trackers))
                    results.missingTrackers.forEach { tracker ->
                        Gap(Size.tiny)
                        Text(text = "- $tracker")
                    }
                }

                Gap(Size.tiny)
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
                Text(text = stringResource(id = R.string.restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
