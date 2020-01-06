package eu.kanade.tachiyomi.extension


import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.notification
import rx.Observable
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class ExtensionUpdateJob : Job() {

    override fun onRunJob(params: Params): Result {
        val extensionManager: ExtensionManager = Injekt.get()
        extensionManager.findAvailableExtensions()

        // Update favorite manga. Destroy service when completed or in case of an error.
        Observable.defer {
            extensionManager.getInstalledExtensionsObservable().map { list ->
                val pendingUpdates = list.filter { it.hasUpdate }
                if (pendingUpdates.isNotEmpty()) {
                    val names = pendingUpdates.map { it.name }
                    NotificationManagerCompat.from(context).apply {
                        notify(Notifications.ID_UPDATES_TO_EXTS,
                            context.notification(Notifications.CHANNEL_UPDATES_TO_EXTS) {
                                setContentTitle(
                                    context.getString(
                                        R.string.update_check_notification_ext_updates, names.size
                                    )
                                )
                                val extNames = if (names.size > 5) {
                                    "${names.take(4).joinToString(", ")}, " + context.getString(
                                        R.string.notification_and_n_more, (names.size - 4)
                                    )
                                } else names.joinToString(", ")
                                setContentText(extNames)
                                setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
                                setSmallIcon(R.drawable.ic_extension_update)
                                color = ContextCompat.getColor(context, R.color.colorAccentLight)
                                setContentIntent(
                                    NotificationReceiver.openExtensionsPendingActivity(
                                        context
                                    )
                                )
                                setAutoCancel(true)
                            })
                    }
                }
                Result.SUCCESS
            }
        }.subscribeOn(Schedulers.io())
            .subscribe({
            }, {
                Timber.e(it)
            }, {
            })
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "ExtensionUpdate"

        fun setupTask() {
            JobRequest.Builder(TAG).setPeriodic(TimeUnit.DAYS.toMillis(1),
                TimeUnit.HOURS.toMillis(1))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build().schedule()
        }

        fun cancelTask() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }
}