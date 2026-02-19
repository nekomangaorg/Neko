package eu.kanade.tachiyomi.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import eu.kanade.tachiyomi.util.system.notificationManager
import org.nekomanga.R

/** Class to manage the basic information of all the notifications used in the app. */
object Notifications {

    object Group {
        const val Download = "group_downloader"
        const val Library = "group_library"
    }

    object Channel {
        object Downloader {
            const val Error = "downloader_error_channel"
            const val Progress = "downloader_progress_channel"
        }

        object Library {
            const val Progress = "library_progress_channel"
            const val Error = "library_errors_channel"
            const val Skipped = "library_skipped_channel"
        }

        const val Installing = "installing_channel"
        const val Status = "status_channel"
        const val Tracking = "tracking_channel"
        const val Updated = "updated_channel"
    }

    object Id {
        object Download {
            const val Progress = -201
            const val Error = -202
        }

        object Library {
            const val Progress = -101
            const val Error = -102
            const val Skipped = -103
        }

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

        object Updated {
            const val Installed = -6
        }
    }

    /** Common notification channel and ids used anywhere. */
    const val CHANNEL_COMMON = "common_channel"
    const val ID_UPDATER = 1
    const val ID_DOWNLOAD_IMAGE = 2
    const val ID_INSTALL = 3
    const val CHANNEL_UPDATED = "updated_channel"
    const val ID_INSTALLED = -6
    private const val GROUP_APP_UPDATES = "org.nekomanga.APP_UPDATES"

    /** Notification channel and ids used by the library updater. */
    const val CHANNEL_NEW_CHAPTERS = "new_chapters_channel"
    const val ID_NEW_CHAPTERS = -301
    const val GROUP_NEW_CHAPTERS = "org.nekomanga.NEW_CHAPTERS"

    /** Notification channel and ids used for backup and restore. */
    private const val GROUP_BACKUP_RESTORE = "group_backup_restore"
    const val CHANNEL_BACKUP_RESTORE_PROGRESS = "backup_restore_progress_channel"
    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val CHANNEL_BACKUP_RESTORE_COMPLETE = "backup_restore_complete_channel"
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504
    const val CHANNEL_BACKUP_RESTORE_ERROR = "backup_restore_complete_channel"
    const val ID_BACKUP_RESTORE_ERROR = -505

    /** Notification channel used for crash log file sharing. */
    const val CHANNEL_CRASH_LOGS = "crash_logs_channel"
    const val ID_CRASH_LOGS = -601

    /** Notification channel used for Incognito Mode */
    const val CHANNEL_INCOGNITO_MODE = "incognito_mode_channel"
    const val ID_INCOGNITO_MODE = -701

    private val deprecatedChannels = listOf("library_channel")

    /**
     * Creates the notification channels introduced in Android Oreo.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        listOf(
                NotificationChannelGroup(
                    GROUP_BACKUP_RESTORE,
                    context.getString(R.string.backup_and_restore),
                ),
                NotificationChannelGroup(Group.Library, context.getString(R.string.library)),
                NotificationChannelGroup(
                    GROUP_BACKUP_RESTORE,
                    context.getString(R.string.group_backup_restore),
                ),
                NotificationChannelGroup(
                    Group.Download,
                    context.getString(R.string.group_downloader),
                ),
                NotificationChannelGroup(GROUP_APP_UPDATES, context.getString(R.string.app_updates)),
            )
            .forEach(context.notificationManager::createNotificationChannelGroup)

        val channels =
            listOf(
                NotificationChannel(
                    CHANNEL_COMMON,
                    context.getString(R.string.common),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                        Channel.Library.Progress,
                        context.getString(R.string.updating_library),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = Group.Library
                        setShowBadge(false)
                    },
                NotificationChannel(
                        Channel.Library.Error,
                        context.getString(R.string.channel_errors),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = Group.Library
                        setShowBadge(false)
                    },
                NotificationChannel(
                        Channel.Library.Skipped,
                        context.getString(R.string.channel_skipped),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = Group.Library
                        setShowBadge(false)
                    },
                NotificationChannel(
                        Channel.Downloader.Progress,
                        context.getString(R.string.downloads),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = Group.Download
                        setShowBadge(false)
                    },
                NotificationChannel(
                        Channel.Downloader.Error,
                        context.getString(R.string.download_error),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = Group.Download
                        setShowBadge(false)
                    },
                NotificationChannel(
                        CHANNEL_NEW_CHAPTERS,
                        context.getString(R.string.new_chapters),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { group = Group.Library },
                NotificationChannel(
                        CHANNEL_BACKUP_RESTORE_PROGRESS,
                        context.getString(R.string.backup_restore_progress),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        group = GROUP_BACKUP_RESTORE
                        setShowBadge(false)
                    },
                NotificationChannel(
                        CHANNEL_BACKUP_RESTORE_COMPLETE,
                        context.getString(R.string.backup_restore_complete),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        group = GROUP_BACKUP_RESTORE
                        setShowBadge(false)
                        setSound(null, null)
                    },
                NotificationChannel(
                        Channel.Updated,
                        context.getString(R.string.update_completed),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { setShowBadge(false) },
                NotificationChannel(
                        CHANNEL_BACKUP_RESTORE_ERROR,
                        context.getString(R.string.restore_error),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        group = GROUP_BACKUP_RESTORE
                        setShowBadge(false)
                        setSound(null, null)
                    },
                NotificationChannel(
                    Channel.Status,
                    context.getString(R.string.status_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                NotificationChannel(
                        CHANNEL_INCOGNITO_MODE,
                        context.getString(R.string.incognito_mode),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { lockscreenVisibility = Notification.VISIBILITY_SECRET },
                NotificationChannel(
                        Channel.Status,
                        context.getString(R.string.pull_follows_to_library),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { setShowBadge(false) },
                NotificationChannel(
                        Channel.Tracking,
                        context.getString(R.string.refresh_tracking_metadata),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { setShowBadge(false) },
                NotificationChannel(
                        Channel.Installing,
                        context.getString(R.string.install),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                        group = GROUP_APP_UPDATES
                    },
                NotificationChannel(
                    CHANNEL_CRASH_LOGS,
                    context.getString(R.string.channel_crash_logs),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                NotificationChannel(
                    CHANNEL_INCOGNITO_MODE,
                    context.getString(R.string.incognito_mode),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        context.notificationManager.createNotificationChannels(channels)
    }

    fun isNotificationChannelEnabled(context: Context, channelId: String?): Boolean {
        if (!channelId.isNullOrBlank()) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId)
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        return false
    }
}
