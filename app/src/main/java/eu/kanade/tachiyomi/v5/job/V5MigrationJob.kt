package eu.kanade.tachiyomi.v5.job

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class V5MigrationJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        V5MigrationService.start(context)
        return Result.success()
    }

    companion object {
        private const val TAG = "V5Migration"

        fun doWorkNow() {
            WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<V5MigrationJob>().build())
        }
    }
}
