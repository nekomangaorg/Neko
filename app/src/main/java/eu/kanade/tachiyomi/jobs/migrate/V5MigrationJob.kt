package eu.kanade.tachiyomi.jobs.migrate

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.coroutineScope
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class V5MigrationJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    val v5MigrationService: V5MigrationService by injectLazy()

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    private val progressNotification =
        applicationContext.notificationBuilder(Notifications.Channel.v5Migration).apply {
            setContentTitle(context.getString(R.string.v5_migration_service))
            setSmallIcon(R.drawable.ic_neko_notification)
            setLargeIcon(notificationBitmap)
            setAutoCancel(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.cancel),
                NotificationReceiver.cancelV5MigrationUpdatePendingBroadcast(context),
            )
        }

    override suspend fun doWork(): Result = coroutineScope {
        withUIContext {
            val notification = progressNotification.build()
            val foregroundInfo = ForegroundInfo(Notifications.Id.V5.Progress, notification)
            setForeground(foregroundInfo)
        }
        try {
            v5MigrationService.migrateLibraryToV5(
                context,
                ::updateNotificationProgress,
                ::completeNotification,
                ::errorNotification,
            )
            return@coroutineScope Result.success()
        } catch (e: Exception) {
            TimberKt.e(e) { "error with v5 migration follows" }
            return@coroutineScope Result.failure()
        }
    }

    private fun updateNotificationProgress(title: String, progress: Int, total: Int) {
        val notification =
            progressNotification.setContentTitle(title).setProgress(total, progress, false).build()
        applicationContext.notificationManager.notify(
            Notifications.Id.V5.Progress,
            notification,
        )
    }

    private fun completeNotification(numberProcessed: Int) {
        val notification =
            applicationContext
                .notificationBuilder(Notifications.Channel.v5Migration)
                .apply {
                    setSmallIcon(R.drawable.ic_neko_notification)
                    setLargeIcon(notificationBitmap)
                    setAutoCancel(true)
                    setContentTitle(context.getString(R.string.v5_migration_complete))
                    setContentText(
                        context.getString(
                            R.string.number_of_migrated,
                            numberProcessed,
                        ),
                    )
                }
                .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.V5.Complete,
            notification,
        )
    }

    private fun errorNotification(errors: List<String>, uri: Uri?) {
        val notification =
            applicationContext
                .notificationBuilder(Notifications.Channel.v5Migration)
                .apply {
                    setContentTitle(context.getString(R.string.v5_migration_service))
                    setSmallIcon(R.drawable.ic_neko_notification)
                    setLargeIcon(notificationBitmap)
                    setAutoCancel(true)
                    setContentTitle(
                        context.resources.getQuantityString(
                            R.plurals.notification_update_failed,
                            errors.size,
                            errors.size,
                        ),
                    )
                    addAction(
                        R.drawable.ic_folder_24dp,
                        context.getString(R.string.view_all_errors),
                        NotificationReceiver.openErrorLogPendingActivity(context, uri!!),
                    )
                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                errors.joinToString("\n") { it.chop(TITLE_MAX_LEN) },
                            ),
                    )
                }
                .build()
        context.applicationContext.notificationManager.notify(
            Notifications.Id.Status.Error,
            notification,
        )
    }

    companion object {
        const val TAG = "V5Migration"
        private const val TITLE_MAX_LEN = 45

        fun doWorkNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<V5MigrationJob>().addTag(TAG).build())
        }
    }
}
