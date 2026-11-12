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
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** WorkManager job that syncs FollowsList to and from Neko */
class StatusSyncJob(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val followsSyncProcessor: FollowsSyncProcessor by injectLazy()
    val source: SourceManager by injectLazy()
    private val loginHelper: MangaDexLoginHelper by injectLazy()
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

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
        val selected = inputData.getBoolean(SELECTED_MANGA, false)
        // A push queued behind another one finds no ids when that one already sent them
        if (selected && mangaDexPreferences.followsPushIds().get().isBlank()) {
            return@coroutineScope Result.success()
        }
        tryToSetForeground()
        TimberKt.d { "Checking if logged in" }
        if (!loginHelper.isLoggedIn()) {
            context.notificationManager.cancel(Notifications.Id.Status.Complete)
            errorNotification()
            // The pushes queued behind this job fail with it
            clearPushIds()
            return@coroutineScope Result.failure()
        }
        var failed = false
        try {
            val syncType = inputData.getString(SYNC_TYPE)
            when {
                selected -> {
                    TimberKt.d { "sync selected manga to MangaDex" }
                    val ids = mangaDexPreferences.followsPushIds().get()
                    followsSyncProcessor.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        ids,
                    )
                    // Last, because WorkManager runs a stopped job again with the same input. Ids
                    // selected during the push stay for the push queued behind this one.
                    removePushIds(ids)
                }
                syncType == entireLibraryToDex -> {
                    TimberKt.d { "sync entire library to MangaDex" }
                    followsSyncProcessor.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        null,
                    )
                }
                syncType == followsFromDex -> {
                    TimberKt.d { "sync from MangaDex" }

                    val total =
                        followsSyncProcessor.fromMangaDex(
                            ::errorNotification,
                            ::updateNotificationProgress,
                            ::completeNotificationFromDex,
                            ::updateManga,
                        )
                    withUIContext {
                        applicationContext.toast(
                            applicationContext.getString(
                                R.string.pull_follows_to_library_toast,
                                total,
                            ),
                            Toast.LENGTH_LONG,
                        )
                    }
                }
                // Queued by a version that put the selected ids in SYNC_TYPE
                !syncType.isNullOrBlank() -> {
                    TimberKt.d { "sync to MangaDex with given ids" }

                    followsSyncProcessor.toMangaDex(
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
                        syncType,
                    )
                }
            }

            return@coroutineScope Result.success()
        } catch (e: Exception) {
            TimberKt.e(e) { "error syncing follows" }
            failed = true
            return@coroutineScope Result.failure()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                delay(3.seconds.inWholeMilliseconds)
                context.notificationManager.cancel(Notifications.Id.Status.Progress)
                context.notificationManager.cancel(Notifications.Id.Status.Complete)
            }
            // A stopped job runs again and needs its ids. A failed one does not, and the pushes
            // queued behind it fail with it, including any selected during the delay above.
            if (failed && !isStopped) clearPushIds()
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

    private suspend fun completeNotificationToDex(total: Int) {
        completeNotification(R.string.sync_follows_complete)
        withUIContext {
            applicationContext.toast(
                applicationContext.getString(R.string.push_favorites_to_mangadex_toast, total),
                Toast.LENGTH_LONG,
            )
        }
    }

    private fun completeNotificationFromDex() {
        completeNotification(R.string.sync_to_follows_complete)
    }

    private suspend fun updateManga(libraryMangaList: List<Long>) {
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
        context.notificationManager.notify(Notifications.Id.Status.Complete, notification)
    }

    private fun errorNotification(errorTxt: String? = null) {
        context.notificationManager.cancel(Notifications.Id.Status.Progress)
        val notification =
            progressNotification
                .setContentTitle(
                    errorTxt ?: context.getString(R.string.not_logged_into_mangadex_cannot_sync)
                )
                .setAutoCancel(true)
                .build()
        context.notificationManager.notify(Notifications.Id.Status.Complete, notification)
    }

    companion object {

        val TAG = "follow_sync_job"
        private const val SYNC_TYPE = "sync_type"

        // The ids wait in followsPushIds, since WorkManager refuses input over 10240 bytes, about
        // 1500 ids
        private const val SELECTED_MANGA = "selected_manga"

        const val entireLibraryToDex: String = "0"
        const val followsFromDex = "1"

        fun startNow(context: Context, syncType: String) {
            enqueue(
                WorkManager.getInstance(context),
                Data.Builder().putString(SYNC_TYPE, syncType).build(),
            )
        }

        /** Guards the read, change and write of followsPushIds by the screen and the job */
        private val pushIdsLock = Any()

        fun startSelectedNow(workManager: WorkManager, mangaIds: List<Long>) {
            if (mangaIds.isEmpty()) return
            val pushIds = Injekt.get<MangaDexPreferences>().followsPushIds()
            synchronized(pushIdsLock) {
                pushIds.set((pushIds.get().toMangaIds() + mangaIds).distinct().joinToString(","))
            }
            // Queued behind a running or waiting sync, where KEEP would drop it
            enqueue(
                workManager,
                Data.Builder().putBoolean(SELECTED_MANGA, true).build(),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
        }

        private fun removePushIds(pushed: String) {
            val pushIds = Injekt.get<MangaDexPreferences>().followsPushIds()
            synchronized(pushIdsLock) {
                val pushedIds = pushed.toMangaIds().toSet()
                val left = pushIds.get().toMangaIds().filterNot { it in pushedIds }
                if (left.isEmpty()) pushIds.delete() else pushIds.set(left.joinToString(","))
            }
        }

        private fun clearPushIds() {
            synchronized(pushIdsLock) {
                Injekt.get<MangaDexPreferences>().followsPushIds().delete()
            }
        }

        private fun String.toMangaIds() = split(",").mapNotNull { it.trim().toLongOrNull() }

        private fun enqueue(
            workManager: WorkManager,
            input: Data,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            val request =
                OneTimeWorkRequestBuilder<StatusSyncJob>()
                    .addTag(TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(input)
                    .build()
            workManager.enqueueUniqueWork(TAG, policy, request)
        }

        fun stop(context: Context) {
            // Cancelling also drops the pushes queued behind the running job
            clearPushIds()
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}
