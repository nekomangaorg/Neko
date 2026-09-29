package eu.kanade.tachiyomi.util

import android.content.Context
import android.net.Uri
import android.os.Process
import eu.kanade.tachiyomi.crash.CrashReportText
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import eu.kanade.tachiyomi.util.system.withUIContext
import java.text.SimpleDateFormat
import java.util.Date
import okio.buffer
import okio.sink
import okio.source
import org.nekomanga.R
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class CrashLogUtil(private val context: Context) {

    val preferences: PreferencesHelper by injectLazy()

    private val notificationBuilder =
        context.notificationBuilder(Notifications.CHANNEL_CRASH_LOGS) {
            setSmallIcon(R.drawable.ic_neko_notification)
        }

    suspend fun dumpLogs() = withNonCancellableContext {
        withIOContext {
            try {
                val storageManager: StorageManager = Injekt.get()

                val uniFile =
                    storageManager
                        .getCrashLogDirectory()
                        ?.createFile(
                            "neko_crash_log-${SimpleDateFormat("yyyyMMddHHmm").format(Date())}.txt"
                        )
                if (uniFile == null) {
                    withUIContext { context.toast(R.string.crash_log_folder_failed) }
                    return@withIOContext
                }

                uniFile.openOutputStream().sink().buffer().use { bufferedSink ->
                    bufferedSink.writeUtf8(CrashReportText.debugInfo())
                    bufferedSink.writeUtf8("\n\n")

                    val pid = Process.myPid()
                    val command = "logcat --pid=$pid *:D -d"
                    val process = Runtime.getRuntime().exec(command)

                    process.inputStream.source().use { source -> bufferedSink.writeAll(source) }

                    process.waitFor()
                }

                showNotification(uniFile.uri)
            } catch (e: Exception) {
                TimberKt.e(e) { "Could not save the crash log" }
                withUIContext { context.toast(R.string.crash_log_save_failed) }
            }
        }
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
