package org.nekomanga.presentation.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.ui.recents.DownloadScreenActions
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadScreen(
    contentPadding: PaddingValues,
    downloads: ImmutableList<DownloadItem>,
    downloaderRunning: Boolean,
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            downloadGroup.entries.forEach { entry ->
                item(entry.key) {
                    Text(
                        modifier = Modifier.padding(start = Size.medium),
                        text = "${entry.key} (${entry.value.size})",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = NekoColors.mediumAlphaHighContrast
                            ),
                    )
                }

                itemsIndexed(
                    entry.value,
                    key = { _, download -> download.chapterItem.chapter.id },
                ) { index, download ->
                    val first = index == 0
                    val last = index == downloads.lastIndex

                    DownloadChapterRow(
                        modifier = Modifier.animateItem(),
                        first = first,
                        last = last,
                        chapter = download,
                        downloaderRunning = downloaderRunning,
                        downloadSwiped = { downloadScreenActions.downloadSwiped(download) },
                        moveDownloadClicked = { direction ->
                            downloadScreenActions.moveDownloadClick(download, direction)
                        },
                        moveSeriesClicked = { direction ->
                            downloadScreenActions.moveSeriesClick(download, direction)
                        },
                        cancelSeriesClicked = { downloadScreenActions.cancelSeriesClick(download) },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(
                        bottom = contentPadding.calculateBottomPadding() - Size.medium,
                        end = Size.small,
                    ),
            onClick = downloadScreenActions.fabClick,
            icon = {
                when (downloaderRunning) {
                    true -> Icon(Icons.Default.Pause, null)
                    false -> Icon(Icons.Default.PlayArrow, null)
                }
            },
            text = {
                when (downloaderRunning) {
                    true -> Text(text = stringResource(R.string.pause))
                    false -> Text(text = stringResource(R.string.resume))
                }
            },
        )
    }
}
