package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.FeedChapter
import org.nekomanga.presentation.components.HeaderCard
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Padding

@Composable
fun FeedUpdatePage(
    contentPadding: PaddingValues,
    feedChapters: ImmutableList<FeedChapter>,
    hasMoreResults: Boolean,
    loadNextPage: () -> Unit,
) {
    val scrollState = rememberLazyListState()

    val grouped = remember(feedChapters) {
        feedChapters.groupBy {
            val cal = Calendar.getInstance()
            cal.time = Date(it.simpleChapter.dateFetch)
            cal[Calendar.HOUR_OF_DAY] = 0
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            cal[Calendar.MILLISECOND] = 0
            cal.time.time
        }.toList()
    }


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {

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
                Row(Modifier.fillMaxWidth()) {
                    MangaCover.Square.invoke(
                        artwork = feedChapter.artwork,
                        shouldOutlineCover = true,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.CenterVertically)
                            .padding(Padding.extraSmall),
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Padding.extraSmall),
                    ) {
                        Text(text = feedChapter.simpleChapter.chapterText, style = MaterialTheme.typography.bodyLarge, overflow = TextOverflow.Ellipsis)
                        Text(text = feedChapter.mangaTitle, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "Updated ${feedChapter.simpleChapter.dateUpload.timeSpanFromNow}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
                            overflow = TextOverflow.Ellipsis,
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
