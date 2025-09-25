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
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.listGridAppBarAction
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun SimilarScreen(
    similarScreenState: State<SimilarScreenState>,
    switchDisplayClick: () -> Unit,
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
    var longClickedMangaId by remember { mutableStateOf<Long?>(null) }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp)) {
                EditCategorySheet(
                    addingToLibrary = true,
                    categories = similarScreenState.value.categories,
                    cancelClick = { scope.launch { sheetState.hide() } },
                    addNewCategory = addNewCategory,
                    confirmClicked = { selectedCategories ->
                        scope.launch { sheetState.hide() }
                        longClickedMangaId?.let { toggleFavorite(it, selectedCategories) }
                    },
                )
            }
        },
    ) {
        NekoScaffold(
            type = NekoScaffoldType.Title,
            onNavigationIconClicked = onBackPress,
            title = stringResource(id = R.string.similar),
            actions = {
                AppBarActions(
                    actions =
                        listOf(
                            listGridAppBarAction(
                                isList = similarScreenState.value.isList,
                                onClick = switchDisplayClick,
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
                                    longClickedMangaId = displayManga.mangaId
                                    sheetState.show()
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
    if (similarScreenState.value.displayManga.isEmpty()) {
        if (similarScreenState.value.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            EmptyScreen(
                message = UiText.StringResource(id = R.string.no_results_found),
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
                groupedManga = similarScreenState.value.displayManga,
                shouldOutlineCover = similarScreenState.value.outlineCovers,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = similarScreenState.value.displayManga,
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
