package eu.kanade.tachiyomi.jobs.customlist

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

/**
 * WorkManager job that syncs FollowsList to and from Neko
 */
class CustomListSyncJob(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val customListSyncService: CustomListSyncService by injectLazy()
    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val progressNotification =
        applicationContext.notificationBuilder(Notifications.Channel.Status).apply {
            setContentTitle(context.getString(R.string.begin_syncing_mdlist))
            setSmallIcon(R.drawable.ic_neko_notification)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelFollowSyncPendingIntent(context),
            )
        }

    override suspend fun doWork(): Result = coroutineScope {
        withUIContext {
            val notification = progressNotification.build()
            val foregroundInfo = ForegroundInfo(Notifications.Id.Status.Progress, notification)
            setForeground(foregroundInfo)
        }
        if (!loginHelper.isLoggedIn()) {
            context.notificationManager.cancel(Notifications.Id.Status.Complete)
            errorNotification()
            return@coroutineScope Result.failure()
        }

        val type = inputData.getInt(SYNC_TO_MANGADEX, TO_DEX)
        val uuids = inputData.getStringArray(SYNC_UUIDS)
        val mangaIds = inputData.getLongArray(MANGA_IDS)

        TimberKt.d { "Number of mangaIds ${mangaIds!!.size}" }

        try {
            when (type) {
                FROM_DEX -> {
                    val total = customListSyncService.fromMangaDex(
                        uuids!!.toList(),
                        ::errorNotification,
                        ::updateNotificationProgress,
                        ::completeNotificationFromDex,
                    )
                    withUIContext {
                        applicationContext.toast(
                            applicationContext.getString(
                                R.string.sync_mdlist_to_library_toast,
                                total,
                            ),
                            Toast.LENGTH_LONG,
                        )
                    }
                }

                else -> {
                    customListSyncService.toMangaDex(
                        uuids!!.toList(),
                        mangaIds!!.toList(),
                        ::updateNotificationProgress,
                        ::completeNotificationToDex,
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
            notification,
        )
    }

    private fun completeNotificationToDex(total: Int) {
        completeNotification(R.string.sync_mdlist_complete)
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
        completeNotification(R.string.sync_to_mdlist_complete)
    }

    private fun completeNotification(@StringRes title: Int) {
        val notification = progressNotification
            .setContentTitle(context.getString(R.string.sync_mdlist_complete))
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification,
        )
    }

    private fun errorNotification(errorTxt: String? = null) {
        val notification = progressNotification
            .setContentTitle(errorTxt ?: context.getString(R.string.not_logged_into_mangadex_cannot_sync))
            .setAutoCancel(true)
            .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Status.Complete,
            notification,
        )
    }

    companion object {

        val TAG = "mdlist_sync_job"
        private const val SYNC_TO_MANGADEX = "sync_to_mangadex"
        private const val SYNC_UUIDS = "sync_uuids"
        private const val MANGA_IDS = "manga_ids"

        const val TO_DEX = 0
        const val FROM_DEX = 1

        fun toMangaDex(context: Context, listUuids: List<String>, mangaIds: List<Long>) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<CustomListSyncJob>().apply {
                    addTag(TAG)
                    setInputData(
                        Data.Builder().putInt(SYNC_TO_MANGADEX, TO_DEX).putStringArray(SYNC_UUIDS, listUuids.toTypedArray())
                            .putLongArray(MANGA_IDS, mangaIds.toLongArray())
                            .build(),
                    )
                }.build(),
            )
        }

        fun fromMangaDex(context: Context, listUuids: List<String>) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<CustomListSyncJob>().apply {
                    addTag(TAG)
                    setInputData(
                        Data.Builder().putInt(SYNC_TO_MANGADEX, FROM_DEX).putStringArray(SYNC_UUIDS, listUuids.toTypedArray())
                            .build(),
                    )
                }.build(),
            )
        }
    }
}
