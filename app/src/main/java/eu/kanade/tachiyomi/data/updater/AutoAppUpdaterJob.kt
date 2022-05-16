package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import kotlinx.coroutines.coroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AutoAppUpdaterJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                return@coroutineScope Result.failure()
            }
            val preferences = Injekt.get<PreferencesHelper>()
            if (preferences.appShouldAutoUpdate() == ONLY_ON_UNMETERED && !context.isConnectedToWifi()) {
                return@coroutineScope Result.failure()
            }
            val result = AppUpdateChecker().checkForUpdate(context, true, doExtrasAfterNewUpdate = false)
            if (result is AppUpdateResult.NewUpdate && !AppUpdateService.isRunning()) {
                AppUpdateNotifier(context).cancel()
                AppUpdateNotifier.releasePageUrl = result.release.releaseLink
                AppUpdateService.start(context, result.release.downloadLink, false)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "AutoUpdateRunner"
        const val ALWAYS = 0
        const val ONLY_ON_UNMETERED = 1
        const val NEVER = 2

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresDeviceIdle(true)
                .build()

            val request = OneTimeWorkRequestBuilder<AutoAppUpdaterJob>()
                .addTag(TAG)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelTask(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }
}
