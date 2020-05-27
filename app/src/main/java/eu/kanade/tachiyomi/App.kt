package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.data.image.coil.CoilSetup
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar

open class App : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        CoilSetup(this)
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
            SecureActivityDelegate.locked = true
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }
}
