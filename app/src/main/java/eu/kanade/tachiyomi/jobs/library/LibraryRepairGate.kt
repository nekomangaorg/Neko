package eu.kanade.tachiyomi.jobs.library

import android.content.Context
import androidx.annotation.StringRes
import androidx.work.WorkInfo
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.backup.BackupRestoreJob
import eu.kanade.tachiyomi.data.download.DownloadJob
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.jobs.tracking.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.jobs.tracking.TrackingSyncJob
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.concurrent.atomic.AtomicBoolean
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Keeps the library repair apart from the jobs that write the same rows. The repair holds the gate
 * while it runs. The gated jobs check it when they start, and their start paths refuse with a
 * toast. WorkManager runs in the app process, so one in-process flag reaches every job.
 */
class LibraryRepairGate(
    context: Context,
    private val showToast: (UiText) -> Unit = { text -> launchUI { context.toast(text) } },
) {
    private val running = AtomicBoolean(false)

    val isRunning: Boolean
        get() = running.get()

    fun tryAcquire(): Boolean = running.compareAndSet(false, true)

    fun release() {
        running.set(false)
    }

    /** Returns true while the repair runs, after a toast that says why nothing started. */
    fun refuseStart(): Boolean {
        val refused = isRunning
        if (refused) showToast(UiText.StringResource(R.string.library_repair_running))
        return refused
    }

    /** Gated jobs that are running now, plus downloads when the queue holds any, paused or not. */
    suspend fun blockingWork(): List<BlockingWork> = withIOContext {
        val workManager = Injekt.get<WorkManager>()
        val runningJobs =
            BlockingWork.entries.filter { work ->
                workManager.getWorkInfosByTag(work.tag).get().any {
                    it.state == WorkInfo.State.RUNNING
                }
            }
        val queued = Injekt.get<DownloadManager>().queueState.value.isNotEmpty()
        (runningJobs + listOfNotNull(BlockingWork.DOWNLOADS.takeIf { queued })).distinct()
    }
}

/** Work the repair waits for, by WorkManager tag, with the name the user sees. */
enum class BlockingWork(val tag: String, @StringRes val label: Int) {
    LIBRARY_UPDATE(LibraryUpdateJob.TAG, R.string.library_repair_work_update),
    RESTORE(BackupRestoreJob.TAG, R.string.library_repair_work_restore),
    MANGADEX_SYNC(StatusSyncJob.TAG, R.string.library_repair_work_sync),
    TRACKING_SYNC(TrackingSyncJob.TAG, R.string.library_repair_work_tracking),
    DELAYED_TRACKING(DelayedTrackingUpdateJob.TAG, R.string.library_repair_work_tracking),
    DOWNLOADS(DownloadJob.TAG, R.string.library_repair_work_downloads),
}

/** The names the user sees for this work, joined by commas. Both tracking jobs show as one name. */
fun List<BlockingWork>.names(context: Context): String {
    return map { context.getString(it.label) }.distinct().joinToString(", ")
}
