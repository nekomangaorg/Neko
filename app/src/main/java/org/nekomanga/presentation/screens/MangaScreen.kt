package org.nekomanga.presentation.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.crazylegend.activity.asActivity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergedManga
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingSuggestedDates
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.PrimaryColorRippleTheme
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.sheets.ArtworkSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.ExternalLinksSheet
import org.nekomanga.presentation.components.sheets.MergeSheet
import org.nekomanga.presentation.components.sheets.TrackingDateSheet
import org.nekomanga.presentation.components.sheets.TrackingSearchSheet
import org.nekomanga.presentation.components.sheets.TrackingSheet
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.Shapes
import java.text.DateFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaScreen(
    manga: Manga,
    artwork: State<Artwork>,
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
    mergedManga: State<MergedManga>,
    alternativeArtwork: State<List<Artwork>>,
    coverActions: CoverActions,
    mergeActions: MergeActions,
    shareClick: (Context) -> Unit = {},
    genreClick: (String) -> Unit = {},
    genreLongClick: (String) -> Unit = {},
    quickReadText: String = "",
    quickReadClick: () -> Unit = {},
    numberOfChapters: Int,
    chapterHeaderClick: () -> Unit = {},
    chapterFilterText: String,
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    val context = LocalContext.current

    var inLibrary by remember { mutableStateOf(manga.favorite) }

    var currentBottomSheet: BottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val vibrantColor = MangaCoverMetadata.getVibrantColor(manga.id!!)

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

    if (themeBasedOffCover && vibrantColor != null) {
        val color = getButtonThemeColor(Color(vibrantColor), isDarkTheme)
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

    val openSheet: (BottomSheetScreen) -> Unit = {
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
                    SheetLayout(
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
                        trackSearchResult = trackSearchResult.value,
                        trackSuggestedDates = trackSuggestedDates.value,
                        externalLinks = externalLinks.value,
                        mergedManga = mergedManga.value,
                        alternativeArtwork = alternativeArtwork.value,
                        coverActions = coverActions,
                        mergeActions = mergeActions,
                        openInBrowser = { url -> context.asActivity().openInBrowser(url) },
                    ) { scope.launch { sheetState.hide() } }
                }
            }
        },
    ) {

        NekoScaffold(
            title = "",
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

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    item {
                        MangaDetailsHeader(
                            manga = manga,
                            artwork = artwork.value,
                            showBackdrop = themeBasedOffCover,
                            isMerged = mergedManga.value is MergedManga.IsMerged,
                            inLibrary = inLibrary,
                            titleLongClick = { title -> titleLongClick(context, title) },
                            creatorLongClick = { creator -> creatorLongClick(context, creator) },
                            themeColor = themeColorState,
                            generatePalette = generatePalette,
                            loggedIntoTrackers = loggedInTrackingServices.value.isNotEmpty(),
                            trackServiceCount = trackServiceCount.value,
                            toggleFavorite = {
                                if (inLibrary.not()) {
                                    openSheet(
                                        BottomSheetScreen.CategoriesSheet(
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
                                    BottomSheetScreen.CategoriesSheet(
                                        addingToLibrary = false,
                                        setCategories = categoryActions.set,
                                    ),
                                )
                            },
                            trackingClick = {
                                openSheet(
                                    BottomSheetScreen.TrackingSheet,
                                )
                            },
                            similarClick = similarClick,
                            artworkClick = { openSheet(BottomSheetScreen.ArtworkSheet) },
                            mergeClick = { openSheet(BottomSheetScreen.MergeSheet) },
                            linksClick = { openSheet(BottomSheetScreen.ExternalLinksSheet) },
                            shareClick = { shareClick(context) },
                            genreClick = genreClick,
                            genreLongClick = genreLongClick,
                            quickReadClick = quickReadClick,
                            quickReadText = quickReadText,
                            numberOfChapters = numberOfChapters,
                            chapterHeaderClick = chapterHeaderClick,
                            chapterFilterText = chapterFilterText,
                        )

                    }
                }
            }
        }

    }
}

@Composable
fun SheetLayout(
    currentScreen: BottomSheetScreen,
    themeColorState: ThemeColorState,
    inLibrary: Boolean,
    allCategories: List<Category>,
    mangaCategories: List<Category>,
    addNewCategory: (String) -> Unit,
    loggedInTrackingServices: List<TrackService>,
    tracks: List<Track>,
    dateFormat: DateFormat,
    title: String,
    trackActions: TrackActions,
    trackSearchResult: TrackSearchResult,
    trackSuggestedDates: TrackingSuggestedDates?,
    externalLinks: List<ExternalLink>,
    alternativeArtwork: List<Artwork>,
    mergedManga: MergedManga,
    openInBrowser: (String) -> Unit,
    coverActions: CoverActions,
    mergeActions: MergeActions,
    openSheet: (BottomSheetScreen) -> Unit,
    closeSheet: () -> Unit,
) {
    val context = LocalContext.current
    when (currentScreen) {
        is BottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = allCategories,
            mangaCategories = mangaCategories,
            themeColorState = themeColorState,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
            addToLibraryClick = currentScreen.addToLibraryClick,
        )
        BottomSheetScreen.TrackingSheet -> TrackingSheet(
            themeColor = themeColorState,
            services = loggedInTrackingServices,
            tracks = tracks,
            dateFormat = dateFormat,
            onLogoClick = openInBrowser,
            onSearchTrackClick = { service, track ->
                closeSheet()
                openSheet(
                    BottomSheetScreen.TrackingSearchSheet(service, track),
                )
            },
            trackStatusChanged = trackActions.statusChange,
            trackScoreChanged = trackActions.scoreChange,
            trackingRemoved = trackActions.remove,
            trackChapterChanged = trackActions.chapterChange,
            trackingStartDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    BottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, trackSuggestedDates),
                )
            },
            trackingFinishDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    BottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, trackSuggestedDates),
                )
            },
        )
        is BottomSheetScreen.TrackingSearchSheet -> {
            //do the initial search this way we dont need to "reset" the state after the sheet closes
            LaunchedEffect(key1 = currentScreen.trackingService.id) {
                trackActions.search(title, currentScreen.trackingService)
            }

            TrackingSearchSheet(
                themeColorState = themeColorState,
                title = title,
                trackSearchResult = trackSearchResult,
                alreadySelectedTrack = currentScreen.alreadySelectedTrack,
                service = currentScreen.trackingService,
                cancelClick = {
                    closeSheet()
                    openSheet(BottomSheetScreen.TrackingSheet)
                },
                searchTracker = { query -> trackActions.search(query, currentScreen.trackingService) },
                openInBrowser = openInBrowser,
                trackingRemoved = trackActions.remove,
                trackSearchItemClick = { trackSearch ->
                    closeSheet()
                    trackActions.searchItemClick(TrackAndService(trackSearch, currentScreen.trackingService))
                    openSheet(BottomSheetScreen.TrackingSheet)
                },
            )
        }
        is BottomSheetScreen.TrackingDateSheet -> {
            TrackingDateSheet(
                themeColorState = themeColorState,
                trackAndService = currentScreen.trackAndService,
                trackingDate = currentScreen.trackingDate,
                trackSuggestedDates = currentScreen.trackSuggestedDates,
                onDismiss = {
                    closeSheet()
                    openSheet(BottomSheetScreen.TrackingSheet)
                },
                trackDateChanged = { trackDateChanged ->
                    closeSheet()
                    trackActions.dateChange(trackDateChanged)
                    openSheet(BottomSheetScreen.TrackingSheet)
                },
            )
        }
        is BottomSheetScreen.ExternalLinksSheet -> {
            ExternalLinksSheet(
                themeColorState = themeColorState, externalLinks = externalLinks,
                onLinkClick = { url ->
                    closeSheet()
                    openInBrowser(url)
                },
            )
        }

        is BottomSheetScreen.MergeSheet -> {
            MergeSheet(
                themeColorState = themeColorState, mergedManga = mergedManga,
                openMergeSource = { url ->
                    closeSheet()
                    openInBrowser(url)
                },
                removeMergeSource = {
                    closeSheet()
                    mergeActions.remove()
                },
            )
        }

        is BottomSheetScreen.ArtworkSheet -> {
            ArtworkSheet(
                themeColorState = themeColorState,
                alternativeArtwork = alternativeArtwork,
                inLibrary = inLibrary,
                saveClick = coverActions.save,
                shareClick = { url -> coverActions.share(context, url) },
                setClick = { url ->
                    closeSheet()
                    coverActions.set(url)
                },
                resetClick = {
                    closeSheet()
                    coverActions.reset()
                },
            )
        }
    }
}

sealed class BottomSheetScreen {
    class CategoriesSheet(
        val addingToLibrary: Boolean = false,
        val setCategories: (List<Category>) -> Unit,
        val addToLibraryClick: () -> Unit = {},
    ) : BottomSheetScreen()

    object TrackingSheet : BottomSheetScreen()
    object ExternalLinksSheet : BottomSheetScreen()
    object MergeSheet : BottomSheetScreen()
    object ArtworkSheet : BottomSheetScreen()
    class TrackingSearchSheet(val trackingService: TrackService, val alreadySelectedTrack: Track?) : BottomSheetScreen()
    class TrackingDateSheet(val trackAndService: TrackAndService, val trackingDate: TrackingDate, val trackSuggestedDates: TrackingSuggestedDates?) : BottomSheetScreen()
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
        false -> luminance * .5f
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

//data class themeColorState(val buttonColor: Color, val rippleTheme: RippleTheme, val textSelectionColors: TextSelectionColors, val containerColor: Color)

