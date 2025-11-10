package eu.kanade.tachiyomi.data.download

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import java.util.regex.Pattern
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
internal class DownloadNotifier(private val context: Context) {

    private val securityPreferences: SecurityPreferences by injectLazy()

    /** Notification builder. */
    private val notificationBuilder by lazy {
        context.notificationBuilder(Notifications.Channel.Downloader.Progress) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setAutoCancel(false)
        }
    }

    private val errorNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.Channel.Downloader.Error) { setAutoCancel(false) }
    }

    /** Status of download. Used for correct notification icon. */
    private var isDownloading = false

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int) {
        context.notificationManager.notify(id, build())
    }

    /**
     * Dismiss the downloader's notification. Downloader error notifications use a different id, so
     * those can only be dismissed by the user.
     */
    fun dismissProgress() {
        context.notificationManager.cancel(Notifications.Id.Download.Progress)
    }

    fun setPlaceholder(download: Download?): NotificationCompat.Builder {
        synchronized(notificationBuilder){
        with(notificationBuilder) {
            // Check if first call.
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                setAutoCancel(false)
                clearActions()
                // Open download manager when clicked
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                color = ContextCompat.getColor(context, R.color.colorSecondary)
                isDownloading = true
                // Pause action
                addAction(
                    R.drawable.ic_pause_24dp,
                    context.getString(R.string.pause),
                    NotificationReceiver.pauseDownloadsPendingBroadcast(context),
                )
            }

            if (download != null && securityPreferences.hideNotificationContent().get()) {
                val title = download.mangaItem.title.chop(15)
                val quotedTitle = Pattern.quote(title)
                val chapter =
                    download.chapterItem.name.replaceFirst(
                        "$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE),
                        "",
                    )
                setContentTitle("$title - $chapter".chop(30))
                setContentText(context.getString(R.string.downloading))
            } else {
                setContentTitle(context.getString(R.string.downloading))
                setContentText(null)
            }
            setProgress(0, 0, true)
            setStyle(null)
        }
            }
        return notificationBuilder
    }

    /**
     * Called when download progress changes.
     *
     * @param download download object containing download information.
     */
    fun onProgressChange(download: Download) {
        // Create notification
        synchronized(notificationBuilder) {
            with(notificationBuilder) {
                // Check if first call.
                if (!isDownloading) {
                    setSmallIcon(android.R.drawable.stat_sys_download)
                    setAutoCancel(false)
                    clearActions()
                    // Open download manager when clicked
                    color = ContextCompat.getColor(context, R.color.iconOutline)
                    setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                    isDownloading = true
                    // Pause action
                    addAction(
                        R.drawable.ic_pause_24dp,
                        context.getString(R.string.pause),
                        NotificationReceiver.pauseDownloadsPendingBroadcast(context),
                    )
                }

                val downloadingProgressText =
                    context
                        .getString(R.string.downloading_progress)
                        .format(download.downloadedImages, download.pages!!.size)

                if (securityPreferences.hideNotificationContent().get()) {
                    setContentTitle(downloadingProgressText)
                } else {
                    val title = download.mangaItem.title.chop(15)
                    val quotedTitle = Pattern.quote(title)
                    val chapter =
                        download.chapterItem.name.replaceFirst(
                            "$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE),
                            "",
                        )
                    setContentTitle("$title - $chapter".chop(30))
                    setContentText(downloadingProgressText)
                }
                setStyle(null)
                setProgress(download.pages!!.size, download.downloadedImages, false)
                setOngoing(true)
                show(Notifications.Id.Download.Progress)
            }
        }
    }

    /** Show notification when download is paused. */
    fun onPaused() {
        synchronized(notificationBuilder) {
            with(notificationBuilder) {
                setContentTitle(context.getString(R.string.paused))
                setContentText(context.getString(R.string.download_paused))
                setSmallIcon(R.drawable.ic_pause_24dp)
                setAutoCancel(false)
                setOngoing(false)
                setProgress(0, 0, false)
                color = ContextCompat.getColor(context, R.color.iconOutline)
                clearActions()
                // Open download manager when clicked
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                // Resume action
                addAction(
                    R.drawable.ic_play_arrow_24dp,
                    context.getString(R.string.resume),
                    NotificationReceiver.resumeDownloadsPendingBroadcast(context),
                )
                // Clear action
                addAction(
                    R.drawable.ic_close_24dp,
                    context.getString(R.string.cancel_all),
                    NotificationReceiver.clearDownloadsPendingBroadcast(context),
                )
                show(Notifications.Id.Download.Progress)
            }
        }

        // Reset initial values
        isDownloading = false
    }

    /** Resets the state once downloads are completed. */
    fun onComplete() {
        dismissProgress()

        // Reset states to default
        isDownloading = false
    }

    /**
     * Called when the downloader receives a warning.
     *
     * @param reason the text to show.
     */
    fun onWarning(reason: String) {
        synchronized(notificationBuilder) {
            with(errorNotificationBuilder) {
                setContentTitle(context.getString(R.string.downloads))
                setContentText(reason)
                color = ContextCompat.getColor(context, R.color.iconOutline)
                setSmallIcon(android.R.drawable.stat_sys_warning)
                setAutoCancel(true)
                clearActions()
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                setProgress(0, 0, false)
                show(Notifications.Id.Download.Error)
            }
        }

        // Reset download information
        isDownloading = false
    }

    /**
     * Called when the downloader receives an error. It's shown as a separate notification to avoid
     * being overwritten.
     *
     * @param error string containing error information.
     * @param chapter string containing chapter title.
     */
    fun onError(
        error: String? = null,
        chapter: String? = null,
        mangaTitle: String? = null,
        customIntent: Intent? = null,
    ) {
        // Create notification
        synchronized(notificationBuilder) {
            with(errorNotificationBuilder) {
                setContentTitle(
                    mangaTitle?.plus(": $chapter") ?: context.getString(R.string.download_error)
                )
                setContentText(
                    error ?: context.getString(R.string.could_not_download_unexpected_error)
                )
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            error ?: context.getString(R.string.could_not_download_unexpected_error)
                        )
                )
                setSmallIcon(android.R.drawable.stat_sys_warning)
                setCategory(NotificationCompat.CATEGORY_ERROR)
                clearActions()
                setAutoCancel(true)
                if (customIntent != null) {
                    setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            0,
                            customIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    )
                } else {
                    setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                }
                color = ContextCompat.getColor(context, R.color.iconOutline)
                setProgress(0, 0, false)
                show(Notifications.Id.Download.Error)
            }
        }

        // Reset download information
        isDownloading = false
    }
}
