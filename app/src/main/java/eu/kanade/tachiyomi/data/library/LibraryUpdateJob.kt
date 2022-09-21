package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.DEVICE_BATTERY_NOT_LOW
import eu.kanade.tachiyomi.data.preference.DEVICE_CHARGING
import eu.kanade.tachiyomi.data.preference.DEVICE_ONLY_ON_WIFI
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import java.util.concurrent.TimeUnit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val preferences = Injekt.get<PreferencesHelper>()
        return if (requiresWifiConnection(preferences) && !context.isConnectedToWifi()) {
            Result.failure()
        } else if (LibraryUpdateService.start(context)) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        const val TAG = "LibraryUpdate"

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.libraryUpdateInterval().get()
            if (interval > 0) {
                val restrictions = preferences.libraryUpdateDeviceRestriction().get()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(DEVICE_CHARGING in restrictions)
                    .setRequiresBatteryNotLow(DEVICE_BATTERY_NOT_LOW in restrictions)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }

        fun requiresWifiConnection(preferences: PreferencesHelper): Boolean {
            val restrictions = preferences.libraryUpdateDeviceRestriction().get()
            return DEVICE_ONLY_ON_WIFI in restrictions
        }
    }
}
