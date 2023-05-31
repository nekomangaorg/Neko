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
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackStatusService
import eu.kanade.tachiyomi.util.system.loggycat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DelayedTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private data class DelayedTracking(val mangaId: Long, val syncId: Int, val lastReadChapter: Float) {
        fun print() = "$mangaId:$syncId:$lastReadChapter"
    }

    override suspend fun doWork(): Result {
        loggycat { "Starting Delayed Tracking Update Job" }
        val preferences = Injekt.get<PreferencesHelper>()
        val db = Injekt.get<DatabaseHelper>()
        val trackManager = Injekt.get<TrackManager>()
        val trackings = preferences.trackingsToAddOnline().get().toMutableSet().mapNotNull {
            val items = it.split(":")
            if (items.size != 3) {
                loggycat(LogPriority.ERROR) {
                    "items size was not 3 after split for: $it"
                }
                null
            } else {
                val mangaId = items[0].toLongOrNull() ?: return@mapNotNull null
                val syncId = items[1].toIntOrNull() ?: return@mapNotNull null
                val chapterNumber = items[2].toFloatOrNull() ?: return@mapNotNull null
                DelayedTracking(mangaId, syncId, chapterNumber)
            }
        }.groupBy { it.mangaId }
        withContext(Dispatchers.IO) {
            val trackingsToAdd = mutableSetOf<String>()
            trackings.forEach { entry ->
                val mangaId = entry.key
                val trackList = db.getTracks(mangaId).executeAsBlocking()
                entry.value.map { delayedTracking ->
                    val service = trackManager.getService(delayedTracking.syncId)
                    val track = trackList.find { track -> track.sync_id == delayedTracking.syncId }
                    if (service != null && track != null && service is TrackStatusService) {
                        try {
                            track.last_chapter_read = delayedTracking.lastReadChapter
                            service.update(track, true)
                            db.insertTrack(track).executeAsBlocking()
                        } catch (e: Exception) {
                            trackingsToAdd.add(delayedTracking.print())
                            loggycat(LogPriority.ERROR, e)
                        }
                    }
                }
            }
            preferences.trackingsToAddOnline().set(trackingsToAdd)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
