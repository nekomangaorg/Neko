package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.SearchOutlineTopAppBar
import org.nekomanga.presentation.components.getTopAppBarColor
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState

@Composable
fun LibraryScreenTopBar(
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    groupByClick: () -> Unit,
    displayOptionsClick: () -> Unit,
) {

    val (color, onColor, useDarkIcons) =
        getTopAppBarColor("", libraryScreenState.selectedItems.isNotEmpty())

    if (libraryScreenState.selectedItems.isNotEmpty()) {} else {

        SearchOutlineTopAppBar(
            onSearch = libraryScreenActions.search,
            searchPlaceHolder = stringResource(R.string.search_library),
            searchPlaceHolderAlt = stringResource(R.string.library_search_hint),
            color = color,
            incognitoMode = libraryScreenState.incognitoMode,
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
