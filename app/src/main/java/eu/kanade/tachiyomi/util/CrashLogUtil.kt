package eu.kanade.tachiyomi.util

import android.content.Context
import android.net.Uri
import android.os.Build
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import eu.kanade.tachiyomi.util.system.withUIContext
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import okio.buffer
import okio.sink
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class CrashLogUtil(private val context: Context) {

    val preferences: PreferencesHelper by injectLazy()

    private val notificationBuilder =
        context.notificationBuilder(Notifications.CHANNEL_CRASH_LOGS) {
            setSmallIcon(R.drawable.ic_neko_notification)
        }

    suspend fun dumpLogs(exception: Throwable? = null) = withNonCancellableContext {
        try {
            val storageManager: StorageManager = Injekt.get<StorageManager>()

            val file = context.createFileInCacheDir("neko_crash_logs.txt")
            Runtime.getRuntime().exec("logcat *:D -d -f ${file.absolutePath}").waitFor()

            val uniFile =
                storageManager
                    .getCrashLogDirectory()
                    ?.createFile(
                        "neko_crash_log-${SimpleDateFormat("yyyyMMddHHmm").format(Date())}.txt")
                    ?: return@withNonCancellableContext

            uniFile
                .openOutputStream()
                .also {
                    // Force overwrite old file
                    (it as? FileOutputStream)?.channel?.truncate(0)
                }
                .sink()
                .buffer()
                .use { bufferedSink ->
                    bufferedSink.writeUtf8(getDebugInfo())
                    if (exception != null) {
                        bufferedSink.writeUtf8(getExceptionBlock(exception))
                        bufferedSink.writeUtf8("\n")
                    }
                    file.readLines().forEach { line ->
                        bufferedSink.writeUtf8(line)
                        bufferedSink.writeUtf8("\n")
                    }
                }

            showNotification(uniFile.uri)
        } catch (e: IOException) {
            withUIContext { context.toast("Failed to get logs") }
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
        """
            .trimIndent()
    }

    private fun getExceptionBlock(exception: Throwable): String {
        return """
            ******************************************************************************************************************************************************************************************************************************
            Exception that caused crash
            ******************************************************************************************************************************************************************************************************************************
            $exception
            ******************************************************************************************************************************************************************************************************************************
            ******************************************************************************************************************************************************************************************************************************
        """
            .trimIndent()
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
                NotificationReceiver.openErrorOrSkippedLogPendingActivity(context, uri),
            )

            addAction(
                R.drawable.ic_share_24dp,
                context.getString(R.string.share),
                NotificationReceiver.shareCrashLogPendingBroadcast(
                    context,
                    uri,
                    Notifications.ID_CRASH_LOGS,
                ),
            )

            context.notificationManager.notify(Notifications.ID_CRASH_LOGS, build())
        }
    }
}
