package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.HomePageManga
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGridItem

@Composable
fun BrowseHomePage(
    browseHomePageManga: PersistentList<HomePageManga>,
    contentPadding: PaddingValues,
    shouldOutlineCover: Boolean,
    isComfortable: Boolean,
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        browseHomePageManga.forEach { homePageManga ->

            item(key = homePageManga.title + homePageManga.titleRes) {
                val headerText = when (homePageManga.titleRes == null) {
                    true -> homePageManga.title ?: ""
                    false -> stringResource(id = homePageManga.titleRes)
                }
                Text(
                    text = headerText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                )
                Gap(4.dp)
            }
            item(key = homePageManga.title + homePageManga.titleRes + browseHomePageManga.size) {
                LazyRow(
                    modifier = Modifier
                        .requiredHeight(250.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item { Gap(8.dp) }
                    items(homePageManga.displayManga, key = { displayManga -> displayManga.mangaId }) { displayManga ->
                        MangaGridItem(
                            displayManga = displayManga,
                            shouldOutlineCover = shouldOutlineCover,
                            isComfortable = isComfortable,
                            onClick = { onClick(displayManga.mangaId) },
                            onLongClick = { onLongClick(displayManga) },
                        )
                    }
                    item { Gap(8.dp) }
                }
            }
        }
    }
}
