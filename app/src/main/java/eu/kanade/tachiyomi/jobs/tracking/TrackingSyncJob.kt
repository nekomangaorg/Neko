package eu.kanade.tachiyomi.jobs.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy

/**
 * WorkManager job that syncs tracking from trackers to Neko
 */
class TrackingSyncJob(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    val trackingSyncService: TrackingSyncService by injectLazy()

    // List containing failed updates
    private val failedUpdates = mutableMapOf<Manga, String?>()

    private val progressNotification =
        with(applicationContext.notificationBuilder(Notifications.Channel.Tracking)) {
            setContentTitle(context.getString(R.string.refresh_tracking_metadata))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelTrackingSyncPendingIntent(context),
            )
        }

    private val completeNotification =
        with(applicationContext.notificationBuilder(Notifications.Channel.Tracking)) {
            setContentTitle(context.getString(R.string.refresh_tracking_complete))
            setSmallIcon(R.drawable.ic_neko_notification)
        }

    override suspend fun doWork(): Result = coroutineScope {
        withUIContext {
            failedUpdates.clear()
            val notification = progressNotification.build()
            val foregroundInfo = ForegroundInfo(Notifications.Id.Tracking.Progress, notification)
            setForeground(foregroundInfo)
        }

        try {
            trackingSyncService.process(
                ::updateNotificationProgress,
                ::completeNotification,
            )

            return@coroutineScope Result.success()
        } catch (e: Exception) {
            loggycat(LogPriority.ERROR, e) { "error refreshing tracking metadata" }
            return@coroutineScope Result.failure()
        } finally {
            launchIO {
                delay(3.seconds.inWholeMilliseconds)
                context.notificationManager.cancel(Notifications.Id.Tracking.Complete)
            }
        }
    }

    private fun updateNotificationProgress(title: String, progress: Int, total: Int) {
        val notification = progressNotification
            .setContentText(title)
            .setProgress(total, progress, false)
            .build()
        applicationContext.notificationManager.notify(
            Notifications.Id.Tracking.Progress,
            notification,
        )
    }

    private fun completeNotification() {
        val notification = completeNotification
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Tracking.Complete,
            notification,
        )
    }

    private fun failed(manga: Manga, error: String) {
        failedUpdates[manga] = error
    }

    companion object {

        val TAG = "tracking_sync_job"

        fun doWorkNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<TrackingSyncJob>()
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
