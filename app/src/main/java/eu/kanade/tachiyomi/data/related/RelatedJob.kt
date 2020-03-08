package eu.kanade.tachiyomi.data.related
//
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest

//
class RelatedJob : Job() {
    override fun onRunJob(params: Params): Result {
        RelatedUpdateService.start(context)
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "RelatedUpdater"

        //
        fun setupTask() {
            JobRequest.Builder(TAG)
                .setPeriodic(24 * 60 * 60 * 1000, 60 * 60 * 1000)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule()
        }

        fun runTaskNow() {
            JobRequest.Builder(TAG)
                .startNow()
                .build()
                .schedule()
        }

        //
        fun cancelTask() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }
}
