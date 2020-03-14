package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.evernote.android.job.JobManager
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.similar.SimilarUpdateJob
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.ui.main.MainActivity
import org.acra.ACRA
import org.acra.ReportField
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.data.StringFormat
import org.acra.sender.HttpSender
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar

@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportFormat = StringFormat.JSON,
    excludeMatchingSharedPreferencesKeys = arrayOf(".*username.*", ".*password.*", ".*token.*"),
    reportContent = arrayOf(
        ReportField.ANDROID_VERSION,
        ReportField.APP_VERSION_CODE,
        ReportField.APP_VERSION_NAME,
        ReportField.PACKAGE_NAME,
        ReportField.REPORT_ID,
        ReportField.STACK_TRACE,
        ReportField.USER_APP_START_DATE,
        ReportField.USER_CRASH_DATE
    )
)
@AcraHttpSender(
    uri = "https://collector.tracepot.com/0ebf5ef8",
    httpMethod = HttpSender.Method.PUT
)

open class App : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        setupJobManager()
        setupNotificationChannels()

        Iconics.init(applicationContext)
        Iconics.registerFont(CommunityMaterial)
        Iconics.registerFont(MaterialDesignDx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // App in background
        val preferences: PreferencesHelper by injectLazy()
        if (preferences.lockAfter().getOrDefault() >= 0) {
            MainActivity.unlocked = false
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        setupAcra()
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
                    SimilarUpdateJob.TAG -> SimilarUpdateJob()
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

    protected open fun setupAcra() {
        ACRA.init(this)
    }
}
