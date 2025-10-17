package org.nekomanga.presentation.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.util.system.openInWebView
import java.text.DateFormat
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.VerticalDivider
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.components.snackbar.snackbarHost
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.mangadetails.ChapterHeader
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheet
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsAppBarActions
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaScreen(
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    snackbar: SharedFlow<SnackbarState>,
    onRefresh: () -> Unit,
    onSearch: (String?) -> Unit,
    generatePalette: (Drawable) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    categoryActions: CategoryActions,
    dateFormat: DateFormat,
    trackActions: TrackActions,
    onSimilarClick: () -> Unit,
    coverActions: CoverActions,
    mergeActions: MergeActions,
    onShareClick: () -> Unit,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    chapterFilterActions: ChapterFilterActions,
    chapterActions: ChapterActions,
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val defaultColorState = defaultThemeColorState()

    val themeColorState =
        remember(screenState.themeBasedOffCovers, screenState.vibrantColor, isDark) {
            // 3. The logic inside here is now non-composable.
            if (screenState.themeBasedOffCovers && screenState.vibrantColor != null) {
                val color = getButtonThemeColor(Color(screenState.vibrantColor), isDark)
                val containerColor =
                    Color(ColorUtils.blendARGB(color.toArgb(), surfaceColor.toArgb(), .706f))
                val onContainerColor =
                    Color(ColorUtils.blendARGB(color.toArgb(), onSurfaceColor.toArgb(), .706f))

                ThemeColorState(
                    primaryColor = color,
                    rippleColor = color.copy(alpha = NekoColors.mediumAlphaLowContrast),
                    rippleConfiguration = nekoRippleConfiguration(color),
                    textSelectionColors = dynamicTextSelectionColor(color),
                    containerColor = containerColor,
                    onContainerColor = onContainerColor,
                    altContainerColor =
                        surfaceColor.surfaceColorAtElevationCustomColor(
                            surfaceColor,
                            color,
                            Size.small,
                        ),
                    onAltContainerColor = color,
                )
            } else {
                defaultColorState
            }
        }

    var currentBottomSheet by remember { mutableStateOf<DetailsBottomSheetScreen?>(null) }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.isVisible }
            .collect { isVisible ->
                if (!isVisible) {
                    currentBottomSheet = null
                }
            }
    }

    BackHandler(enabled = currentBottomSheet != null) { currentBottomSheet = null }

    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbar.collect { state ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result =
                    snackbarHostState.showSnackbar(
                        message = state.getFormattedMessage(context),
                        actionLabel = state.getFormattedActionLabel(context),
                        duration = state.snackbarDuration,
                        withDismissAction = true,
                    )
                if (result == SnackbarResult.ActionPerformed) state.action?.invoke()
            }
        }
    }

    fun openSheet(sheet: DetailsBottomSheetScreen) {
        scope.launch {
            currentBottomSheet = sheet
            sheetState.show()
        }
    }

    if (currentBottomSheet != null) {

        ModalBottomSheet(
            onDismissRequest = { currentBottomSheet = null },
            sheetState = sheetState,
            // shape = RoundedCornerShape(topStart = Shapes.sheetRadius, topEnd =
            // Shapes.sheetRadius),
            content = {
                DetailsBottomSheet(
                    currentScreen = currentBottomSheet!!,
                    themeColorState = themeColorState,
                    mangaDetailScreenState = screenState,
                    addNewCategory = categoryActions.addNew,
                    dateFormat = dateFormat,
                    trackActions = trackActions,
                    coverActions = coverActions,
                    mergeActions = mergeActions,
                    chapterFilterActions = chapterFilterActions,
                    openInWebView = { url, title -> context.openInWebView(url, title) },
                    onNavigate = { newSheet ->
                        scope.launch {
                            if (newSheet == null) {
                                scope.launch { sheetState.hide() }
                            } else {
                                currentBottomSheet = newSheet
                            }
                        }
                    },
                )
            },
        )
    }
    NekoScaffold(
        type = NekoScaffoldType.Search,
        onNavigationIconClicked = onBackPressed,
        themeColorState = themeColorState,
        searchPlaceHolder = stringResource(id = R.string.search_chapters),
        incognitoMode = screenState.incognitoMode,
        onSearch = onSearch,
        snackBarHost = snackbarHost(snackbarHostState, themeColorState.primaryColor),
        actions = {
            MangaDetailsAppBarActions(
                chapterActions = chapterActions,
                themeColorState = themeColorState,
                chapters = screenState.activeChapters,
            )
        },
    ) { incomingPaddingValues ->
        PullRefresh(
            isRefreshing = screenState.isRefreshing,
            onRefresh = onRefresh,
            trackColor = themeColorState.primaryColor,
        ) {
            val isTablet =
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                    !screenState.forcePortrait

            val onToggleFavoriteAction =
                remember(
                    screenState.inLibrary,
                    screenState.allCategories,
                    screenState.hasDefaultCategory,
                ) {
                    {
                        if (!screenState.inLibrary && screenState.allCategories.isNotEmpty()) {
                            if (screenState.hasDefaultCategory) {
                                onToggleFavorite(true)
                            } else {
                                openSheet(
                                    DetailsBottomSheetScreen.CategoriesSheet(
                                        addingToLibrary = true,
                                        setCategories = categoryActions.set,
                                        addToLibraryClick = { onToggleFavorite(false) },
                                    )
                                )
                            }
                        } else {
                            onToggleFavorite(false)
                        }
                    }
                }

            if (isTablet) {
                SideBySideLayout(
                    incomingPadding = incomingPaddingValues,
                    screenState = screenState,
                    windowSizeClass = windowSizeClass,
                    themeColorState = themeColorState,
                    chapterActions = chapterActions,
                    informationActions = informationActions,
                    descriptionActions = descriptionActions,
                    onSimilarClick = onSimilarClick,
                    onShareClick = onShareClick,
                    onToggleFavorite = onToggleFavoriteAction,
                    generatePalette = generatePalette,
                    onOpenSheet = ::openSheet,
                    categoryActions = categoryActions,
                )
            } else {
                VerticalLayout(
                    incomingPadding = incomingPaddingValues,
                    screenState = screenState,
                    windowSizeClass = windowSizeClass,
                    themeColorState = themeColorState,
                    chapterActions = chapterActions,
                    informationActions = informationActions,
                    descriptionActions = descriptionActions,
                    onSimilarClick = onSimilarClick,
                    onShareClick = onShareClick,
                    onToggleFavorite = onToggleFavoriteAction,
                    generatePalette = generatePalette,
                    onOpenSheet = ::openSheet,
                    categoryActions = categoryActions,
                )
            }

            if (screenState.removedChapters.isNotEmpty()) {
                RemovedChaptersDialog(
                    themeColorState = themeColorState,
                    chapters = screenState.removedChapters,
                    onConfirm = {
                        chapterActions.delete(screenState.removedChapters)
                        chapterActions.clearRemoved()
                    },
                    onDismiss = { chapterActions.clearRemoved() },
                )
            }
        }
    }
}

private fun LazyListScope.chapterList(
    chapters: PersistentList<ChapterItem>,
    screenState: MangaConstants.MangaDetailScreenState,
    themeColorState: ThemeColorState,
    chapterActions: ChapterActions,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    item(key = "chapter_header") {
        ChapterHeader(
            themeColor = themeColorState,
            numberOfChapters = chapters.size,
            filterText = screenState.chapterFilterText,
            onClick = { onOpenSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
        )
    }

    itemsIndexed(items = chapters, key = { _, chapter -> chapter.chapter.id }) { index, chapterItem
        ->
        ChapterRow(
            themeColor = themeColorState,
            chapterItem = chapterItem,
            shouldHideChapterTitles =
                screenState.chapterFilter.hideChapterTitles == ToggleableState.On,
            onClick = { chapterActions.open(chapterItem) },
            onBookmark = {
                chapterActions.mark(
                    listOf(chapterItem),
                    if (chapterItem.chapter.bookmark) ChapterMarkActions.UnBookmark(true)
                    else ChapterMarkActions.Bookmark(true),
                )
            },
            onRead = {
                chapterActions.mark(
                    listOf(chapterItem),
                    if (chapterItem.chapter.read) ChapterMarkActions.Unread(true)
                    else ChapterMarkActions.Read(true),
                )
            },
            onWebView = { chapterActions.openInBrowser(chapterItem) },
            onComment = { chapterActions.openComment(chapterItem.chapter.mangaDexChapterId) },
            onDownload = { downloadAction ->
                chapterActions.download(listOf(chapterItem), downloadAction)
            },
            markPrevious = { read ->
                val chaptersToMark = screenState.activeChapters.subList(0, index)
                val altChapters =
                    if (index == screenState.activeChapters.lastIndex) emptyList()
                    else
                        screenState.activeChapters.slice(
                            index + 1..screenState.activeChapters.lastIndex
                        )
                val action =
                    if (read) ChapterMarkActions.PreviousRead(true, altChapters)
                    else ChapterMarkActions.PreviousUnread(true, altChapters)
                chapterActions.mark(chaptersToMark, action)
            },
            blockScanlator = { blockType, blocked ->
                chapterActions.blockScanlator(blockType, blocked)
            },
        )
    }
}

@Composable
private fun VerticalLayout(
    incomingPadding: PaddingValues,
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    themeColorState: ThemeColorState,
    categoryActions: CategoryActions,
    chapterActions: ChapterActions,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    onSimilarClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    generatePalette: (Drawable) -> Unit,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    val contentPadding =
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            MangaDetailsHeader(
                mangaDetailScreenState = screenState,
                windowSizeClass = windowSizeClass,
                isLoggedIntoTrackers = screenState.loggedInTrackService.isNotEmpty(),
                themeColorState = themeColorState,
                generatePalette = generatePalette,
                toggleFavorite = onToggleFavorite,
                onCategoriesClick = {
                    onOpenSheet(
                        DetailsBottomSheetScreen.CategoriesSheet(
                            setCategories = categoryActions.set
                        )
                    )
                },
                onTrackingClick = { onOpenSheet(DetailsBottomSheetScreen.TrackingSheet) },
                onArtworkClick = { onOpenSheet(DetailsBottomSheetScreen.ArtworkSheet) },
                onSimilarClick = onSimilarClick,
                onMergeClick = { onOpenSheet(DetailsBottomSheetScreen.MergeSheet) },
                onLinksClick = { onOpenSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
                onShareClick = onShareClick,
                descriptionActions = descriptionActions,
                informationActions = informationActions,
                onQuickReadClick = { chapterActions.openNext() },
            )
        }
        chapterList(
            chapters =
                if (screenState.isSearching) screenState.searchChapters
                else screenState.activeChapters,
            screenState = screenState,
            themeColorState = themeColorState,
            chapterActions = chapterActions,
            onOpenSheet = onOpenSheet,
        )
    }
}

@Composable
private fun SideBySideLayout(
    incomingPadding: PaddingValues,
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    themeColorState: ThemeColorState,
    categoryActions: CategoryActions,
    chapterActions: ChapterActions,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    onSimilarClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    generatePalette: (Drawable) -> Unit,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    val detailsContentPadding =
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
    val chapterContentPadding =
        PaddingValues(
            bottom = detailsContentPadding.calculateBottomPadding(),
            top = incomingPadding.calculateTopPadding(),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(.5f).fillMaxHeight(),
            contentPadding = detailsContentPadding,
        ) {
            item(key = "header") {
                MangaDetailsHeader(
                    mangaDetailScreenState = screenState,
                    windowSizeClass = windowSizeClass,
                    isLoggedIntoTrackers = screenState.loggedInTrackService.isNotEmpty(),
                    themeColorState = themeColorState,
                    generatePalette = generatePalette,
                    toggleFavorite = onToggleFavorite,
                    onCategoriesClick = {
                        onOpenSheet(
                            DetailsBottomSheetScreen.CategoriesSheet(
                                setCategories = categoryActions.set
                            )
                        )
                    },
                    onTrackingClick = { onOpenSheet(DetailsBottomSheetScreen.TrackingSheet) },
                    onArtworkClick = { onOpenSheet(DetailsBottomSheetScreen.ArtworkSheet) },
                    onSimilarClick = onSimilarClick,
                    onMergeClick = { onOpenSheet(DetailsBottomSheetScreen.MergeSheet) },
                    onLinksClick = { onOpenSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
                    onShareClick = onShareClick,
                    descriptionActions = descriptionActions,
                    informationActions = informationActions,
                    onQuickReadClick = { chapterActions.openNext() },
                )
            }
        }

        VerticalDivider(Modifier.align(Alignment.TopCenter))

        LazyColumn(
            modifier =
                Modifier.align(Alignment.TopEnd).fillMaxWidth(.5f).fillMaxHeight().clipToBounds(),
            contentPadding = chapterContentPadding,
        ) {
            chapterList(
                chapters =
                    if (screenState.isSearching) screenState.searchChapters
                    else screenState.activeChapters,
                screenState = screenState,
                themeColorState = themeColorState,
                chapterActions = chapterActions,
                onOpenSheet = onOpenSheet,
            )
        }
    }
}

private fun getButtonThemeColor(buttonColor: Color, isNightMode: Boolean): Color {
    val color1 = buttonColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(color1).toFloat()

    val color2 =
        when (isNightMode) {
            true -> Color.White.toArgb()
            false -> Color.Black.toArgb()
        }

    val ratio =
        when (isNightMode) {
            true -> (-(luminance - 1)) * .33f
            false -> luminance * .3f
        }

    return when ((isNightMode && luminance <= 0.6) || (!isNightMode && luminance > 0.4)) {
        true -> Color(ColorUtils.blendARGB(color1, color2, ratio))
        false -> buttonColor
    }
}

@Composable
private fun ChapterRow(
    themeColorState: ThemeColorState,
    mangaDetailScreenState: State<MangaConstants.MangaDetailScreenState>,
    chapterActions: ChapterActions,
    chapterItem: ChapterItem,
    index: Int,
) {
    ChapterRow(
        themeColor = themeColorState,
        chapterItem = chapterItem,
        shouldHideChapterTitles =
            mangaDetailScreenState.value.chapterFilter.hideChapterTitles == ToggleableState.On,
        onClick = { chapterActions.open(chapterItem) },
        onBookmark = {
            chapterActions.mark(
                listOf(chapterItem),
                if (chapterItem.chapter.bookmark) ChapterMarkActions.UnBookmark(true)
                else ChapterMarkActions.Bookmark(true),
            )
        },
        onRead = {
            chapterActions.mark(
                listOf(chapterItem),
                when (chapterItem.chapter.read) {
                    true -> ChapterMarkActions.Unread(true)
                    false -> ChapterMarkActions.Read(true)
                },
            )
        },
        onWebView = { chapterActions.openInBrowser(chapterItem) },
        onComment = { chapterActions.openComment(chapterItem.chapter.mangaDexChapterId) },
        onDownload = { downloadAction ->
            chapterActions.download(listOf(chapterItem), downloadAction)
        },
        markPrevious = { read ->
            val chaptersToMark = mangaDetailScreenState.value.activeChapters.subList(0, index)
            val lastIndex = mangaDetailScreenState.value.activeChapters.lastIndex
            val altChapters =
                if (index == lastIndex) {
                    emptyList()
                } else {
                    mangaDetailScreenState.value.activeChapters.slice(
                        IntRange(index + 1, lastIndex)
                    )
                }
            val action =
                when (read) {
                    true -> ChapterMarkActions.PreviousRead(true, altChapters)
                    false -> ChapterMarkActions.PreviousUnread(true, altChapters)
                }
            chapterActions.mark(chaptersToMark, action)
        },
        blockScanlator = { blockType, blocked -> chapterActions.blockScanlator(blockType, blocked) },
    )
}
