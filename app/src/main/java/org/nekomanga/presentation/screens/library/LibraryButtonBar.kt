package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.nekomanga.presentation.screens.library.LibraryScreenActions
import org.nekomanga.presentation.screens.library.LibraryScreenState
import org.nekomanga.presentation.screens.library.filter.FilterBookmarked
import org.nekomanga.presentation.screens.library.filter.FilterCompleted
import org.nekomanga.presentation.screens.library.filter.FilterDownloaded
import org.nekomanga.presentation.screens.library.filter.FilterMangaType
import org.nekomanga.presentation.screens.library.filter.FilterMerged
import org.nekomanga.presentation.screens.library.filter.FilterMissingChapters
import org.nekomanga.presentation.screens.library.filter.FilterTracked
import org.nekomanga.presentation.screens.library.filter.FilterUnavailable
import org.nekomanga.presentation.screens.library.filter.FilterUnread
import org.nekomanga.presentation.screens.library.filter.LibraryFilterType
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.icons.CollapseAllIcon
import org.nekomanga.presentation.components.icons.ExpandAllIcon
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryButtonBar(
    modifier: Modifier = Modifier,
    libraryScreenActions: LibraryScreenActions,
    libraryScreenState: LibraryScreenState,
    showCollapseAll: Boolean,
    groupByClick: () -> Unit,
) {
    val filterScrollState = rememberScrollState()

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(filterScrollState),
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        Gap(Size.tiny)

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            AnimatedVisibility(showCollapseAll) {
                FilledTonalButton(
                    shapes =
                        ButtonShapes(
                            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                        ),
                    onClick = { libraryScreenActions.collapseExpandAllCategories() },
                ) {
                    Icon(
                        imageVector =
                            if (libraryScreenState.allCollapsed) ExpandAllIcon else CollapseAllIcon,
                        contentDescription =
                            stringResource(
                                id =
                                    if (libraryScreenState.allCollapsed) R.string.expand
                                    else R.string.collapse
                            ),
                    )
                }
            }

            FilledTonalButton(
                shapes =
                    ButtonShapes(
                        shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                        pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                    ),
                onClick = groupByClick,
            ) {
                Text(text = stringResource(R.string.group_library_by))
            }
        }
        AnimatedVisibility(libraryScreenState.hasActiveFilters) {
            FilledIconButton(onClick = libraryScreenActions.clearActiveFilters) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.clear),
                )
            }
        }

        val unreadToggleList =
            listOf(
                FilterUnread.NotStarted,
                FilterUnread.InProgress,
                FilterUnread.Unread,
                FilterUnread.Read,
            )

        val downloadToggleList = listOf(FilterDownloaded.Downloaded, FilterDownloaded.NotDownloaded)
        val completedToggleList = listOf(FilterCompleted.Completed, FilterCompleted.Ongoing)
        val mangaTypeToggleList =
            listOf(FilterMangaType.Manga, FilterMangaType.Manhwa, FilterMangaType.Manhua)
        val bookmarkToggleList = listOf(FilterBookmarked.Bookmarked, FilterBookmarked.NotBookmarked)
        val missingToggleList =
            listOf(FilterMissingChapters.MissingChapter, FilterMissingChapters.NoMissingChapters)
        val unavailableToggleList =
            listOf(FilterUnavailable.Unavailable, FilterUnavailable.NoUnavailable)
        val mergedToggleList = listOf(FilterMerged.Merged, FilterMerged.NotMerged)
        val trackedToggleList = listOf(FilterTracked.Tracked, FilterTracked.NotTracked)
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterUnread,
            unreadToggleList,
            libraryScreenActions.filterToggled,
        )
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterDownloaded,
            downloadToggleList,
            libraryScreenActions.filterToggled,
        )
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterCompleted,
            completedToggleList,
            libraryScreenActions.filterToggled,
        )
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterMangaType,
            mangaTypeToggleList,
            libraryScreenActions.filterToggled,
        )
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterBookmarked,
            bookmarkToggleList,
            libraryScreenActions.filterToggled,
        )
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterMissingChapters,
            missingToggleList,
            libraryScreenActions.filterToggled,
        )
        if (libraryScreenState.showUnavailableFilter) {
            ConnectedToggleButtons(
                libraryScreenState.libraryFilters.filterUnavailable,
                unavailableToggleList,
                libraryScreenActions.filterToggled,
            )
        }
        ConnectedToggleButtons(
            libraryScreenState.libraryFilters.filterMerged,
            mergedToggleList,
            libraryScreenActions.filterToggled,
        )
        if (libraryScreenState.showTrackedFilter) {
            ConnectedToggleButtons(
                libraryScreenState.libraryFilters.filterTracked,
                trackedToggleList,
                libraryScreenActions.filterToggled,
            )
        }

        Gap(Size.small)
    }
}

@Composable
private fun ConnectedToggleButtons(
    current: LibraryFilterType,
    buttons: List<LibraryFilterType>,
    toggleFilter: (LibraryFilterType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        buttons.forEachIndexed { index, buttonFilterType ->
            ToggleButton(
                checked = buttonFilterType == current,
                onCheckedChange = { toggleFilter(buttonFilterType.toggle(it)) },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        buttons.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(buttonFilterType.UiText().asString())
            }
        }
    }
}

private data class ToggleButtonFields(
    val buttonChecked: Boolean,
    val buttonOnCheckedChange: (Boolean) -> Unit,
    val buttonText: String,
)
