package org.nekomanga.presentation.screens

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.core.graphics.ColorUtils
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MangaScreenGeneralState
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.util.system.openInWebView
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.components.snackbar.snackbarHost
import org.nekomanga.presentation.screens.mangadetails.ChapterHeader
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheet
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.screens.mangadetails.OverflowOptions
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaScreen(
    generalState: State<MangaScreenGeneralState>,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    trackMergeState: State<MangaConstants.MangaScreenTrackMergeState>,
    windowSizeClass: WindowSizeClass,
    snackbar: SharedFlow<SnackbarState>,
    isRefreshing: State<Boolean>,
    isSearching: State<Boolean>,
    onRefresh: () -> Unit,
    onSearch: (String?) -> Unit,
    generatePalette: (Drawable) -> Unit = {},
    toggleFavorite: (Boolean) -> Unit,
    categoryActions: CategoryActions,
    dateFormat: DateFormat,
    trackActions: TrackActions,
    similarClick: () -> Unit = {},
    coverActions: CoverActions,
    mergeActions: MergeActions,
    shareClick: () -> Unit,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    chapterFilterActions: ChapterFilterActions,
    chapterActions: ChapterActions,
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        )
    /** CLose the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
                when (result) {
                    SnackbarResult.ActionPerformed -> state.action?.invoke()
                    SnackbarResult.Dismissed -> state.dismissAction?.invoke()
                }
            }
        }
    }

    var currentBottomSheet: DetailsBottomSheetScreen? by remember { mutableStateOf(null) }

    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface

    val defaultThemeColorState = defaultThemeColorState()

    var themeColorState by remember { mutableStateOf(defaultThemeColorState) }

    if (generalState.value.themeBasedOffCovers && generalState.value.vibrantColor != null) {
        val color = getButtonThemeColor(Color(generalState.value.vibrantColor!!), isDarkTheme)
        themeColorState =
            ThemeColorState(
                buttonColor = color,
                rippleConfiguration = nekoRippleConfiguration(color),
                textSelectionColors = dynamicTextSelectionColor(color),
                altContainerColor =
                    Color(ColorUtils.blendARGB(color.toArgb(), surfaceColor.toArgb(), .706f)),
            )
    }

    // set the current sheet to null when bottom sheet is closed
    LaunchedEffect(key1 = sheetState.isVisible) {
        if (!sheetState.isVisible) {
            currentBottomSheet = null
        }
    }

    fun openSheet(sheet: DetailsBottomSheetScreen) {
        scope.launch {
            currentBottomSheet = sheet
            sheetState.show()
        }
    }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = Shapes.sheetRadius, topEnd = Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                currentBottomSheet?.let { currentSheet ->
                    DetailsBottomSheet(
                        currentScreen = currentSheet,
                        themeColorState = themeColorState,
                        generalState = generalState,
                        mangaState = mangaState,
                        trackMergeState = trackMergeState,
                        addNewCategory = categoryActions.addNew,
                        dateFormat = dateFormat,
                        openSheet = ::openSheet,
                        trackActions = trackActions,
                        coverActions = coverActions,
                        mergeActions = mergeActions,
                        chapterFilterActions = chapterFilterActions,
                        openInWebView = { url, title ->
                            (context as? Activity)?.openInWebView(url, title)
                        },
                        closeSheet = { scope.launch { sheetState.hide() } },
                    )
                }
            }
        },
    ) {
        NekoScaffold(
            type = NekoScaffoldType.Search,
            themeColorState = themeColorState,
            onNavigationIconClicked = onBackPressed,
            onSearch = onSearch,
            searchPlaceHolder = stringResource(id = R.string.search_chapters),
            snackBarHost = snackbarHost(snackbarHostState, themeColorState.buttonColor),
            actions = {
                OverflowOptions(
                    chapterActions = chapterActions,
                    chaptersProvider = { generalState.value.activeChapters },
                )
            },
        ) { incomingPaddingValues ->
            PullRefresh(
                refreshing = isRefreshing.value,
                onRefresh = onRefresh,
                indicatorOffset = incomingPaddingValues.calculateTopPadding(),
                backgroundColor = themeColorState.buttonColor,
                contentColor = MaterialTheme.colorScheme.surface,
            ) {
                val mangaDetailContentPadding =
                    PaddingValues(
                        bottom =
                            WindowInsets.navigationBars
                                .only(WindowInsetsSides.Bottom)
                                .asPaddingValues()
                                .calculateBottomPadding()
                    )

                val chapterContentPadding =
                    PaddingValues(
                        bottom =
                            WindowInsets.navigationBars
                                .only(WindowInsetsSides.Bottom)
                                .asPaddingValues()
                                .calculateBottomPadding(),
                        top = incomingPaddingValues.calculateTopPadding(),
                    )

                CompositionLocalProvider(
                    LocalRippleConfiguration provides themeColorState.rippleConfiguration,
                    LocalTextSelectionColors provides themeColorState.textSelectionColors,
                ) {
                    if (
                        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                            !generalState.value.forcePortrait
                    ) {
                        SideBySideLayout(
                            mangaDetailContentPadding = mangaDetailContentPadding,
                            chapterContentPadding = chapterContentPadding,
                            details = {
                                Details(
                                    themeColorState = themeColorState,
                                    mangaState = mangaState,
                                    generalState = generalState,
                                    isSearching = isSearching,
                                    trackMergeState = trackMergeState,
                                    windowSizeClass = windowSizeClass,
                                    chapterActions = chapterActions,
                                    categoryActions = categoryActions,
                                    descriptionActions = descriptionActions,
                                    informationActions = informationActions,
                                    generatePalette = generatePalette,
                                    openSheet = { sheet -> openSheet(sheet) },
                                    similarClick = similarClick,
                                    shareClick = shareClick,
                                    toggleFavorite = toggleFavorite,
                                )
                            },
                            chapterHeader = {
                                ChapterHeader(
                                    themeColorState = themeColorState,
                                    isSearching = isSearching,
                                    generalState = generalState,
                                    openSheet = { sheet -> openSheet(sheet) },
                                )
                            },
                            chaptersProvider = {
                                when (isSearching.value) {
                                    true -> generalState.value.searchChapters
                                    false -> generalState.value.activeChapters
                                }
                            },
                            chapterRow = { index, chapterItem ->
                                ChapterRow(
                                    themeColorState = themeColorState,
                                    generalState = generalState,
                                    chapterActions = chapterActions,
                                    scope = scope,
                                    chapterItem = chapterItem,
                                    index = index,
                                )
                            },
                        )
                    } else {
                        VerticalLayout(
                            contentPadding = mangaDetailContentPadding,
                            details = {
                                Details(
                                    themeColorState = themeColorState,
                                    mangaState = mangaState,
                                    generalState = generalState,
                                    isSearching = isSearching,
                                    trackMergeState = trackMergeState,
                                    windowSizeClass = windowSizeClass,
                                    chapterActions = chapterActions,
                                    categoryActions = categoryActions,
                                    descriptionActions = descriptionActions,
                                    informationActions = informationActions,
                                    generatePalette = generatePalette,
                                    openSheet = { sheet -> openSheet(sheet) },
                                    similarClick = similarClick,
                                    shareClick = shareClick,
                                    toggleFavorite = toggleFavorite,
                                )
                            },
                            chapterHeader = {
                                ChapterHeader(
                                    themeColorState = themeColorState,
                                    isSearching = isSearching,
                                    generalState = generalState,
                                    openSheet = { sheet -> openSheet(sheet) },
                                )
                            },
                            chaptersProvider = {
                                when (isSearching.value) {
                                    true -> generalState.value.searchChapters
                                    false -> generalState.value.activeChapters
                                }
                            },
                            chapterRow = { index, chapterItem ->
                                ChapterRow(
                                    themeColorState = themeColorState,
                                    generalState = generalState,
                                    chapterActions = chapterActions,
                                    scope = scope,
                                    chapterItem = chapterItem,
                                    index = index,
                                )
                            },
                        )
                    }

                    if (generalState.value.removedChapters.isNotEmpty()) {
                        RemovedChaptersDialog(
                            themeColorState = themeColorState,
                            chapters = generalState.value.removedChapters,
                            onConfirm = {
                                chapterActions.delete(generalState.value.removedChapters)
                                chapterActions.clearRemoved
                            },
                            onDismiss = chapterActions.clearRemoved,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalLayout(
    contentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chaptersProvider: () -> ImmutableList<ChapterItem>,
    chapterRow: @Composable (Int, ChapterItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = 1) { details() }

        item(key = 2) { chapterHeader() }

        itemsIndexed(items = chaptersProvider(), key = { _, chapter -> chapter.chapter.id }) {
            index,
            chapter ->
            chapterRow(index, chapter)
        }
    }
}

@Composable
private fun SideBySideLayout(
    mangaDetailContentPadding: PaddingValues,
    chapterContentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chaptersProvider: () -> ImmutableList<ChapterItem>,
    chapterRow: @Composable (Int, ChapterItem) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(.5f).fillMaxHeight(),
            contentPadding = mangaDetailContentPadding,
        ) {
            item { details() }
        }

        VerticalDivider(Modifier.align(Alignment.TopCenter))

        LazyColumn(
            modifier =
                Modifier.align(Alignment.TopEnd).fillMaxWidth(.5f).fillMaxHeight().clipToBounds(),
            contentPadding = chapterContentPadding,
        ) {
            item { chapterHeader() }
            itemsIndexed(items = chaptersProvider(), key = { _, chapter -> chapter.chapter.id }) {
                index,
                chapter ->
                chapterRow(index, chapter)
            }
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
    generalState: State<MangaScreenGeneralState>,
    chapterActions: ChapterActions,
    scope: CoroutineScope,
    chapterItem: ChapterItem,
    index: Int,
) {
    ChapterRow(
        themeColor = themeColorState,
        title = chapterItem.chapter.name,
        scanlator = chapterItem.chapter.scanlator,
        uploader = chapterItem.chapter.uploader,
        language = chapterItem.chapter.language,
        chapterNumber = chapterItem.chapter.chapterNumber.toDouble(),
        dateUploaded = chapterItem.chapter.dateUpload,
        lastPageRead = chapterItem.chapter.lastPageRead,
        pagesLeft = chapterItem.chapter.pagesLeft,
        read = chapterItem.chapter.read,
        bookmark = chapterItem.chapter.bookmark,
        isMerged = chapterItem.chapter.isMergedChapter(),
        isUnavailable = chapterItem.chapter.isUnavailable,
        downloadStateProvider = { chapterItem.downloadState },
        downloadProgressProvider = { chapterItem.downloadProgress },
        shouldHideChapterTitles =
            generalState.value.chapterFilter.hideChapterTitles == ToggleableState.On,
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
            val chaptersToMark = generalState.value.activeChapters.subList(0, index)
            val lastIndex = generalState.value.activeChapters.lastIndex
            val altChapters =
                if (index == lastIndex) {
                    emptyList()
                } else {
                    generalState.value.activeChapters.slice(IntRange(index + 1, lastIndex))
                }
            val action =
                when (read) {
                    true -> ChapterMarkActions.PreviousRead(true, altChapters)
                    false -> ChapterMarkActions.PreviousUnread(true, altChapters)
                }
            chapterActions.mark(chaptersToMark, action)
        },
        blockScanlator = { scanlator -> chapterActions.blockScanlator(scanlator) },
    )
}

@Composable
fun ChapterHeader(
    themeColorState: ThemeColorState,
    isSearching: State<Boolean>,
    generalState: State<MangaScreenGeneralState>,
    openSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    ChapterHeader(
        themeColor = themeColorState,
        numberOfChaptersProvider = {
            when (isSearching.value) {
                true -> generalState.value.searchChapters.size
                false -> generalState.value.activeChapters.size
            }
        },
        filterTextProvider = { generalState.value.chapterFilterText },
        onClick = { openSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
    )
}

@Composable
fun Details(
    themeColorState: ThemeColorState,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    generalState: State<MangaScreenGeneralState>,
    isSearching: State<Boolean>,
    trackMergeState: State<MangaConstants.MangaScreenTrackMergeState>,
    windowSizeClass: WindowSizeClass,
    chapterActions: ChapterActions,
    categoryActions: CategoryActions,
    descriptionActions: DescriptionActions,
    informationActions: InformationActions,
    generatePalette: (Drawable) -> Unit,
    openSheet: (DetailsBottomSheetScreen) -> Unit,
    similarClick: () -> Unit,
    shareClick: () -> Unit,
    toggleFavorite: (Boolean) -> Unit,
) {
    MangaDetailsHeader(
        mangaState = mangaState,
        generalState = generalState,
        isSearching = isSearching.value,
        windowSizeClass = windowSizeClass,
        informationActions = informationActions,
        themeColorState = themeColorState,
        generatePalette = generatePalette,
        isLoggedIntoTrackersProvider = { trackMergeState.value.loggedInTrackService.isNotEmpty() },
        toggleFavorite = {
            if (!mangaState.value.inLibrary && generalState.value.allCategories.isNotEmpty()) {
                if (generalState.value.hasDefaultCategory) {
                    toggleFavorite(true)
                } else {
                    openSheet(
                        DetailsBottomSheetScreen.CategoriesSheet(
                            addingToLibrary = true,
                            setCategories = categoryActions.set,
                            addToLibraryClick = { toggleFavorite(false) },
                        )
                    )
                }
            } else {
                toggleFavorite(false)
            }
        },
        moveCategories = {
            openSheet(
                DetailsBottomSheetScreen.CategoriesSheet(
                    addingToLibrary = false,
                    setCategories = categoryActions.set,
                )
            )
        },
        trackingClick = { openSheet(DetailsBottomSheetScreen.TrackingSheet) },
        similarClick = similarClick,
        artworkClick = { openSheet(DetailsBottomSheetScreen.ArtworkSheet) },
        mergeClick = { openSheet(DetailsBottomSheetScreen.MergeSheet) },
        linksClick = { openSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
        shareClick = shareClick,
        descriptionActions = descriptionActions,
        quickReadClick = { chapterActions.openNext() },
    )
}

class ThemeColorState(
    buttonColor: Color,
    rippleConfiguration: RippleConfiguration,
    textSelectionColors: TextSelectionColors,
    altContainerColor: Color,
) {
    var buttonColor by mutableStateOf(buttonColor)
    var rippleConfiguration by mutableStateOf(rippleConfiguration)
    var textSelectionColors by mutableStateOf(textSelectionColors)
    var altContainerColor by mutableStateOf(altContainerColor)
}

@Composable
fun defaultThemeColorState(): ThemeColorState {
    return ThemeColorState(
        buttonColor = MaterialTheme.colorScheme.secondary,
        rippleConfiguration = nekoRippleConfiguration(MaterialTheme.colorScheme.primary),
        textSelectionColors = LocalTextSelectionColors.current,
        altContainerColor =
            Color(
                ColorUtils.blendARGB(
                    MaterialTheme.colorScheme.secondary.toArgb(),
                    MaterialTheme.colorScheme.surface.toArgb(),
                    .706f,
                )
            ),
    )
}
