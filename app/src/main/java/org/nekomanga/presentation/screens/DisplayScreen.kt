package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.browse.DisplayScreenSheet
import org.nekomanga.presentation.screens.browse.DisplaySheetScreen
import org.nekomanga.presentation.theme.Size

@Composable
fun DisplayScreen(
    displayScreenState: State<DisplayScreenState>,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    onBackPress: () -> Unit,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    loadNextPage: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            categories = displayScreenState.value.categories,
                            isList = displayScreenState.value.isList,
                            libraryEntryVisibility = displayScreenState.value.libraryEntryVisibility,
                        )
                    }
                }
            },
        )
    }
    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onBackPress,
        incognitoMode = displayScreenState.value.incognitoMode,
        title =
            if (displayScreenState.value.titleRes != null)
                stringResource(id = displayScreenState.value.titleRes!!)
            else displayScreenState.value.title,
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = {
                                scope.launch {
                                    openSheet(
                                        DisplaySheetScreen.BrowseDisplayOptionsSheet(
                                            showIsList = true,
                                            switchDisplayClick = switchDisplayClick,
                                            libraryEntryVisibilityClick =
                                                libraryEntryVisibilityClick,
                                        )
                                    )
                                }
                            },
                        )
                    )
            )
        },
        content = { incomingContentPadding ->
            val contentPadding =
                PaddingValues(
                    bottom =
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues()
                            .calculateBottomPadding(),
                    top = incomingContentPadding.calculateTopPadding(),
                )

            PullRefresh(
                isRefreshing = displayScreenState.value.isRefreshing,
                onRefresh = onRefresh,
            ) {
                val haptic = LocalHapticFeedback.current
                fun mangaLongClick(displayManga: DisplayManga) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!displayManga.inLibrary && displayScreenState.value.promptForCategories) {
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

                if (
                    (displayScreenState.value.isPageLoading &&
                        displayScreenState.value.page == 1) ||
                        (displayScreenState.value.isRefreshing)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else if (displayScreenState.value.error != null) {
                    EmptyScreen(
                        message = UiText.String(displayScreenState.value.error!!),
                        actions =
                            if (displayScreenState.value.page == 1) {
                                persistentListOf(
                                    Action(
                                        text = UiText.StringResource(R.string.retry),
                                        onClick = onRefresh,
                                    )
                                )
                            } else {
                                persistentListOf()
                            },
                        contentPadding = incomingContentPadding,
                    )
                } else {
                    if (displayScreenState.value.isList) {
                        MangaListWithHeader(
                            groupedManga = displayScreenState.value.filteredDisplayManga,
                            shouldOutlineCover = displayScreenState.value.outlineCovers,
                            contentPadding = contentPadding,
                            onClick = openManga,
                            onLongClick = ::mangaLongClick,
                            lastPage = displayScreenState.value.endReached,
                            loadNextItems = loadNextPage,
                        )
                    } else {
                        MangaGridWithHeader(
                            groupedManga = displayScreenState.value.filteredDisplayManga,
                            shouldOutlineCover = displayScreenState.value.outlineCovers,
                            columns =
                                numberOfColumns(rawValue = displayScreenState.value.rawColumnCount),
                            isComfortable = displayScreenState.value.isComfortableGrid,
                            contentPadding = contentPadding,
                            onClick = openManga,
                            onLongClick = ::mangaLongClick,
                            lastPage = displayScreenState.value.endReached,
                            loadNextItems = loadNextPage,
                        )
                    }
                    if (
                        displayScreenState.value.isPageLoading && displayScreenState.value.page > 1
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
                            val stroke =
                                remember(strokeWidth) {
                                    Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                }
                            LinearWavyProgressIndicator(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .align(Alignment.TopStart)
                                        .statusBarsPadding(),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor =
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                                stroke = stroke,
                                trackStroke = stroke,
                            )
                        }
                    }
                }
            }
        },
    )
}
