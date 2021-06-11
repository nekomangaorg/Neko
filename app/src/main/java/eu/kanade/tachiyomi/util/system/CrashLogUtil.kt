package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.storage.getUriCompat
import java.io.File
import java.io.IOException

class CrashLogUtil(private val context: Context) {

    private val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_CRASH_LOGS) {
        setSmallIcon(R.drawable.ic_neko_notification)
    }

    fun dumpLogs() {
        try {
            val file = File(context.externalCacheDir, "neko_crash_logs.txt")
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            Runtime.getRuntime().exec("logcat *:E -d -f ${file.absolutePath}")

            showNotification(file.getUriCompat(context))
        } catch (e: IOException) {
            context.toast("Failed to get logs")
        }
    }

    private fun showNotification(uri: Uri) {
        context.notificationManager.cancel(Notifications.ID_CRASH_LOGS)

        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.crash_log_saved))

            addAction(
                R.drawable.ic_folder_24dp,
                context.getString(R.string.open_log),
                NotificationReceiver.openErrorLogPendingActivity(context, uri)
            )

            context.notificationManager.notify(Notifications.ID_CRASH_LOGS, build())
        }
    }
}
