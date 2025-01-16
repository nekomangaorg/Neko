package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.feed.FeedManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FeedSummaryPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    updatesFeedMangaList: ImmutableList<FeedManga> = persistentListOf(),
    historyFeedMangaList: ImmutableList<FeedManga> = persistentListOf(),
) {
    val scrollState = rememberLazyListState()
    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material.Text(
                    text = "New chapters",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                )
            }
        }
        items(updatesFeedMangaList) { Text("updated chapter") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material.Text(
                    text = "Continue reading",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                )
            }
        }
        items(historyFeedMangaList) { Text("partially read chapter") }
    }
}
