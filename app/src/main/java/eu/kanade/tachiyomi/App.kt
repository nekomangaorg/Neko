package eu.kanade.tachiyomi

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.multidex.MultiDex
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.crash.CrashActivity
import eu.kanade.tachiyomi.crash.GlobalExceptionHandler
import eu.kanade.tachiyomi.data.image.coil.CoilSetup
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.notification
import java.security.Security
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.conscrypt.Conscrypt
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.logging.CrashReportingTree
import org.nekomanga.logging.DebugReportingTree
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.injectLazy

open class App : Application(), DefaultLifecycleObserver {

    val preferences: PreferencesHelper by injectLazy()
    val networkPreferences: NetworkPreferences by injectLazy()
    val securityPreferences: SecurityPreferences by injectLazy()

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        super<Application>.onCreate()

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        kotlin.runCatching {
            CookieManager.getInstance()
        }.onFailure {
            Toast.makeText(applicationContext, "Error! App requires WebView to be installed", Toast.LENGTH_LONG).show()
        }

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Avoid potential crashes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) kotlin.runCatching { WebView.setDataDirectorySuffix(process) }
        }

        Injekt.importModule(PreferenceModule(this))
        Injekt.importModule(AppModule(this))

        if (!BuildConfig.DEBUG) {
            TimberKt.plant(CrashReportingTree())
        }
        if (networkPreferences.verboseLogging().get()) {
            TimberKt.plant(DebugReportingTree())
        }


        CoilSetup(this)
        setupNotificationChannels()

        Iconics.init(applicationContext)
        Iconics.registerFont(CommunityMaterial)
        Iconics.registerFont(MaterialDesignDx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        MangaCoverMetadata.load()
        preferences.nightMode().changes().onEach {
            AppCompatDelegate.setDefaultNightMode(it)
        }.launchIn((ProcessLifecycleOwner.get().lifecycleScope))

        // Show notification to disable Incognito Mode when it's enabled
        securityPreferences.incognitoMode().changes()
            .onEach { enabled ->
                val notificationManager = NotificationManagerCompat.from(this)
                if (enabled) {
                    disableIncognitoReceiver.register()
                    val notification = notification(Notifications.CHANNEL_INCOGNITO_MODE) {
                        val incogText = getString(R.string.incognito_mode)
                        setContentTitle(incogText)
                        setContentText(getString(R.string.turn_off_, incogText))
                        setSmallIcon(R.drawable.ic_incognito_24dp)
                        setOngoing(true)

                        val pendingIntent = PendingIntent.getBroadcast(
                            this@App,
                            0,
                            Intent(ACTION_DISABLE_INCOGNITO_MODE),
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        setContentIntent(pendingIntent)
                    }
                    notificationManager.notify(Notifications.ID_INCOGNITO_MODE, notification)
                } else {
                    disableIncognitoReceiver.unregister()
                    notificationManager.cancel(Notifications.ID_INCOGNITO_MODE)
                }
            }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!AuthenticatorUtil.isAuthenticating && securityPreferences.lockAfter().get() >= 0) {
            SecureActivityDelegate.locked = true
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LibraryPresenter.onLowMemory()
        RecentsPresenter.onLowMemory()
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }

    private inner class DisableIncognitoReceiver : BroadcastReceiver() {
        private var registered = false

        override fun onReceive(context: Context, intent: Intent) {
            securityPreferences.incognitoMode().set(false)
        }

        fun register() {
            if (!registered) {
                registerReceiver(this, IntentFilter(ACTION_DISABLE_INCOGNITO_MODE))
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                unregisterReceiver(this)
                registered = false
            }
        }
    }
}

private const val ACTION_DISABLE_INCOGNITO_MODE = "tachi.action.DISABLE_INCOGNITO_MODE"
