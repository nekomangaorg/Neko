package org.nekomanga.usecases.tracking

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.runCatching
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.matchingTrack
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.domain.track.toTrackServiceItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AutoAddTrackers(
    private val preferences: PreferencesHelper = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val trackRepository: TrackRepository = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService(),
    private val searchTracker: SearchTracker = SearchTracker(),
    private val registerTracking: RegisterTracking = RegisterTracking(),
) {

    private suspend fun isOnline(): Boolean {
        return downloadManager.networkStateFlow().firstOrNull()?.isOnline == true
    }

    suspend operator fun invoke(
        mangaItem: MangaItem,
        loggedInTrackerService: List<TrackServiceItem>,
        tracks: List<TrackItem>,
        onShowSnackbar: (message: String?, prefixRes: Int?) -> Unit,
    ) {
        val autoAddTracker = preferences.autoAddTracker().get()
        val manga = mangaItem.toManga()
        loggedInTrackerService
            .firstOrNull { it.isMdList }
            ?.let {
                val mdList = trackManager.mdList
                var track = tracks.firstOrNull { mdList.matchingTrack(it) }?.toDbTrack()
                if (track == null) {
                    track = mdList.createInitialTracker(manga)

                    if (isOnline()) {
                        // Try to bind the new track to the remote service (e.g., get a remote
                        // ID)
                        runCatching { mdList.bind(track) } // Assumes bind() mutates the 'track' object
                            .onErr { exception ->
                                TimberKt.e(exception) { "Error binding new MangaDex track" }
                            }
                    }
                    // Save the new track (with or without remote data) to the local DB *once*
                    trackRepository.insertTrack(track)
                }
                val autoAddStatus = mangaDexPreferences.autoAddToMangaDexLibrary().get()
                val canAutoAdd =
                    mangaItem.favorite && FollowStatus.isUnfollowed(track.status) && isOnline()

                if (canAutoAdd) {
                    val newStatus =
                        when (autoAddStatus) {
                            1 -> FollowStatus.PLAN_TO_READ.int
                            3 -> FollowStatus.READING.int
                            2 -> FollowStatus.ON_HOLD.int
                            else -> null // Preference is not set to auto-add
                        }

                    if (newStatus != null) {
                        track.status = newStatus
                        val trackingUpdate =
                            updateTrackingService.await(
                                track.toTrackItem(),
                                mdList.toTrackServiceItem(),
                            )
                        handleTrackingUpdate(trackingUpdate, onShowSnackbar)
                    }
                }
            }

        if (autoAddTracker.size <= 1 || !mangaItem.favorite) return

        val validContentRatings = preferences.autoTrackContentRatingSelections().get()
        val contentRating = manga.getContentRating()

        if (contentRating != null && !validContentRatings.contains(contentRating.lowercase())) return

        if (!isOnline()) {
            onShowSnackbar("No network connection, cannot autolink tracker", null)
            return
        }

        val existingTrackIds = tracks.map { it.trackServiceId }.toSet()

        coroutineScope {
            autoAddTracker
                .mapNotNull { it.toIntOrNull() } // Safely convert preference strings to Ints
                .map { autoAddTrackerId ->
                    async {
                        val trackService =
                            loggedInTrackerService.firstOrNull { it.id == autoAddTrackerId }
                                ?: return@async // Not logged in to this service, skip

                        if (trackService.id in existingTrackIds) return@async // Already tracked, skip

                        // Check if the manga has a remote ID for this service
                        val id =
                            trackManager.getIdFromManga(trackService, manga)
                                ?: return@async // No ID found, skip

                        // We are online, not tracked, and have a remote ID. Proceed.
                        val trackResult =
                            searchTracker.byId(
                                id = id,
                                service = trackService,
                                manga = manga,
                                previouslyTracker = false,
                            )

                        when (trackResult) {
                            is TrackingConstants.TrackSearchResult.Success -> {
                                val trackSearchItem = trackResult.trackSearchResult.firstOrNull()
                                if (trackSearchItem != null) {
                                    // Found a match, register it
                                    val trackingUpdate =
                                        registerTracking.await(
                                            TrackAndService(
                                                trackSearchItem.trackItem,
                                                trackService,
                                            ),
                                            mangaItem.id,
                                        )
                                    handleTrackingUpdate(trackingUpdate, onShowSnackbar)
                                } else {
                                    TimberKt.w {
                                        "Auto-track search for ${trackService.id} was successful but returned no results."
                                    }
                                }
                            }

                            is TrackingConstants.TrackSearchResult.Error -> {
                                // Show a specific error for *this* tracker
                                onShowSnackbar(
                                    " error trying to autolink tracking. ${trackResult.errorMessage}",
                                    trackResult.trackerNameRes,
                                )
                            }

                            else -> Unit
                        }
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun handleTrackingUpdate(
        trackingUpdate: TrackingUpdate,
        onShowSnackbar: (message: String?, prefixRes: Int?) -> Unit,
    ) {
        if (trackingUpdate is TrackingUpdate.Error) {
            TimberKt.e(trackingUpdate.exception) { "handle tracking update had error" }
            onShowSnackbar(trackingUpdate.message, null)
        }
    }
}
