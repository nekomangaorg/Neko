package org.nekomanga.presentation.components.dialog

import android.os.Build
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.nekomanga.presentation.components.MarkdownRender
import eu.kanade.tachiyomi.data.updater.Release
import org.nekomanga.R

/** Dialog that shows when an app update is available */
@Composable
fun AppUpdateDialog(release: Release, onDismissRequest: () -> Unit, onConfirm: (Release) -> Unit) {
    val body = release.info.substringBeforeLast("Downloads & Checksums")
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.new_version_available)) },
        modifier = Modifier.fillMaxHeight(.8f),
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    MarkdownRender(
                        content = body,
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(release) }) {
                val text =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.update
                    else R.string.download
                Text(text = stringResource(id = text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.ignore))
            }
        },
    )
}


