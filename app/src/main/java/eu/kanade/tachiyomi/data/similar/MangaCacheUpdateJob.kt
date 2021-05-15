package eu.kanade.tachiyomi.data.similar

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class MangaCacheUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        MangaCacheUpdateService.start(context)
        return Result.success()
    }

    companion object {
        const val TAG = "MangaCacheUpdate"

        fun doWorkNow() {
            val work = OneTimeWorkRequestBuilder<MangaCacheUpdateJob>()
            val data = Data.Builder()
            work.setInputData(data.build())
            WorkManager.getInstance().enqueue(work.build())
        }

    }
}