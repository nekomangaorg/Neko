package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import java.text.DateFormat
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.sheets.ArtworkSheet
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.sheets.ExternalLinksSheet
import org.nekomanga.presentation.components.sheets.FilterChapterSheet
import org.nekomanga.presentation.components.sheets.MergeSheet
import org.nekomanga.presentation.components.sheets.TrackingDateSheet
import org.nekomanga.presentation.components.sheets.TrackingSearchSheet
import org.nekomanga.presentation.components.sheets.TrackingSheet
import org.nekomanga.presentation.screens.ThemeColorState

/**
 * Sealed class that holds the types of bottom sheets the details screen can show
 */
sealed class DetailsBottomSheetScreen {
    class CategoriesSheet(
        val addingToLibrary: Boolean = false,
        val setCategories: (List<CategoryItem>) -> Unit,
        val addToLibraryClick: () -> Unit = {},
    ) : DetailsBottomSheetScreen()

    object TrackingSheet : DetailsBottomSheetScreen()
    object ExternalLinksSheet : DetailsBottomSheetScreen()
    object MergeSheet : DetailsBottomSheetScreen()
    object ArtworkSheet : DetailsBottomSheetScreen()
    object FilterChapterSheet : DetailsBottomSheetScreen()
    class TrackingSearchSheet(val trackingService: TrackServiceItem, val alreadySelectedTrack: TrackItem?) : DetailsBottomSheetScreen()
    class TrackingDateSheet(
        val trackAndService: TrackingConstants.TrackAndService,
        val trackingDate: TrackingConstants.TrackingDate,
        val trackSuggestedDates: TrackingConstants.TrackingSuggestedDates?,
    ) : DetailsBottomSheetScreen()
}

@Composable
fun DetailsBottomSheet(
    currentScreen: DetailsBottomSheetScreen,
    themeColorState: ThemeColorState,
    generalState: State<MangaConstants.MangaScreenGeneralState>,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    trackMergeState: State<MangaConstants.MangaScreenTrackMergeState>,
    addNewCategory: (String) -> Unit,
    dateFormat: DateFormat,
    trackActions: MangaConstants.TrackActions,
    openInWebView: (String, String) -> Unit,
    coverActions: MangaConstants.CoverActions,
    mergeActions: MangaConstants.MergeActions,
    chapterFilterActions: MangaConstants.ChapterFilterActions,
    openSheet: (DetailsBottomSheetScreen) -> Unit,
    closeSheet: () -> Unit,
) {
    val context = LocalContext.current
    when (currentScreen) {
        is DetailsBottomSheetScreen.CategoriesSheet -> EditCategorySheet(
            addingToLibrary = currentScreen.addingToLibrary,
            categories = generalState.value.allCategories,
            mangaCategories = generalState.value.currentCategories,
            themeColorState = themeColorState,
            cancelClick = closeSheet,
            addNewCategory = addNewCategory,
            confirmClicked = currentScreen.setCategories,
            addToLibraryClick = currentScreen.addToLibraryClick,
        )
        is DetailsBottomSheetScreen.TrackingSheet -> TrackingSheet(
            themeColor = themeColorState,
            inLibrary = mangaState.value.inLibrary,
            servicesProvider = { trackMergeState.value.loggedInTrackService },
            tracksProvider = { trackMergeState.value.tracks },
            dateFormat = dateFormat,
            onLogoClick = openInWebView,
            onSearchTrackClick = { service, track ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingSearchSheet(service, track),
                )
            },
            trackStatusChanged = trackActions.statusChange,
            trackScoreChanged = trackActions.scoreChange,
            trackingRemoved = trackActions.remove,
            trackChapterChanged = trackActions.chapterChange,
            trackingStartDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, generalState.value.trackingSuggestedDates),
                )
            },
            trackingFinishDateClick = { trackAndService, trackingDate ->
                closeSheet()
                openSheet(
                    DetailsBottomSheetScreen.TrackingDateSheet(trackAndService, trackingDate, generalState.value.trackingSuggestedDates),
                )
            },
        )
        is DetailsBottomSheetScreen.TrackingSearchSheet -> {
            // do the initial search this way we dont need to "reset" the state after the sheet closes
            LaunchedEffect(key1 = currentScreen.trackingService.id) {
                trackActions.search(mangaState.value.originalTitle, currentScreen.trackingService)
            }

            TrackingSearchSheet(
                themeColorState = themeColorState,
                title = mangaState.value.originalTitle,
                trackSearchResult = trackMergeState.value.trackSearchResult,
                alreadySelectedTrack = currentScreen.alreadySelectedTrack,
                service = currentScreen.trackingService,
                cancelClick = {
                    closeSheet()
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
                searchTracker = { query -> trackActions.search(query, currentScreen.trackingService) },
                openInBrowser = openInWebView,
                trackingRemoved = trackActions.remove,
                trackSearchItemClick = { trackSearch ->
                    closeSheet()
                    trackActions.searchItemClick(TrackingConstants.TrackAndService(trackSearch.trackItem, currentScreen.trackingService))
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.TrackingDateSheet -> {
            TrackingDateSheet(
                themeColorState = themeColorState,
                trackAndService = currentScreen.trackAndService,
                trackingDate = currentScreen.trackingDate,
                trackSuggestedDates = currentScreen.trackSuggestedDates,
                onDismiss = {
                    closeSheet()
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
                trackDateChanged = { trackDateChanged ->
                    closeSheet()
                    trackActions.dateChange(trackDateChanged)
                    openSheet(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.ExternalLinksSheet -> {
            ExternalLinksSheet(
                themeColorState = themeColorState,
                externalLinks = mangaState.value.externalLinks,
                onLinkClick = { url, title ->
                    closeSheet()
                    openInWebView(url, title)
                },
            )
        }

        is DetailsBottomSheetScreen.MergeSheet -> {
            if (mangaState.value.isMerged is MergeConstants.IsMergedManga.No) {
                LaunchedEffect(key1 = 1) {
                    mergeActions.search(mangaState.value.originalTitle)
                }
            }
            MergeSheet(
                themeColorState = themeColorState,
                isMergedManga = mangaState.value.isMerged,
                title = mangaState.value.originalTitle,
                altTitles = mangaState.value.alternativeTitles,
                mergeSearchResults = trackMergeState.value.mergeSearchResult,
                openMergeSource = { url, title ->
                    closeSheet()
                    openInWebView(url, title)
                },
                removeMergeSource = {
                    closeSheet()
                    mergeActions.remove()
                },
                cancelClick = {
                    closeSheet()
                },
                search = mergeActions.search,
                mergeMangaClick = { mergeManga ->
                    closeSheet()
                    mergeActions.add(mergeManga)
                },
            )
        }

        is DetailsBottomSheetScreen.ArtworkSheet -> {
            ArtworkSheet(
                themeColorState = themeColorState,
                alternativeArtwork = mangaState.value.alternativeArtwork,
                inLibrary = mangaState.value.inLibrary,
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
        is DetailsBottomSheetScreen.FilterChapterSheet -> {
            FilterChapterSheet(
                themeColorState = themeColorState,
                sortFilter = generalState.value.chapterSortFilter,
                changeSort = chapterFilterActions.changeSort,
                changeFilter = chapterFilterActions.changeFilter,
                filter = generalState.value.chapterFilter,
                scanlatorFilter = generalState.value.chapterScanlatorFilter,
                languageFilter = generalState.value.chapterLanguageFilter,
                hideTitlesFilter = generalState.value.hideChapterTitles,
                changeScanlatorFilter = chapterFilterActions.changeScanlator,
                changeLanguageFilter = chapterFilterActions.changeLanguage,
                changeHideTitles = chapterFilterActions.hideTitles,
                setAsGlobal = chapterFilterActions.setAsGlobal,
            )
        }
    }
}
