package eu.kanade.tachiyomi.jobs.follows

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

/** WorkManager job that syncs FollowsList to and from Neko */
class StatusSyncJob(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val followsSyncProcessor: FollowsSyncProcessor by injectLazy()
    val source: SourceManager by injectLazy()
    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val progressNotification =
        applicationContext.notificationBuilder(Notifications.Channel.Status).apply {
            setContentTitle(context.getString(R.string.syncing_follows))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelFollowSyncPendingIntent(context),
            )
        }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = progressNotification.build()
        val id = Notifications.Id.Status.Progress
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        TimberKt.d { "Doing work" }
        tryToSetForeground()
        TimberKt.d { "Checking if logged in" }
        if (!loginHelper.isLoggedIn()) {
            context.notificationManager.cancel(Notifications.Id.Status.Complete)
            errorNotification()
            return@coroutineScope Result.failure()
        }
        try {
            when (val ids = inputData.getString(SYNC_TO_MANGADEX)) {
                null,
                "0" -> {
                    TimberKt.d { "sync to MangaDex" }
                    followsSyncProcessor.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        null,
                    )
                }
                "1" -> {
                    TimberKt.d { "sync from MangaDex" }

                    val total =
                        followsSyncProcessor.fromMangaDex(
                            ::errorNotification,
                            ::updateNotificationProgress,
                            ::completeNotificationFromDex,
                            ::updateManga)
                    withUIContext {
                        applicationContext.toast(
                            applicationContext.getString(
                                R.string.sync_follows_to_library_toast,
                                total,
                            ),
                            Toast.LENGTH_LONG,
                        )
                    }
                }
                else -> {
                    TimberKt.d { "sync to MangaDex with given ids" }

                    followsSyncProcessor.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        ids,
                    )
                }
            }

            return@coroutineScope Result.success()
        } catch (e: Exception) {
            TimberKt.e(e) { "error syncing follows" }
            return@coroutineScope Result.failure()
        } finally {
            launchIO {
                delay(3.seconds.inWholeMilliseconds)
                context.notificationManager.cancel(Notifications.Id.Status.Progress)
                context.notificationManager.cancel(Notifications.Id.Status.Complete)
            }
        }
    }

    private fun updateNotificationProgress(title: String, progress: Int, total: Int) {
        val notification =
            progressNotification.setContentTitle(title).setProgress(total, progress, false).build()
        applicationContext.notificationManager.notify(
            Notifications.Id.Status.Progress,
            notification,
        )
    }

    private fun completeNotificationToDex(total: Int) {
        completeNotification(R.string.sync_follows_complete)
        launchUI {
            applicationContext.toast(
                applicationContext.getString(
                    R.string.push_favorites_to_mangadex_toast,
                    total,
                ),
                Toast.LENGTH_LONG,
            )
        }
    }

    private fun completeNotificationFromDex() {
        completeNotification(R.string.sync_to_follows_complete)
    }

    private fun updateManga(libraryMangaList: List<Long>) {
        if (libraryMangaList.isNotEmpty()) {
            LibraryUpdateJob.startNow(context.applicationContext, mangaIdsToUse = libraryMangaList)
        }
    }

    private fun completeNotification(@StringRes title: Int) {
        context.notificationManager.cancel(Notifications.Id.Status.Progress)

        val notification =
            progressNotification
                .setContentTitle(context.getString(R.string.sync_follows_complete))
                .build()
        context.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification,
        )
    }

    private fun errorNotification(errorTxt: String? = null) {
        context.notificationManager.cancel(Notifications.Id.Status.Progress)
        val notification =
            progressNotification
                .setContentTitle(
                    errorTxt ?: context.getString(R.string.not_logged_into_mangadex_cannot_sync))
                .setAutoCancel(true)
                .build()
        context.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification,
        )
    }

    companion object {

        val TAG = "follow_sync_job"
        private const val SYNC_TO_MANGADEX = "sync_to_mangadex"

        const val entireLibraryToDex: String = "0"
        const val entireFollowsFromDex = "1"

        fun startNow(context: Context, syncToMangadex: String) {
            val request =
                OneTimeWorkRequestBuilder<StatusSyncJob>()
                    .addTag(TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        Data.Builder().putString(SYNC_TO_MANGADEX, syncToMangadex).build(),
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}
