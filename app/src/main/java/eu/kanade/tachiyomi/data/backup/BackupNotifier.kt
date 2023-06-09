package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import java.util.concurrent.TimeUnit
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy

internal class BackupNotifier(private val context: Context) {

    private val securityPreferences: SecurityPreferences by injectLazy()

    private val progressNotificationBuilder =
        context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(false)
            color = context.contextCompatColor(R.color.new_neko_accent)
            setOngoing(true)
        }

    private val completeNotificationBuilder =
        context.notificationBuilder(Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_neko_notification)
            color = context.contextCompatColor(R.color.new_neko_accent)
            setAutoCancel(false)
        }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notificationManager.notify(id, build())
    }

    fun showBackupProgress(): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.creating_backup))

            setProgress(0, 0, true)
            setOnlyAlertOnce(true)
        }

        builder.show(Notifications.ID_BACKUP_PROGRESS)

        return builder
    }

    fun showBackupError(error: String?) {
        context.notificationManager.cancel(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_failed))
            setContentText(error)

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showBackupComplete(unifile: UniFile) {
        context.notificationManager.cancel(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_created))
            setContentText(unifile.filePath ?: unifile.name)

            // Clear old actions if they exist
            clearActions()

            addAction(
                R.drawable.ic_share_24dp,
                context.getString(R.string.share),
                NotificationReceiver.shareBackupPendingBroadcast(
                    context,
                    unifile.uri,
                    Notifications.ID_BACKUP_COMPLETE,
                ),
            )

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showRestoreProgress(
        content: String = "",
        progress: Int = 0,
        maxAmount: Int = 100,
    ): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.restoring_backup))

            if (!securityPreferences.hideNotificationContent().get()) {
                setContentText(content)
            }

            setProgress(maxAmount, progress, false)
            setOnlyAlertOnce(true)

            // Clear old actions if they exist
            clearActions()

            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.stop),
                NotificationReceiver.cancelRestorePendingBroadcast(
                    context,
                    Notifications.ID_RESTORE_PROGRESS,
                ),
            )
        }

        builder.show(Notifications.ID_RESTORE_PROGRESS)

        return builder
    }

    fun showRestoreError(error: String?) {
        context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.restore_error))
            setContentText(error)

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }

    fun showRestoreComplete(time: Long, errorCount: Int, path: String?, file: String?) {
        context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

        val timeString = context.getString(
            R.string.restore_duration,
            TimeUnit.MILLISECONDS.toMinutes(time),
            TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(time),
            ),
        )

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.restore_completed))
            setContentText(
                context.resources.getQuantityString(
                    R.plurals.restore_completed_message,
                    errorCount,
                    timeString,
                    errorCount,
                ),
            )

            // Clear old actions if they exist
            clearActions()

            if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
                val destFile = File(path, file)
                val uri = destFile.getUriCompat(context)

                addAction(
                    R.drawable.ic_eye_24dp,
                    context.getString(R.string.open_log),
                    NotificationReceiver.openErrorLogPendingActivity(context, uri),
                )
            }

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }
}
