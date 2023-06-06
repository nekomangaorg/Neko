package eu.kanade.tachiyomi.data.preference

import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import org.nekomanga.core.preferences.PreferenceValues as Values
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
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
import java.security.SecureRandom
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum

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

class PreferencesHelper(val context: Context, val preferenceStore: PreferenceStore) {

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

    fun getInt(key: String, default: Int) = this.preferenceStore.getInt(key, default)
    fun getStringPref(key: String, default: String = "") = this.preferenceStore.getString(key, default)

    fun startingTab() = this.preferenceStore.getInt(Keys.startingTab, 0)
    fun backReturnsToStart() = this.preferenceStore.getBoolean(Keys.backToStart, true)

    fun hasDeniedA11FilePermission() = this.preferenceStore.getBoolean(Keys.deniedA11FilePermission, false)

    fun nightMode() = this.preferenceStore.getInt(Keys.nightMode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private val supportsDynamic = DynamicColors.isDynamicColorAvailable()
    fun lightTheme() = this.preferenceStore.getEnum(Keys.lightTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun darkTheme() = this.preferenceStore.getEnum(Keys.darkTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun pageTransitions() = this.preferenceStore.getBoolean(Keys.enableTransitions, true)

    fun pagerCutoutBehavior() = this.preferenceStore.getInt(Keys.pagerCutoutBehavior, 0)

    fun doubleTapAnimSpeed() = this.preferenceStore.getInt(Keys.doubleTapAnimationSpeed, 500)

    fun showPageNumber() = this.preferenceStore.getBoolean(Keys.showPageNumber, true)

    fun trueColor() = this.preferenceStore.getBoolean(Keys.trueColor, false)

    fun fullscreen() = this.preferenceStore.getBoolean(Keys.fullscreen, true)

    fun keepScreenOn() = this.preferenceStore.getBoolean(Keys.keepScreenOn, true)

    fun customBrightness() = this.preferenceStore.getBoolean(Keys.customBrightness, false)

    fun customBrightnessValue() = this.preferenceStore.getInt(Keys.customBrightnessValue, 0)

    fun colorFilter() = this.preferenceStore.getBoolean(Keys.colorFilter, false)

    fun colorFilterValue() = this.preferenceStore.getInt(Keys.colorFilterValue, 0)

    fun colorFilterMode() = this.preferenceStore.getInt(Keys.colorFilterMode, 0)

    fun defaultReadingMode() =
        this.preferenceStore.getInt(Keys.defaultReadingMode, ReadingModeType.RIGHT_TO_LEFT.flagValue)

    fun defaultOrientationType() =
        this.preferenceStore.getInt(Keys.defaultOrientationType, OrientationType.FREE.flagValue)

    fun imageScaleType() = this.preferenceStore.getInt(Keys.imageScaleType, 1)

    fun doublePageGap() = this.preferenceStore.getInt(Keys.doublePageGap, 0)

    fun zoomStart() = this.preferenceStore.getInt(Keys.zoomStart, 1)

    fun readerTheme() = this.preferenceStore.getInt(Keys.readerTheme, 2)

    fun cropBorders() = this.preferenceStore.getBoolean(Keys.cropBorders, false)

    fun cropBordersWebtoon() = this.preferenceStore.getBoolean(Keys.cropBordersWebtoon, false)

    fun navigateToPan() = this.preferenceStore.getBoolean("navigate_pan", true)

    fun landscapeZoom() = this.preferenceStore.getBoolean("landscape_zoom", false)

    fun grayscale() = this.preferenceStore.getBoolean("pref_grayscale", false)

    fun invertedColors() = this.preferenceStore.getBoolean("pref_inverted_colors", false)

    fun webtoonSidePadding() = this.preferenceStore.getInt(Keys.webtoonSidePadding, 0)

    fun webtoonEnableZoomOut() = this.preferenceStore.getBoolean(Keys.webtoonEnableZoomOut, false)

    fun readWithLongTap() = this.preferenceStore.getBoolean(Keys.readWithLongTap, true)

    fun readWithVolumeKeys() = this.preferenceStore.getBoolean(Keys.readWithVolumeKeys, false)

    fun readWithVolumeKeysInverted() = this.preferenceStore.getBoolean(Keys.readWithVolumeKeysInverted, false)

    fun navigationModePager() = this.preferenceStore.getInt(Keys.navigationModePager, 0)

    fun navigationModeWebtoon() = this.preferenceStore.getInt(Keys.navigationModeWebtoon, 0)

    fun pagerNavInverted() =
        this.preferenceStore.getEnum(Keys.pagerNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun webtoonNavInverted() =
        this.preferenceStore.getEnum(Keys.webtoonNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun pageLayout() = this.preferenceStore.getInt(Keys.pageLayout, PageLayout.AUTOMATIC.value)

    fun automaticSplitsPage() = this.preferenceStore.getBoolean(Keys.automaticSplitsPage, false)

    fun invertDoublePages() = this.preferenceStore.getBoolean(Keys.invertDoublePages, false)

    fun webtoonPageLayout() = this.preferenceStore.getInt(Keys.webtoonPageLayout, PageLayout.SINGLE_PAGE.value)

    fun webtoonReaderHideThreshold() = this.preferenceStore.getEnum("reader_hide_threshold", Values.ReaderHideThreshold.LOW)

    fun webtoonInvertDoublePages() = this.preferenceStore.getBoolean(Keys.webtoonInvertDoublePages, false)

    fun readerBottomButtons() = this.preferenceStore.getStringSet(
        Keys.readerBottomButtons,
        ReaderBottomButton.BUTTONS_DEFAULTS,
    )

    fun showNavigationOverlayNewUser() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUser, true)

    fun showNavigationOverlayNewUserWebtoon() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUserWebtoon, true)

    fun preloadSize() = this.preferenceStore.getInt(Keys.preloadSize, 6)

    fun updateOnlyNonCompleted() = this.preferenceStore.getBoolean(Keys.updateOnlyNonCompleted, false)

    fun updateOnlyWhenTrackingIsNotFinished() = this.preferenceStore.getBoolean(Keys.updateOnlyWhenTrackingIsNotFinished, false)

    fun autoUpdateTrack() = this.preferenceStore.getBoolean(Keys.autoUpdateTrack, true)

    fun trackMarkedAsRead() = this.preferenceStore.getBoolean(Keys.trackMarkedAsRead, false)

    fun trackingsToAddOnline() = this.preferenceStore.getStringSet(Keys.trackingsToAddOnline, emptySet())

    fun lastUsedCatalogueSource() = this.preferenceStore.getLong(Keys.lastUsedCatalogueSource, -1)

    fun lastUsedCategory() = this.preferenceStore.getInt(Keys.lastUsedCategory, 0)

    fun lastUsedSources() = this.preferenceStore.getStringSet("last_used_sources", emptySet())

    fun lastVersionCode() = this.preferenceStore.getInt("last_version_code", 0)

    fun browseAsList() = this.preferenceStore.getBoolean(Keys.catalogueAsList, false)

    fun browseShowLibrary() = this.preferenceStore.getBoolean(Keys.catalogueShowLibrary, true)

    fun enabledLanguages() =
        this.preferenceStore.getStringSet(Keys.enabledLanguages, setOf("en", Locale.getDefault().language))

    fun sourceSorting() = this.preferenceStore.getInt(Keys.sourcesSort, 0)

    fun sourceUsername(source: Source) = this.preferenceStore.getString(Keys.sourceUsername(source.id), "")

    fun sourcePassword(source: Source) = this.preferenceStore.getString(Keys.sourcePassword(source.id), "")

    fun sourceUrl(source: Source) = this.preferenceStore.getString(Keys.sourceUrl(source.id), "")

    fun mangaDexUserName() = this.preferenceStore.getString(Keys.mangadexUserName, "")

    fun removeMangaDexUserName() = mangaDexUserName().delete()

    fun removeOldCredentials(source: Source) {

        this.preferenceStore.getString(Keys.sourceUsername(source.id)).delete()
        this.preferenceStore.getString(Keys.sourcePassword(source.id)).delete()
    }

    fun setKomgaCredentials(source: Source, username: String, password: String, url: String) {
        this.preferenceStore.getString(Keys.sourceUsername(source.id)).set(username)
        this.preferenceStore.getString(Keys.sourcePassword(source.id)).set(password)
        this.preferenceStore.getString(Keys.sourceUrl(source.id)).set(url)
    }

    fun trackUsername(sync: TrackService) = this.preferenceStore.getString(Keys.trackUsername(sync.id))

    fun trackPassword(sync: TrackService) = this.preferenceStore.getString(Keys.trackPassword(sync.id))

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        this.preferenceStore.getString(Keys.trackUsername(sync.id)).set(username)
        this.preferenceStore.getString(Keys.trackPassword(sync.id)).set(password)
    }

    fun trackToken(sync: TrackService) = this.preferenceStore.getString(Keys.trackToken(sync.id))

    fun anilistScoreType() = this.preferenceStore.getString("anilist_score_type", "POINT_10")

    fun backupsDirectory() = this.preferenceStore.getString(Keys.backupDirectory, defaultBackupDir.toString())

    fun dateFormat(format: String = this.preferenceStore.getString(Keys.dateFormat, "").get()): DateFormat =
        when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }

    fun downloadsDirectory() =
        this.preferenceStore.getString(Keys.downloadsDirectory, defaultDownloadsDir.toString())

    fun downloadOnlyOverWifi() = this.preferenceStore.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun folderPerManga() = this.preferenceStore.getBoolean(Keys.folderPerManga, false)

    fun librarySearchSuggestion() = this.preferenceStore.getString(Keys.librarySearchSuggestion, "")

    fun showLibrarySearchSuggestions() =
        this.preferenceStore.getBoolean(Keys.showLibrarySearchSuggestions, false)

    fun lastLibrarySuggestion() = this.preferenceStore.getLong("last_library_suggestion", 0L)

    fun numberOfBackups() = this.preferenceStore.getInt(Keys.numberOfBackups, 2)

    fun backupInterval() = this.preferenceStore.getInt(Keys.backupInterval, 0)

    fun removeAfterReadSlots() = this.preferenceStore.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = this.preferenceStore.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun libraryUpdateInterval() = this.preferenceStore.getInt(Keys.libraryUpdateInterval, 24)

    fun libraryUpdateLastTimestamp() = this.preferenceStore.getLong("library_update_last_timestamp", 0L)

    fun libraryUpdateDeviceRestriction() =
        this.preferenceStore.getStringSet(Keys.libraryUpdateDeviceRestriction, emptySet())

    fun libraryUpdateCategories() = this.preferenceStore.getStringSet(Keys.libraryUpdateCategories, emptySet())
    fun libraryUpdateCategoriesExclude() = this.preferenceStore.getStringSet(Keys.libraryUpdateCategoriesExclude, emptySet())

    fun libraryUpdatePrioritization() = this.preferenceStore.getInt(Keys.libraryUpdatePrioritization, 0)

    fun libraryLayout() = this.preferenceStore.getInt(Keys.libraryLayout, LibraryItem.LAYOUT_COMFORTABLE_GRID)

    fun gridSize() = this.preferenceStore.getFloat(Keys.gridSize, 1f)

    fun uniformGrid() = this.preferenceStore.getBoolean(Keys.uniformGrid, true)

    fun outlineOnCovers() = this.preferenceStore.getBoolean(Keys.outlineOnCovers, true)

    fun downloadBadge() = this.preferenceStore.getBoolean(Keys.downloadBadge, false)

    fun filterDownloaded() = this.preferenceStore.getInt(Keys.filterDownloaded, 0)

    fun filterUnread() = this.preferenceStore.getInt(Keys.filterUnread, 0)

    fun filterCompleted() = this.preferenceStore.getInt(Keys.filterCompleted, 0)

    fun filterBookmarked() = this.preferenceStore.getInt("pref_filter_bookmarked_key", 0)

    fun filterTracked() = this.preferenceStore.getInt(Keys.filterTracked, 0)

    fun filterMangaType() = this.preferenceStore.getInt(Keys.filterMangaType, 0)

    fun showEmptyCategoriesWhileFiltering() =
        this.preferenceStore.getBoolean(Keys.showEmptyCategoriesFiltering, false)

    fun filterMerged() = this.preferenceStore.getInt(Keys.filterMerged, 0)

    fun filterMissingChapters() = this.preferenceStore.getInt(Keys.filterMissingChapters, 0)

    fun librarySortingMode() = this.preferenceStore.getInt(Keys.librarySortingMode, 0)

    fun librarySortingAscending() = this.preferenceStore.getBoolean("library_sorting_ascending", true)

    fun automaticUpdates() = this.preferenceStore.getBoolean(Keys.automaticUpdates, true)

    fun collapsedCategories() = this.preferenceStore.getStringSet("collapsed_categories", mutableSetOf())

    fun collapsedDynamicCategories() =
        this.preferenceStore.getStringSet("collapsed_dynamic_categories", mutableSetOf())

    fun collapsedDynamicAtBottom() = this.preferenceStore.getBoolean("collapsed_dynamic_at_bottom", false)

    fun hiddenSources() = this.preferenceStore.getStringSet("hidden_catalogues", mutableSetOf())

    fun pinnedCatalogues() = this.preferenceStore.getStringSet("pinned_catalogues", mutableSetOf())

    fun saveChaptersAsCBZ() = this.preferenceStore.getBoolean("save_chapter_as_cbz", true)

    fun splitTallImages() = this.preferenceStore.getBoolean("split_tall_images", false)

    fun doublePageRotate() = this.preferenceStore.getBoolean("double_page_rotate", false)

    fun doublePageRotateReverse() = this.preferenceStore.getBoolean("double_page_rotate_reverse", false)

    fun downloadNewChapters() = this.preferenceStore.getBoolean(Keys.downloadNew, false)

    fun downloadNewChaptersInCategories() = this.preferenceStore.getStringSet(Keys.downloadNewCategories, emptySet())
    fun excludeCategoriesInDownloadNew() = this.preferenceStore.getStringSet(Keys.downloadNewCategoriesExclude, emptySet())

    fun lang() = this.preferenceStore.getString(Keys.lang, "")

    fun langsToShow() = this.preferenceStore.getString(Keys.langToShow, "en")

    fun autoDownloadWhileReading() = this.preferenceStore.getInt("auto_download_while_reading", 0)

    fun defaultCategory() = this.preferenceStore.getInt(Keys.defaultCategory, -1)

    fun skipRead() = this.preferenceStore.getBoolean(Keys.skipRead, false)

    fun skipFiltered() = this.preferenceStore.getBoolean(Keys.skipFiltered, true)

    fun skipDuplicates() = this.preferenceStore.getBoolean(Keys.skipDuplicates, false)

    fun removeArticles() = this.preferenceStore.getBoolean(Keys.removeArticles, false)

    fun migrateFlags() = this.preferenceStore.getInt("migrate_flags", Int.MAX_VALUE)

    fun trustedSignatures() = this.preferenceStore.getStringSet("trusted_signatures", emptySet())

    // using string instead of set so it is ordered
    fun migrationSources() = this.preferenceStore.getString("migrate_sources", "")

    fun useSourceWithMost() = this.preferenceStore.getBoolean("use_source_with_most", false)

    fun skipPreMigration() = this.preferenceStore.getBoolean(Keys.skipPreMigration, false)

    fun defaultMangaOrder() = this.preferenceStore.getString("default_manga_order", "")

    fun refreshCoversToo() = this.preferenceStore.getBoolean(Keys.refreshCoversToo, true)

    fun updateOnRefresh() = this.preferenceStore.getInt(Keys.updateOnRefresh, -1)

    fun recentsViewType() = this.preferenceStore.getInt("recents_view_type", 0)

    fun showRecentsDownloads() =
        this.preferenceStore.getEnum(Keys.showDLsInRecents, RecentMangaAdapter.ShowRecentsDLs.All)

    fun showRecentsRemHistory() = this.preferenceStore.getBoolean(Keys.showRemHistoryInRecents, true)

    fun showReadInAllRecents() = this.preferenceStore.getBoolean(Keys.showReadInAllRecents, false)

    fun showUpdatedTime() = this.preferenceStore.getBoolean(Keys.showUpdatedTime, false)

    fun sortFetchedTime() = this.preferenceStore.getBoolean("sort_fetched_time", false)

    fun groupChaptersUpdates() = this.preferenceStore.getBoolean(Keys.groupChaptersUpdates, false)

    fun groupChaptersHistory() = this.preferenceStore.getBoolean(Keys.groupChaptersHistory, true)

    fun showTitleFirstInRecents() = this.preferenceStore.getBoolean(Keys.showTitleFirstInRecents, false)

    fun lastExtCheck() = this.preferenceStore.getLong("last_ext_check", 0)

    fun lastAppCheck() = this.preferenceStore.getLong("last_app_check", 0)

    fun unreadBadgeType() = this.preferenceStore.getInt("unread_badge_type", 2)

    fun categoryNumberOfItems() = this.preferenceStore.getBoolean(Keys.categoryNumberOfItems, false)

    fun hideStartReadingButton() = this.preferenceStore.getBoolean("hide_reading_button", false)

    fun alwaysShowChapterTransition() = this.preferenceStore.getBoolean(Keys.alwaysShowChapterTransition, true)

    fun deleteRemovedChapters() = this.preferenceStore.getInt(Keys.deleteRemovedChapters, 0)

    fun showAllCategories() = this.preferenceStore.getBoolean("show_all_categories", true)

    fun showAllCategoriesWhenSearchingSingleCategory() = this.preferenceStore.getBoolean("show_all_categories_when_searching_single_category", false)

    fun hopperGravity() = this.preferenceStore.getInt("hopper_gravity", 1)

    fun filterOrder() = this.preferenceStore.getString("filter_order", FilterBottomSheet.Filters.DEFAULT_ORDER)

    fun hopperLongPressAction() = this.preferenceStore.getInt(Keys.hopperLongPress, 0)

    fun hideHopper() = this.preferenceStore.getBoolean("hide_hopper", false)

    fun hideButtonText() = this.preferenceStore.getBoolean(Keys.hideMangaDetailButtonText, false)

    fun extraLargeBackdrop() = this.preferenceStore.getBoolean(Keys.extraLargeBackdrop, false)

    fun wrapAltTitles() = this.preferenceStore.getBoolean(Keys.wrapAltTitles, false)

    fun fasterLibraryUpdates() = this.preferenceStore.getBoolean(Keys.fasterLibraryUpdates, false)

    fun autohideHopper() = this.preferenceStore.getBoolean(Keys.autoHideHopper, true)

    fun groupLibraryBy() = this.preferenceStore.getInt("group_library_by", 0)

    fun showCategoryInTitle() = this.preferenceStore.getBoolean("category_in_title", false)

    fun onlySearchPinned() = this.preferenceStore.getBoolean(Keys.onlySearchPinned, false)

    // Tutorial preferences
    fun shownFilterTutorial() = this.preferenceStore.getBoolean("shown_filter_tutorial", false)

    fun shownChapterSwipeTutorial() = this.preferenceStore.getBoolean("shown_swipe_tutorial", false)

    fun shownDownloadQueueTutorial() = this.preferenceStore.getBoolean("shown_download_queue", false)

    fun shownLongPressCategoryTutorial() = this.preferenceStore.getBoolean("shown_long_press_category", false)

    fun shownHopperSwipeTutorial() = this.preferenceStore.getBoolean("shown_hopper_swipe", false)

    fun shownDownloadSwipeTutorial() = this.preferenceStore.getBoolean("shown_download_tutorial", false)

    fun hideBottomNavOnScroll() = this.preferenceStore.getBoolean("false_key") // this.preferenceStore.getBoolean(Keys.hideBottomNavOnScroll, false)

    fun sideNavIconAlignment() = this.preferenceStore.getInt(Keys.sideNavIconAlignment, 1)

    fun themeMangaDetails() = this.preferenceStore.getBoolean(Keys.themeMangaDetails, true)

    fun useLargeToolbar() = this.preferenceStore.getBoolean("use_large_toolbar", false)

    fun dohProvider() = this.preferenceStore.getInt(Keys.dohProvider, -1)

    fun showSeriesInShortcuts() = this.preferenceStore.getBoolean(Keys.showSeriesInShortcuts, true)
    fun openChapterInShortcuts() = this.preferenceStore.getBoolean(Keys.openChapterInShortcuts, true)

    fun hasPromptedBeforeUpdateAll() = this.preferenceStore.getBoolean("has_prompted_update_all", false)

    fun sideNavMode() = this.preferenceStore.getInt(Keys.sideNavMode, 0)

    fun appShouldAutoUpdate() =
        this.preferenceStore.getInt(Keys.shouldAutoUpdate, AutoAppUpdaterJob.ONLY_ON_UNMETERED)

    fun filterChapterByRead() = this.preferenceStore.getInt(Keys.defaultChapterFilterByRead, Manga.SHOW_ALL)

    fun filterChapterByDownloaded() =
        this.preferenceStore.getInt(Keys.defaultChapterFilterByDownloaded, Manga.SHOW_ALL)

    fun filterChapterByBookmarked() =
        this.preferenceStore.getInt(Keys.defaultChapterFilterByBookmarked, Manga.SHOW_ALL)

    fun sortChapterOrder() =
        this.preferenceStore.getInt(Keys.defaultChapterSortBySourceOrNumber, Manga.CHAPTER_SORTING_SOURCE)

    fun hideChapterTitlesByDefault() = this.preferenceStore.getBoolean(Keys.hideChapterTitles, false)

    fun chaptersDescAsDefault() = this.preferenceStore.getBoolean(Keys.chaptersDescAsDefault, true)

    fun sortChapterByAscendingOrDescending() =
        this.preferenceStore.getInt(Keys.defaultChapterSortByAscendingOrDescending, Manga.CHAPTER_SORT_DESC)

    fun blockedScanlators() =
        this.preferenceStore.getStringSet(Keys.blockedScanlators, emptySet())

    fun coverRatios() = this.preferenceStore.getStringSet(Keys.coverRatios, emptySet())

    fun coverColors() = this.preferenceStore.getStringSet(Keys.coverColors, emptySet())

    fun coverVibrantColors() = this.preferenceStore.getStringSet(Keys.coverVibrantColors, emptySet())

    fun useStaggeredGrid() = this.preferenceStore.getBoolean("use_staggered_grid", false)

    fun dataSaver() = this.preferenceStore.getBoolean(Keys.dataSaver, false)

    fun thumbnailQuality() = this.preferenceStore.getInt(Keys.thumbnailQuality, 0)

    fun usePort443Only() = this.preferenceStore.getBoolean(Keys.enablePort443Only, false)

    fun verboseLogging() = this.preferenceStore.getBoolean(Keys.verboseLogging, BuildConfig.DEBUG)

    fun showContentRatingFilter() = this.preferenceStore.getBoolean(Keys.showContentRatingFilter, true)

    fun addToLibraryAsPlannedToRead() = this.preferenceStore.getBoolean(Keys.addToLibraryAsPlannedToRead, false)

    fun contentRatingSelections() = this.preferenceStore.getStringSet(Keys.contentRating, setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive))

    fun autoTrackContentRatingSelections() =
        this.preferenceStore.getStringSet(
            Keys.autoTrackContentRating,
            setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive, MdConstants.ContentRating.erotica, MdConstants.ContentRating.pornographic),
        )

    fun autoAddTracker() = this.preferenceStore.getStringSet(Keys.autoAddTracker, setOf(TrackManager.MDLIST.toString()))

    fun setAutoAddTracker(trackersToAutoAdd: Set<String>) {
        autoAddTracker().set(trackersToAutoAdd)
    }

    fun sessionToken() = this.preferenceStore.getString(Keys.sessionToken, "")

    fun refreshToken() = this.preferenceStore.getString(Keys.refreshToken, "")

    fun removeTokens() {
        sessionToken().delete()
        refreshToken().delete()
        lastRefreshTime().delete()
        codeVerifier().delete()
    }

    fun setTokens(refresh: String, session: String) {
        val time = if (refresh.isBlank() && session.isBlank()) {
            0
        } else {
            System.currentTimeMillis()
        }

        sessionToken().set(session)
        refreshToken().set(refresh)
        lastRefreshTime().set(time)
    }

    fun lastRefreshTime() = this.preferenceStore.getLong(Keys.lastRefreshTokenTime, 0)
    fun readingSync() = this.preferenceStore.getBoolean(Keys.readingSync, false)

    fun mangadexSyncToLibraryIndexes() =
        this.preferenceStore.getStringSet(Keys.mangadexSyncToLibraryIndexes, emptySet())

    fun codeVerifier(): Preference<String> {
        val codeVerifier = this.preferenceStore.getString(Keys.mangadexCodeVerifier)
        return when (codeVerifier.get().isEmpty()) {
            false -> codeVerifier
            true -> {
                val secureRandom = SecureRandom()
                val bytes = ByteArray(64)
                secureRandom.nextBytes(bytes)
                val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                val newCodeVerifier = Base64.encodeToString(bytes, encoding)
                this.preferenceStore.getString(Keys.mangadexCodeVerifier).set(newCodeVerifier)
                this.preferenceStore.getString(Keys.mangadexCodeVerifier)
            }

        }
    }
}
