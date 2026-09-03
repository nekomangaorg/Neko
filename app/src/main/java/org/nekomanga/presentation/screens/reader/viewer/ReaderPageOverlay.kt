package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.source.model.Page
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun ReaderPageLoadingOverlay(
    status: Page.State,
    progress: Int,
    modifier: Modifier = Modifier,
) {
    val isLoading =
        status == Page.State.QUEUE ||
            status == Page.State.LOAD_PAGE ||
            status == Page.State.DOWNLOAD_IMAGE

    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(Size.medium),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Size.small),
            ) {
                if (progress > 0) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(Size.huge),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        strokeWidth = Size.extraTiny,
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Size.huge),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = Size.extraTiny,
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderPageErrorOverlay(
    visible: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(Size.medium),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Size.medium),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_error_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Size.extraLarge),
                )
                Text(
                    text = stringResource(id = R.string.decode_image_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text(text = stringResource(id = R.string.retry)) }
            }
        }
    }
}
