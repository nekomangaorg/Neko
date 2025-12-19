package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.SearchOutlineTopAppBar
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun LibraryScreenTopBar(
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    onSearchLoaded: () -> Unit = {},
    groupByClick: () -> Unit,
    editCategoryClick: () -> Unit,
    displayOptionsClick: () -> Unit,
    removeFromLibraryClick: () -> Unit,
    removeActionClick: (MangaConstants.DownloadAction) -> Unit,
    markActionClick: (ChapterMarkActions) -> Unit,
) {

    val selectionMode =
        remember(libraryScreenState.selectedItems.isNotEmpty()) {
            libraryScreenState.selectedItems.isNotEmpty()
        }

    val (color, onColor, useDarkIcons) = getTopAppBarColor("", selectionMode)

    if (selectionMode) {
        TitleTopAppBar(
            color = color,
            onColor = onColor,
            title = "Selected: ${libraryScreenState.selectedItems.size}",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationIconLabel = stringResource(id = R.string.back),
            onNavigationIconClicked = { libraryScreenActions.clearSelectedManga() },
            incognitoMode = libraryScreenState.incognitoMode,
            actions = {
                LibraryAppBarActions(
                    downloadChapters = libraryScreenActions.downloadChapters,
                    removeDownloads = { removeAction -> removeActionClick(removeAction) },
                    shareManga = libraryScreenActions.shareManga,
                    syncMangaToDexClick = libraryScreenActions.syncMangaToDex,
                    editCategoryClick = editCategoryClick,
                    markChapterClick = { markAction -> markActionClick(markAction) },
                    removeFromLibraryClick = removeFromLibraryClick,
                )
            },
            scrollBehavior = scrollBehavior,
        )
    } else {

        SearchOutlineTopAppBar(
            onSearch = libraryScreenActions.search,
            searchPlaceHolder = stringResource(R.string.search_library),
            searchPlaceHolderAlt = stringResource(R.string.library_search_hint),
            color = color,
            incognitoMode = libraryScreenState.incognitoMode,
            initialSearch = libraryScreenState.initialSearch,
            onSearchLoaded = onSearchLoaded,
            actions = {
                AppBarActions(
                    actions =
                        listOf(
                            AppBar.Action(
                                title = UiText.StringResource(R.string.settings),
                                icon = Icons.Outlined.Tune,
                                onClick = displayOptionsClick,
                            ),
                            mainDropDown,
                        )
                )
            },
            underHeaderActions = {
                AnimatedVisibility(
                    !libraryScreenState.selectedItems.isNotEmpty() &&
                        libraryScreenState.showLibraryButtonBar
                ) {
                    LibraryButtonBar(
                        libraryScreenActions = libraryScreenActions,
                        libraryScreenState = libraryScreenState,
                        showCollapseAll =
                            libraryScreenState.items.size > 1 &&
                                !libraryScreenState.horizontalCategories,
                        groupByClick = groupByClick,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}
