package eu.kanade.tachiyomi.jobs.tracking

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class TrackingSyncService {

    val db: DatabaseHelper = Injekt.get()
    val trackManager: TrackManager = Injekt.get()

    val scope = CoroutineScope(Dispatchers.IO)

    suspend fun process(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ) {

        var count = 0
        val librayMangaList = db.getLibraryMangaList().executeAsBlocking()
        val loggedServices = trackManager.services.filter { it.isLogged }

        librayMangaList.forEach { manga ->
            updateNotification(manga.title, count++, librayMangaList.size)

            val tracks = db.getTracks(manga).executeOnIO()
            tracks.forEach { track ->
                val service = trackManager.getService(track.sync_id)
                if (service != null && service in loggedServices) {
                    scope.launchIO {
                        try {
                            val newTrack = service.refresh(track)
                            db.insertTrack(newTrack).executeOnIO()
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                XLog.e(e)
                            }
                        }
                    }
                }
                delay(Duration.Companion.seconds(1))
            }
        }
        completeNotification()
    }
}
