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
import com.mikepenz.markdown.Markdown
import com.mikepenz.markdown.MarkdownColors
import com.mikepenz.markdown.MarkdownDefaults
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.GithubRelease

/**
 * Dialog that shows when an app update is available
 */
@Composable
fun AppUpdateDialog(release: GithubRelease, onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    val body = release.info.substringBeforeLast("| Variant | SHA-256")
    val url = release.downloadLink
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.new_version_available))
        },
        modifier = Modifier
            .fillMaxHeight(.8f),
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Markdown(
                        content = body,
                        colors = markdownColors(),
                        typography = markdownTypography(),
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }) {
                val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.update else R.string.download
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
private fun markdownColors(): MarkdownColors {
    return MarkdownDefaults.markdownColors(
        textColor = MaterialTheme.colorScheme.onSurface,
        backgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun markdownTypography() =
    MarkdownDefaults.markdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        body1 = MaterialTheme.typography.bodyMedium,
        body2 = MaterialTheme.typography.bodySmall,
    )
