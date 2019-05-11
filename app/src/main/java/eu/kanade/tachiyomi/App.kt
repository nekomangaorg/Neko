package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.support.multidex.MultiDex
import com.evernote.android.job.JobManager
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.registry.default.DefaultRegistrar

open class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        setupJobManager()
        setupNotificationChannels()

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    protected open fun setupJobManager() {
        try {
            JobManager.create(this).addJobCreator { tag ->
                when (tag) {
                    LibraryUpdateJob.TAG -> LibraryUpdateJob()
                    UpdaterJob.TAG -> UpdaterJob()
                    BackupCreatorJob.TAG -> BackupCreatorJob()
                    else -> null
                }
            }
        } catch (e: Exception) {
            Timber.w("Can't initialize job manager")
        }
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }

}
