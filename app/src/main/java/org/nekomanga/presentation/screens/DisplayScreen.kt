package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import eu.kanade.tachiyomi.ui.source.latest.DisplayViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.browse.DisplayScreenSheet
import org.nekomanga.presentation.screens.browse.DisplaySheetScreen
import org.nekomanga.presentation.screens.display.DisplayTopBar
import org.nekomanga.presentation.theme.Size

@Composable
fun DisplayScreen(
    viewModel: DisplayViewModel,
    onBackPressed: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
) {
    val screenState by viewModel.displayScreenState.collectAsStateWithLifecycle()

    DisplayWrapper(
        displayScreenState = screenState,
        switchDisplayClick = viewModel::switchDisplayMode,
        libraryEntryVisibilityClick = viewModel::switchLibraryEntryVisibility,
        onBackPress = onBackPressed,
        openManga = { mangaId: Long -> onNavigateTo(Screens.Manga(mangaId)) },
        addNewCategory = viewModel::addNewCategory,
        toggleFavorite = viewModel::toggleFavorite,
        loadNextPage = viewModel::loadNextItems,
        retryClick = viewModel::loadNextItems,
    )
}

@Composable
private fun DisplayWrapper(
    displayScreenState: DisplayScreenState,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    onBackPress: () -> Unit,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    loadNextPage: () -> Unit,
    retryClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val updateTopBar = LocalBarUpdater.current

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
                            categories = displayScreenState.categories,
                            isList = displayScreenState.isList,
                            libraryEntryVisibility = displayScreenState.libraryEntryVisibility,
                        )
                    }
                }
            },
        )
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val screenBars = remember {
        ScreenBars(
            topBar = {
                DisplayTopBar(
                    screenState = displayScreenState,
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
            scrollBehavior = scrollBehavior,
        )
    }
    DisposableEffect(Unit) {
        updateTopBar(screenBars)
        onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
    }
    val haptic = LocalHapticFeedback.current
    fun mangaLongClick(displayManga: DisplayManga) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!displayManga.inLibrary && displayScreenState.promptForCategories) {
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
    }

    if (displayScreenState.isLoading && displayScreenState.page == 1) {
        Box(modifier = Modifier.fillMaxSize()) {
            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else if (displayScreenState.error != null) {
        EmptyScreen(
            message = UiText.String(displayScreenState.error!!),
            actions =
                if (displayScreenState.page == 1)
                    persistentListOf(
                        Action(text = UiText.StringResource(R.string.retry), onClick = retryClick)
                    )
                else persistentListOf(),
        )
    } else {
        if (displayScreenState.isList) {
            MangaList(
                mangaList = displayScreenState.filteredDisplayManga,
                shouldOutlineCover = displayScreenState.outlineCovers,
                onClick = openManga,
                onLongClick = ::mangaLongClick,
                lastPage = displayScreenState.endReached,
                loadNextItems = loadNextPage,
            )
        } else {
            MangaGrid(
                mangaList = displayScreenState.filteredDisplayManga,
                shouldOutlineCover = displayScreenState.outlineCovers,
                columns = numberOfColumns(rawValue = displayScreenState.rawColumnCount),
                isComfortable = displayScreenState.isComfortableGrid,
                onClick = openManga,
                onLongClick = ::mangaLongClick,
                lastPage = displayScreenState.endReached,
                loadNextItems = loadNextPage,
            )
        }
        if (displayScreenState.isLoading && displayScreenState.page != 1) {
            Box(Modifier.fillMaxSize()) {
                val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
                val stroke =
                    remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
                LinearWavyProgressIndicator(
                    modifier =
                        Modifier.fillMaxWidth().align(Alignment.TopStart).statusBarsPadding(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                    stroke = stroke,
                    trackStroke = stroke,
                )
            }
        }
    }
}
