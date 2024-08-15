package eu.kanade.tachiyomi.jobs.tracking

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackStatusService
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import java.util.concurrent.TimeUnit
import org.nekomanga.domain.track.store.DelayedTrackingStore
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DelayedTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private data class DelayedTracking(
        val mangaId: Long,
        val syncId: Int,
        val lastReadChapter: Float
    ) {
        fun print() = "$mangaId:$syncId:$lastReadChapter"
    }

    override suspend fun doWork(): Result {
        TimberKt.d { "Starting Delayed Tracking Update Job" }
        if (runAttemptCount > 3) {
            return Result.failure()
        }

        val delayedTrackingStore = Injekt.get<DelayedTrackingStore>()
        val db = Injekt.get<DatabaseHelper>()
        val trackManager = Injekt.get<TrackManager>()

        withIOContext {
            delayedTrackingStore
                .getItems()
                .mapNotNull {
                    val track = db.getTrackByTrackId(it.trackId).executeOnIO()
                    if (track == null) {
                        delayedTrackingStore.remove(it.trackId)
                    }
                    track?.last_chapter_read = it.lastChapterRead
                    track
                }
                .forEach { track ->
                    TimberKt.d {
                        "Updating delayed track item: ${track.manga_id}, last chapter read: ${track.last_chapter_read}"
                    }
                    withNonCancellableContext {
                        val service = trackManager.getService(track.sync_id)
                        when (service == null) {
                            true -> delayedTrackingStore.remove(track.id!!)
                            false -> {
                                try {
                                    service.update(track, true)
                                    db.insertTrack(track).executeAsBlocking()
                                } catch (e: Exception) {
                                    delayedTrackingStore.add(track.id!!, track.last_chapter_read)
                                    TimberKt.e(e) { "Error inserting for delayed tracker" }
                                }
                            }
                        }
                    }
                }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request =
                OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
