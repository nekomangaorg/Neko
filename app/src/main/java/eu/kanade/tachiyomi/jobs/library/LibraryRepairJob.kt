package eu.kanade.tachiyomi.jobs.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.tracking.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import java.io.File
import kotlinx.coroutines.CancellationException
import org.nekomanga.domain.track.store.DelayedTrackingStore
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Runs [LibraryRepairer] once. The job holds [LibraryRepairGate] for the whole run and repairs
 * nothing while work that writes the same rows is running.
 */
class LibraryRepairJob(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val notifier = LibraryRepairNotifier(context)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notifier.showProgress(0, 0).build()
        val id = Notifications.Id.LibraryRepair.Progress
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {
        val gate = Injekt.get<LibraryRepairGate>()
        if (!gate.tryAcquire()) return Result.success()
        val result = RepairResult()
        var repairStarted = false
        try {
            val blocking = gate.blockingWork()
            if (blocking.isNotEmpty()) {
                notifier.showBlocked(blocking)
                return Result.success()
            }
            repairStarted = true
            tryToSetForeground()
            val throttle = ProgressThrottle(PROGRESS_INTERVAL_MS) { SystemClock.elapsedRealtime() }
            Injekt.get<LibraryRepairer>().repair(result) { done, total ->
                if (throttle.tryPass()) notifier.showProgress(done, total)
            }
            notifier.showComplete(result, writeLog(result))
            return Result.success()
        } catch (e: CancellationException) {
            notifier.showStopped(result, writeLog(result))
            throw e
        } catch (e: Exception) {
            TimberKt.e(e) { "Library repair failed" }
            notifier.showError(e.message)
            return Result.failure()
        } finally {
            gate.release()
            // Each retry while the gate is held counts toward the delayed tracking update's limit,
            // so it is queued again with a fresh count
            if (repairStarted && Injekt.get<DelayedTrackingStore>().getItems().isNotEmpty()) {
                DelayedTrackingUpdateJob.setupTask(applicationContext)
            }
        }
    }

    /** Writes the titles to check into the cache folder, or returns null when there are none. */
    private fun writeLog(result: RepairResult): File? {
        val sections =
            listOf(
                    "Skipped" to result.skipped.map { "${it.title}: ${it.reason.describe()}" },
                    "Failed" to result.failed,
                    "Tracking conflicts" to
                        result.trackingConflicts.map {
                            "${it.title}: ${serviceName(it.serviceId)}"
                        },
                )
                .filter { (_, lines) -> lines.isNotEmpty() }
        if (sections.isEmpty()) return null
        val dir = applicationContext.externalCacheDir ?: return null
        return try {
            val text =
                sections.joinToString("\n\n") { (header, lines) ->
                    (listOf(header) + lines).joinToString("\n")
                }
            File(dir, LOG_FILE).apply { writeText(text + "\n") }
        } catch (e: Exception) {
            TimberKt.e(e) { "Could not write the library repair log" }
            null
        }
    }

    private fun SkipReason.describe(): String =
        when (this) {
            SkipReason.NotMangaDex -> "not a MangaDex url or source"
            is SkipReason.DownloadFolders ->
                "more than one download folder exists (${names.joinToString(", ")})"
        }

    private fun serviceName(serviceId: Int): String {
        val service = Injekt.get<TrackManager>().getService(serviceId)
        return service?.let { applicationContext.getString(it.nameRes()) } ?: "service $serviceId"
    }

    companion object {
        const val TAG = "LibraryRepair"
        private const val LOG_FILE = "neko_repair.log"
        private const val PROGRESS_INTERVAL_MS = 1000L

        fun start(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<LibraryRepairJob>()
                    .addTag(TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}

/**
 * Lets the first call through, then at most one call per [intervalMs]. Progress goes through it
 * because Android drops updates to a posted notification while an app posts more than five a
 * second, and the result notification is such an update when an earlier result is still showing.
 */
internal class ProgressThrottle(private val intervalMs: Long, private val now: () -> Long) {
    private var last: Long? = null

    fun tryPass(): Boolean {
        val time = now()
        val previous = last
        if (previous != null && time - previous < intervalMs) return false
        last = time
        return true
    }
}
