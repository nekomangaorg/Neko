package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.functions.numberOfColumns

@Composable
fun DisplayScreenContent(
    displayScreenType: DisplayScreenType,
    groupedManga: ImmutableMap<Int, PersistentList<DisplayManga>>,
    isList: Boolean,
    isComfortable: Boolean,
    rawColumns: Float,
    shouldOutlineCover: Boolean,
    contentPadding: PaddingValues,
    mangaClick: (Long) -> Unit,
    mangaLongClick: (DisplayManga) -> Unit,
    loadNextPage: () -> Unit,
    endReached: Boolean,
) {
    when (displayScreenType) {
        is DisplayScreenType.Similar -> {
            if (isList) {
                MangaListWithHeader(
                    groupedManga = groupedManga,
                    shouldOutlineCover = shouldOutlineCover,
                    contentPadding = contentPadding,
                    onClick = mangaClick,
                    onLongClick = mangaLongClick,
                )
            } else {
                MangaGridWithHeader(
                    groupedManga = groupedManga,
                    shouldOutlineCover = shouldOutlineCover,
                    columns = numberOfColumns(rawValue = rawColumns),
                    isComfortable = isComfortable,
                    contentPadding = contentPadding,
                    onClick = mangaClick,
                    onLongClick = mangaLongClick,
                )
            }
        }
        else -> {
            PaginatedScreenContent(
                mangaList = groupedManga[0] ?: emptyList(),
                isList = isList,
                isComfortable = isComfortable,
                rawColumns = rawColumns,
                shouldOutlineCover = shouldOutlineCover,
                contentPadding = contentPadding,
                mangaClick = mangaClick,
                mangaLongClick = mangaLongClick,
                loadNextPage = loadNextPage,
                endReached = endReached,
            )
        }
    }
}

@Composable
private fun PaginatedScreenContent(
    mangaList: List<DisplayManga>,
    isList: Boolean,
    isComfortable: Boolean,
    rawColumns: Float,
    shouldOutlineCover: Boolean,
    contentPadding: PaddingValues,
    mangaClick: (Long) -> Unit,
    mangaLongClick: (DisplayManga) -> Unit,
    loadNextPage: () -> Unit,
    endReached: Boolean,
) {
    if (isList) {
        MangaList(
            mangaList = mangaList,
            shouldOutlineCover = shouldOutlineCover,
            contentPadding = contentPadding,
            onClick = mangaClick,
            onLongClick = mangaLongClick,
            lastPage = endReached,
            loadNextItems = loadNextPage,
        )
    } else {
        MangaGrid(
            mangaList = mangaList,
            shouldOutlineCover = shouldOutlineCover,
            columns = numberOfColumns(rawValue = rawColumns),
            isComfortable = isComfortable,
            contentPadding = contentPadding,
            onClick = mangaClick,
            onLongClick = mangaLongClick,
            lastPage = endReached,
            loadNextItems = loadNextPage,
        )
    }
}
