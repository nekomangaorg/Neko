package org.nekomanga.presentation.screens.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.manga.MangaConstants
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
import org.nekomanga.presentation.components.theme.ThemeColorState

/** Sealed class that holds the types of bottom sheets the details screen can show */
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

    class TrackingSearchSheet(
        val trackingService: TrackServiceItem,
        val alreadySelectedTrack: TrackItem?,
    ) : DetailsBottomSheetScreen()

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
    mangaDetailScreenState: MangaConstants.MangaDetailScreenState,
    addNewCategory: (String) -> Unit,
    dateFormat: DateFormat,
    trackActions: MangaConstants.TrackActions,
    openInWebView: (String, String) -> Unit,
    coverActions: MangaConstants.CoverActions,
    mergeActions: MangaConstants.MergeActions,
    chapterFilterActions: MangaConstants.ChapterFilterActions,
    onNavigate: (DetailsBottomSheetScreen?) -> Unit,
) {
    val context = LocalContext.current
    when (currentScreen) {
        is DetailsBottomSheetScreen.CategoriesSheet ->
            EditCategorySheet(
                addingToLibrary = currentScreen.addingToLibrary,
                categories = mangaDetailScreenState.category.allCategories,
                mangaCategories = mangaDetailScreenState.category.currentCategories,
                themeColorState = themeColorState,
                cancelClick = { onNavigate(null) },
                addNewCategory = addNewCategory,
                confirmClicked = currentScreen.setCategories,
                addToLibraryClick = currentScreen.addToLibraryClick,
            )
        is DetailsBottomSheetScreen.TrackingSheet ->
            TrackingSheet(
                themeColor = themeColorState,
                inLibrary = mangaDetailScreenState.manga.inLibrary,
                servicesProvider = { mangaDetailScreenState.track.loggedInTrackService },
                tracksProvider = { mangaDetailScreenState.track.tracks },
                dateFormat = dateFormat,
                onLogoClick = openInWebView,
                onSearchTrackClick = { service, track ->
                    onNavigate(DetailsBottomSheetScreen.TrackingSearchSheet(service, track))
                },
                trackStatusChanged = trackActions.statusChange,
                trackScoreChanged = trackActions.scoreChange,
                trackingRemoved = trackActions.remove,
                trackChapterChanged = trackActions.chapterChange,
                trackingStartDateClick = { trackAndService, trackingDate ->
                    onNavigate(
                        DetailsBottomSheetScreen.TrackingDateSheet(
                            trackAndService,
                            trackingDate,
                            mangaDetailScreenState.track.trackingSuggestedDates,
                        )
                    )
                },
                trackingFinishDateClick = { trackAndService, trackingDate ->
                    onNavigate(
                        DetailsBottomSheetScreen.TrackingDateSheet(
                            trackAndService,
                            trackingDate,
                            mangaDetailScreenState.track.trackingSuggestedDates,
                        )
                    )
                },
            )
        is DetailsBottomSheetScreen.TrackingSearchSheet -> {
            // do the initial search this way we dont need to "reset" the state after the sheet
            // closes
            LaunchedEffect(key1 = currentScreen.trackingService.id) {
                trackActions.search(
                    mangaDetailScreenState.manga.originalTitle,
                    currentScreen.trackingService,
                )
            }

            TrackingSearchSheet(
                themeColorState = themeColorState,
                title = mangaDetailScreenState.manga.originalTitle,
                trackSearchResult = mangaDetailScreenState.track.trackSearchResult,
                alreadySelectedTrack = currentScreen.alreadySelectedTrack,
                service = currentScreen.trackingService,
                cancelClick = { onNavigate(DetailsBottomSheetScreen.TrackingSheet) },
                searchTracker = { query ->
                    trackActions.search(query, currentScreen.trackingService)
                },
                openInBrowser = openInWebView,
                trackingRemoved = trackActions.remove,
                trackSearchItemClick = { trackSearch ->
                    trackActions.searchItemClick(
                        TrackingConstants.TrackAndService(
                            trackSearch.trackItem,
                            currentScreen.trackingService,
                        )
                    )
                    onNavigate(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.TrackingDateSheet -> {
            TrackingDateSheet(
                themeColorState = themeColorState,
                trackAndService = currentScreen.trackAndService,
                trackingDate = currentScreen.trackingDate,
                trackSuggestedDates = currentScreen.trackSuggestedDates,
                onDismiss = { onNavigate(DetailsBottomSheetScreen.TrackingSheet) },
                trackDateChanged = { trackDateChanged ->
                    trackActions.dateChange(trackDateChanged)
                    onNavigate(DetailsBottomSheetScreen.TrackingSheet)
                },
            )
        }
        is DetailsBottomSheetScreen.ExternalLinksSheet -> {
            ExternalLinksSheet(
                themeColorState = themeColorState,
                externalLinks = mangaDetailScreenState.manga.externalLinks,
                onLinkClick = { url, title ->
                    onNavigate(null)
                    openInWebView(url, title)
                },
            )
        }
        is DetailsBottomSheetScreen.MergeSheet -> {
            MergeSheet(
                themeColorState = themeColorState,
                isMergedManga = mangaDetailScreenState.manga.isMerged,
                title = mangaDetailScreenState.manga.originalTitle,
                altTitles = mangaDetailScreenState.manga.alternativeTitles,
                mergeSearchResults = mangaDetailScreenState.merge.mergeSearchResult,
                openMergeSource = { url, title ->
                    onNavigate(null)
                    openInWebView(url, title)
                },
                removeMergeSource = { mergeType ->
                    onNavigate(null)
                    mergeActions.remove(mergeType)
                },
                cancelClick = { onNavigate(null) },
                search = mergeActions.search,
                mergeMangaClick = { mergeManga ->
                    onNavigate(null)
                    mergeActions.add(mergeManga)
                },
                validMergeTypes = mangaDetailScreenState.merge.validMergeTypes,
            )
        }
        is DetailsBottomSheetScreen.ArtworkSheet -> {
            ArtworkSheet(
                themeColorState = themeColorState,
                alternativeArtwork = mangaDetailScreenState.manga.alternativeArtwork,
                inLibrary = mangaDetailScreenState.manga.inLibrary,
                saveClick = { artwork ->
                    onNavigate(null)
                    coverActions.save(artwork)
                },
                shareClick = { url -> coverActions.share(context, url) },
                setClick = { url ->
                    onNavigate(null)
                    coverActions.set(url)
                },
                resetClick = {
                    onNavigate(null)
                    coverActions.reset()
                },
            )
        }
        is DetailsBottomSheetScreen.FilterChapterSheet -> {
            FilterChapterSheet(
                themeColorState = themeColorState,
                sortFilter = mangaDetailScreenState.chapters.chapterSortFilter,
                changeSort = chapterFilterActions.changeSort,
                changeFilter = chapterFilterActions.changeFilter,
                filter = mangaDetailScreenState.chapters.chapterFilter,
                scanlatorFilter = mangaDetailScreenState.chapters.chapterScanlatorFilter,
                sourceFilter = mangaDetailScreenState.chapters.chapterSourceFilter,
                languageFilter = mangaDetailScreenState.chapters.chapterLanguageFilter,
                changeScanlatorFilter = chapterFilterActions.changeScanlator,
                changeLanguageFilter = chapterFilterActions.changeLanguage,
                setAsGlobal = chapterFilterActions.setAsGlobal,
            )
        }
    }
}
