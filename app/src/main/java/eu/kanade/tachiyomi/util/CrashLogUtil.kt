package eu.kanade.tachiyomi.util

import android.content.Context
import android.net.Uri
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import java.io.IOException
import uy.kohesive.injekt.injectLazy

class CrashLogUtil(private val context: Context) {

    val preferences: PreferencesHelper by injectLazy()

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_CRASH_LOGS) {
        setSmallIcon(R.drawable.ic_neko_notification)
    }

    fun dumpLogs() {
        try {
            val file = context.createFileInCacheDir("neko_crash_logs.txt")
            file.appendText(getDebugInfo() + "\n")

            val logLevel = when (preferences.verboseLogging()) {
                true -> "*:*"
                false -> "*:E"
            }

            Runtime.getRuntime().exec("logcat $logLevel -d -f ${file.absolutePath}")

            showNotification(file.getUriCompat(context))
        } catch (e: IOException) {
            context.toast("Failed to get logs")
        }
    }

    fun getDebugInfo(): String {
        return """
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, ${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TIME})
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Android build ID: ${Build.DISPLAY}
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE}
            Device model: ${Build.MODEL}
            Device product name: ${Build.PRODUCT}
        """.trimIndent()
    }

    private fun showNotification(uri: Uri) {
        context.notificationManager.cancel(Notifications.ID_CRASH_LOGS)

        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.crash_log_saved))

            // Clear old actions if they exist
            clearActions()

            addAction(
                R.drawable.ic_bug_report_24dp,
                context.getString(R.string.open_log),
                NotificationReceiver.openErrorLogPendingActivity(context, uri),
            )

            addAction(
                R.drawable.ic_share_24dp,
                context.getString(R.string.share),
                NotificationReceiver.shareCrashLogPendingBroadcast(context, uri, Notifications.ID_CRASH_LOGS),
            )

            context.notificationManager.notify(Notifications.ID_CRASH_LOGS, build())
        }
    }
}
