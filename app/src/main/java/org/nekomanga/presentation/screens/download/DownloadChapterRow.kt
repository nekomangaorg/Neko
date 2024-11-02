package org.nekomanga.presentation.screens.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeAction
import org.nekomanga.R
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.presentation.components.ChapterSwipe
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadChapterRow(chapter: DownloadItem, downloadSwiped: () -> Unit) {
    val swipeAction =
        SwipeAction(
            icon = rememberVectorPainter(Icons.Filled.DeleteForever),
            background = MaterialTheme.colorScheme.secondary,
            onSwipe = downloadSwiped,
        )

    ChapterSwipe(
        modifier = Modifier.padding(vertical = Dp(1f)),
        startSwipeAction = swipeAction,
        endSwipeAction = swipeAction,
    ) {
        ChapterRow(chapter)
    }
}

@Composable
private fun ChapterRow(download: DownloadItem) {

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
                text = download.chapterItem.chapter.name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.mediumAlphaLowContrast
                            )
                    ),
                maxLines = 1,
            )

            when (download.chapterItem.downloadState == Download.State.QUEUE) {
                true ->
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                false -> {
                    val animatedProgress by
                        animateFloatAsState(
                            targetValue = download.chapterItem.downloadProgress.toFloat() / 100,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { animatedProgress },
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
