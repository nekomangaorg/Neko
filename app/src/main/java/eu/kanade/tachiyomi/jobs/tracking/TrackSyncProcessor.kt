package eu.kanade.tachiyomi.jobs.tracking

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.tracking.TrackUseCases
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
            val trackUseCases: TrackUseCases = Injekt.get()

            val validContentRatings = preferences.autoTrackContentRatingSelections().get()
            val autoAddTrackerIds = autoAddTracker.map { it.toInt() }
            val loggedServicesMap = loggedServices.associateBy { it.id }

            libraryMangaList.forEach { manga ->
                updateNotification(manga.title, count++, libraryMangaList.size)

                val tracks = db.getTracks(manga).executeOnIO()
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
                                                            trackUseCases.searchTracker
                                                                .awaitNonFlow(
                                                                    "",
                                                                    trackManager
                                                                        .getService(trackService.id)
                                                                        ?.toTrackServiceItem()
                                                                        ?: return@async,
                                                                    manga,
                                                                    false,
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
                                        db.insertTrack(newTrack).executeOnIO()
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
