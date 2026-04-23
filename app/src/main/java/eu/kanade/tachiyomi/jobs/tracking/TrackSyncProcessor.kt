package eu.kanade.tachiyomi.jobs.tracking

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.tracking.TrackUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSyncProcessor(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    val mangaRepository: MangaRepository = Injekt.get()
    val trackRepository: TrackRepository = Injekt.get()
    val trackManager: TrackManager = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    suspend fun process(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ) =
        withContext(dispatcher) {
            var count = 0
            val libraryMangaList = mangaRepository.getLibraryList()
            val loggedServices = trackManager.services.values.filter { it.isLogged() }
            val autoAddTracker = preferences.autoAddTracker().get()
            val trackUseCases: TrackUseCases = Injekt.get()

            val validContentRatings = preferences.autoTrackContentRatingSelections().get()
            val autoAddTrackerIds = autoAddTracker.map { it.toInt() }
            val loggedServicesMap = loggedServices.associateBy { it.id }

            val tracksByMangaId =
                libraryMangaList
                    .mapNotNull { it.id }
                    .chunked(900)
                    .map { chunk -> async { trackRepository.getTracksForMangaByIds(chunk) } }
                    .awaitAll()
                    .flatten()
                    .groupBy { it.manga_id }

            libraryMangaList.forEach { manga ->
                updateNotification(manga.title, count++, libraryMangaList.size)

                val tracks = tracksByMangaId[manga.id] ?: emptyList()
                val trackSyncIds = tracks.map { it.sync_id }.toSet()

                coroutineScope {
                    if (autoAddTrackerIds.isNotEmpty()) {
                        val contentRating = manga.getContentRating()
                        if (
                            contentRating == null ||
                                validContentRatings.contains(contentRating.lowercase())
                        ) {
                            autoAddTrackerIds
                                .mapNotNull { autoAddTrackerId ->
                                    if (!trackSyncIds.contains(autoAddTrackerId)) {
                                        val trackService = loggedServicesMap[autoAddTrackerId]
                                        if (trackService != null) {
                                            async {
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
                                                            trackUseCases.searchTracker.byId(
                                                                id = id,
                                                                service = trackServiceItem,
                                                                manga = manga,
                                                                previouslyTracker = false,
                                                            )
                                                        when (trackResult) {
                                                            is TrackingConstants.TrackSearchResult.Success -> {
                                                                val trackSearchItem =
                                                                    trackResult.trackSearchResult[0]
                                                                trackUseCases.registerTracking
                                                                    .await(
                                                                        TrackingConstants
                                                                            .TrackAndService(
                                                                                trackSearchItem
                                                                                    .trackItem,
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
                                        } else null
                                    } else null
                                }
                                .awaitAll()
                        }
                    }

                    tracks
                        .mapNotNull { track ->
                            val service = trackManager.getService(track.sync_id)
                            if (service != null && service in loggedServices) {
                                async {
                                    try {
                                        val newTrack = service.refresh(track)
                                        trackRepository.insertTrack(newTrack)
                                    } catch (e: Exception) {
                                        if (e !is CancellationException) {
                                            TimberKt.e(e)
                                        }
                                    }
                                }
                            } else null
                        }
                        .awaitAll()
                }
            }
            completeNotification()
        }
}
