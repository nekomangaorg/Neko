package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.DisplayMangaHolder
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.NoResultsEmptyScreen

@Composable
fun BrowseFilterPage(
    displayMangaHolder: DisplayMangaHolder,
    isList: Boolean,
    isComfortableGrid: Boolean,
    outlineCovers: Boolean,
    rawColumnCount: Float,
    pageLoading: Boolean,
    lastPage: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
    loadNextPage: () -> Unit,
) {

    if (displayMangaHolder.allDisplayManga.isEmpty()) {
        NoResultsEmptyScreen(contentPadding)
    } else {
        if (isList) {
            MangaList(
                mangaList = displayMangaHolder.filteredDisplayManga,
                shouldOutlineCover = outlineCovers,
                contentPadding = contentPadding,
                onClick = onClick,
                onLongClick = onLongClick,
                lastPage = lastPage,
                loadNextItems = loadNextPage,
            )
        } else {
            MangaGrid(
                mangaList = displayMangaHolder.filteredDisplayManga,
                shouldOutlineCover = outlineCovers,
                contentPadding = contentPadding,
                columns = numberOfColumns(rawValue = rawColumnCount),
                isComfortable = isComfortableGrid,
                onClick = onClick,
                onLongClick = onLongClick,
                lastPage = lastPage,
                loadNextItems = loadNextPage,
            )
        }
        if (pageLoading) {
            Box(Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 8.dp + contentPadding.calculateBottomPadding(), top = contentPadding.calculateTopPadding())
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
