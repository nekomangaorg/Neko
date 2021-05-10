package eu.kanade.tachiyomi

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import eu.kanade.tachiyomi.v5.job.V5MigrationJob
import eu.kanade.tachiyomi.v5.job.V5MigrationService
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val context = preferences.context
        val oldVersion = preferences.lastVersionCode().getOrDefault()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            if (oldVersion < 38) {
                if (preferences.automaticUpdates()) {
                    UpdaterJob.setupTask()
                }
            }

            if (oldVersion < 39) {
                // Restore jobs after migrating from Evernote's job scheduler to WorkManager.
                if (BuildConfig.INCLUDE_UPDATER && preferences.automaticUpdates()) {
                    UpdaterJob.setupTask()
                }
            }
            if (oldVersion < 53) {
                LibraryUpdateJob.setupTask()
                BackupCreatorJob.setupTask()
            }
            if (oldVersion < 95 && oldVersion != 0) {
                // Force MAL log out due to login flow change
                val trackManager = Injekt.get<TrackManager>()
                trackManager.myAnimeList.logout()
                context.toast(R.string.myanimelist_relogin)
            }

            if (oldVersion < 113 && oldVersion != 0) {
                // Force MAL log out due to login flow change
                // v67: switched from scraping to WebView
                // v68: switched from WebView to OAuth
                val trackManager = Injekt.get<TrackManager>()
                if (trackManager.myAnimeList.isLogged) {
                    trackManager.myAnimeList.logout()
                    context.toast(R.string.myanimelist_relogin)
                }
            }



            return true
        }


        // Temp testing
        V5MigrationJob.setupTask()

        return false
    }
}
