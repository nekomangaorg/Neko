package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterDifference
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters
import org.nekomanga.R
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.presentation.theme.Size

@Composable
fun ReaderTransitionPage(
    transition: ChapterTransition,
    manga: MangaItem?,
    downloadManager: DownloadManager,
    onRetry: (ReaderChapter) -> Unit,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTap?.invoke() },
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = Size.mediumLarge,
                    end = Size.mediumLarge,
                    top =
                        if (transition is ChapterTransition.Prev) {
                            Size.appBarHeight + Size.large
                        } else {
                            Size.small
                        },
                    bottom = Size.large,
                ),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(Size.mediumLarge),
            elevation =
                CardDefaults.elevatedCardElevation(defaultElevation = Size.small - Size.extraTiny),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onTap?.invoke() },
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.large, vertical = Size.largePlus),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (transition) {
                    is ChapterTransition.Prev -> {
                        PrevChapterTransitionContent(
                            transition = transition,
                            manga = manga,
                            downloadManager = downloadManager,
                        )
                    }
                    is ChapterTransition.Next -> {
                        NextChapterTransitionContent(
                            transition = transition,
                            manga = manga,
                            downloadManager = downloadManager,
                        )
                    }
                }

                MissingChapterWarningSection(transition = transition)

                transition.to?.let { targetChapter ->
                    Spacer(modifier = Modifier.height(Size.mediumLarge))
                    ChapterPreloadStatusSection(
                        chapter = targetChapter,
                        onRetry = { onRetry(targetChapter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrevChapterTransitionContent(
    transition: ChapterTransition.Prev,
    manga: MangaItem?,
    downloadManager: DownloadManager,
) {
    val prevChapter = transition.to
    if (prevChapter != null) {
        val isPrevDownloaded =
            manga?.let { downloadManager.isChapterDownloaded(prevChapter.chapter, it.toManga()) }
                ?: false
        val isCurrentDownloaded =
            manga?.let {
                downloadManager.isChapterDownloaded(transition.from.chapter, it.toManga())
            } ?: false

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.previous_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            ) {
                Text(
                    text = prevChapter.chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPrevDownloaded != isCurrentDownloaded) {
                    Spacer(modifier = Modifier.width(Size.small))
                    DownloadStatusIcon(isDownloaded = isPrevDownloaded)
                }
            }

            Spacer(modifier = Modifier.height(Size.mediumLarge))

            Text(
                text = stringResource(R.string.current_chapter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = transition.from.chapter.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.theres_no_previous_chapter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Size.smedium),
        )
    }
}

@Composable
private fun NextChapterTransitionContent(
    transition: ChapterTransition.Next,
    manga: MangaItem?,
    downloadManager: DownloadManager,
) {
    val nextChapter = transition.to
    if (nextChapter != null) {
        val isCurrentDownloaded =
            manga?.let {
                downloadManager.isChapterDownloaded(transition.from.chapter, it.toManga())
            } ?: false
        val isNextDownloaded =
            manga?.let { downloadManager.isChapterDownloaded(nextChapter.chapter, it.toManga()) }
                ?: false

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.finished_chapter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = transition.from.chapter.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            )

            Spacer(modifier = Modifier.height(Size.mediumLarge))

            Text(
                text = stringResource(R.string.next_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            ) {
                Text(
                    text = nextChapter.chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isNextDownloaded != isCurrentDownloaded) {
                    Spacer(modifier = Modifier.width(Size.small))
                    DownloadStatusIcon(isDownloaded = isNextDownloaded)
                }
            }
        }
    } else {
        Text(
            text = stringResource(R.string.theres_no_next_chapter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Size.smedium),
        )
    }
}

@Composable
private fun DownloadStatusIcon(isDownloaded: Boolean) {
    Icon(
        painter =
            painterResource(
                if (isDownloaded) R.drawable.ic_file_download_24dp else R.drawable.ic_cloud_24dp
            ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(Size.large - Size.extraTiny),
    )
}

@Composable
private fun MissingChapterWarningSection(transition: ChapterTransition) {
    if (transition.to == null) return

    val hasMissing =
        when (transition) {
            is ChapterTransition.Prev -> hasMissingChapters(transition.from, transition.to)
            is ChapterTransition.Next -> hasMissingChapters(transition.to, transition.from)
        }

    if (!hasMissing) return

    val diff =
        when (transition) {
            is ChapterTransition.Prev -> calculateChapterDifference(transition.from, transition.to)
            is ChapterTransition.Next -> calculateChapterDifference(transition.to, transition.from)
        }

    Spacer(modifier = Modifier.height(Size.medium))
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(Size.small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Size.smedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Size.large),
            )
            Spacer(modifier = Modifier.width(Size.small))
            Text(
                text =
                    pluralStringResource(
                        R.plurals.missing_chapters_warning,
                        diff.toInt(),
                        diff.toInt(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun ChapterPreloadStatusSection(
    chapter: ReaderChapter,
    onRetry: () -> Unit,
) {
    val state by chapter.stateFlow.collectAsStateWithLifecycle()

    when (val currentState = state) {
        is ReaderChapter.State.Wait,
        is ReaderChapter.State.Loading,
        is ReaderChapter.State.Loaded -> {}
        is ReaderChapter.State.Error -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = Size.small),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.failed_to_load_pages_,
                            currentState.error.message ?: "",
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Size.small))
                Button(onClick = onRetry) { Text(text = stringResource(R.string.retry)) }
            }
        }
    }
}
