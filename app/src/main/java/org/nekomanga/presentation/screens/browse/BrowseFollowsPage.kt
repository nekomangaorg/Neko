package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.source.browse.DisplayMangaHolder
import org.nekomanga.R
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.EmptyScreen

@Composable
fun BrowseFollowsPage(
    displayMangaHolder: DisplayMangaHolder,
    isList: Boolean,
    isComfortableGrid: Boolean,
    outlineCovers: Boolean,
    rawColumnCount: Float,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
) {
    if (displayMangaHolder.allDisplayManga.isEmpty()) {
        EmptyScreen(
            message = UiText.StringResource(resourceId = R.string.no_results_found),
            contentPadding = contentPadding,
        )
    } else {
        if (isList) {
            MangaListWithHeader(
                groupedManga = displayMangaHolder.groupedDisplayManga,
                shouldOutlineCover = outlineCovers,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = displayMangaHolder.groupedDisplayManga,
                shouldOutlineCover = outlineCovers,
                columns = numberOfColumns(rawValue = rawColumnCount),
                isComfortable = isComfortableGrid,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
            )
        }
    }
}
