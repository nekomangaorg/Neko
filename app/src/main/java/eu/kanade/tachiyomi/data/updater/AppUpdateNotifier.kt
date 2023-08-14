package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.notificationManager

/**
 * DownloadNotifier is used to show notifications when downloading and update.
 *
 * @param context context of application.
 */
internal class AppUpdateNotifier(private val context: Context) {

    /**
     * Builder to manage notifications.
     */
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(context, Notifications.CHANNEL_COMMON)
    }

    companion object {
        var releasePageUrl: String? = null
    }

    /**
     * Call to show notification.
     *
     * @param id id of the notification channel.
     */
    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_UPDATER) {
        context.notificationManager.notify(id, build())
    }

    fun promptUpdate(body: String, url: String, releaseUrl: String) {
        val intent = Intent(context, AppUpdateService::class.java).apply {
            putExtra(AppUpdateService.EXTRA_DOWNLOAD_URL, url)
            putExtra(AppUpdateService.EXTRA_NOTIFY_ON_INSTALL, true)
        }

        val pendingIntent = NotificationReceiver.openUpdatePendingActivity(context, body, url)
        releasePageUrl = releaseUrl
        with(notificationBuilder) {
            setContentTitle(context.getString(R.string.app_name_neko))
            setContentText(context.getString(R.string.new_version_available))
            setContentIntent(pendingIntent)
            setAutoCancel(true)
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            color = context.getResourceColor(R.attr.colorSecondary)
            clearActions()
            val isOnA12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            // Download action
            addAction(
                android.R.drawable.stat_sys_download_done,
                context.getString(if (isOnA12) R.string.update else R.string.download),
                PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            addReleasePageAction()
        }
        notificationBuilder.show()
    }

    private fun NotificationCompat.Builder.addReleasePageAction() {
        releasePageUrl?.let { releaseUrl ->
            val releaseIntent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            addAction(
                R.drawable.ic_new_releases_24dp,
                context.getString(R.string.release_page),
                PendingIntent.getActivity(context, releaseUrl.hashCode(), releaseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
        }
    }

    /**
     * Call when apk download starts.
     *
     * @param title tile of notification.
     */
    fun onDownloadStarted(title: String): NotificationCompat.Builder {
        with(notificationBuilder) {
            setContentTitle(title)
            setContentText(context.getString(R.string.downloading))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setAutoCancel(false)
            setOngoing(true)
            clearActions()

            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelUpdateDownloadPendingBroadcast(context),
            )
            addReleasePageAction()
        }
        notificationBuilder.show()
        return notificationBuilder
    }

    /**
     * Call when apk download progress changes.
     *
     * @param progress progress of download (xx%/100).
     */
    fun onProgressChange(progress: Int) {
        with(notificationBuilder) {
            setProgress(100, progress, false)
            setOnlyAlertOnce(true)
        }
        notificationBuilder.show()
    }

    fun onInstalling() {
        with(NotificationCompat.Builder(context, Notifications.Channel.Installing)) {
            setContentTitle(context.getString(R.string.installing))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setProgress(0, 0, true)
            setOnlyAlertOnce(true)
            clearActions()
            show(Notifications.ID_INSTALL)
        }
    }

    /**
     * Call when apk download is finished.
     *
     * @param uri path location of apk.
     */
    fun onDownloadFinished(uri: Uri) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.download_complete))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setAutoCancel(false)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            // Install action
            setContentIntent(NotificationHandler.installApkPendingActivity(context, uri))
            clearActions()
            addAction(
                R.drawable.ic_system_update_24dp,
                context.getString(R.string.install),
                NotificationHandler.installApkPendingActivity(context, uri),
            )
            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.dismissNotificationPendingBroadcast(context, Notifications.ID_INSTALL),
            )
            addReleasePageAction()
        }
        notificationBuilder.show(Notifications.ID_INSTALL)
    }

    /**
     * Call when apk download is finished.
     *
     * @param uri path location of apk.
     */
    fun onInstallFinished() {
        with(NotificationCompat.Builder(context, Notifications.Channel.Updated)) {
            setContentTitle(context.getString(R.string.updated_to_, BuildConfig.VERSION_NAME))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            setOngoing(false)
            setProgress(0, 0, false)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                context.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setContentIntent(pendingIntent)
            clearActions()
            addAction(
                R.drawable.ic_system_update_24dp,
                context.getString(R.string.open),
                pendingIntent,
            )
            addReleasePageAction()
            show(Notifications.Id.Updated.Installed)
        }
    }

    /**
     * Call when apk download throws a error
     *
     * @param url web location of apk to download.
     */
    fun onDownloadError(url: String) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.download_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setOnlyAlertOnce(false)
            setAutoCancel(false)
            setProgress(0, 0, false)
            color = ContextCompat.getColor(context, R.color.secondaryTachiyomi)
            clearActions()
            // Retry action
            addAction(
                R.drawable.ic_refresh_24dp,
                context.getString(R.string.retry),
                AppUpdateService.downloadApkPendingService(context, url),
            )
            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.dismissNotificationPendingBroadcast(
                    context,
                    Notifications.ID_UPDATER,
                ),
            )
            addReleasePageAction()
        }
        notificationBuilder.show(Notifications.ID_UPDATER)
    }

    fun onInstallError(uri: Uri) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.could_not_install_update))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setOnlyAlertOnce(false)
            setAutoCancel(false)
            setProgress(0, 0, false)
            color = ContextCompat.getColor(context, R.color.secondaryTachiyomi)
            clearActions()
            // Retry action
            addAction(
                R.drawable.ic_refresh_24dp,
                context.getString(R.string.retry),
                NotificationHandler.installApkPendingActivity(context, uri),
            )
            // Cancel action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.dismissNotificationPendingBroadcast(
                    context,
                    Notifications.ID_UPDATER,
                ),
            )
            addReleasePageAction()
        }
        notificationBuilder.show(Notifications.ID_UPDATER)
    }

    fun cancel() {
        NotificationReceiver.dismissNotification(context, Notifications.ID_UPDATER)
    }

    fun cancelInstallNotification() {
        NotificationReceiver.dismissNotification(context, Notifications.ID_INSTALL)
    }
}
