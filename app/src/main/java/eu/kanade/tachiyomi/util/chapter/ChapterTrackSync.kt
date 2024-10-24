package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.jobs.tracking.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import kotlinx.coroutines.delay
import org.nekomanga.domain.track.store.DelayedTrackingStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Starts the service that updates the last chapter read in sync services. This operation will run
 * in a background thread and errors are ignored.
 */
fun updateTrackChapterMarkedAsRead(
    newLastChapter: Chapter?,
    mangaId: Long?,
    delay: Long = 3000,
    fetchTracks: (suspend () -> Unit)? = null,
) {
    val preferences = Injekt.get<PreferencesHelper>()
    if (!preferences.trackMarkedAsRead().get()) return
    mangaId ?: return

    val newChapterRead = newLastChapter?.chapter_number ?: 0f

    launchIO {
        withNonCancellableContext {
            delay(delay)
            updateTrackChapterRead(mangaId, newChapterRead)
            fetchTracks?.invoke()
        }
    }
}

suspend fun updateTrackChapterRead(
    mangaId: Long?,
    newChapterRead: Float,
    retryWhenOnline: Boolean = false,
    onError: ((TrackService, String?) -> Unit)? = null,
) {
    withNonCancellableContext {
        val preferences = Injekt.get<PreferencesHelper>()
        val db = Injekt.get<DatabaseHelper>()
        val trackManager = Injekt.get<TrackManager>()

        val trackList = db.getTracks(mangaId).executeAsBlocking()
        trackList.map { track ->
            val service = trackManager.getService(track.sync_id)
            if (service != null && service.isLogged() && newChapterRead > track.last_chapter_read) {
                if (retryWhenOnline && !preferences.context.isOnline()) {
                    delayTrackingUpdate(preferences.context, newChapterRead, track)
                } else if (preferences.context.isOnline()) {
                    try {
                        track.last_chapter_read = newChapterRead
                        service.update(track, true)
                        db.insertTrack(track).executeAsBlocking()
                    } catch (e: Exception) {
                        onError?.invoke(service, e.localizedMessage)
                        if (retryWhenOnline) {
                            delayTrackingUpdate(preferences.context, newChapterRead, track)
                        }
                    }
                }
            }
        }
    }
}

private fun delayTrackingUpdate(context: Context, newChapterRead: Float, track: Track) {
    val delayedTrackingStore = Injekt.get<DelayedTrackingStore>()
    delayedTrackingStore.add(track.id!!, newChapterRead)
    DelayedTrackingUpdateJob.setupTask(context)
}
