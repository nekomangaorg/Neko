package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.source.browse.DisplayMangaHolder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
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
        val groupedManga =
            remember(displayMangaHolder) {
                displayMangaHolder.filteredDisplayManga
                    .groupBy { it.displayTextRes!! }
                    .map { entry ->
                        entry.key to
                            entry.value.map { it.copy(displayTextRes = null) }.toImmutableList()
                    }
                    .toMap()
                    .toImmutableMap()
            }

        if (isList) {
            MangaListWithHeader(
                groupedManga = groupedManga,
                shouldOutlineCover = outlineCovers,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = groupedManga,
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
