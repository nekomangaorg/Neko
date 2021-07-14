package eu.kanade.tachiyomi.jobs.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration

/**
 * WorkManager job that syncs tracking from trackers to Neko
 */
class TrackingSyncJob(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    val trackingSyncService: TrackingSyncService by injectLazy()

    private val progressNotification =
        with(applicationContext.notificationBuilder(Notifications.Channel.Tracking)) {
            setContentTitle(context.getString(R.string.refresh_tracking_metadata))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelTrackingSyncPendingIntent(context)
            )
        }

    override suspend fun doWork(): Result = coroutineScope {
        withUIContext {
            val notification = progressNotification.build()
            val foregroundInfo = ForegroundInfo(Notifications.Id.Tracking.Progress, notification)
            setForeground(foregroundInfo)
        }

        try {
            trackingSyncService.process(
                ::updateNotificationProgress,
                ::completeNotification
            )

            return@coroutineScope Result.success()
        } catch (e: Exception) {
            XLog.e("error refreshing tracking metadata", e)
            return@coroutineScope Result.failure()
        } finally {
            launchIO {
                delay(Duration.seconds(3).inWholeMilliseconds)
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
            notification
        )
    }

    private fun completeNotification() {
        val notification = progressNotification
            .setContentTitle(context.getString(R.string.refresh_tracking_complete))
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Tracking.Complete,
            notification
        )
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
