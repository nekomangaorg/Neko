package eu.kanade.tachiyomi.data.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.notificationManager

/**
 * Class to manage the basic information of all the notifications used in the app.
 */
object Notifications {

    object Channel {
        const val Status = "status_channel"
        const val Tracking = "tracking_channel"
    }

    object Id {
        object Status {
            const val Progress = -1001
            const val Complete = -1002
            const val Error = -1003
        }

        object Tracking {
            const val Progress = -2001
            const val Complete = -2002
            const val Error = -2003
        }
    }

    /**
     * Common notification channel and ids used anywhere.
     */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_UPDATER = 1
    const val ID_DOWNLOAD_IMAGE = 2
    const val ID_INSTALL = 3

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_LIBRARY = "library_channel"
    const val ID_LIBRARY_PROGRESS = -101
    const val ID_LIBRARY_ERROR = -102

    /**
     * Notification channel and ids used by the downloader.
     */
    private const val GROUP_DOWNLOADER = "group_downloader"
    const val CHANNEL_DOWNLOADER_PROGRESS = "downloader_progress_channel"
    const val ID_DOWNLOAD_CHAPTER_PROGRESS = -201
    const val CHANNEL_DOWNLOADER_ERROR = "downloader_error_channel"
    const val ID_DOWNLOAD_CHAPTER_ERROR = -202
    const val CHANNEL_DOWNLOADER_COMPLETE = "downloader_complete_channel"
    const val ID_DOWNLOAD_CHAPTER_COMPLETE = -203

    /**
     * Notification channel and ids used by the library updater.
     */
    const val CHANNEL_NEW_CHAPTERS = "new_chapters_channel"
    const val ID_NEW_CHAPTERS = -301
    const val GROUP_NEW_CHAPTERS = "eu.kanade.tachiyomi.NEW_CHAPTERS"

    /**
     * Notification channel and ids used for backup and restore.
     */
    private const val GROUP_BACKUP_RESTORE = "group_backup_restore"
    const val CHANNEL_BACKUP_RESTORE_PROGRESS = "backup_restore_progress_channel"
    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val CHANNEL_BACKUP_RESTORE_COMPLETE = "backup_restore_complete_channel"
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504
    const val CHANNEL_BACKUP_RESTORE_ERROR = "backup_restore_complete_channel"
    const val ID_BACKUP_RESTORE_ERROR = -505

    /**
     * Notification channel used for crash log file sharing.
     */
    const val CHANNEL_CRASH_LOGS = "crash_logs_channel"
    const val ID_CRASH_LOGS = -601

    /**
     * Notification channel for migration.
     */
    const val CHANNEL_V5_MIGRATION = "v5_migration_channel"
    const val ID_V5_MIGRATION_PROGRESS = -901
    const val ID_V5_MIGRATION_ERROR = -902

    /**
     * Creates the notification channels introduced in Android Oreo.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        listOf(
            NotificationChannelGroup(
                GROUP_BACKUP_RESTORE,
                context.getString(R.string.group_backup_restore)
            ),
            NotificationChannelGroup(GROUP_DOWNLOADER, context.getString(R.string.group_downloader))
        ).forEach(context.notificationManager::createNotificationChannelGroup)

        val channels = listOf(
            NotificationChannel(
                CHANNEL_COMMON,
                context.getString(R.string.common),
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_LIBRARY,
                context.getString(R.string.updating_library),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_PROGRESS,
                context.getString(R.string.downloads),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_COMPLETE,
                context.getString(R.string.download_complete),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_DOWNLOADER_ERROR,
                context.getString(R.string.download_error),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_DOWNLOADER
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_NEW_CHAPTERS,
                context.getString(R.string.new_chapters),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_BACKUP_RESTORE_PROGRESS,
                context.getString(R.string.backup_restore_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                group = GROUP_BACKUP_RESTORE
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_BACKUP_RESTORE_COMPLETE,
                context.getString(R.string.backup_restore_complete),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = GROUP_BACKUP_RESTORE
                setShowBadge(false)
                setSound(null, null)
            },
            NotificationChannel(
                CHANNEL_BACKUP_RESTORE_ERROR,
                context.getString(R.string.restore_error),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = GROUP_BACKUP_RESTORE
                setShowBadge(false)
                setSound(null, null)
            },
            NotificationChannel(
                Channel.Status,
                context.getString(R.string.status_channel),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_V5_MIGRATION,
                context.getString(R.string.v5_migration_service),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            },
            NotificationChannel(
                Channel.Status,
                context.getString(R.string.sync_follows_to_library),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            },
            NotificationChannel(
                Channel.Tracking,
                context.getString(R.string.refresh_tracking_metadata),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_CRASH_LOGS,
                context.getString(R.string.channel_crash_logs),
                NotificationManager.IMPORTANCE_HIGH
            )
        ).forEach(context.notificationManager::createNotificationChannel)
    }
}
