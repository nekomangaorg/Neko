package org.nekomanga.presentation.screens.download

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoSwipeToDismiss
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadChapterRow(download: Download, downloadSwiped: () -> Unit) {
    val dismissState = rememberDismissState(initialValue = DismissValue.Default)
    NekoSwipeToDismiss(
        state = dismissState,
        modifier = Modifier.padding(vertical = Dp(1f)),
        background = {
            when (dismissState.dismissDirection) {
                DismissDirection.EndToStart -> Background(alignment = Alignment.CenterEnd)
                DismissDirection.StartToEnd -> Background(alignment = Alignment.CenterStart)
                else -> Unit
            }
        },
        dismissContent = { ChapterRow(download) },
    )
    if (
        dismissState.isDismissed(DismissDirection.EndToStart) ||
            dismissState.isDismissed(DismissDirection.StartToEnd)
    ) {
        val scope = rememberCoroutineScope()
        LaunchedEffect(key1 = dismissState.dismissDirection) {
            scope.launch {
                dismissState.reset()
                downloadSwiped()
            }
        }
    }
}

@Composable
private fun ChapterRow(download: Download) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(top = Size.small, bottom = Size.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier.align(Alignment.CenterVertically)
                    .padding(horizontal = Size.medium)
                    .fillMaxWidth(.9f),
            verticalArrangement = Arrangement.spacedBy(Size.tiny),
        ) {
            Text(
                text = download.mangaItem.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = download.chapterItem.name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.mediumAlphaLowContrast
                            )
                    ),
                maxLines = 1,
            )
            when (download.status == Download.State.QUEUE) {
                true ->
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                false -> {
                    val currentProgress by
                        remember(download.progress.toFloat()) {
                            mutableFloatStateOf(download.progress.toFloat())
                        }

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { currentProgress },
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier.padding(end = Size.medium),
        )
    }
}

@Composable
private fun Background(alignment: Alignment) {
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Size.large),
        contentAlignment = alignment,
    ) {
        Column(modifier = Modifier.align(alignment)) {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = stringResource(id = R.string.remove_download),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
