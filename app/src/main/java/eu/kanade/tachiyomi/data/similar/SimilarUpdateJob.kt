package eu.kanade.tachiyomi.data.similar

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SimilarUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val localFile = inputData.getString("localFile")
        val cachedManga = inputData.getBoolean("cachedManga", false)

        SimilarUpdateService.start(context, localFile, cachedManga)
        return Result.success()
    }

    companion object {
        const val TAG = "RelatedUpdate"

        fun setupTask(skipInitial: Boolean = false) {

            val preferences = Injekt.get<PreferencesHelper>()
            val enabled = preferences.similarEnabled().get()
            val interval = preferences.similarUpdateInterval().getOrDefault()
            if (enabled) {

                // We are enabled, so construct the constraints
                val wifiRestriction = if (preferences.similarOnlyOverWifi())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(wifiRestriction)
                    .build()

                // If we are not skipping the initial then run it right now
                // Note that we won't run it if the constraints are not satisfied
                if (!skipInitial) {
                    WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SimilarUpdateJob>().setConstraints(constraints).build())
                }

                // Finally build the periodic request
                val request = PeriodicWorkRequestBuilder<SimilarUpdateJob>(
                    interval.toLong(), TimeUnit.DAYS,
                    1, TimeUnit.HOURS
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()
                if (interval > 0) {
                    WorkManager.getInstance().enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
                } else {
                    WorkManager.getInstance().cancelAllWorkByTag(TAG)
                }
            } else {
                WorkManager.getInstance().cancelAllWorkByTag(TAG)
            }
        }

        fun doWorkNow(updateCachedManga: Boolean = false) {
            val work = OneTimeWorkRequestBuilder<SimilarUpdateJob>()
            if (updateCachedManga) {
                val data = Data.Builder()
                data.putBoolean("cachedManga", true)
                work.setInputData(data.build())
            }
            WorkManager.getInstance().enqueue(work.build())
        }

        fun doWorkNowLocal(localFile: Uri) {
            val data = Data.Builder()
            data.putString("localFile", localFile.toString())
            val work = OneTimeWorkRequestBuilder<SimilarUpdateJob>()
            work.setInputData(data.build())
            WorkManager.getInstance().enqueue(work.build())
        }
    }
}
