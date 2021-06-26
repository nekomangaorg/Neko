package eu.kanade.tachiyomi.data.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StatusSyncJob(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

    /*  val followsSyncService: FollowsSyncService by injectLazy()

      override suspend fun doWork(): Result = coroutineScope {
          val notification =
              NotificationCompat.Builder(applicationContext, Notifications.Channel.Status)
                  .setContentTitle("Updating similar manga database")
                  .setSmallIcon(R.drawable.ic_neko_notification)
                  .setOngoing(true)
                  .setAutoCancel(true)
                  .build()
          val foregroundInfo = ForegroundInfo(Notifications.Id.Status.Progress, notification)
          setForeground(foregroundInfo)

          *//*      followsSyncService.syncFrom().collect {
                  return@coroutineScope Result.success()
              }*//*
        Result.success()

    }

    companion object {
        fun doWorkNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<StatusSyncJob>().apply {

                }.build()
            )
        }
    }*/
}