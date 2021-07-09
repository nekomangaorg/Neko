package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.os.Build
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
import eu.kanade.tachiyomi.util.log.XLogSetup
import org.conscrypt.Conscrypt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar
import java.security.Security

open class App : Application(), LifecycleObserver {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate() {
        super.onCreate()
        XLogSetup(this)

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        CoilSetup(this)
        setupNotificationChannels()

        Iconics.init(applicationContext)
        Iconics.registerFont(CommunityMaterial)
        Iconics.registerFont(MaterialDesignDx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Reset Incognito Mode on relaunch
        preferences.incognitoMode().set(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Suppress("unused")
    fun onAppBackgrounded() {
        // App in background
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
