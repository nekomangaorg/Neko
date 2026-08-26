package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    dynamicCovers: Boolean,
    rawColumnCount: Float,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
) {
    var collapsedGroups by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val toggleGroupCollapse: (Int) -> Unit = { groupId ->
        collapsedGroups =
            if (groupId in collapsedGroups) collapsedGroups - groupId else collapsedGroups + groupId
    }
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
                dynamicCover = dynamicCovers,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
                collapsedGroups = collapsedGroups,
                onToggleGroupCollapse = toggleGroupCollapse,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = displayMangaHolder.groupedDisplayManga,
                shouldOutlineCover = outlineCovers,
                dynamicCover = dynamicCovers,
                columns = numberOfColumns(rawValue = rawColumnCount),
                isComfortable = isComfortableGrid,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
                collapsedGroups = collapsedGroups,
                onToggleGroupCollapse = toggleGroupCollapse,
            )
        }
    }
}
