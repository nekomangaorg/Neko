package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.fredporciuncula.flow.preferences.Preference
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.updater.AutoAppUpdaterJob
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.recents.RecentMangaAdapter
import eu.kanade.tachiyomi.util.system.Themes
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun <T> Preference<T>.asImmediateFlow(block: (value: T) -> Unit): Flow<T> {
    block(get())
    return asFlow()
        .onEach { block(it) }
}

fun <T> Preference<T>.asImmediateFlowIn(scope: CoroutineScope, block: (value: T) -> Unit): Job {
    block(get())
    return asFlow()
        .onEach { block(it) }
        .launchIn(scope)
}

fun Preference<Boolean>.toggle() = set(!get())

operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

operator fun <T> Preference<Set<T>>.plusAssign(item: Collection<T>) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: Collection<T>) {
    set(get() - item)
}

class PreferencesHelper(val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val flowPrefs = FlowSharedPreferences(prefs)

    private val defaultFolder = context.getString(R.string.app_name) + when (BuildConfig.DEBUG) {
        true -> "_DEBUG"
        false -> ""
    }

    private val defaultDownloadsDir = Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                defaultFolder,
            "downloads",
        ),
    )

    private val defaultBackupDir = Uri.fromFile(
        File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                defaultFolder,
            "backup",
        ),
    )

    fun getInt(key: String, default: Int) = flowPrefs.getInt(key, default)
    fun getStringPref(key: String, default: String = "") = flowPrefs.getString(key, default)
    fun getStringSet(key: String, default: Set<String>) = flowPrefs.getStringSet(key, default)

    fun startingTab() = flowPrefs.getInt(Keys.startingTab, 0)
    fun backReturnsToStart() = flowPrefs.getBoolean(Keys.backToStart, true)

    fun hasDeniedA11FilePermission() = flowPrefs.getBoolean(Keys.deniedA11FilePermission, false)

    fun clear() = prefs.edit().clear().apply()

    fun nightMode() = flowPrefs.getInt(Keys.nightMode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private val supportsDynamic = DynamicColors.isDynamicColorAvailable()
    fun lightTheme() =
        flowPrefs.getEnum(Keys.lightTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun darkTheme() =
        flowPrefs.getEnum(Keys.darkTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun pageTransitions() = flowPrefs.getBoolean(Keys.enableTransitions, true)

    fun pagerCutoutBehavior() = flowPrefs.getInt(Keys.pagerCutoutBehavior, 0)

    fun doubleTapAnimSpeed() = flowPrefs.getInt(Keys.doubleTapAnimationSpeed, 500)

    fun showPageNumber() = flowPrefs.getBoolean(Keys.showPageNumber, true)

    fun trueColor() = flowPrefs.getBoolean(Keys.trueColor, false)

    fun fullscreen() = flowPrefs.getBoolean(Keys.fullscreen, true)

    fun keepScreenOn() = flowPrefs.getBoolean(Keys.keepScreenOn, true)

    fun customBrightness() = flowPrefs.getBoolean(Keys.customBrightness, false)

    fun customBrightnessValue() = flowPrefs.getInt(Keys.customBrightnessValue, 0)

    fun colorFilter() = flowPrefs.getBoolean(Keys.colorFilter, false)

    fun colorFilterValue() = flowPrefs.getInt(Keys.colorFilterValue, 0)

    fun colorFilterMode() = flowPrefs.getInt(Keys.colorFilterMode, 0)

    fun defaultReadingMode() =
        prefs.getInt(Keys.defaultReadingMode, ReadingModeType.RIGHT_TO_LEFT.flagValue)

    fun defaultOrientationType() =
        flowPrefs.getInt(Keys.defaultOrientationType, OrientationType.FREE.flagValue)

    fun imageScaleType() = flowPrefs.getInt(Keys.imageScaleType, 1)

    fun doublePageGap() = flowPrefs.getInt(Keys.doublePageGap, 0)

    fun zoomStart() = flowPrefs.getInt(Keys.zoomStart, 1)

    fun readerTheme() = flowPrefs.getInt(Keys.readerTheme, 2)

    fun cropBorders() = flowPrefs.getBoolean(Keys.cropBorders, false)

    fun cropBordersWebtoon() = flowPrefs.getBoolean(Keys.cropBordersWebtoon, false)

    fun navigateToPan() = flowPrefs.getBoolean("navigate_pan", true)

    fun landscapeZoom() = flowPrefs.getBoolean("landscape_zoom", false)

    fun grayscale() = flowPrefs.getBoolean("pref_grayscale", false)

    fun invertedColors() = flowPrefs.getBoolean("pref_inverted_colors", false)

    fun webtoonSidePadding() = flowPrefs.getInt(Keys.webtoonSidePadding, 0)

    fun webtoonEnableZoomOut() = flowPrefs.getBoolean(Keys.webtoonEnableZoomOut, false)

    fun readWithLongTap() = flowPrefs.getBoolean(Keys.readWithLongTap, true)

    fun readWithVolumeKeys() = flowPrefs.getBoolean(Keys.readWithVolumeKeys, false)

    fun readWithVolumeKeysInverted() = flowPrefs.getBoolean(Keys.readWithVolumeKeysInverted, false)

    fun navigationModePager() = flowPrefs.getInt(Keys.navigationModePager, 0)

    fun navigationModeWebtoon() = flowPrefs.getInt(Keys.navigationModeWebtoon, 0)

    fun pagerNavInverted() =
        flowPrefs.getEnum(Keys.pagerNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun webtoonNavInverted() =
        flowPrefs.getEnum(Keys.webtoonNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun pageLayout() = flowPrefs.getInt(Keys.pageLayout, PageLayout.AUTOMATIC.value)

    fun automaticSplitsPage() = flowPrefs.getBoolean(Keys.automaticSplitsPage, false)

    fun invertDoublePages() = flowPrefs.getBoolean(Keys.invertDoublePages, false)

    fun webtoonPageLayout() = flowPrefs.getInt(Keys.webtoonPageLayout, PageLayout.SINGLE_PAGE.value)

    fun webtoonReaderHideThreshold() = flowPrefs.getEnum("reader_hide_threshold", Values.ReaderHideThreshold.LOW)

    fun webtoonInvertDoublePages() = flowPrefs.getBoolean(Keys.webtoonInvertDoublePages, false)

    fun readerBottomButtons() = flowPrefs.getStringSet(
        Keys.readerBottomButtons,
        ReaderBottomButton.BUTTONS_DEFAULTS,
    )

    fun showNavigationOverlayNewUser() =
        flowPrefs.getBoolean(Keys.showNavigationOverlayNewUser, true)

    fun showNavigationOverlayNewUserWebtoon() =
        flowPrefs.getBoolean(Keys.showNavigationOverlayNewUserWebtoon, true)

    fun preloadSize() = flowPrefs.getInt(Keys.preloadSize, 6)

    fun updateOnlyNonCompleted() = prefs.getBoolean(Keys.updateOnlyNonCompleted, false)

    fun autoUpdateTrack() = prefs.getBoolean(Keys.autoUpdateTrack, true)

    fun trackMarkedAsRead() = prefs.getBoolean(Keys.trackMarkedAsRead, false)

    fun trackingsToAddOnline() = flowPrefs.getStringSet(Keys.trackingsToAddOnline, emptySet())

    fun lastUsedCatalogueSource() = flowPrefs.getLong(Keys.lastUsedCatalogueSource, -1)

    fun lastUsedCategory() = flowPrefs.getInt(Keys.lastUsedCategory, 0)

    fun lastUsedSources() = flowPrefs.getStringSet("last_used_sources", emptySet())

    fun lastVersionCode() = flowPrefs.getInt("last_version_code", 0)

    fun browseAsList() = flowPrefs.getBoolean(Keys.catalogueAsList, false)

    fun browseShowLibrary() = flowPrefs.getBoolean(Keys.catalogueShowLibrary, true)

    fun enabledLanguages() =
        flowPrefs.getStringSet(Keys.enabledLanguages, setOf("en", Locale.getDefault().language))

    fun sourceSorting() = flowPrefs.getInt(Keys.sourcesSort, 0)

    fun sourceUsername(source: Source) = prefs.getString(Keys.sourceUsername(source.id), "")

    fun sourcePassword(source: Source) = prefs.getString(Keys.sourcePassword(source.id), "")

    fun setSourceCredentials(source: Source, username: String, password: String) {
        prefs.edit()
            .putString(Keys.sourceUsername(source.id), username)
            .putString(Keys.sourcePassword(source.id), password)
            .apply()
    }

    fun trackUsername(sync: TrackService) = prefs.getString(Keys.trackUsername(sync.id), "")

    fun trackPassword(sync: TrackService) = prefs.getString(Keys.trackPassword(sync.id), "")

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        prefs.edit()
            .putString(Keys.trackUsername(sync.id), username)
            .putString(Keys.trackPassword(sync.id), password)
            .apply()
    }

    fun trackToken(sync: TrackService) = flowPrefs.getString(Keys.trackToken(sync.id), "")

    fun anilistScoreType() = flowPrefs.getString("anilist_score_type", "POINT_10")

    fun backupsDirectory() = flowPrefs.getString(Keys.backupDirectory, defaultBackupDir.toString())

    fun dateFormat(format: String = flowPrefs.getString(Keys.dateFormat, "").get()): DateFormat =
        when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }

    fun downloadsDirectory() =
        flowPrefs.getString(Keys.downloadsDirectory, defaultDownloadsDir.toString())

    fun downloadOnlyOverWifi() = prefs.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun folderPerManga() = prefs.getBoolean(Keys.folderPerManga, false)

    fun librarySearchSuggestion() = flowPrefs.getString(Keys.librarySearchSuggestion, "")

    fun showLibrarySearchSuggestions() =
        flowPrefs.getBoolean(Keys.showLibrarySearchSuggestions, false)

    fun lastLibrarySuggestion() = flowPrefs.getLong("last_library_suggestion", 0L)

    fun numberOfBackups() = flowPrefs.getInt(Keys.numberOfBackups, 2)

    fun backupInterval() = flowPrefs.getInt(Keys.backupInterval, 0)

    fun removeAfterReadSlots() = prefs.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = prefs.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun libraryUpdateInterval() = flowPrefs.getInt(Keys.libraryUpdateInterval, 24)

    fun libraryUpdateLastTimestamp() = flowPrefs.getLong("library_update_last_timestamp", 0L)

    fun libraryUpdateDeviceRestriction() =
        flowPrefs.getStringSet(Keys.libraryUpdateDeviceRestriction, emptySet())

    fun libraryUpdateCategories() = flowPrefs.getStringSet(Keys.libraryUpdateCategories, emptySet())
    fun libraryUpdateCategoriesExclude() = flowPrefs.getStringSet(Keys.libraryUpdateCategoriesExclude, emptySet())

    fun libraryUpdatePrioritization() = flowPrefs.getInt(Keys.libraryUpdatePrioritization, 0)

    fun libraryLayout() = flowPrefs.getInt(Keys.libraryLayout, LibraryItem.LAYOUT_COMFORTABLE_GRID)

    fun gridSize() = flowPrefs.getFloat(Keys.gridSize, 1f)

    fun uniformGrid() = flowPrefs.getBoolean(Keys.uniformGrid, true)

    fun outlineOnCovers() = flowPrefs.getBoolean(Keys.outlineOnCovers, true)

    fun downloadBadge() = flowPrefs.getBoolean(Keys.downloadBadge, false)

    fun filterDownloaded() = flowPrefs.getInt(Keys.filterDownloaded, 0)

    fun filterUnread() = flowPrefs.getInt(Keys.filterUnread, 0)

    fun filterCompleted() = flowPrefs.getInt(Keys.filterCompleted, 0)

    fun filterTracked() = flowPrefs.getInt(Keys.filterTracked, 0)

    fun filterMangaType() = flowPrefs.getInt(Keys.filterMangaType, 0)

    fun showEmptyCategoriesWhileFiltering() =
        flowPrefs.getBoolean(Keys.showEmptyCategoriesFiltering, false)

    fun filterMerged() = flowPrefs.getInt(Keys.filterMerged, 0)

    fun filterMissingChapters() = flowPrefs.getInt(Keys.filterMissingChapters, 0)

    fun librarySortingMode() = flowPrefs.getInt(Keys.librarySortingMode, 0)

    fun librarySortingAscending() = flowPrefs.getBoolean("library_sorting_ascending", true)

    fun automaticUpdates() = prefs.getBoolean(Keys.automaticUpdates, true)

    fun collapsedCategories() = flowPrefs.getStringSet("collapsed_categories", mutableSetOf())

    fun collapsedDynamicCategories() =
        flowPrefs.getStringSet("collapsed_dynamic_categories", mutableSetOf())

    fun collapsedDynamicAtBottom() = flowPrefs.getBoolean("collapsed_dynamic_at_bottom", false)

    fun hiddenSources() = flowPrefs.getStringSet("hidden_catalogues", mutableSetOf())

    fun pinnedCatalogues() = flowPrefs.getStringSet("pinned_catalogues", mutableSetOf())

    fun saveChaptersAsCBZ() = flowPrefs.getBoolean("save_chapter_as_cbz", true)

    fun splitTallImages() = flowPrefs.getBoolean("split_tall_images", false)

    fun downloadNewChapters() = flowPrefs.getBoolean(Keys.downloadNew, false)

    fun downloadNewChaptersInCategories() = flowPrefs.getStringSet(Keys.downloadNewCategories, emptySet())
    fun excludeCategoriesInDownloadNew() = flowPrefs.getStringSet(Keys.downloadNewCategoriesExclude, emptySet())

    fun lang() = prefs.getString(Keys.lang, "")

    fun langsToShow() = flowPrefs.getString(Keys.langToShow, "en")

    fun autoDownloadWhileReading() = flowPrefs.getInt("auto_download_while_reading", 0)

    fun defaultCategory() = prefs.getInt(Keys.defaultCategory, -1)

    fun skipRead() = prefs.getBoolean(Keys.skipRead, false)

    fun skipFiltered() = prefs.getBoolean(Keys.skipFiltered, true)

    fun useBiometrics() = flowPrefs.getBoolean(Keys.useBiometrics, false)

    fun lockAfter() = flowPrefs.getInt(Keys.lockAfter, 0)

    fun lastUnlock() = flowPrefs.getLong(Keys.lastUnlock, 0)

    fun secureScreen() = flowPrefs.getEnum("secure_screen_v2", Values.SecureScreenMode.INCOGNITO)

    fun hideNotificationContent() = prefs.getBoolean(Keys.hideNotificationContent, false)

    fun removeArticles() = flowPrefs.getBoolean(Keys.removeArticles, false)

    fun migrateFlags() = flowPrefs.getInt("migrate_flags", Int.MAX_VALUE)

    fun trustedSignatures() = flowPrefs.getStringSet("trusted_signatures", emptySet())

    // using string instead of set so it is ordered
    fun migrationSources() = flowPrefs.getString("migrate_sources", "")

    fun useSourceWithMost() = flowPrefs.getBoolean("use_source_with_most", false)

    fun skipPreMigration() = flowPrefs.getBoolean(Keys.skipPreMigration, false)

    fun defaultMangaOrder() = flowPrefs.getString("default_manga_order", "")

    fun refreshCoversToo() = flowPrefs.getBoolean(Keys.refreshCoversToo, true)

    fun updateOnRefresh() = flowPrefs.getInt(Keys.updateOnRefresh, -1)

    fun recentsViewType() = flowPrefs.getInt("recents_view_type", 0)

    fun showRecentsDownloads() =
        flowPrefs.getEnum(Keys.showDLsInRecents, RecentMangaAdapter.ShowRecentsDLs.All)

    fun showRecentsRemHistory() = flowPrefs.getBoolean(Keys.showRemHistoryInRecents, true)

    fun showReadInAllRecents() = flowPrefs.getBoolean(Keys.showReadInAllRecents, false)

    fun showUpdatedTime() = flowPrefs.getBoolean(Keys.showUpdatedTime, false)

    fun sortFetchedTime() = flowPrefs.getBoolean("sort_fetched_time", false)

    fun groupChaptersUpdates() = flowPrefs.getBoolean(Keys.groupChaptersUpdates, false)

    fun groupChaptersHistory() = flowPrefs.getBoolean(Keys.groupChaptersHistory, true)

    fun showTitleFirstInRecents() = flowPrefs.getBoolean(Keys.showTitleFirstInRecents, false)

    fun lastExtCheck() = flowPrefs.getLong("last_ext_check", 0)

    fun lastAppCheck() = flowPrefs.getLong("last_app_check", 0)

    fun unreadBadgeType() = flowPrefs.getInt("unread_badge_type", 2)

    fun categoryNumberOfItems() = flowPrefs.getBoolean(Keys.categoryNumberOfItems, false)

    fun hideStartReadingButton() = flowPrefs.getBoolean("hide_reading_button", false)

    fun alwaysShowChapterTransition() = flowPrefs.getBoolean(Keys.alwaysShowChapterTransition, true)

    fun deleteRemovedChapters() = flowPrefs.getInt(Keys.deleteRemovedChapters, 0)

    fun showAllCategories() = flowPrefs.getBoolean("show_all_categories", true)

    fun showAllCategoriesWhenSearchingSingleCategory() = flowPrefs.getBoolean("show_all_categories_when_searching_single_category", false)

    fun hopperGravity() = flowPrefs.getInt("hopper_gravity", 1)

    fun filterOrder() = flowPrefs.getString("filter_order", FilterBottomSheet.Filters.DEFAULT_ORDER)

    fun hopperLongPressAction() = flowPrefs.getInt(Keys.hopperLongPress, 0)

    fun hideHopper() = flowPrefs.getBoolean("hide_hopper", false)

    fun hideButtonText() = flowPrefs.getBoolean(Keys.hideMangaDetailButtonText, false)

    fun extraLargeBackdrop() = flowPrefs.getBoolean(Keys.extraLargeBackdrop, false)

    fun fasterLibraryUpdates() = flowPrefs.getBoolean(Keys.fasterLibraryUpdates, false)

    fun autohideHopper() = flowPrefs.getBoolean(Keys.autoHideHopper, true)

    fun groupLibraryBy() = flowPrefs.getInt("group_library_by", 0)

    fun showCategoryInTitle() = flowPrefs.getBoolean("category_in_title", false)

    fun onlySearchPinned() = flowPrefs.getBoolean(Keys.onlySearchPinned, false)

    // Tutorial preferences
    fun shownFilterTutorial() = flowPrefs.getBoolean("shown_filter_tutorial", false)

    fun shownChapterSwipeTutorial() = flowPrefs.getBoolean("shown_swipe_tutorial", false)

    fun shownDownloadQueueTutorial() = flowPrefs.getBoolean("shown_download_queue", false)

    fun shownLongPressCategoryTutorial() = flowPrefs.getBoolean("shown_long_press_category", false)

    fun shownHopperSwipeTutorial() = flowPrefs.getBoolean("shown_hopper_swipe", false)

    fun shownDownloadSwipeTutorial() = flowPrefs.getBoolean("shown_download_tutorial", false)

    fun hideBottomNavOnScroll() = flowPrefs.getBoolean(Keys.hideBottomNavOnScroll, true)

    fun sideNavIconAlignment() = flowPrefs.getInt(Keys.sideNavIconAlignment, 1)

    fun themeMangaDetails() = prefs.getBoolean(Keys.themeMangaDetails, true)

    fun useLargeToolbar() = flowPrefs.getBoolean("use_large_toolbar", false)

    fun dohProvider() = prefs.getInt(Keys.dohProvider, -1)

    fun showSeriesInShortcuts() = prefs.getBoolean(Keys.showSeriesInShortcuts, true)
    fun openChapterInShortcuts() = prefs.getBoolean(Keys.openChapterInShortcuts, true)

    fun incognitoMode() = flowPrefs.getBoolean(Keys.incognitoMode, false)

    fun hasPromptedBeforeUpdateAll() = flowPrefs.getBoolean("has_prompted_update_all", false)

    fun sideNavMode() = flowPrefs.getInt(Keys.sideNavMode, 0)

    fun appShouldAutoUpdate() =
        prefs.getInt(Keys.shouldAutoUpdate, AutoAppUpdaterJob.ONLY_ON_UNMETERED)

    fun filterChapterByRead() = flowPrefs.getInt(Keys.defaultChapterFilterByRead, Manga.SHOW_ALL)

    fun filterChapterByDownloaded() =
        flowPrefs.getInt(Keys.defaultChapterFilterByDownloaded, Manga.SHOW_ALL)

    fun filterChapterByBookmarked() =
        flowPrefs.getInt(Keys.defaultChapterFilterByBookmarked, Manga.SHOW_ALL)

    fun sortChapterOrder() =
        flowPrefs.getInt(Keys.defaultChapterSortBySourceOrNumber, Manga.CHAPTER_SORTING_SOURCE)

    fun hideChapterTitlesByDefault() = flowPrefs.getBoolean(Keys.hideChapterTitles, false)

    fun chaptersDescAsDefault() = flowPrefs.getBoolean(Keys.chaptersDescAsDefault, true)

    fun sortChapterByAscendingOrDescending() =
        prefs.getInt(Keys.defaultChapterSortByAscendingOrDescending, Manga.CHAPTER_SORT_DESC)

    fun blockedScanlators() =
        flowPrefs.getStringSet(Keys.blockedScanlators, emptySet())

    fun coverRatios() = flowPrefs.getStringSet(Keys.coverRatios, emptySet())

    fun coverColors() = flowPrefs.getStringSet(Keys.coverColors, emptySet())

    fun coverVibrantColors() = flowPrefs.getStringSet(Keys.coverVibrantColors, emptySet())

    fun useStaggeredGrid() = flowPrefs.getBoolean("use_staggered_grid", false)

    fun dataSaver() = prefs.getBoolean(Keys.dataSaver, false)

    fun thumbnailQuality() = prefs.getInt(Keys.thumbnailQuality, 0)

    fun usePort443Only() = prefs.getBoolean(Keys.enablePort443Only, false)

    fun logLevel() = prefs.getInt(Keys.logLevel, 0)

    fun showContentRatingFilter(): Boolean = prefs.getBoolean(Keys.showContentRatingFilter, true)

    fun addToLibraryAsPlannedToRead(): Boolean =
        prefs.getBoolean(Keys.addToLibraryAsPlannedToRead, false)

    fun contentRatingSelections(): MutableSet<String> =
        prefs.getStringSet(
            Keys.contentRating,
            setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive),
        )!!

    fun autoTrackContentRatingSelections(): MutableSet<String> =
        prefs.getStringSet(
            Keys.autoTrackContentRating,
            setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive, MdConstants.ContentRating.erotica, MdConstants.ContentRating.pornographic),
        )!!

    fun autoAddTracker() = flowPrefs.getStringSet(Keys.autoAddTracker, setOf(TrackManager.MDLIST.toString()))

    fun setAutoAddTracker(trackersToAutoAdd: Set<String>) = prefs.edit().putStringSet(Keys.autoAddTracker, trackersToAutoAdd).apply()

    fun sessionToken() = prefs.getString(Keys.sessionToken, "")

    fun refreshToken() = prefs.getString(Keys.refreshToken, "")

    fun setTokens(refresh: String, session: String) {
        val time = if (refresh.isBlank() && session.isBlank()) {
            0
        } else {
            System.currentTimeMillis()
        }
        prefs.edit()
            .putString(Keys.sessionToken, session)
            .putString(Keys.refreshToken, refresh)
            .putLong(Keys.lastRefreshTokenTime, time)
            .apply()
    }

    fun lastRefreshTime(): Long {
        return prefs.getLong(Keys.lastRefreshTokenTime, 0)
    }

    fun readingSync(): Boolean = prefs.getBoolean(Keys.readingSync, false)

    fun mangadexSyncToLibraryIndexes() =
        flowPrefs.getStringSet(Keys.mangadexSyncToLibraryIndexes, emptySet())
}
