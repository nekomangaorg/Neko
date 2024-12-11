package org.nekomanga.presentation.screens.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.ui.recents.DownloadScreenActions
import eu.kanade.tachiyomi.ui.recents.DownloaderStatus
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size
import soup.compose.material.motion.MaterialFade

@Composable
fun DownloadScreen(
    contentPadding: PaddingValues,
    downloads: ImmutableList<DownloadItem>,
    downloaderStatus: DownloaderStatus,
    downloadScreenActions: DownloadScreenActions,
) {

    val scrollState = rememberLazyListState()

    val downloadGroup =
        remember(downloads) {
            downloads.groupBy {
                MergeType.getMergeTypeFromName(it.chapterItem.chapter.scanlator)?.name
                    ?: MdConstants.name
            }
        }

    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
    ) {
        LazyColumn(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = Size.small, end = Size.small, bottom = Size.large),
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Size.medium),
        ) {
            stickyHeader(key = downloaderStatus) {
                MaterialFade(visible = downloaderStatus == DownloaderStatus.NetworkPaused) {
                    ElevatedCard(
                        modifier =
                            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(Size.medium),
                        colors =
                            CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Size.smedium),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.no_wifi_connection),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
            downloadGroup.entries.forEach { entry ->
                item(entry.key) {
                    ElevatedCard {
                        Text(
                            modifier = Modifier.padding(top = Size.small).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "${entry.key} (${entry.value.size})",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = NekoColors.mediumAlphaHighContrast
                                ),
                        )
                        entry.value.forEachIndexed { index, download ->
                            val first = index == 0
                            val last = index == downloads.lastIndex

                            DownloadChapterRow(
                                modifier = Modifier.animateItem(),
                                first = first,
                                last = last,
                                chapter = download,
                                downloadSwiped = { downloadScreenActions.downloadSwiped(download) },
                                moveDownloadClicked = { direction ->
                                    downloadScreenActions.moveDownloadClick(download, direction)
                                },
                                moveSeriesClicked = { direction ->
                                    downloadScreenActions.moveSeriesClick(download, direction)
                                },
                                cancelSeriesClicked = {
                                    downloadScreenActions.cancelSeriesClick(download)
                                },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = downloaderStatus != DownloaderStatus.NetworkPaused,
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            ExtendedFloatingActionButton(
                modifier =
                    Modifier.padding(
                        bottom = contentPadding.calculateBottomPadding() - Size.medium,
                        end = Size.small,
                    ),
                onClick = downloadScreenActions.fabClick,
                icon = {
                    when (downloaderStatus) {
                        DownloaderStatus.Running -> Icon(Icons.Default.Pause, null)
                        else -> Icon(Icons.Default.PlayArrow, null)
                    }
                },
                text = {
                    when (downloaderStatus) {
                        DownloaderStatus.Running -> Text(text = stringResource(R.string.pause))
                        else -> Text(text = stringResource(R.string.resume))
                    }
                },
            )
        }
    }
}
