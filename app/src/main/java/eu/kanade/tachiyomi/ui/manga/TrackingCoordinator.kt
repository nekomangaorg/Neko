package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackListService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackStatusService
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import java.time.ZoneId
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackSearchItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**  */
class TrackingCoordinator {
    private val db: DatabaseHelper by injectLazy()
    private val trackManager: TrackManager = Injekt.get()

    /** Update tracker with new status */
    suspend fun updateTrackStatus(
        statusIndex: Int,
        trackAndService: TrackingConstants.TrackAndService
    ): TrackingUpdate {
        val service = trackManager.getService(trackAndService.service.id)
        var track =
            trackAndService.track.copy(status = trackAndService.service.statusList!![statusIndex])

        if (service is TrackStatusService &&
            service.isCompletedStatus(statusIndex) &&
            track.totalChapters > 0) {
            track = track.copy(lastChapterRead = track.totalChapters.toFloat())
        }
        return updateTrackingStatusService(track, trackAndService.service)
    }

    /** Update tracker with new status */
    suspend fun updateTrackLists(
        listIdsToAdd: List<String>,
        listIdsToRemove: List<String>,
        trackAndService: TrackingConstants.TrackAndService
    ): TrackingUpdate {
        return updateTrackingListService(
            trackAndService.track, trackAndService.service, listIdsToAdd, listIdsToRemove)
    }

    /** Update tracker with new score */
    suspend fun updateTrackScore(
        scoreIndex: Int,
        trackAndService: TrackingConstants.TrackAndService
    ): TrackingUpdate {
        val trackItem =
            trackAndService.track.copy(
                score = trackAndService.service.indexToScore(scoreIndex),
            )

        return if (trackAndService.service.isMdList) {
            runCatching {
                    trackManager.mdList.updateScore(trackItem.toDbTrack())
                    TrackingUpdate.Success
                }
                .getOrElse { TrackingUpdate.Error("Error updating MangaDex Score", it) }
        } else {
            updateTrackingStatusService(trackItem, trackAndService.service)
        }
    }

    /** Update the tracker with the new chapter information */
    suspend fun updateTrackChapter(
        newChapterNumber: Int,
        trackAndService: TrackingConstants.TrackAndService
    ): TrackingUpdate {
        val track = trackAndService.track.copy(lastChapterRead = newChapterNumber.toFloat())
        return updateTrackingStatusService(track, trackAndService.service)
    }

    /** Update the tracker with the start/finished date */
    suspend fun updateTrackDate(
        trackDateChange: TrackingConstants.TrackDateChange
    ): TrackingUpdate {
        val date =
            when (trackDateChange) {
                is TrackingConstants.TrackDateChange.EditTrackingDate -> {
                    trackDateChange.newDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                else -> 0L
            }
        val track =
            when (trackDateChange.readingDate) {
                TrackingConstants.ReadingDate.Start ->
                    trackDateChange.trackAndService.track.copy(startedReadingDate = date)
                TrackingConstants.ReadingDate.Finish ->
                    trackDateChange.trackAndService.track.copy(finishedReadingDate = date)
            }

        return updateTrackingStatusService(track, trackDateChange.trackAndService.service)
    }

    /** Register tracker */
    suspend fun registerTracking(
        trackAndService: TrackingConstants.TrackAndService,
        mangaId: Long
    ): TrackingUpdate {
        return runCatching {
                val trackItem =
                    trackAndService.track.copy(
                        mangaId = mangaId,
                    )

                val track =
                    trackManager
                        .getService(trackAndService.service.id)!!
                        .bind(trackItem.toDbTrack())
                db.insertTrack(track).executeOnIO()
                TrackingUpdate.Success
            }
            .getOrElse { exception ->
                TimberKt.e(exception) { "Error registering tracker" }
                TrackingUpdate.Error("Error registering tracker", exception)
            }
    }

    /** Remove a tracker with an option to remove it from the tracking service */
    suspend fun removeTracking(
        alsoRemoveFromTracker: Boolean,
        serviceItem: TrackServiceItem,
        mangaId: Long
    ): TrackingUpdate {
        val service = trackManager.getService(serviceItem.id)!!
        val tracks = db.getTracks(mangaId).executeOnIO().filter { it.sync_id == service.id }
        db.deleteTrackForManga(mangaId, service).executeOnIO()
        if (alsoRemoveFromTracker && service.canRemoveFromService()) {
            withNonCancellableContext {
                tracks.forEach {
                    runCatching { service.removeFromService(it) }
                        .onFailure { TimberKt.e(it) { "Unable to remove from service" } }
                }
            }
        }
        return TrackingUpdate.Success
    }

    /** Updates the remote tracking service with tracking changes */
    suspend fun updateTrackingStatusService(
        track: TrackItem,
        service: TrackServiceItem
    ): TrackingUpdate {
        return runCatching {
                val updatedTrack =
                    (trackManager.getService(service.id)!! as TrackStatusService).update(
                        track.toDbTrack())
                db.insertTrack(updatedTrack).executeOnIO()
                TrackingUpdate.Success
            }
            .getOrElse { TrackingUpdate.Error("Error updating tracker", it) }
    }

    /** Updates the remote tracking service with the list changes */
    suspend fun updateTrackingListService(
        track: TrackItem,
        service: TrackServiceItem,
        idsToAdd: List<String> = emptyList(),
        idsToRemove: List<String> = emptyList()
    ): TrackingUpdate {
        return runCatching {
                val service = (trackManager.getService(service.id)!! as TrackListService)

                var track = track.toDbTrack()

                if (idsToAdd.isNotEmpty()) {
                    track = service.addToLists(track, idsToAdd)
                }

                if (idsToRemove.isNotEmpty()) {
                    track = service.removeFromLists(track, idsToRemove)
                }

                db.insertTrack(track).executeOnIO()
                TrackingUpdate.Success
            }
            .getOrElse { TrackingUpdate.Error("Error updating tracker", it) }
    }

    /** Search Tracker */
    suspend fun searchTracker(
        title: String,
        service: TrackServiceItem,
        manga: Manga,
        previouslyTracker: Boolean
    ) =
        flow {
                emit(TrackingConstants.TrackSearchResult.Loading)
                val results =
                    trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
                emit(
                    when (results.isEmpty()) {
                        true -> TrackingConstants.TrackSearchResult.NoResult
                        false ->
                            TrackingConstants.TrackSearchResult.Success(
                                results.map { it.toTrackSearchItem() }.toImmutableList())
                    },
                )
            }
            .catch {
                TimberKt.e(it) { "error searching tracker" }
                emit(
                    TrackingConstants.TrackSearchResult.Error(
                        it.message ?: "Error searching tracker", service.nameRes))
            }

    /** Search Tracker */
    suspend fun searchTrackerNonFlow(
        title: String,
        service: TrackServiceItem,
        manga: Manga,
        previouslyTracker: Boolean
    ): TrackingConstants.TrackSearchResult {
        return kotlin
            .runCatching {
                val results =
                    trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
                when (results.isEmpty()) {
                    true -> TrackingConstants.TrackSearchResult.NoResult
                    false ->
                        TrackingConstants.TrackSearchResult.Success(
                            results.map { it.toTrackSearchItem() }.toImmutableList())
                }
            }
            .getOrElse {
                TimberKt.e(it) { "error searching tracker" }
                TrackingConstants.TrackSearchResult.Error(
                    it.message ?: "Error searching tracker", service.nameRes)
            }
    }
}

sealed class TrackingUpdate {
    object Success : TrackingUpdate()

    data class Error(val message: String, val exception: Throwable) : TrackingUpdate()
}
