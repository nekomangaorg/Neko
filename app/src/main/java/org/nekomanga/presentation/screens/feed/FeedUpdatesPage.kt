package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.FeedChapter
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.HeaderCard
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Padding

@Composable
fun FeedUpdatePage(
    contentPadding: PaddingValues,
    feedChapters: ImmutableList<FeedChapter>,
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    groupChaptersUpdates: Boolean,
    toggleGroupChaptersUpdates: () -> Unit,
    mangaClick: (Long) -> Unit,
    loadNextPage: () -> Unit,
) {
    val scrollState = rememberLazyListState()

    val grouped = remember(feedChapters, groupChaptersUpdates) {
        val chapters = when (groupChaptersUpdates) {
            true -> {
                feedChapters.groupBy { it.mangaTitle }.map { entry ->
                    entry.value.first().copy(
                        totalChapter = entry.value.size,
                    )
                }
            }

            false -> feedChapters
        }

        chapters.groupBy {
            val cal = Calendar.getInstance()
            cal.time = Date(it.simpleChapter.dateFetch)
            cal[Calendar.HOUR_OF_DAY] = 0
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            cal[Calendar.MILLISECOND] = 0
            cal.time.time

        }.toList()
    }

    val updatedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    var chapterDropdown by remember { mutableStateOf(false) }

    val themeColorState = defaultThemeColorState()


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {

        item {
            CheckboxRow(checkedState = groupChaptersUpdates, checkedChange = { toggleGroupChaptersUpdates() }, rowText = stringResource(id = R.string.group_chapters_together))
        }
        items(grouped) { group ->
            HeaderCard {
                Text(
                    text = "Fetched ${group.first.timeSpanFromNow}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(Padding.small)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Gap(Padding.small)
            group.second.forEach { feedChapter ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(color = Color.Magenta),
                ) {
                    MangaCover.Square.invoke(
                        artwork = feedChapter.artwork,
                        shouldOutlineCover = outlineCovers,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.CenterVertically)
                            .padding(Padding.extraSmall),
                        onClick = { mangaClick(feedChapter.mangaId) },
                    )
                    Column(
                        Modifier
                            .fillMaxWidth(.88f)
                            .padding(horizontal = Padding.extraSmall),
                    ) {

                        val textColor = when (feedChapter.simpleChapter.read) {
                            true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
                            false -> LocalContentColor.current
                        }


                        Text(text = feedChapter.mangaTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                        Text(text = feedChapter.simpleChapter.chapterText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                        Text(
                            text = "Updated ${feedChapter.simpleChapter.dateUpload.timeSpanFromNow}",
                            style = MaterialTheme.typography.bodySmall,
                            color = updatedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (feedChapter.totalChapter > 1) {
                            Text(
                                text = "${feedChapter.totalChapter} other recently added chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = updatedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                    }
                    Box(modifier = Modifier.align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
                        DownloadButton(
                            themeColorState.buttonColor,
                            Download.State.NOT_DOWNLOADED,
                            0f,
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        when (Download.State.NOT_DOWNLOADED) {
                                            Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                                            else -> chapterDropdown = true
                                        }
                                    },
                                    onLongClick = {},
                                ),
                        )

                    }

                }
                Gap(Padding.small)

                LaunchedEffect(scrollState) {
                    if (hasMoreResults && feedChapters.indexOf(feedChapter) >= feedChapters.size - 4) {
                        loadNextPage()
                    }
                }
            }
        }
    }
}
