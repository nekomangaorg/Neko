package eu.kanade.tachiyomi.jobs.follows

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration

/**
 * WorkManager job that syncs FollowsList to and from Neko
 */
class StatusSyncJob(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    val followsSyncService: FollowsSyncService by injectLazy()
    val source: SourceManager by injectLazy()

    private val progressNotification =
        applicationContext.notificationBuilder(Notifications.Channel.Status).apply {
            setContentTitle(context.getString(R.string.syncing_follows))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelFollowSyncPendingIntent(context)
            )
        }

    override suspend fun doWork(): Result = coroutineScope {
        withUIContext {
            val notification = progressNotification.build()
            val foregroundInfo = ForegroundInfo(Notifications.Id.Status.Progress, notification)
            setForeground(foregroundInfo)
        }
        if (source.getMangadex().isLogged().not()) {
            context.notificationManager.cancel(Notifications.Id.Status.Complete)
            errorNotification()
            return@coroutineScope Result.failure()
        }
        try {

            when (val ids = inputData.getString(SYNC_TO_MANGADEX)) {
                null, "0" -> {
                    followsSyncService.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        null
                    )
                }

                "1" -> {
                    val total = followsSyncService.fromMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationFromDex
                    )
                    withUIContext {
                        applicationContext.toast(
                            applicationContext.getString(
                                R.string.sync_follows_to_library_toast,
                                total
                            ),
                            Toast.LENGTH_LONG
                        )
                    }
                }
                else -> {
                    followsSyncService.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex, ids
                    )
                }
            }

            return@coroutineScope Result.success()
        } catch (e: Exception) {
            XLog.e("error syncing follows", e)
            return@coroutineScope Result.failure()
        } finally {
            launchIO {
                delay(Duration.seconds(3).inWholeMilliseconds)
                context.notificationManager.cancel(Notifications.Id.Status.Complete)
            }
        }
    }

    private fun updateNotificationProgress(title: String, progress: Int, total: Int) {
        val notification = progressNotification
            .setContentTitle(title)
            .setProgress(total, progress, false)
            .build()
        applicationContext.notificationManager.notify(
            Notifications.Id.Status.Progress,
            notification
        )
    }

    private fun completeNotificationToDex(total: Int) {
        completeNotification(R.string.sync_follows_complete)
        launchUI {
            applicationContext.toast(
                applicationContext.getString(
                    R.string.push_favorites_to_mangadex_toast,
                    total
                ),
                Toast.LENGTH_LONG
            )
        }
    }

    private fun completeNotificationFromDex() {
        completeNotification(R.string.sync_to_follows_complete)
    }

    private fun completeNotification(@StringRes title: Int) {
        val notification = progressNotification
            .setContentTitle(context.getString(R.string.sync_follows_complete))
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification
        )
    }

    private fun errorNotification() {
        val notification = progressNotification
            .setContentTitle(context.getString(R.string.not_logged_into_mangadex_cannot_sync))

            .setOngoing(true)
            .setAutoCancel(true)
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification
        )
    }

    companion object {

        val TAG = "follow_sync_job"
        private const val SYNC_TO_MANGADEX = "sync_to_mangadex"

        const val entireLibraryToDex: String = "0"
        const val entireFollowsFromDex = "1"

        fun doWorkNow(context: Context, syncToMangadex: String) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<StatusSyncJob>().apply {
                    addTag(TAG)
                    setInputData(
                        Data.Builder().putString(SYNC_TO_MANGADEX, syncToMangadex)
                            .build()
                    )
                }.build()
            )
        }
    }
}
