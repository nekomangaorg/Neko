package eu.kanade.tachiyomi.jobs.library

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.lang.orUnknownError
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import org.nekomanga.R

/** Notifications of [LibraryRepairJob], on the backup and restore channels. */
class LibraryRepairNotifier(private val context: Context) {

    private val progressBuilder =
        context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_neko_notification)
            color = context.contextCompatColor(R.color.iconOutline)
            setAutoCancel(false)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setContentTitle(context.getString(R.string.library_repair_progress))
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.stop),
                NotificationReceiver.cancelLibraryRepairPendingBroadcast(context),
            )
        }

    private val completeBuilder =
        context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_neko_notification)
            color = context.contextCompatColor(R.color.iconOutline)
            setAutoCancel(false)
        }

    /** Updates the progress notification and posts it once [total] is known. */
    fun showProgress(done: Int, total: Int): NotificationCompat.Builder {
        progressBuilder.setProgress(total, done, total == 0)
        if (total > 0) {
            context.notificationManager.notify(
                Notifications.Id.LibraryRepair.Progress,
                progressBuilder.build(),
            )
        }
        return progressBuilder
    }

    fun showBlocked(work: List<BlockingWork>) {
        showResult(
            R.string.library_repair_not_started,
            context.getString(R.string.library_repair_blocked, work.names(context)),
            logFile = null,
        )
    }

    fun showComplete(result: RepairResult, logFile: File?) {
        showResult(R.string.library_repair_complete, counts(result), logFile)
    }

    fun showStopped(result: RepairResult, logFile: File?) {
        showResult(R.string.library_repair_stopped, counts(result), logFile)
    }

    fun showError(message: String?) {
        showResult(R.string.library_repair_error, message.orUnknownError(context), logFile = null)
    }

    private fun counts(result: RepairResult): String =
        context.getString(
            R.string.library_repair_result,
            result.merged,
            result.fixed,
            result.skipped.size,
            result.failed.size,
        )

    private fun showResult(@StringRes title: Int, text: String, logFile: File?) {
        context.notificationManager.cancel(Notifications.Id.LibraryRepair.Progress)
        with(completeBuilder) {
            setContentTitle(context.getString(title))
            setContentText(text)
            // The list of running work and the counts can take more than one line
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
            clearActions()
            if (logFile != null) {
                addAction(
                    R.drawable.ic_eye_24dp,
                    context.getString(R.string.open_log),
                    NotificationReceiver.openErrorOrSkippedLogPendingActivity(
                        context,
                        logFile.getUriCompat(context),
                    ),
                )
            }
            context.notificationManager.notify(Notifications.Id.LibraryRepair.Complete, build())
        }
    }
}
