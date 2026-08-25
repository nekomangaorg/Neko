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
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import java.util.concurrent.TimeUnit
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.store.DelayedTrackingStore
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DelayedTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        TimberKt.d { "Starting Delayed Tracking Update Job" }
        if (runAttemptCount > 3) {
            return Result.failure()
        }

        val delayedTrackingStore = Injekt.get<DelayedTrackingStore>()
        val trackRepository = Injekt.get<TrackRepository>()
        val trackManager = Injekt.get<TrackManager>()

        var hasErrors = false

        withIOContext {
            delayedTrackingStore.getItems().forEach { item ->
                val track = trackRepository.getTrackById(item.trackId)
                if (track == null) {
                    delayedTrackingStore.remove(item.trackId)
                    return@forEach
                }
                if (item.lastChapterRead <= track.last_chapter_read) {
                    delayedTrackingStore.remove(item.trackId)
                    return@forEach
                }
                track.last_chapter_read = item.lastChapterRead
                TimberKt.d {
                    "Updating delayed track item: ${track.manga_id}, last chapter read: ${track.last_chapter_read}"
                }
                withNonCancellableContext {
                    val service = trackManager.getService(track.sync_id)
                    if (service == null) {
                        delayedTrackingStore.remove(item.trackId)
                    } else {
                        try {
                            val updatedTrack = service.update(track, true)
                            trackRepository.insertTrack(updatedTrack)
                            delayedTrackingStore.remove(item.trackId)
                        } catch (e: Exception) {
                            hasErrors = true
                            TimberKt.e(e) { "Error inserting for delayed tracker" }
                        }
                    }
                }
            }
        }

        return if (hasErrors) {
            Result.retry()
        } else {
            Result.success()
        }
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
