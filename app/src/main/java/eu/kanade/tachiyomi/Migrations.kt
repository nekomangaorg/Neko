package eu.kanade.tachiyomi

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.updater.AppUpdateJob
import eu.kanade.tachiyomi.data.updater.AppUpdateService
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.toast
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper, scope: CoroutineScope): Boolean {
        val context = preferences.context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            remove(AppUpdateService.NOTIFY_ON_INSTALL_KEY)
        }
        val oldVersion = preferences.lastVersionCode().get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            // Always set up background tasks to ensure they're running
            if (BuildConfig.INCLUDE_UPDATER) {
                AppUpdateJob.setupTask(context)
            }
            LibraryUpdateJob.setupTask(context)
            BackupCreatorJob.setupTask(context)

            if (oldVersion == 0) {
                return BuildConfig.DEBUG
            }

            if (oldVersion < 39) {
                if (BuildConfig.INCLUDE_UPDATER) {
                    AppUpdateJob.setupTask(context)
                }
            }
            if (oldVersion < 53) {
                LibraryUpdateJob.setupTask(context)
                BackupCreatorJob.setupTask(context)
            }
            if (oldVersion < 95) {
                // Force MAL log out due to login flow change
                val trackManager = Injekt.get<TrackManager>()
                trackManager.myAnimeList.logout()
                context.toast(R.string.myanimelist_relogin)
            }
            if (oldVersion < 113) {
                // Force MAL log out due to login flow change
                // v67: switched from scraping to WebView
                // v68: switched from WebView to OAuth
                val trackManager = Injekt.get<TrackManager>()
                if (trackManager.myAnimeList.isLogged()) {
                    trackManager.myAnimeList.logout()
                    context.toast(R.string.myanimelist_relogin)
                }
            }
            if (oldVersion < 115) {
                // Migrate DNS over HTTPS setting
                val wasDohEnabled = prefs.getBoolean("enable_doh", false)
                if (wasDohEnabled) {
                    prefs.edit {
                        putInt(PreferenceKeys.dohProvider, PREF_DOH_CLOUDFLARE)
                        remove("enable_doh")
                    }
                }
                // Handle removed every 1 or 2, 3 hour library updates
                val updateInterval = preferences.libraryUpdateInterval().get()
                if (updateInterval == 1 || updateInterval == 2 || updateInterval == 3) {
                    preferences.libraryUpdateInterval().set(6)
                    LibraryUpdateJob.setupTask(context, 6)
                }
            }

            if (oldVersion < 120) {
                // Migrate Rotation and Viewer values to default values for viewer_flags
                val newOrientation = when (prefs.getInt("pref_rotation_type_key", 1)) {
                    1 -> OrientationType.FREE.flagValue
                    2 -> OrientationType.PORTRAIT.flagValue
                    3 -> OrientationType.LANDSCAPE.flagValue
                    4 -> OrientationType.LOCKED_PORTRAIT.flagValue
                    5 -> OrientationType.LOCKED_LANDSCAPE.flagValue
                    else -> OrientationType.FREE.flagValue
                }

                // Reading mode flag and prefValue is the same value
                val newReadingMode = prefs.getInt("pref_default_viewer_key", 1)

                prefs.edit {
                    putInt("pref_default_orientation_type_key", newOrientation)
                    remove("pref_rotation_type_key")
                    putInt("pref_default_reading_mode_key", newReadingMode)
                    remove("pref_default_viewer_key")
                }
            }
            if (oldVersion < 142) {
                scope.launchIO {
                    LibraryPresenter.updateRatiosAndColors()
                }
                val oldReaderTap = prefs.getBoolean("reader_tap", true)
                if (!oldReaderTap) {
                    preferences.navigationModePager().set(5)
                    preferences.navigationModeWebtoon().set(5)
                }
            }
            if (oldVersion < 151) {
                val oldDLAfterReading = prefs.getInt("auto_download_after_reading", 0)
                if (oldDLAfterReading > 0) {
                    preferences.autoDownloadWhileReading().set(max(2, oldDLAfterReading))
                }
            }
            if (oldVersion < 157) {
                LibraryUpdateJob.setupTask(context, 0)
                LibraryUpdateJob.setupTask(context)
            }

            return true
        }
        return false
    }
}
