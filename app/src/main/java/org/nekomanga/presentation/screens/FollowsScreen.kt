package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.follows.FollowsScreenState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.message
import org.nekomanga.presentation.components.ListGridActionButton
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Shapes

@Composable
fun FollowsScreen(
    followsScreenState: State<FollowsScreenState>,
    switchDisplayClick: () -> Unit,
    onBackPress: () -> Unit,
    mangaClick: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    retryClick: () -> Unit,
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
                    categories = followsScreenState.value.categories,
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
            title = stringResource(id = R.string.follows),
            onNavigationIconClicked = onBackPress,
            actions = {
                ListGridActionButton(
                    isList = followsScreenState.value.isList,
                    buttonClicked = switchDisplayClick,
                )
            },
        ) { incomingPaddingValues ->

            if (followsScreenState.value.isLoading) {
                LoadingScreen(incomingPaddingValues)
            } else if (followsScreenState.value.error != null) {
                val message = followsScreenState.value.error!!.message(LocalContext.current)
                EmptyScreen(
                    icon = Icons.Default.ErrorOutline,
                    iconSize = 176.dp,
                    message = message,
                    actions = persistentListOf(Action(R.string.retry, retryClick)),
                )
            } else {
                val haptic = LocalHapticFeedback.current

                FollowsContent(
                    followsScreenState = followsScreenState,
                    paddingValues = incomingPaddingValues,
                    mangaClick = mangaClick,
                    mangaLongClick = { displayManga: DisplayManga ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!displayManga.inLibrary && followsScreenState.value.promptForCategories) {
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
private fun FollowsContent(
    followsScreenState: State<FollowsScreenState>,
    paddingValues: PaddingValues = PaddingValues(),
    mangaClick: (Long) -> Unit,
    mangaLongClick: (DisplayManga) -> Unit,
) {
    if (followsScreenState.value.displayManga.isEmpty()) {
        EmptyScreen(
            iconicImage = CommunityMaterial.Icon.cmd_compass_off,
            iconSize = 176.dp,
            message = stringResource(id = R.string.no_results_found),
        )
    } else {
        val contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                .asPaddingValues().calculateBottomPadding(),
            top = paddingValues.calculateTopPadding(),
        )

        if (followsScreenState.value.isList) {
            MangaListWithHeader(
                groupedManga = followsScreenState.value.displayManga,
                shouldOutlineCover = followsScreenState.value.outlineCovers,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = followsScreenState.value.displayManga,
                shouldOutlineCover = followsScreenState.value.outlineCovers,
                columns = numberOfColumns(rawValue = followsScreenState.value.rawColumnCount),
                isComfortable = followsScreenState.value.isComfortableGrid,
                contentPadding = contentPadding,
                onClick = mangaClick,
                onLongClick = mangaLongClick,
            )
        }
    }
}
