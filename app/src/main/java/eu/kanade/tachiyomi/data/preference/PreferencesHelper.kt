package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences.Preference
import com.f2prateek.rx.preferences.RxSharedPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.Mangadex
import java.io.File
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

fun <T> Preference<T>.getOrDefault(): T = get() ?: defaultValue()!!

fun Preference<Boolean>.invert(): Boolean = getOrDefault().let { set(!it); !it }

class PreferencesHelper(val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val rxPrefs = RxSharedPreferences.create(prefs)

    private val defaultDownloadsDir = Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name), "downloads"))

    private val defaultRelatedFilepath = Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator,
                    "downloads" + File.separator + "mangas.json"))

    private val defaultBackupDir = Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name), "backup"))

    fun getInt(key: String, default: Int?) = rxPrefs.getInteger(key, default)
    fun getStringPref(key: String, default: String?) = rxPrefs.getString(key, default)
    fun getStringSet(key: String, default: Set<String>) = rxPrefs.getStringSet(key, default)

    fun startScreen() = prefs.getInt(Keys.startScreen, 1)

    fun clear() = prefs.edit().clear().apply()

    fun theme() = prefs.getInt(Keys.theme, 1)

    fun rotation() = rxPrefs.getInteger(Keys.rotation, 1)

    fun pageTransitions() = rxPrefs.getBoolean(Keys.enableTransitions, true)

    fun doubleTapAnimSpeed() = rxPrefs.getInteger(Keys.doubleTapAnimationSpeed, 500)

    fun showPageNumber() = rxPrefs.getBoolean(Keys.showPageNumber, true)

    fun r18() = prefs.getString(Keys.showR18, "0")

    fun imageServer() = prefs.getString(Keys.imageServer, Mangadex.SERVER_PREF_ENTRY_VALUES.first())


    fun trueColor() = rxPrefs.getBoolean(Keys.trueColor, false)

    fun fullscreen() = rxPrefs.getBoolean(Keys.fullscreen, true)

    fun keepScreenOn() = rxPrefs.getBoolean(Keys.keepScreenOn, true)

    fun customBrightness() = rxPrefs.getBoolean(Keys.customBrightness, false)

    fun customBrightnessValue() = rxPrefs.getInteger(Keys.customBrightnessValue, 0)

    fun colorFilter() = rxPrefs.getBoolean(Keys.colorFilter, false)

    fun colorFilterValue() = rxPrefs.getInteger(Keys.colorFilterValue, 0)

    fun colorFilterMode() = rxPrefs.getInteger(Keys.colorFilterMode, 0)

    fun defaultViewer() = prefs.getInt(Keys.defaultViewer, 1)

    fun imageScaleType() = rxPrefs.getInteger(Keys.imageScaleType, 1)

    fun zoomStart() = rxPrefs.getInteger(Keys.zoomStart, 1)

    fun readerTheme() = rxPrefs.getInteger(Keys.readerTheme, 0)

    fun cropBorders() = rxPrefs.getBoolean(Keys.cropBorders, false)

    fun cropBordersWebtoon() = rxPrefs.getBoolean(Keys.cropBordersWebtoon, false)

    fun marginRatioWebtoon() = rxPrefs.getInteger(Keys.marginRatioWebtoon, 0)

    fun marginBetweenPagesWebtoon() = rxPrefs.getBoolean(Keys.marginBetweenPagesWebtoon, false)

    fun readWithTapping() = rxPrefs.getBoolean(Keys.readWithTapping, true)

    fun readWithLongTap() = rxPrefs.getBoolean(Keys.readWithLongTap, true)

    fun readWithVolumeKeys() = rxPrefs.getBoolean(Keys.readWithVolumeKeys, false)

    fun readWithVolumeKeysInverted() = rxPrefs.getBoolean(Keys.readWithVolumeKeysInverted, false)

    fun portraitColumns() = rxPrefs.getInteger(Keys.portraitColumns, 0)

    fun landscapeColumns() = rxPrefs.getInteger(Keys.landscapeColumns, 0)

    fun updateOnlyNonCompleted() = prefs.getBoolean(Keys.updateOnlyNonCompleted, false)

    fun autoUpdateTrack() = prefs.getBoolean(Keys.autoUpdateTrack, true)

    fun lastUsedCatalogueSource() = rxPrefs.getLong(Keys.lastUsedCatalogueSource, -1)

    fun lastUsedCategory() = rxPrefs.getInteger(Keys.lastUsedCategory, 0)

    fun lastVersionCode() = rxPrefs.getInteger("last_version_code", 0)

    fun catalogueAsList() = rxPrefs.getBoolean(Keys.catalogueAsList, false)

    fun enabledLanguages() = rxPrefs.getStringSet(Keys.enabledLanguages, setOf("en"))

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

    fun trackToken(sync: TrackService) = rxPrefs.getString(Keys.trackToken(sync.id), "")

    fun anilistScoreType() = rxPrefs.getString("anilist_score_type", "POINT_10")

    fun backupsDirectory() = rxPrefs.getString(Keys.backupDirectory, defaultBackupDir.toString())

    fun downloadsDirectory() = rxPrefs.getString(Keys.downloadsDirectory, defaultDownloadsDir.toString())

    fun downloadOnlyOverWifi() = prefs.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun relatedFilePath() = rxPrefs.getString(Keys.relatedFilePath, defaultRelatedFilepath.toString())

    fun relatedShowTab() = prefs.getBoolean(Keys.relatedShowTab, false)

    fun numberOfBackups() = rxPrefs.getInteger(Keys.numberOfBackups, 1)

    fun backupInterval() = rxPrefs.getInteger(Keys.backupInterval, 0)

    fun removeAfterReadSlots() = prefs.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = prefs.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun libraryUpdateInterval() = rxPrefs.getInteger(Keys.libraryUpdateInterval, 0)

    fun libraryUpdateRestriction() = prefs.getStringSet(Keys.libraryUpdateRestriction, emptySet())

    fun libraryUpdateCategories() = rxPrefs.getStringSet(Keys.libraryUpdateCategories, emptySet())

    fun libraryAsList() = rxPrefs.getBoolean(Keys.libraryAsList, false)

    fun filterDownloaded() = rxPrefs.getInteger(Keys.filterDownloaded, 0)

    fun filterUnread() = rxPrefs.getInteger(Keys.filterUnread, 0)

    fun filterCompleted() = rxPrefs.getInteger(Keys.filterCompleted, 0)

    fun filterTracked() = rxPrefs.getInteger(Keys.filterTracked, 0)

    fun librarySortingMode() = rxPrefs.getInteger(Keys.librarySortingMode, 0)

    fun librarySortingAscending() = rxPrefs.getBoolean("library_sorting_ascending", true)

    fun automaticUpdates() = prefs.getBoolean(Keys.automaticUpdates, true)

    fun hiddenCatalogues() = rxPrefs.getStringSet("hidden_catalogues", emptySet())

    fun downloadNew() = rxPrefs.getBoolean(Keys.downloadNew, false)

    fun downloadNewCategories() = rxPrefs.getStringSet(Keys.downloadNewCategories, emptySet())

    fun lang() = prefs.getString(Keys.lang, "")

    fun defaultCategory() = prefs.getInt(Keys.defaultCategory, -1)

    fun skipRead() = prefs.getBoolean(Keys.skipRead, false)

    fun useBiometrics() = rxPrefs.getBoolean(Keys.useBiometrics, false)

    fun lockAfter() = rxPrefs.getInteger(Keys.lockAfter, 0)

    fun lastUnlock() = rxPrefs.getLong(Keys.lastUnlock, 0)

    fun skipHidden() = prefs.getBoolean(Keys.skipHidden, false)

    fun migrateFlags() = rxPrefs.getInteger("migrate_flags", Int.MAX_VALUE)

    fun trustedSignatures() = rxPrefs.getStringSet("trusted_signatures", emptySet())

    fun upgradeFilters() {
        val filterDl = rxPrefs.getBoolean(Keys.filterDownloaded, false).getOrDefault()
        val filterUn = rxPrefs.getBoolean(Keys.filterUnread, false).getOrDefault()
        val filterCm = rxPrefs.getBoolean(Keys.filterCompleted, false).getOrDefault()
        filterDownloaded().set(if (filterDl) 1 else 0)
        filterUnread().set(if (filterUn) 1 else 0)
        filterCompleted().set(if (filterCm) 1 else 0)
    }
}
