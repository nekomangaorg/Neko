package eu.kanade.tachiyomi

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.data.updater.AppUpdateJob
import org.nekomanga.BuildConfig

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val context = preferences.context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { remove(AppDownloadInstallJob.NOTIFY_ON_INSTALL_KEY) }
        val oldVersion = preferences.lastVersionCode().get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            // Always set up background tasks to ensure they're running
            when (BuildConfig.INCLUDE_UPDATER) {
                true -> AppUpdateJob.setupTask(context)
                false -> AppUpdateJob.cancelTask(context)
            }

            LibraryUpdateJob.setupTask(context)
            BackupCreatorJob.setupTask(context, 12)

            return true
        }
        return false
    }
}
