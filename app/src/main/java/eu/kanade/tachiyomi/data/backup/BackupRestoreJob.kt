package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.jobs.library.LibraryRepairGate
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupRestoreJob(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val notifier = BackupNotifier(context)
    private val restorer = BackupRestorer(context, notifier)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notifier.showRestoreProgress(progress = -1).build()
        val id = Notifications.ID_RESTORE_PROGRESS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {
        if (Injekt.get<LibraryRepairGate>().isRunning) return Result.retry()
        tryToSetForeground()

        val uriPath = inputData.getString(BackupConst.EXTRA_URI) ?: return Result.failure()

        val uri = Uri.parse(uriPath) ?: return Result.failure()

        withIOContext {
            try {
                if (!restorer.restoreBackup(uri)) {
                    notifier.showRestoreError(context.getString(R.string.restoring_backup_canceled))
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    notifier.showRestoreError(context.getString(R.string.restoring_backup_canceled))
                } else {
                    restorer.writeErrorLog()
                    notifier.showRestoreError(exception.message)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val TAG = "BackupRestorer"

        fun start(context: Context, uri: Uri) {
            if (Injekt.get<LibraryRepairGate>().refuseStart()) return
            val request =
                OneTimeWorkRequestBuilder<BackupRestoreJob>()
                    .addTag(TAG)
                    .setInputData(workDataOf(BackupConst.EXTRA_URI to uri.toString()))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }

        fun isRunningFlow(context: Context): Flow<Boolean> =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(TAG).map { list ->
                list.any { !it.state.isFinished }
            }
    }
}
