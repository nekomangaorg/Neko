package eu.kanade.tachiyomi.data.backup

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RestoreHelper(val context: Context) {

    /**
     * Pending intent of action that cancels the library update
     */
    val cancelIntent by lazy {
        NotificationReceiver.cancelRestorePendingBroadcast(context, Notifications.ID_RESTORE_PROGRESS)
    }

    /**
     * keep a partially constructed progress notification for resuse
     */
    val progressNotification by lazy {
        NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS)
            .setContentTitle(context.getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_neko_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setColor(ContextCompat.getColor(context, R.color.neko_green_darker))
            .addAction(R.drawable.ic_close_24dp, context.getString(android.R.string.cancel), cancelIntent)
    }

    /**Get the PendingIntent for the error log
     *
     */
    fun getErrorLogIntent(path: String, file: String): PendingIntent {
        val destFile = File(path, file)
        val uri = destFile.getUriCompat(context.applicationContext)
        return NotificationReceiver.openFileExplorerPendingActivity(context, uri)
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(current: Int, total: Int, title: String) {
        context.notificationManager.notify(
            Notifications.ID_RESTORE_PROGRESS,
            progressNotification
                .setContentTitle(title.chop(30))
                .setContentText(
                    context.getString(
                        R.string.restoring_progress,
                        current,
                        total
                    )
                )
                .setProgress(total, current, false)
                .build()
        )
    }

    /**Show an error notification if something happens that prevents the restore from starting/working
     *
     */
    fun showErrorNotification(errorMessage: String) {
        val resultNotification = NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_ERROR)
            .setContentTitle(context.getString(R.string.restore_error))
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_error_24dp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ContextCompat.getColor(context, R.color.md_red_500))
        context.notificationManager.notify(Notifications.ID_BACKUP_RESTORE_ERROR, resultNotification.build())
    }

    /**
     * Show the result notification with option to show the error log
     */
    fun showResultNotification(
        path: String?,
        file: String?,
        categoriesAmount: Int,
        restoreProgress: Int,
        restoreAmount: Int,
        skippedAmount: Int,
        totalAmount: Int,
        cancelled: Int,
        errors: List<String>,
        trackingErrors: List<String>,
    ) {
        val content = mutableListOf<String>()
        if (categoriesAmount > 0) {
            content.add(
                context.resources.getQuantityString(
                    R.plurals.restore_categories,
                    categoriesAmount,
                    categoriesAmount
                )
            )
        }

        content.add(
            context.getString(
                R.string.restore_completed_successful,
                restoreProgress
                    .toString(),
                restoreAmount.toString()
            )
        )

        content.add(
            context.getString(
                R.string.restore_completed_errors,
                errors.size.toString()
            )
        )

        if (skippedAmount > 0) {
            content.add(
                context.getString(
                    R.string.restore_skipped,
                    skippedAmount.toString(),
                    totalAmount.toString()
                )
            )
        }

        val trackingErrors = trackingErrors.distinct()
        if (trackingErrors.isNotEmpty()) {
            val trackingErrorsString = trackingErrors.distinct().joinToString("\n")
            content.add(trackingErrorsString)
        }
        if (cancelled > 0) {
            content.add(context.getString(R.string.restore_content_skipped, cancelled))
        }

        val restoreString = content.joinToString("\n")

        val resultNotification = NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE)
            .setContentTitle(context.getString(R.string.restore_completed))
            .setContentText(restoreString)
            .setStyle(NotificationCompat.BigTextStyle().bigText(restoreString))
            .setSmallIcon(R.drawable.ic_neko_notification)
            .setColor(ContextCompat.getColor(context, R.color.neko_green_darker))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (errors.isNotEmpty() && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
            resultNotification.addAction(
                R.drawable.ic_close_24dp,
                context.getString(
                    R.string
                        .view_all_errors
                ),
                getErrorLogIntent(path, file)
            )
        }
        context.notificationManager.notify(Notifications.ID_RESTORE_COMPLETE, resultNotification.build())
    }

    /**
     * Write errors to error log
     */
    fun writeErrorLog(errors: List<String>, skippedAmount: Int, skippedTitles: List<String>): File {
        try {
            if (errors.isNotEmpty() || skippedTitles.isNotEmpty()) {
                val destFile = File(context.externalCacheDir, "neko_restore.log")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                destFile.bufferedWriter().use { out ->
                    if (skippedAmount > 0) {
                        out.write("skipped titles: \n")
                        skippedTitles.forEach { message ->
                            out.write("$message\n")
                        }
                    }
                    if (errors.isNotEmpty()) {
                        out.write("\n\nErrors: \n")
                        errors.forEach { message ->
                            out.write("$message\n")
                        }
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            XLog.e(e)
        }
        return File("")
    }
}
