package org.nekomanga.presentation.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.crazylegend.activity.asActivity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MangaScreenGeneralState
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInWebView
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.PrimaryColorRippleTheme
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.snackbar.snackbarHost
import org.nekomanga.presentation.screens.mangadetails.ChapterHeader
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheet
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.screens.mangadetails.OverflowOptions
import org.nekomanga.presentation.theme.Shapes

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaScreen(
    generalState: State<MangaScreenGeneralState>,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    trackMergeState: State<MangaConstants.MangaScreenTrackMergeState>,
    windowSizeClass: WindowSizeClass,
    snackbar: SharedFlow<SnackbarState>,
    isRefreshing: State<Boolean>,
    onRefresh: () -> Unit,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (Context, String) -> Unit,
    creatorLongClick: (Context, String) -> Unit,
    toggleFavorite: (Boolean) -> Unit,
    categoryActions: CategoryActions,
    dateFormat: DateFormat,
    trackActions: TrackActions,
    similarClick: () -> Unit = {},
    coverActions: CoverActions,
    mergeActions: MergeActions,
    shareClick: (Context) -> Unit,
    descriptionActions: DescriptionActions,
    chapterFilterActions: ChapterFilterActions,
    chapterActions: ChapterActions,
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    /**
     * CLose the bottom sheet on back if its open
     */
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch { sheetState.hide() }
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbar.collect { state ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
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

    var currentBottomSheet: DetailsBottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface

    val defaultThemeColorState = defaultThemeColorState()

    var themeColorState by remember {
        mutableStateOf(
            defaultThemeColorState,
        )
    }

    if (generalState.value.themeBasedOffCovers && generalState.value.vibrantColor != null) {
        val color = getButtonThemeColor(Color(generalState.value.vibrantColor!!), isDarkTheme)
        themeColorState = ThemeColorState(
            buttonColor = color,
            rippleTheme = DynamicRippleTheme(color),
            textSelectionColors = dynamicTextSelectionColor(color),
            altContainerColor = Color(ColorUtils.blendARGB(color.toArgb(), surfaceColor.toArgb(), .706f)),
        )
    }

    // set the current sheet to null when bottom sheet is closed
    if (sheetState.isVisible.not()) {
        currentBottomSheet = null
    }

    val openSheet: (DetailsBottomSheetScreen) -> Unit = {
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
                    DetailsBottomSheet(
                        currentScreen = currentSheet,
                        themeColorState = themeColorState,
                        generalState = generalState,
                        mangaState = mangaState,
                        trackMergeState = trackMergeState,
                        addNewCategory = categoryActions.addNew,
                        dateFormat = dateFormat,
                        openSheet = openSheet,
                        trackActions = trackActions,
                        coverActions = coverActions,
                        mergeActions = mergeActions,
                        chapterFilterActions = chapterFilterActions,
                        openInWebView = { url, title -> context.asActivity().openInWebView(url, title) },
                    ) { scope.launch { sheetState.hide() } }
                }
            }
        },
    ) {
        NekoScaffold(
            title = "",
            themeColorState = themeColorState,
            onNavigationIconClicked = onBackPressed,
            snackBarHost = snackbarHost(snackbarHostState, themeColorState.buttonColor),
            actions = {
                OverflowOptions(chapterActions = chapterActions, chaptersProvider = { generalState.value.activeChapters })
            },
        ) { incomingPaddingValues ->
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = isRefreshing.value),
                modifier = Modifier.fillMaxSize(),
                onRefresh = onRefresh,
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshingOffset = incomingPaddingValues.calculateTopPadding(),
                        refreshTriggerDistance = trigger,
                        backgroundColor = themeColorState.buttonColor,
                        contentColor = MaterialTheme.colorScheme.surface,

                    )
                },
            ) {
                val mangaDetailContentPadding =
                    PaddingValues(
                        bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                            .asPaddingValues().calculateBottomPadding(),
                    )

                val chapterContentPadding =
                    PaddingValues(
                        bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                            .asPaddingValues().calculateBottomPadding(),
                        top = incomingPaddingValues.calculateTopPadding(),
                    )

                fun details() = @Composable {
                    MangaDetailsHeader(
                        mangaState = mangaState,
                        generalState = generalState,
                        windowSizeClass = windowSizeClass,
                        titleLongClick = { title: String -> titleLongClick(context, title) },
                        creatorLongClick = { creator: String -> creatorLongClick(context, creator) },
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
                                        ),
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
                                ),
                            )
                        },
                        trackingClick = { openSheet(DetailsBottomSheetScreen.TrackingSheet) },
                        similarClick = similarClick,
                        artworkClick = { openSheet(DetailsBottomSheetScreen.ArtworkSheet) },
                        mergeClick = { openSheet(DetailsBottomSheetScreen.MergeSheet) },
                        linksClick = { openSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
                        shareClick = { shareClick(context) },
                        descriptionActions = descriptionActions,
                        quickReadClick = { chapterActions.openNext(context) },
                    )
                }

                fun chapterHeader() = @Composable {
                    ChapterHeader(
                        themeColor = themeColorState,
                        numberOfChaptersProvider = { generalState.value.activeChapters.size },
                        filterTextProvider = { generalState.value.chapterFilterText },
                        onClick = { openSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
                    )
                }

                fun chapterRow() = @Composable { index: Int, chapterItem: ChapterItem ->
                    ChapterRow(
                        themeColor = themeColorState,
                        title = chapterItem.chapter.name,
                        scanlator = chapterItem.chapter.scanlator,
                        language = chapterItem.chapter.language,
                        chapterNumber = chapterItem.chapter.chapterNumber.toDouble(),
                        dateUploaded = chapterItem.chapter.dateUpload,
                        lastPageRead = chapterItem.chapter.lastPageRead,
                        pagesLeft = chapterItem.chapter.pagesLeft,
                        read = chapterItem.chapter.read,
                        bookmark = chapterItem.chapter.bookmark,
                        downloadStateProvider = { chapterItem.downloadState },
                        downloadProgressProvider = { chapterItem.downloadProgress },
                        shouldHideChapterTitles = generalState.value.hideChapterTitles,
                        onClick = { chapterActions.open(context, chapterItem) },
                        onBookmark = {
                            chapterActions.mark(
                                listOf(chapterItem),
                                if (chapterItem.chapter.bookmark) MangaConstants.MarkAction.UnBookmark(true) else MangaConstants.MarkAction.Bookmark(true),
                            )
                        },
                        onRead = {
                            chapterActions.mark(
                                listOf(chapterItem),
                                when (chapterItem.chapter.read) {
                                    true -> MangaConstants.MarkAction.Unread(true)
                                    false -> MangaConstants.MarkAction.Read(true)
                                },
                            )
                        },
                        onWebView = { context.asActivity().openInBrowser(chapterItem.chapter.fullUrl()) },
                        onDownload = { downloadAction ->
                            chapterActions.download(listOf(chapterItem), downloadAction)
                        },
                        markPrevious = { read ->

                            val chaptersToMark = generalState.value.activeChapters.subList(0, index)
                            val lastIndex = generalState.value.activeChapters.lastIndex
                            val altChapters = if (index == lastIndex) {
                                emptyList()
                            } else {
                                generalState.value.activeChapters.slice(IntRange(index + 1, lastIndex))
                            }
                            val action = when (read) {
                                true -> MangaConstants.MarkAction.PreviousRead(true, altChapters)
                                false -> MangaConstants.MarkAction.PreviousUnread(true, altChapters)
                            }
                            chapterActions.mark(chaptersToMark, action)
                        },
                        blockScanlator = { scanlator -> chapterActions.blockScanlator(scanlator) },
                    )
                }

                CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
                    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                        ExpandedLayout(
                            mangaDetailContentPadding = mangaDetailContentPadding,
                            chapterContentPadding = chapterContentPadding,
                            details = details(),
                            chapterHeader = chapterHeader(),
                            chaptersProvider = { generalState.value.activeChapters },
                            chapterRow = chapterRow(),
                        )
                    } else {
                        NormalLayout(
                            contentPadding = mangaDetailContentPadding,
                            details = details(),
                            chapterHeader = chapterHeader(),
                            chaptersProvider = { generalState.value.activeChapters },
                            chapterRow = chapterRow(),
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
private fun NormalLayout(
    contentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chaptersProvider: () -> ImmutableList<ChapterItem>,
    chapterRow: @Composable (Int, ChapterItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            details()
        }

        item {
            chapterHeader()
        }

        itemsIndexed(items = chaptersProvider(), key = { _, chapter -> chapter.chapter.id }) { index, chapter ->
            chapterRow(index, chapter)
        }
    }
}

@Composable
private fun ExpandedLayout(
    mangaDetailContentPadding: PaddingValues,
    chapterContentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chaptersProvider: () -> ImmutableList<ChapterItem>,
    chapterRow: @Composable (Int, ChapterItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(.5f)
                .fillMaxHeight(),
            contentPadding = mangaDetailContentPadding,
        ) {
            item { details() }
        }

        VerticalDivider(Modifier.align(Alignment.TopCenter))

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(.5f)
                .fillMaxHeight()
                .clipToBounds(),
            contentPadding = chapterContentPadding,
        ) {
            item { chapterHeader() }
            itemsIndexed(items = chaptersProvider(), key = { _, chapter -> chapter.chapter.id }) { index, chapter ->
                chapterRow(index, chapter)
            }
        }
    }
}

private fun getButtonThemeColor(buttonColor: Color, isNightMode: Boolean): Color {
    val color1 = buttonColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(color1).toFloat()

    val color2 = when (isNightMode) {
        true -> Color.White.toArgb()
        false -> Color.Black.toArgb()
    }

    val ratio = when (isNightMode) {
        true -> (-(luminance - 1)) * .33f
        false -> luminance * .3f
    }

    return when ((isNightMode && luminance <= 0.6) || (isNightMode.not() && luminance > 0.4)) {
        true -> Color(ColorUtils.blendARGB(color1, color2, ratio))
        false -> buttonColor
    }
}

class ThemeColorState(buttonColor: Color, rippleTheme: RippleTheme, textSelectionColors: TextSelectionColors, altContainerColor: Color) {
    var buttonColor by mutableStateOf(buttonColor)
    var rippleTheme by mutableStateOf(rippleTheme)
    var textSelectionColors by mutableStateOf(textSelectionColors)
    var altContainerColor by mutableStateOf(altContainerColor)
}

@Composable
fun defaultThemeColorState(): ThemeColorState {
    return ThemeColorState(
        buttonColor = MaterialTheme.colorScheme.secondary,
        rippleTheme = PrimaryColorRippleTheme,
        textSelectionColors = LocalTextSelectionColors.current,
        altContainerColor = Color(ColorUtils.blendARGB(MaterialTheme.colorScheme.secondary.toArgb(), MaterialTheme.colorScheme.surface.toArgb(), .706f)),
    )
}
