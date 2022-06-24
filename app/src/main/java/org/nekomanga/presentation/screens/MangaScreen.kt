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
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.manga.track.TrackSearchResult
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.PrimaryColorRippleTheme
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.TrackingSearchSheet
import org.nekomanga.presentation.components.sheets.TrackingSheet
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.Shapes
import java.text.DateFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaScreen(
    manga: Manga,
    themeBasedOffCover: Boolean = true,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (Context, String) -> Unit,
    creatorLongClick: (Context, String) -> Unit,
    toggleFavorite: () -> Boolean = { true },
    categories: StateFlow<List<Category>>,
    mangaCategories: StateFlow<List<Category>>,
    setCategories: (List<Category>) -> Unit = {},
    addNewCategory: (String) -> Unit = {},
    loggedInTrackingServices: StateFlow<List<TrackService>>,
    trackServiceCount: StateFlow<Int>,
    tracks: StateFlow<List<Track>>,
    dateFormat: DateFormat,
    trackStatusChanged: (Int, Track, TrackService) -> Unit,
    trackScoreChanged: (Int, Track, TrackService) -> Unit,
    searchTracker: (String, TrackService) -> Unit,
    trackSearchResult: StateFlow<TrackSearchResult>,
    trackSearchItemClick: (Track, TrackService) -> Unit,
    trackingRemoved: (Boolean, TrackService) -> Unit,
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
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
    val categoriesState = categories.collectAsState()
    val mangaCategoriesState = mangaCategories.collectAsState()
    val loggedInTrackingServiceState = loggedInTrackingServices.collectAsState()
    val trackState = tracks.collectAsState()
    val trackSearchResultState = trackSearchResult.collectAsState()
    val trackServiceCountState = trackServiceCount.collectAsState()
    val context = LocalContext.current

    var inLibrary by remember { mutableStateOf(manga.favorite) }

    var currentBottomSheet: BottomSheetScreen? by remember {
        mutableStateOf(null)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val buttonColor = remember {
        when (themeBasedOffCover && manga.vibrantCoverColor != null) {
            true -> {
                val color = getButtonThemeColor(Color(manga.vibrantCoverColor!!), isDarkTheme)
                color
            }
            false -> secondaryColor
        }
    }

    val rippleTheme = when (buttonColor != secondaryColor) {
        true -> DynamicRippleTheme(buttonColor)
        false -> PrimaryColorRippleTheme
    }

    val themeColors = ThemeColors(buttonColor, rippleTheme, dynamicTextSelectionColor(buttonColor))

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
                        themeColors = themeColors,
                        addNewCategory = addNewCategory,
                        allCategories = categoriesState.value,
                        mangaCategories = mangaCategoriesState.value,
                        loggedInTrackingServices = loggedInTrackingServiceState.value,
                        tracks = trackState.value,
                        dateFormat = dateFormat,
                        openSheet = openSheet,
                        trackStatusChanged = trackStatusChanged,
                        trackScoreChanged = trackScoreChanged,
                        title = manga.title,
                        trackSearchResult = trackSearchResultState.value,
                        searchTracker = searchTracker,
                        openInBrowser = { url -> context.asActivity().openInBrowser(url) },
                        trackSearchItemClick = trackSearchItemClick,
                        trackingRemoved = trackingRemoved,
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

            val contentPadding =
                PaddingValues(
                    bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        .asPaddingValues().calculateBottomPadding(),
                )

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                item {
                    MangaDetailsHeader(
                        manga = manga,
                        inLibrary = inLibrary,
                        titleLongClick = { title -> titleLongClick(context, title) },
                        creatorLongClick = { creator -> creatorLongClick(context, creator) },
                        themeColor = themeColors,
                        generatePalette = generatePalette,
                        loggedIntoTrackers = loggedInTrackingServiceState.value.isNotEmpty(),
                        trackServiceCount = trackServiceCountState.value,
                        toggleFavorite = {
                            if (inLibrary.not()) {
                                openSheet(
                                    BottomSheetScreen.CategoriesSheet(
                                        addingToLibrary = true,
                                        setCategories = setCategories,
                                        addToLibraryClick = { inLibrary = toggleFavorite() },
                                    ),
                                )
                            } else {
                                inLibrary = toggleFavorite()
                            }
                        },
                        categories = categoriesState.value,
                        moveCategories = {
                            openSheet(
                                BottomSheetScreen.CategoriesSheet(
                                    addingToLibrary = false,
                                    setCategories = setCategories,
                                ),
                            )
                        },
                        trackingClick = {
                            openSheet(
                                BottomSheetScreen.TrackingSheet,
                            )
                        },
                        artworkClick = artworkClick,
                        similarClick = similarClick,
                        mergeClick = mergeClick,
                        linksClick = linksClick,
                        shareClick = shareClick,
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

@Composable
fun SheetLayout(
    currentScreen: BottomSheetScreen,
    themeColors: ThemeColors,
    allCategories: List<Category>,
    mangaCategories: List<Category>,
    addNewCategory: (String) -> Unit,
    loggedInTrackingServices: List<TrackService>,
    tracks: List<Track>,
    dateFormat: DateFormat,
    trackStatusChanged: (Int, Track, TrackService) -> Unit,
    trackScoreChanged: (Int, Track, TrackService) -> Unit,
    title: String,
    trackSearchResult: TrackSearchResult,
    searchTracker: (String, TrackService) -> Unit,
    openInBrowser: (String) -> Unit,
    trackSearchItemClick: (Track, TrackService) -> Unit,
    trackingRemoved: (Boolean, TrackService) -> Unit,
    openSheet: (BottomSheetScreen) -> Unit,
    closeSheet: () -> Unit,
) {

    when (currentScreen) {
        is BottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = allCategories,
            mangaCategories = mangaCategories,
            themeColors = themeColors,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
            addToLibraryClick = currentScreen.addToLibraryClick,
        )
        BottomSheetScreen.TrackingSheet -> TrackingSheet(
            themeColors = themeColors,
            services = loggedInTrackingServices,
            tracks = tracks,
            dateFormat = dateFormat,
            onLogoClick = openInBrowser,
            onSearchTrackClick = { service ->
                closeSheet()
                openSheet(
                    BottomSheetScreen.TrackingSearchSheet(service),
                )
            },
            trackStatusChanged = trackStatusChanged,
            trackScoreChanged = trackScoreChanged,
            trackingRemoved = trackingRemoved,
        )
        is BottomSheetScreen.TrackingSearchSheet -> {
            //do the initial search this way we dont need to "reset" the state after the sheet closes
            LaunchedEffect(key1 = currentScreen.trackingService.id) {
                searchTracker(title, currentScreen.trackingService)
            }

            TrackingSearchSheet(
                themeColors = themeColors,
                title = title,
                trackSearchResult = trackSearchResult,
                cancelClick = {
                    closeSheet()
                    openSheet(BottomSheetScreen.TrackingSheet)
                },
                searchTracker = { query -> searchTracker(query, currentScreen.trackingService) },
                openInBrowser = openInBrowser,
                trackSearchItemClick = { trackSearch ->
                    closeSheet()
                    trackSearchItemClick(trackSearch, currentScreen.trackingService)
                    openSheet(BottomSheetScreen.TrackingSheet)
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
    class TrackingSearchSheet(val trackingService: TrackService) : BottomSheetScreen()

    //object Screen2 : BottomSheetScreen()
    // class Screen3(val argument: String) : BottomSheetScreen()
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

data class ThemeColors(val buttonColor: Color, val rippleTheme: RippleTheme, val textSelectionColors: TextSelectionColors)

