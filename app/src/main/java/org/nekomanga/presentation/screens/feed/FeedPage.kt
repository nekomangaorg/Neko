package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.recents.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.FeedManga
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun FeedPage(
    contentPadding: PaddingValues,
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    mangaClick: (Long) -> Unit,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
    historyScreenGrouping: FeedHistoryGroup,
) {
    val scrollState = rememberLazyListState()

    val updatedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    var chapterDropdown by remember { mutableStateOf(false) }

    val themeColorState = defaultThemeColorState()


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        items(feedMangaList) { feedManga ->
            Gap(Padding.small)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.small),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Shapes.coverRadius))
                        .clickable { mangaClick(feedManga.mangaId) }
                        .align(Alignment.CenterVertically),
                ) {
                    MangaCover.Square.invoke(
                        artwork = feedManga.artwork,
                        shouldOutlineCover = outlineCovers,
                        modifier = Modifier
                            .size(56.dp),
                    )
                }
                Gap(Padding.small)

                Column(
                    Modifier
                        .fillMaxWidth(.88f),
                ) {

                    val textColor = when (feedManga.simpleChapter.first().read) {
                        true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
                        false -> LocalContentColor.current
                    }


                    Text(text = feedManga.mangaTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                    Text(text = feedManga.simpleChapter.first().chapterText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                    val subtitleText = when (feedScreenType) {
                        FeedScreenType.History -> "Read  ${feedManga.simpleChapter.first().lastRead.timeSpanFromNow}"
                        FeedScreenType.Updates -> "Updated ${feedManga.simpleChapter.first().dateUpload.timeSpanFromNow}"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = updatedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (feedManga.totalChapter > 1) {
                        Text(
                            text = "${feedManga.totalChapter} other recently added chapters",
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
                if (hasMoreResults && feedMangaList.indexOf(feedManga) >= feedMangaList.size - 4) {
                    loadNextPage()
                }
            }
        }
    }
}
