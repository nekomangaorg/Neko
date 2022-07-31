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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import com.crazylegend.activity.asActivity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.PrimaryColorRippleTheme
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.screens.mangadetails.ChapterHeader
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheet
import org.nekomanga.presentation.screens.mangadetails.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.Shapes
import java.text.DateFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaScreen(
    manga: Manga,
    artwork: State<Artwork>,
    vibrantColor: State<Int?>,
    isRefreshing: State<Boolean>,
    onRefresh: () -> Unit,
    themeBasedOffCover: Boolean = true,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (Context, String) -> Unit,
    creatorLongClick: (Context, String) -> Unit,
    toggleFavorite: () -> Boolean = { true },
    categoryActions: CategoryActions,
    categories: State<List<Category>>,
    mangaCategories: State<List<Category>>,
    loggedInTrackingServices: State<List<TrackService>>,
    trackServiceCount: State<Int>,
    tracks: State<List<Track>>,
    trackSuggestedDates: State<TrackingSuggestedDates?>,
    dateFormat: DateFormat,
    trackActions: TrackActions,
    trackSearchResult: State<TrackSearchResult>,
    similarClick: () -> Unit = {},
    externalLinks: State<List<ExternalLink>>,
    isMergedManga: State<IsMergedManga>,
    mergeSearchResult: State<MergeSearchResult>,
    alternativeArtwork: State<List<Artwork>>,
    coverActions: CoverActions,
    mergeActions: MergeActions,
    shareClick: (Context) -> Unit = {},
    genreClick: (String) -> Unit = {},
    genreLongClick: (String) -> Unit = {},
    quickReadText: State<NextUnreadChapter>,
    chapterFilterText: State<String>,
    chapters: State<List<ChapterItem>>,
    removedChapters: State<List<ChapterItem>>,
    chapterSortFilter: State<MangaConstants.SortFilter>,
    chapterFilter: State<MangaConstants.Filter>,
    scanlatorFilter: State<MangaConstants.ScanlatorFilter>,
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

    var inLibrary by remember { mutableStateOf(manga.favorite) }
    val hasRemovedChapters by remember { mutableStateOf(removedChapters.value) }

    var currentBottomSheet: DetailsBottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val isTablet = LocalConfiguration.current.screenWidthDp.dp >= 600.dp
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val defaultTextSelection = LocalTextSelectionColors.current

    var themeColorState by remember {
        mutableStateOf(
            ThemeColorState(
                buttonColor = secondaryColor,
                rippleTheme = PrimaryColorRippleTheme,
                textSelectionColors = defaultTextSelection,
                altContainerColor = Color(ColorUtils.blendARGB(secondaryColor.toArgb(), surfaceColor.toArgb(), .706f)),
            ),
        )
    }

    if (themeBasedOffCover && vibrantColor.value != null) {
        val color = getButtonThemeColor(Color(vibrantColor.value!!), isDarkTheme)
        themeColorState = ThemeColorState(
            buttonColor = color,
            rippleTheme = DynamicRippleTheme(color),
            textSelectionColors = dynamicTextSelectionColor(color),
            altContainerColor = Color(ColorUtils.blendARGB(color.toArgb(), surfaceColor.toArgb(), .706f)),
        )
    }

    //set the current sheet to null when bottom sheet is closed
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
                        inLibrary = inLibrary,
                        addNewCategory = categoryActions.addNew,
                        allCategories = categories.value,
                        mangaCategories = mangaCategories.value,
                        loggedInTrackingServices = loggedInTrackingServices.value,
                        tracks = tracks.value,
                        dateFormat = dateFormat,
                        openSheet = openSheet,
                        trackActions = trackActions,
                        title = manga.title,
                        altTitles = manga.getAltTitles(),
                        trackSearchResult = trackSearchResult.value,
                        trackSuggestedDates = trackSuggestedDates.value,
                        externalLinks = externalLinks.value,
                        isMergedManga = isMergedManga.value,
                        alternativeArtwork = alternativeArtwork.value,
                        coverActions = coverActions,
                        mergeActions = mergeActions,
                        mergeSearchResult = mergeSearchResult.value,
                        chapterSortFilter = chapterSortFilter.value,
                        chapterFilter = chapterFilter.value,
                        scanlatorFilter = scanlatorFilter.value,
                        chapterFilterActions = chapterFilterActions,
                        openInBrowser = { url -> context.asActivity().openInBrowser(url) },
                    ) { scope.launch { sheetState.hide() } }
                }
            }
        },
    ) {

        NekoScaffold(
            title = "",
            themeColorState = themeColorState,
            onNavigationIconClicked = onBackPressed,
            actions = {

            },
        ) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = isRefreshing.value),
                modifier = Modifier.fillMaxSize(),
                onRefresh = onRefresh,
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshingOffset = it.calculateTopPadding(),
                        refreshTriggerDistance = trigger,
                        backgroundColor = themeColorState.buttonColor,
                        contentColor = MaterialTheme.colorScheme.surface,

                        )
                },
            ) {

                val contentPadding =
                    PaddingValues(
                        bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                            .asPaddingValues().calculateBottomPadding(),
                    )

                val details = @Composable {
                    MangaDetailsHeader(
                        manga = manga,
                        artwork = artwork.value,
                        showBackdrop = themeBasedOffCover,
                        isMerged = isMergedManga.value is IsMergedManga.Yes,
                        inLibrary = inLibrary,
                        isTablet = isTablet,
                        titleLongClick = { title: String -> titleLongClick(context, title) },
                        creatorLongClick = { creator: String -> creatorLongClick(context, creator) },
                        themeColorState = themeColorState,
                        generatePalette = generatePalette,
                        loggedIntoTrackers = loggedInTrackingServices.value.isNotEmpty(),
                        trackServiceCount = trackServiceCount.value,
                        toggleFavorite = {
                            if (inLibrary.not()) {
                                openSheet(
                                    DetailsBottomSheetScreen.CategoriesSheet(
                                        addingToLibrary = true,
                                        setCategories = categoryActions.set,
                                        addToLibraryClick = { inLibrary = toggleFavorite() },
                                    ),
                                )
                            } else {
                                inLibrary = toggleFavorite()
                            }
                        },
                        categories = categories.value,
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
                        genreClick = genreClick,
                        genreLongClick = genreLongClick,
                        quickReadClick = { chapterActions.openNext(context) },
                        quickReadText = quickReadText.value,
                    )
                }

                val chapterHeader = @Composable {
                    ChapterHeader(
                        themeColor = themeColorState,
                        numberOfChapters = chapters.value.size,
                        filterText = chapterFilterText.value,
                        onClick = { openSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
                    )
                }

                val chapterRow = @Composable { index: Int, chapter: ChapterItem ->
                    ChapterRow(
                        themeColor = themeColorState,
                        chapterItem = chapter,
                        onClick = { chapterActions.open(context, chapter) },
                        onBookmark = { chapterActions.bookmark(chapter) },
                        onRead = {
                            chapterActions.markRead(
                                listOf(chapter),
                                !chapter.chapter.read,
                            )
                        },
                        onWebView = { context.asActivity().openInBrowser(chapter.chapter.fullUrl()) },
                        onDownload = { downloadAction ->
                            chapterActions.download(listOf(chapter), downloadAction)
                        },
                        markPrevious = { read ->
                            val chaptersToMark = chapters.value.subList(0, index)
                            chapterActions.markRead(chaptersToMark, read)
                        },
                    )
                }


                CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {

                    if (isTablet) {
                        TabletLayout(
                            contentPadding = contentPadding,
                            details = details,
                            chapterHeader = chapterHeader,
                            chapters = chapters.value,
                            chapterRow = chapterRow,
                        )
                    } else {
                        NonTablet(contentPadding = contentPadding, details = details, chapterHeader = chapterHeader, chapters = chapters, chapterRow = chapterRow)
                    }

                    if (hasRemovedChapters.isNotEmpty()) {
                        RemovedChaptersDialog(
                            themeColorState = themeColorState, chapters = hasRemovedChapters,
                            onConfirm = { removeDeleted ->
                                if (removeDeleted) {
                                    chapterActions.delete(hasRemovedChapters)
                                    chapterActions.clearRemoved
                                }
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
private fun NonTablet(
    contentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chapters: State<List<ChapterItem>>,
    chapterRow: @Composable (Int, ChapterItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            details()
        }

        item {
            chapterHeader()
        }

        itemsIndexed(items = chapters.value, key = { _, chapter -> chapter.chapter.id }) { index, chapter ->
            chapterRow(index, chapter)
        }
    }
}

@Composable
private fun TabletLayout(
    contentPadding: PaddingValues,
    details: @Composable () -> Unit,
    chapterHeader: @Composable () -> Unit,
    chapters: List<ChapterItem>,
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
            contentPadding = contentPadding,
        ) {
            item { details() }
        }

        VerticalDivider(
            Modifier
                .zIndex(100f)
                .align(Alignment.TopCenter),
        )

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(.5f)
                .fillMaxHeight()
                .zIndex(0f),
            contentPadding = contentPadding,
        ) {
            item { chapterHeader() }
            itemsIndexed(items = chapters, key = { _, chapter -> chapter.chapter.id }) { index, chapter ->
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

