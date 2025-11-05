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
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import eu.kanade.tachiyomi.data.updater.GithubRelease
import org.nekomanga.R

/** Dialog that shows when an app update is available */
@Composable
fun AppUpdateDialog(
    release: GithubRelease,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val body = release.info.substringBeforeLast("Downloads & Checksums")
    val url = release.downloadLink
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.new_version_available)) },
        modifier = Modifier.fillMaxHeight(.8f),
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Markdown(
                        content = body,
                        colors = nekoMarkdownColors(),
                        typography = nekoMarkdownTypography(),
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }) {
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

@Composable
private fun nekoMarkdownColors() =
    markdownColor(
        text = MaterialTheme.colorScheme.onSurface,
        codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    )

@Composable
private fun nekoMarkdownTypography() =
    markdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        paragraph = MaterialTheme.typography.bodyMedium,
        text = MaterialTheme.typography.bodySmall,
    )
