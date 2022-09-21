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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.similar.SimilarScreenState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.ListGridActionButton
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Shapes

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
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    var longClickedMangaId by remember { mutableStateOf<Long?>(null) }

    /**
     * Close the bottom sheet on back if its open
     */
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch { sheetState.hide() }
    }

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
                        longClickedMangaId?.let {
                            toggleFavorite(it, selectedCategories)
                        }
                    },
                )
            }
        },
    ) {
        NekoScaffold(
            title = stringResource(id = R.string.similar),
            onNavigationIconClicked = onBackPress,
            actions = {
                ListGridActionButton(
                    isList = similarScreenState.value.isList,
                    buttonClicked = switchDisplayClick,
                )
            },
        ) { incomingPaddingValues ->
            SwipeRefresh(
                state = rememberSwipeRefreshState(similarScreenState.value.isRefreshing),
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        refreshingOffset = (incomingPaddingValues.calculateTopPadding() * 2),
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,

                    )
                },
            ) {
                val haptic = LocalHapticFeedback.current

                SimilarContent(
                    similarScreenState = similarScreenState,
                    paddingValues = incomingPaddingValues,
                    refreshing = onRefresh,
                    mangaClick = mangaClick,
                    mangaLongClick = { displayManga: DisplayManga ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!displayManga.inLibrary && similarScreenState.value.promptForCategories) {
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
        }
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
    if (!similarScreenState.value.isRefreshing) {
        if (similarScreenState.value.displayManga.isEmpty()) {
            EmptyScreen(
                iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                iconSize = 176.dp,
                message = stringResource(id = R.string.no_results_found),
                actions = persistentListOf(Action(R.string.retry, refreshing)),
            )
        } else {
            val contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    .asPaddingValues().calculateBottomPadding(),
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
}
