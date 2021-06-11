package eu.kanade.tachiyomi.v5.job

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager

class V5MigrationNotifier(private val context: Context) {

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelV5MigrationUpdatePendingBroadcast(context)
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_V5_MIGRATION) {
            setContentTitle(context.getString(R.string.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            addAction(R.drawable.ic_close_24dp, context.getString(android.R.string.cancel), cancelIntent)
        }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(manga: SManga, current: Int, total: Int) {
        val title = manga.title
        context.notificationManager.notify(
            Notifications.ID_V5_MIGRATION_PROGRESS,
            progressNotificationBuilder
                .setContentTitle(title)
                .setProgress(total, current, false)
                .build()
        )
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(chapter: SChapter, current: Int, total: Int) {
        val title = chapter.chapter_title
        context.notificationManager.notify(
            Notifications.ID_V5_MIGRATION_PROGRESS,
            progressNotificationBuilder
                .setContentTitle(title)
                .setProgress(total, current, false)
                .build()
        )
    }

    /**
     * Shows notification containing update entries that failed with action to open full log.
     *
     * @param errors List of entry titles that failed to update.
     * @param uri Uri for error log file containing all titles that failed.
     */
    fun showUpdateErrorNotification(errors: List<String>, uri: Uri?) {
        if (errors.isEmpty()) {
            return
        }
        context.notificationManager.notify(
            Notifications.ID_V5_MIGRATION_ERROR,
            context.notificationBuilder(Notifications.CHANNEL_V5_MIGRATION) {
                setContentTitle(context.resources.getQuantityString(R.plurals.notification_update_failed, errors.size, errors.size))
                addAction(
                    R.drawable.nnf_ic_file_folder,
                    context.getString(R.string.view_all_errors),
                    NotificationReceiver.openErrorLogPendingActivity(context, uri!!)
                )
                setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        errors.joinToString("\n") {
                            it.chop(TITLE_MAX_LEN)
                        }
                    )
                )
                setSmallIcon(R.drawable.ic_neko_notification)
            }.build()
        )
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        context.notificationManager.cancel(Notifications.ID_V5_MIGRATION_PROGRESS)
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = MainActivity.SHORTCUT_RECENTLY_UPDATED
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val MAX_CHAPTERS = 5
        private const val TITLE_MAX_LEN = 45
        private const val ICON_SIZE = 192
    }
}
