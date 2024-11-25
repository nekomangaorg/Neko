package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.recents.RecentMangaAdapter
import java.security.SecureRandom
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.theme.Themes
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

    fun getInt(key: String, default: Int) = this.preferenceStore.getInt(key, default)

    fun getStringPref(key: String, default: String = "") =
        this.preferenceStore.getString(key, default)

    fun startingTab() = this.preferenceStore.getInt(Keys.startingTab, 0)

    fun backReturnsToStart() = this.preferenceStore.getBoolean(Keys.backToStart, true)

    fun hasShownNotifPermission() =
        this.preferenceStore.getBoolean("has_shown_notification_permission", false)

    fun hasShownOnboarding() =
        preferenceStore.getBoolean(Preference.appStateKey("onboarding_complete"), false)

    fun nightMode() =
        this.preferenceStore.getInt(Keys.nightMode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private val supportsDynamic = DynamicColors.isDynamicColorAvailable()

    fun lightTheme() =
        this.preferenceStore.getEnum(
            Keys.lightTheme,
            if (supportsDynamic) Themes.Monet else Themes.Neko,
        )

    fun darkTheme() =
        this.preferenceStore.getEnum(
            Keys.darkTheme,
            if (supportsDynamic) Themes.Monet else Themes.Neko,
        )

    fun showNavigationOverlayNewUser() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUser, true)

    fun showNavigationOverlayNewUserWebtoon() =
        this.preferenceStore.getBoolean(Keys.showNavigationOverlayNewUserWebtoon, true)

    fun autoUpdateTrack() = this.preferenceStore.getBoolean(Keys.autoUpdateTrack, true)

    fun trackMarkedAsRead() = this.preferenceStore.getBoolean(Keys.trackMarkedAsRead, false)

    fun lastVersionCode() = this.preferenceStore.getInt("last_version_code", 0)

    fun browseAsList() = this.preferenceStore.getBoolean(Keys.catalogueAsList, false)

    fun browseShowLibrary() = this.preferenceStore.getBoolean(Keys.catalogueShowLibrary, true)

    fun sourceUsername(source: Source) =
        this.preferenceStore.getString(Keys.sourceUsername(source.id), "")

    fun sourcePassword(source: Source) =
        this.preferenceStore.getString(Keys.sourcePassword(source.id), "")

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

    fun trackUsername(sync: TrackService) =
        this.preferenceStore.getString(Keys.trackUsername(sync.id))

    fun trackPassword(sync: TrackService) =
        this.preferenceStore.getString(Keys.trackPassword(sync.id))

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        this.preferenceStore.getString(Keys.trackUsername(sync.id)).set(username)
        this.preferenceStore.getString(Keys.trackPassword(sync.id)).set(password)
        this.preferenceStore.getBoolean("track_token_expired_${sync.id}").set(false)
    }

    fun trackToken(sync: TrackService) = this.preferenceStore.getString(Keys.trackToken(sync.id))

    fun trackAuthExpired(tracker: TrackService) =
        preferenceStore.getBoolean("track_token_expired_${tracker.id}", false)

    fun anilistScoreType() = this.preferenceStore.getString("anilist_score_type", "POINT_10")

    fun dateFormat(
        format: String = this.preferenceStore.getString(Keys.dateFormat, "").get()
    ): DateFormat =
        when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }

    fun downloadOnlyOverWifi() = this.preferenceStore.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun folderPerManga() = this.preferenceStore.getBoolean(Keys.folderPerManga, false)

    fun removeAfterReadSlots() = this.preferenceStore.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() =
        this.preferenceStore.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun saveChaptersAsCBZ() = this.preferenceStore.getBoolean("save_chapter_as_cbz", true)

    fun downloadNewChapters() = this.preferenceStore.getBoolean("download_new")

    fun downloadNewChaptersInCategories() =
        this.preferenceStore.getStringSet("download_new_categories")

    fun excludeCategoriesInDownloadNew() =
        this.preferenceStore.getStringSet("download_new_categories_exclude")

    fun langsToShow() = this.preferenceStore.getString(Keys.langToShow, "en")

    fun autoDownloadWhileReading() = this.preferenceStore.getInt("auto_download_while_reading", 0)

    fun defaultCategory() = this.preferenceStore.getInt(Keys.defaultCategory, -1)

    fun recentsViewType() = this.preferenceStore.getInt("recents_view_type", 0)

    fun showRecentsDownloads() =
        this.preferenceStore.getEnum(Keys.showDLsInRecents, RecentMangaAdapter.ShowRecentsDLs.All)

    fun showRecentsRemHistory() =
        this.preferenceStore.getBoolean(Keys.showRemHistoryInRecents, true)

    fun showReadInAllRecents() = this.preferenceStore.getBoolean(Keys.showReadInAllRecents, false)

    fun showUpdatedTime() = this.preferenceStore.getBoolean(Keys.showUpdatedTime, false)

    fun sortFetchedTime() = this.preferenceStore.getBoolean("sort_fetched_time", false)

    fun groupChaptersUpdates() = this.preferenceStore.getBoolean(Keys.groupChaptersUpdates, false)

    fun groupChaptersHistory() = this.preferenceStore.getBoolean(Keys.groupChaptersHistory, true)

    fun showTitleFirstInRecents() =
        this.preferenceStore.getBoolean(Keys.showTitleFirstInRecents, false)

    fun lastAppCheck() = this.preferenceStore.getLong("last_app_check", 0)

    fun deleteRemovedChapters() = this.preferenceStore.getInt(Keys.deleteRemovedChapters, 0)

    // Tutorial preferences
    fun shownFilterTutorial() = this.preferenceStore.getBoolean("shown_filter_tutorial", false)

    fun shownLongPressCategoryTutorial() =
        this.preferenceStore.getBoolean("shown_long_press_category")

    fun shownHopperSwipeTutorial() = this.preferenceStore.getBoolean("shown_hopper_swipe")

    fun shownChapterSwipeTutorial() = this.preferenceStore.getBoolean("shown_swipe_tutorial", false)

    fun shownDownloadQueueTutorial() =
        this.preferenceStore.getBoolean("shown_download_queue", false)

    fun shownDownloadSwipeTutorial() =
        this.preferenceStore.getBoolean("shown_download_tutorial", false)

    fun hideBottomNavOnScroll() =
        this.preferenceStore.getBoolean(
            "false_key"
        ) // this.preferenceStore.getBoolean(Keys.hideBottomNavOnScroll, false)

    fun sideNavIconAlignment() = this.preferenceStore.getInt(Keys.sideNavIconAlignment, 1)

    fun useLargeToolbar() = this.preferenceStore.getBoolean("use_large_toolbar", false)

    fun showSeriesInShortcuts() = this.preferenceStore.getBoolean(Keys.showSeriesInShortcuts, true)

    fun openChapterInShortcuts() =
        this.preferenceStore.getBoolean(Keys.openChapterInShortcuts, true)

    fun sideNavMode() = this.preferenceStore.getInt(Keys.sideNavMode, 0)

    fun appShouldAutoUpdate() =
        this.preferenceStore.getInt(Keys.shouldAutoUpdate, AppDownloadInstallJob.ONLY_ON_UNMETERED)

    fun blockedScanlators() = this.preferenceStore.getStringSet(Keys.blockedScanlators, emptySet())

    fun dataSaver() = this.preferenceStore.getBoolean(Keys.dataSaver, false)

    fun thumbnailQuality() = this.preferenceStore.getInt(Keys.thumbnailQuality, 0)

    fun usePort443Only() = this.preferenceStore.getBoolean(Keys.enablePort443Only, false)

    fun showContentRatingFilter() =
        this.preferenceStore.getBoolean(Keys.showContentRatingFilter, true)

    fun addToLibraryAsPlannedToRead() =
        this.preferenceStore.getBoolean(Keys.addToLibraryAsPlannedToRead, false)

    fun contentRatingSelections() =
        this.preferenceStore.getStringSet(
            Keys.contentRating,
            setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive),
        )

    fun autoTrackContentRatingSelections() =
        this.preferenceStore.getStringSet(
            Keys.autoTrackContentRating,
            setOf(
                MdConstants.ContentRating.safe,
                MdConstants.ContentRating.suggestive,
                MdConstants.ContentRating.erotica,
                MdConstants.ContentRating.pornographic,
            ),
        )

    fun autoAddTracker() =
        this.preferenceStore.getStringSet(
            Keys.autoAddTracker,
            setOf(TrackManager.MDLIST.toString()),
        )

    fun setAutoAddTracker(trackersToAutoAdd: Set<String>) {
        autoAddTracker().set(trackersToAutoAdd)
    }

    fun sessionToken() = this.preferenceStore.getString(Keys.sessionToken)

    fun refreshToken() = this.preferenceStore.getString(Keys.refreshToken)

    fun removeTokens() {
        sessionToken().delete()
        refreshToken().delete()
        lastRefreshTime().delete()
        codeVerifier().delete()
    }

    fun setTokens(refresh: String, session: String) {
        val time =
            if (refresh.isBlank() && session.isBlank()) {
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
