package eu.kanade.tachiyomi.jobs.tracking

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingCoordinator
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSyncProcessor(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    val db: DatabaseHelper = Injekt.get()
    val trackManager: TrackManager = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    suspend fun process(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ) =
        withContext(dispatcher) {
            var count = 0
            val libraryMangaList = db.getLibraryMangaList().executeAsBlocking()
            val loggedServices = trackManager.services.values.filter { it.isLogged() }
            val autoAddTracker = preferences.autoAddTracker().get()
            val trackingCoordinator: TrackingCoordinator = Injekt.get()

            val validContentRatings = preferences.autoTrackContentRatingSelections().get()
            val autoAddTrackerIds = autoAddTracker.map { it.toInt() }
            val loggedServicesMap = loggedServices.associateBy { it.id }

            libraryMangaList.forEach { manga ->
                updateNotification(manga.title, count++, libraryMangaList.size)

                val tracks = db.getTracks(manga).executeOnIO()
                val trackSyncIds = tracks.map { it.sync_id }.toSet()

                coroutineScope {
                    if (autoAddTrackerIds.size > 1) {
                        val contentRating = manga.getContentRating()
                        if (
                            contentRating == null ||
                                validContentRatings.contains(contentRating.lowercase())
                        ) {
                            autoAddTrackerIds.forEach { autoAddTrackerId ->
                                if (!trackSyncIds.contains(autoAddTrackerId)) {
                                    val trackService = loggedServicesMap[autoAddTrackerId]
                                    if (trackService != null) {
                                        launch {
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
                                        delay(1.seconds)
                                    }
                                }
                            }
                        }
                    }

                    tracks.forEach { track ->
                        val service = trackManager.getService(track.sync_id)
                        if (service != null && service in loggedServices) {
                            launch {
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
            }
            completeNotification()
        }
}
