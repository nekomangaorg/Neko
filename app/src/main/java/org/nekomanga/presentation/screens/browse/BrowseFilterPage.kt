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
import eu.kanade.tachiyomi.ui.source.browse.DisplayMangaHolder
import org.nekomanga.R
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.EmptyScreen
import org.nekomanga.presentation.theme.Size

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
        EmptyScreen(
            message = UiText.StringResource(resourceId = R.string.no_results_found),
            contentPadding = contentPadding,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
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
                LinearProgressIndicator(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = Size.small),
                )
            }
        }
    }
}
