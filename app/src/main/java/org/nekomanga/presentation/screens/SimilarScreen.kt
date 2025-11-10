package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.similar.SimilarScreenState
import eu.kanade.tachiyomi.ui.similar.SimilarViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.browse.DisplayScreenSheet
import org.nekomanga.presentation.screens.browse.DisplaySheetScreen
import org.nekomanga.presentation.screens.similar.SimilarTopBar
import org.nekomanga.presentation.theme.Size

@Composable
fun SimilarScreen(
    viewModel: SimilarViewModel,
    onBackPressed: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
) {

    val screenState by viewModel.similarScreenState.collectAsStateWithLifecycle()

    SimilarWrapper(
        similarScreenState = screenState,
        switchDisplayClick = viewModel::switchDisplayMode,
        libraryEntryVisibilityClick = viewModel::switchLibraryEntryVisibility,
        onBackPress = onBackPressed,
        mangaClick = { id -> onNavigateTo(Screens.Manga(id)) },
        addNewCategory = viewModel::addNewCategory,
        toggleFavorite = viewModel::toggleFavorite,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun SimilarWrapper(
    similarScreenState: SimilarScreenState,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    onBackPress: () -> Unit,
    mangaClick: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    onRefresh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pullRefreshState =
        remember(similarScreenState.isRefreshing) {
            PullRefreshState(
                enabled = true,
                isRefreshing = similarScreenState.isRefreshing,
                onRefresh = onRefresh,
            )
        }

    var currentBottomSheet: DisplaySheetScreen? by remember { mutableStateOf(null) }

    LaunchedEffect(currentBottomSheet) {
        if (currentBottomSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val openSheet: (DisplaySheetScreen) -> Unit = { scope.launch { currentBottomSheet = it } }

    if (currentBottomSheet != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { currentBottomSheet = null },
            content = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    currentBottomSheet?.let { currentSheet ->
                        DisplayScreenSheet(
                            currentScreen = currentSheet,
                            addNewCategory = addNewCategory,
                            contentPadding =
                                WindowInsets.navigationBars
                                    .only(WindowInsetsSides.Bottom)
                                    .asPaddingValues(),
                            closeSheet = { currentBottomSheet = null },
                            categories = similarScreenState.categories,
                            isList = similarScreenState.isList,
                            libraryEntryVisibility = similarScreenState.libraryEntryVisibility,
                        )
                    }
                }
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val haptic = LocalHapticFeedback.current

    ChildScreenScaffold(
        pullRefreshState = pullRefreshState,
        scrollBehavior = scrollBehavior,
        topBar = {
            SimilarTopBar(
                screenState = similarScreenState,
                onNavigationIconClicked = onBackPress,
                scrollBehavior = scrollBehavior,
                onSettingClick = {
                    scope.launch {
                        openSheet(
                            DisplaySheetScreen.BrowseDisplayOptionsSheet(
                                showIsList = true,
                                switchDisplayClick = switchDisplayClick,
                                libraryEntryVisibilityClick = libraryEntryVisibilityClick,
                            )
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        SimilarContent(
            contentPadding = contentPadding,
            similarScreenState = similarScreenState,
            onRefresh = onRefresh,
            mangaClick = mangaClick,
            mangaLongClick = { displayManga: DisplayManga ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!displayManga.inLibrary && similarScreenState.promptForCategories) {
                    scope.launch {
                        openSheet(
                            DisplaySheetScreen.CategoriesSheet(
                                setCategories = { selectedCategories ->
                                    scope.launch { sheetState.hide() }
                                    toggleFavorite(displayManga.mangaId, selectedCategories)
                                }
                            )
                        )
                    }
                } else {
                    toggleFavorite(displayManga.mangaId, emptyList())
                }
            },
        )
    }
}

@Composable
private fun SimilarContent(
    similarScreenState: SimilarScreenState,
    contentPadding: PaddingValues = PaddingValues(),
    onRefresh: () -> Unit,
    mangaClick: (Long) -> Unit,
    mangaLongClick: (DisplayManga) -> Unit,
) {
    if (similarScreenState.filteredDisplayManga.isEmpty()) {
        if (similarScreenState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            EmptyScreen(
                message = UiText.StringResource(resourceId = R.string.no_results_found),
                actions =
                    persistentListOf(
                        Action(text = UiText.StringResource(R.string.retry), onClick = onRefresh)
                    ),
            )
        }
    } else {
        if (similarScreenState.isList) {
            MangaListWithHeader(
                groupedManga = similarScreenState.filteredDisplayManga,
                shouldOutlineCover = similarScreenState.outlineCovers,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = similarScreenState.filteredDisplayManga,
                shouldOutlineCover = similarScreenState.outlineCovers,
                columns = numberOfColumns(rawValue = similarScreenState.rawColumnCount),
                isComfortable = similarScreenState.isComfortableGrid,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        }
    }
}
