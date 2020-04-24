package eu.kanade.tachiyomi.data.similar

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SimilarUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        SimilarUpdateService.start(context)
        return Result.success()
    }

    companion object {
        const val TAG = "RelatedUpdate"

        fun setupTask() {

            val preferences = Injekt.get<PreferencesHelper>()
            val enabled = preferences.similarEnabled()
            if (enabled) {
                val restrictions = preferences.similarUpdateRestriction()!!
                val acRestriction = "ac" in restrictions
                val wifiRestriction = if ("wifi" in restrictions)
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(wifiRestriction)
                    .setRequiresCharging(acRestriction)
                    .build()

                val request = PeriodicWorkRequestBuilder<SimilarUpdateJob>(3, TimeUnit.DAYS, 1, TimeUnit.HOURS)
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SimilarUpdateJob>().build())
                WorkManager.getInstance().enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance().cancelAllWorkByTag(TAG)
            }
        }
    }
}
