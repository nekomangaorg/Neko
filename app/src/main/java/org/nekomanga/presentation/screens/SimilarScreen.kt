package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.similar.SimilarScreenState
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
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun SimilarScreen(
    similarScreenState: State<SimilarScreenState>,
    switchDisplayClick: () -> Unit,
    libraryEntryVisibilityClick: (Int) -> Unit,
    onBackPress: () -> Unit,
    mangaClick: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    onRefresh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        )

    var currentBottomSheet: DisplaySheetScreen? by remember { mutableStateOf(null) }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val openSheet: (DisplaySheetScreen) -> Unit = {
        scope.launch {
            currentBottomSheet = it
            sheetState.show()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                currentBottomSheet?.let { currentSheet ->
                    DisplayScreenSheet(
                        currentScreen = currentSheet,
                        addNewCategory = addNewCategory,
                        contentPadding =
                            WindowInsets.navigationBars
                                .only(WindowInsetsSides.Bottom)
                                .asPaddingValues(),
                        closeSheet = { scope.launch { sheetState.hide() } },
                        categories = similarScreenState.value.categories,
                        isList = similarScreenState.value.isList,
                        libraryEntryVisibility = similarScreenState.value.libraryEntryVisibility,
                    )
                }
            }
        },
    ) {
        NekoScaffold(
            type = NekoScaffoldType.Title,
            onNavigationIconClicked = onBackPress,
            incognitoMode = similarScreenState.value.incognitoMode,
            title = stringResource(id = R.string.similar),
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
            content = { incomingPaddingValues ->
                PullRefresh(
                    refreshing = similarScreenState.value.isRefreshing,
                    onRefresh = onRefresh,
                    indicatorOffset = (incomingPaddingValues.calculateTopPadding() + Size.huge),
                ) {
                    val haptic = LocalHapticFeedback.current

                    SimilarContent(
                        similarScreenState = similarScreenState,
                        paddingValues = incomingPaddingValues,
                        refreshing = onRefresh,
                        mangaClick = mangaClick,
                        mangaLongClick = { displayManga: DisplayManga ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (
                                !displayManga.inLibrary &&
                                    similarScreenState.value.promptForCategories
                            ) {
                                scope.launch {
                                    openSheet(
                                        DisplaySheetScreen.CategoriesSheet(
                                            setCategories = { selectedCategories ->
                                                scope.launch { sheetState.hide() }
                                                toggleFavorite(
                                                    displayManga.mangaId,
                                                    selectedCategories,
                                                )
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
            },
        )
    }
}

@Composable
private fun SimilarContent(
    similarScreenState: State<SimilarScreenState>,
    paddingValues: PaddingValues = PaddingValues(),
    refreshing: () -> Unit,
    mangaClick: (Long) -> Unit,
    mangaLongClick: (DisplayManga) -> Unit,
) {
    if (similarScreenState.value.filteredDisplayManga.isEmpty()) {
        if (similarScreenState.value.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            EmptyScreen(
                message = UiText.StringResource(resourceId = R.string.no_results_found),
                actions =
                    persistentListOf(
                        Action(text = UiText.StringResource(R.string.retry), onClick = refreshing)
                    ),
            )
        }
    } else {
        val contentPadding =
            PaddingValues(
                bottom =
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues()
                        .calculateBottomPadding(),
                top = paddingValues.calculateTopPadding(),
            )

        if (similarScreenState.value.isList) {
            MangaListWithHeader(
                groupedManga = similarScreenState.value.filteredDisplayManga,
                shouldOutlineCover = similarScreenState.value.outlineCovers,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = similarScreenState.value.filteredDisplayManga,
                shouldOutlineCover = similarScreenState.value.outlineCovers,
                columns = numberOfColumns(rawValue = similarScreenState.value.rawColumnCount),
                isComfortable = similarScreenState.value.isComfortableGrid,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        }
    }
}
