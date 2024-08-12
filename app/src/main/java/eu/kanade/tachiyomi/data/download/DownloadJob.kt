package eu.kanade.tachiyomi.data.download

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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

        var networkCheck =
            checkNetworkState(
                applicationContext.activeNetworkState(),
                preferences.downloadOnlyOverWifi().get(),
            )
        var active = networkCheck && downloadManager.downloaderStart()

        if (!active) {
            return Result.failure()
        }
        tryToSetForeground()

        coroutineScope {
            combineTransform(
                    applicationContext.networkStateFlow(),
                    preferences.downloadOnlyOverWifi().changes(),
                    transform = { a, b -> emit(checkNetworkState(a, b)) },
                )
                .onEach { networkCheck = it }
                .launchIn(this)
        }

        // Keep the worker running when needed
        while (active) {
            active = !isStopped && downloadManager.isRunning && networkCheck
        }

        return Result.success()
    }

    private fun checkNetworkState(state: NetworkState, requireWifi: Boolean): Boolean {
        return if (state.isOnline) {
            val noWifi = requireWifi && !state.isWifi
            if (noWifi) {
                downloadManager.downloaderStop(
                    applicationContext.getString(R.string.no_wifi_connection),
                )
            }
            !noWifi
        } else {
            downloadManager.downloaderStop(
                applicationContext.getString(R.string.no_network_connection))
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
