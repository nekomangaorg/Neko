package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenType
import eu.kanade.tachiyomi.ui.source.browse.FilterActions
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.system.SideNavMode
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.ListGridActionButton
import org.nekomanga.presentation.components.Loading
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.ShowLibraryEntriesActionButton
import org.nekomanga.presentation.extensions.surfaceColorAtElevation
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.browse.BrowseBottomSheet
import org.nekomanga.presentation.screens.browse.BrowseBottomSheetScreen
import org.nekomanga.presentation.screens.browse.BrowseHomePage
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BrowseScreen(
    browseScreenState: State<BrowseScreenState>,
    switchDisplayClick: () -> Unit,
    switchLibraryVisibilityClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
    onBackPress: () -> Unit,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>, BrowseScreenType) -> Unit,
    loadNextPage: () -> Unit,
    retryClick: () -> Unit,
    filterActions: FilterActions,
    changeScreenType: (BrowseScreenType) -> Unit,
    homeScreenTitleClick: (DisplayScreenType) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    var currentBottomSheet: BrowseBottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    val browseScreenType = browseScreenState.value.screenType

    // var sortType by remember { mutableStateOf(Sort.Entries) }

    var longClickedMangaId by remember { mutableStateOf<Long?>(null) }

    /**
     * Close the bottom sheet on back if its open
     */
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch { sheetState.hide() }
    }

    val sidePadding = remember(windowSizeClass.widthSizeClass) {
        when (browseScreenState.value.sideNavMode) {
            SideNavMode.NEVER -> PaddingValues()
            SideNavMode.ALWAYS -> Padding.sideAppBarPaddingValues
            SideNavMode.DEFAULT -> {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Expanded -> Padding.sideAppBarPaddingValues
                    else -> PaddingValues()
                }
            }
        }
    }

    // set the current sheet to null when bottom sheet is closed
    if (!sheetState.isVisible) {
        currentBottomSheet = null
    }

    val openSheet: (BrowseBottomSheetScreen) -> Unit = {
        scope.launch {
            currentBottomSheet = it
            sheetState.show()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp)) {
                currentBottomSheet?.let { currentSheet ->
                    BrowseBottomSheet(
                        currentScreen = currentSheet,
                        browseScreenState = browseScreenState,
                        addNewCategory = addNewCategory,
                        bottomPadding = Padding.bottomAppBarPaddingValues.calculateBottomPadding(),
                        closeSheet = { scope.launch { sheetState.hide() } },
                        filterActions = filterActions,
                    )
                }
            }
        },
    ) {
        NekoScaffold(
            modifier = Modifier.padding(sidePadding),
            title = stringResource(id = R.string.browse),
            onNavigationIconClicked = onBackPress,
            actions = {
                ListGridActionButton(
                    isList = browseScreenState.value.isList,
                    buttonClicked = switchDisplayClick,
                )
                ShowLibraryEntriesActionButton(
                    showEntries = browseScreenState.value.showLibraryEntries,
                    buttonClicked = switchLibraryVisibilityClick,
                )
            },
        ) { incomingContentPadding ->
            val contentPadding =
                PaddingValues(
                    bottom = Padding.bottomAppBarPaddingValues.calculateBottomPadding(),
                    top = incomingContentPadding.calculateTopPadding(),
                )

            val haptic = LocalHapticFeedback.current
            fun mangaLongClick(displayManga: DisplayManga) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!displayManga.inLibrary && browseScreenState.value.promptForCategories) {
                    scope.launch {
                        longClickedMangaId = displayManga.mangaId
                        openSheet(
                            BrowseBottomSheetScreen.CategoriesSheet(
                                setCategories = { selectedCategories ->
                                    scope.launch { sheetState.hide() }
                                    longClickedMangaId?.let {
                                        toggleFavorite(it, selectedCategories, browseScreenType)
                                    }
                                },
                            ),
                        )
                    }
                } else {
                    toggleFavorite(displayManga.mangaId, emptyList(), browseScreenType)
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding()),
            ) {

                ScreenTypeHeader(
                    screenType = browseScreenType,
                    isLoggedIn = browseScreenState.value.isLoggedIn,
                    screenTypeClick = { newScreenType: BrowseScreenType ->

                        val sameScreen = browseScreenType == newScreenType
                        val newIsFilterScreen = newScreenType == BrowseScreenType.Filter

                        if (sameScreen && !newIsFilterScreen) {
                            //do nothing
                        } else if ((sameScreen && newIsFilterScreen) || newIsFilterScreen) {
                            openSheet(
                                BrowseBottomSheetScreen.FilterSheet(),
                            )
                        } else {
                            changeScreenType(newScreenType)
                        }

                    },
                )

                if (browseScreenState.value.isLoading) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Loading(
                            Modifier
                                .zIndex(1f)
                                .padding(8.dp)
                                .padding(top = contentPadding.calculateTopPadding())
                                .align(Alignment.TopCenter),
                        )
                    }
                } else if (browseScreenState.value.error != null) {
                    EmptyScreen(
                        icon = Icons.Default.ErrorOutline,
                        iconSize = 176.dp,
                        message = browseScreenState.value.error,
                        actions = persistentListOf(Action(R.string.retry, retryClick)),
                        contentPadding = incomingContentPadding,
                    )
                } else {

                    when (browseScreenType) {
                        BrowseScreenType.Homepage -> BrowseHomePage(
                            browseHomePageManga = browseScreenState.value.homePageManga,
                            shouldOutlineCover = browseScreenState.value.outlineCovers,
                            titleClick = homeScreenTitleClick,
                            onClick = { id -> openManga(id) },
                            onLongClick = ::mangaLongClick,
                        )
                        BrowseScreenType.Follows -> {
                            if (browseScreenState.value.displayMangaHolder.allDisplayManga.isEmpty()) {
                                EmptyScreen(
                                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                                    iconSize = 176.dp,
                                    message = stringResource(id = R.string.no_results_found),
                                )
                            } else {
                                val groupedManga = remember(browseScreenState.value.displayMangaHolder) {
                                    browseScreenState.value.displayMangaHolder.filteredDisplayManga
                                        .groupBy { it.displayTextRes!! }
                                        .map { entry ->
                                            entry.key to entry.value.map { it.copy(displayTextRes = null) }.toImmutableList()
                                        }.toMap()
                                        .toImmutableMap()
                                }

                                if (browseScreenState.value.isList) {
                                    MangaListWithHeader(
                                        groupedManga = groupedManga,
                                        shouldOutlineCover = browseScreenState.value.outlineCovers,
                                        onClick = openManga,
                                        onLongClick = ::mangaLongClick,
                                    )
                                } else {
                                    MangaGridWithHeader(
                                        groupedManga = groupedManga,
                                        shouldOutlineCover = browseScreenState.value.outlineCovers,
                                        columns = numberOfColumns(rawValue = browseScreenState.value.rawColumnCount),
                                        isComfortable = browseScreenState.value.isComfortableGrid,
                                        onClick = openManga,
                                        onLongClick = ::mangaLongClick,
                                    )
                                }
                            }
                        }
                        BrowseScreenType.Filter -> {
                            if (browseScreenState.value.displayMangaHolder.allDisplayManga.isEmpty()) {
                                EmptyScreen(
                                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                                    iconSize = 176.dp,
                                    message = stringResource(id = R.string.no_results_found),
                                )
                            } else {
                                if (browseScreenState.value.isList) {
                                    MangaList(
                                        mangaList = browseScreenState.value.displayMangaHolder.filteredDisplayManga,
                                        shouldOutlineCover = browseScreenState.value.outlineCovers,
                                        contentPadding = contentPadding,
                                        onClick = openManga,
                                        onLongClick = ::mangaLongClick,
                                        loadNextItems = loadNextPage,
                                    )
                                } else {
                                    MangaGrid(
                                        mangaList = browseScreenState.value.displayMangaHolder.filteredDisplayManga,
                                        shouldOutlineCover = browseScreenState.value.outlineCovers,
                                        columns = numberOfColumns(rawValue = browseScreenState.value.rawColumnCount),
                                        isComfortable = browseScreenState.value.isComfortableGrid,
                                        contentPadding = contentPadding,
                                        onClick = openManga,
                                        onLongClick = ::mangaLongClick,
                                        loadNextItems = loadNextPage,
                                    )
                                }
                                if (browseScreenState.value.isLoading && browseScreenState.value.page != 1) {
                                    Box(Modifier.fillMaxSize()) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .padding(bottom = contentPadding.calculateBottomPadding())
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                        BrowseScreenType.None -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenTypeHeader(screenType: BrowseScreenType, isLoggedIn: Boolean, screenTypeClick: (BrowseScreenType) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        item {
            Gap(8.dp)
        }
        customChip(
            isSelected = screenType == BrowseScreenType.Homepage,
            onClick = { screenTypeClick(BrowseScreenType.Homepage) },
            label = R.string.home_page,
        )
        if (isLoggedIn) {
            customChip(
                isSelected = screenType == BrowseScreenType.Follows,
                onClick = { screenTypeClick(BrowseScreenType.Follows) },
                label = R.string.follows,
            )
        }

        customChip(
            isSelected = screenType == BrowseScreenType.Filter,
            onClick = { screenTypeClick(BrowseScreenType.Filter) },
            label = R.string.filter,
        )
    }
}

private fun LazyListScope.customChip(isSelected: Boolean, onClick: () -> Unit, @StringRes label: Int) {
    item(key = label) {
        FilterChip(
            selected = isSelected,
            onClick = onClick,
            shape = RoundedCornerShape(100),
            label = { Text(text = stringResource(id = label), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

