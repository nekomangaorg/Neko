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
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class DelayedTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        XLog.d("Starting Delayed Tracking Update Job")
        val preferences = Injekt.get<PreferencesHelper>()
        val db = Injekt.get<DatabaseHelper>()
        val trackManager = Injekt.get<TrackManager>()
        val trackings = preferences.trackingsToAddOnline().get().toMutableSet().mapNotNull {
            val items = it.split(":")
            if (items.size != 3) {
                null
            } else {
                val mangaId = items[0].toLongOrNull() ?: return@mapNotNull null
                val trackId = items[1].toIntOrNull() ?: return@mapNotNull null
                val chapterNumber = items[2].toIntOrNull() ?: return@mapNotNull null
                mangaId to (trackId to chapterNumber)
            }
        }.groupBy { it.first }
        withContext(Dispatchers.IO) {
            trackings.forEach {
                val mangaId = it.key
                val manga = db.getManga(mangaId).executeAsBlocking() ?: return@withContext
                val trackList = db.getTracks(manga).executeAsBlocking()
                it.value.map { tC ->
                    val trackChapter = tC.second
                    val service = trackManager.getService(trackChapter.first)
                    val track = trackList.find { track -> track.sync_id == trackChapter.first }
                    if (service != null && track != null) {
                        try {
                            track.last_chapter_read = trackChapter.second
                            service.update(track, true)
                            db.insertTrack(track).executeAsBlocking()
                        } catch (e: Exception) {
                            XLog.e(e)
                        }
                    }
                }
            }
            preferences.trackingsToAddOnline().set(emptySet())
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
