package org.nekomanga.presentation.screens.manga

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.R
import org.nekomanga.presentation.components.bars.SearchTopAppBar
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun MangaScreenTopBar(
    screenState: MangaConstants.MangaDetailScreenState,
    chapterActions: MangaConstants.ChapterActions,
    themeColorState: ThemeColorState,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClick: () -> Unit,
    onSearch: (String?) -> Unit,
) {
    SearchTopAppBar(
        onSearchText = onSearch,
        searchPlaceHolder = stringResource(id = R.string.search_chapters),
        color = Color.Transparent,
        navigationIconLabel = stringResource(R.string.back),
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        incognitoMode = screenState.incognitoMode,
        onNavigationIconClicked = onNavigationIconClick,
        actions = {
            MangaDetailsAppBarActions(
                chapterActions = chapterActions,
                themeColorState = themeColorState,
                chapters = screenState.activeChapters,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}
