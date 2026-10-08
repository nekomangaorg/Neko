package eu.kanade.tachiyomi.data.download

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.lifecycle.asFlow
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import org.nekomanga.R
import uy.kohesive.injekt.injectLazy

class DownloadJob(val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {
    private val downloadManager by injectLazy<DownloadManager>()
    private val preferences by injectLazy<PreferencesHelper>()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val firstDL = downloadManager.queueState.value.firstOrNull()
        val notification = DownloadNotifier(context).setPlaceholder(firstDL).build()
        val id = Notifications.Id.Download.Progress
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {

        val networkCheck =
            checkNetworkState(
                applicationContext.activeNetworkState(),
                preferences.downloadOnlyOverUnmetered().get(),
            )
        if (!networkCheck) {
            return Result.failure()
        }
        // A REPLACE restart can land while the downloader is running, and Downloader.start
        // refuses then. This worker takes over, since the one it replaced is gone.
        val active = downloadManager.isRunning || downloadManager.downloaderStart()

        if (!active) {
            return Result.success()
        }
        try {
            tryToSetForeground()

            coroutineScope {
                // checkNetworkState stops the downloader when the network drops, or turns
                // metered while unmetered-only is on
                val networkWatcher =
                    combine(
                            applicationContext.networkStateFlow(),
                            preferences.downloadOnlyOverUnmetered().changes(),
                        ) { state, requireUnmetered ->
                            checkNetworkState(state, requireUnmetered)
                        }
                        .launchIn(this)

                // Keep the worker running while the downloader runs. The network flow never
                // completes, so the watcher has to be cancelled before this scope can return.
                while (!isStopped && downloadManager.isRunning) {
                    delay(1.seconds)
                }
                networkWatcher.cancel()
            }
        } catch (e: Exception) {
            // The worker fails, so the downloader would keep going without it
            if (e !is CancellationException && downloadManager.isRunning) {
                downloadManager.pauseDownloads()
            }
            throw e
        } finally {
            // When the system stops the worker (quota, foreground service timeout) the
            // downloader would keep going without a foreground service, so pause it
            if (stoppedBySystem() && downloadManager.isRunning) {
                downloadManager.pauseDownloads()
            }
        }

        return Result.success()
    }

    /**
     * A cancel from the app is a REPLACE restart or [stop], and neither should touch the
     * downloader. getStopReason is marked API 31 because its values follow JobParameters, but it
     * reads a field WorkManager sets on every API level: CANCELLED_BY_APP for a cancel, UNKNOWN for
     * a system stop below API 31.
     */
    @SuppressLint("NewApi")
    private fun stoppedBySystem() = isStopped && stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP

    private fun checkNetworkState(state: NetworkState, requireUnmetered: Boolean): Boolean {
        return if (state.isOnline) {
            val noWifi = requireUnmetered && !state.isUnmetered
            if (noWifi) {
                downloadManager.downloaderStop(
                    applicationContext.getString(R.string.no_unmetered_connection)
                )
            }
            !noWifi
        } else {
            downloadManager.downloaderStop(
                applicationContext.getString(R.string.no_network_connection)
            )
            false
        }
    }

    companion object {
        private const val TAG = "Downloader"

        fun start(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<DownloadJob>()
                    .addTag(TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }

        fun isRunning(context: Context): Boolean {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWork(TAG).get().let { list
                ->
                list.count { it.state == WorkInfo.State.RUNNING } == 1
            }
        }

        fun isRunningFlow(context: Context): Flow<Boolean> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(TAG)
                .asFlow()
                .map { list -> list.count { it.state == WorkInfo.State.RUNNING } == 1 }
        }
    }
}
