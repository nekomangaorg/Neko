package eu.kanade.tachiyomi.jobs.tracking

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingCoordinator
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSyncProcessor {

    val db: DatabaseHelper = Injekt.get()
    val trackManager: TrackManager = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    val scope = CoroutineScope(Dispatchers.IO)

    suspend fun process(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ) {
        var count = 0
        val libraryMangaList = db.getLibraryMangaList().executeAsBlocking()
        val loggedServices = trackManager.services.values.filter { it.isLogged() }
        val autoAddTracker = preferences.autoAddTracker().get()
        val trackingCoordinator: TrackingCoordinator = Injekt.get()

        libraryMangaList.forEach { manga ->
            updateNotification(manga.title, count++, libraryMangaList.size)

            val tracks = db.getTracks(manga).executeOnIO()

            if (autoAddTracker.size > 1) {
                val validContentRatings = preferences.autoTrackContentRatingSelections().get()
                val contentRating = manga.getContentRating()
                if (
                    contentRating == null || validContentRatings.contains(contentRating.lowercase())
                ) {
                    autoAddTracker
                        .map { it.toInt() }
                        .map { autoAddTrackerId ->
                            if (tracks.firstOrNull { it.sync_id == autoAddTrackerId } == null) {
                                loggedServices
                                    .firstOrNull { it.id == autoAddTrackerId }
                                    ?.let { trackService ->
                                        scope.launchIO {
                                            try {
                                                val trackServiceItem =
                                                    trackService.toTrackServiceItem()
                                                val id =
                                                    trackManager.getIdFromManga(
                                                        trackServiceItem,
                                                        manga,
                                                    )
                                                if (id != null) {
                                                    val trackResult =
                                                        trackingCoordinator.searchTrackerNonFlow(
                                                            "",
                                                            trackManager
                                                                .getService(trackService.id)!!
                                                                .toTrackServiceItem(),
                                                            manga,
                                                            false,
                                                        )
                                                    when (trackResult) {
                                                        is TrackingConstants.TrackSearchResult.Success -> {
                                                            val trackSearchItem =
                                                                trackResult.trackSearchResult[0]
                                                            trackingCoordinator.registerTracking(
                                                                TrackingConstants.TrackAndService(
                                                                    trackSearchItem.trackItem,
                                                                    trackServiceItem,
                                                                ),
                                                                manga.id!!,
                                                            )
                                                        }
                                                        else -> Unit
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                if (e !is CancellationException) {
                                                    TimberKt.e(e)
                                                }
                                            }
                                        }
                                    }
                                delay(1.seconds)
                            }
                        }
                }
            }

            tracks.forEach { track ->
                val service = trackManager.getService(track.sync_id)
                if (service != null && service in loggedServices) {
                    scope.launchIO {
                        try {
                            val newTrack = service.refresh(track)
                            db.insertTrack(newTrack).executeOnIO()
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                TimberKt.e(e)
                            }
                        }
                    }
                    delay(1.seconds)
                }
            }
        }
        completeNotification()
    }
}
