package eu.kanade.tachiyomi.jobs.tracking

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackingSyncService {

    val db: DatabaseHelper = Injekt.get()
    val trackManager: TrackManager = Injekt.get()

    suspend fun process(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ) {
        var count = 0
        val librayMangaList = db.getLibraryMangaList().executeAsBlocking()
        val loggedServices = trackManager.services.filter { it.isLogged }

        librayMangaList.forEach { manga ->
            updateNotification(manga.title, count++, librayMangaList.size)

            val tracks = db.getTracks(manga).executeAsBlocking()
            tracks.forEach { track ->
                val service = trackManager.getService(track.sync_id)
                if (service != null && service in loggedServices) {
                    try {
                        val newTrack = service.refresh(track)
                        db.insertTrack(newTrack).executeAsBlocking()
                    } catch (e: Exception) {
                        XLog.e(e)
                    }
                }
            }
        }
        completeNotification()
    }
}