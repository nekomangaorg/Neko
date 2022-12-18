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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import java.util.Calendar
import java.util.Date
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.FeedChapter
import org.nekomanga.presentation.components.MangaCover
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

            Text(text = "Fetched: ${group.first.timeSpanFromNow}", style = MaterialTheme.typography.labelLarge)

            group.second.forEach { feedChapter ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        MangaCover.Square.invoke(
                            artwork = feedChapter.artwork,
                            shouldOutlineCover = true,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(Padding.extraSmall),
                        )
                        Column(Modifier.fillMaxWidth()) {
                            Text(text = feedChapter.simpleChapter.chapterTitle)
                            Text(text = feedChapter.mangaTitle)
                            Text(text = feedChapter.simpleChapter.dateUpload.timeSpanFromNow)
                        }

                    }
                }
                Gap(Padding.extraSmall)
                LaunchedEffect(scrollState) {
                    if (hasMoreResults && feedChapters.indexOf(feedChapter) >= feedChapters.size - 4) {
                        loadNextPage()
                    }
                }
            }

        }
    }
}
