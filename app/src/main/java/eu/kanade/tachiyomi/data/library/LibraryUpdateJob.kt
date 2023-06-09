package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import java.util.concurrent.TimeUnit
import org.nekomanga.core.preferences.DEVICE_BATTERY_NOT_LOW
import org.nekomanga.core.preferences.DEVICE_CHARGING
import org.nekomanga.core.preferences.DEVICE_ONLY_ON_WIFI
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val libraryPreferences = Injekt.get<LibraryPreferences>()
        return if (requiresWifiConnection(libraryPreferences) && !context.isConnectedToWifi()) {
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
            val libraryPreferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: libraryPreferences.updateInterval().get()
            if (interval > 0) {
                val restrictions = libraryPreferences.updateRestrictions().get()

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

        fun requiresWifiConnection(libraryPreferences: LibraryPreferences): Boolean {
            val restrictions = libraryPreferences.updateRestrictions().get()
            return DEVICE_ONLY_ON_WIFI in restrictions
        }
    }
}
