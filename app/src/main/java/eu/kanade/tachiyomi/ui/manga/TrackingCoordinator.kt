package eu.kanade.tachiyomi.ui.manga

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackSearchItem
import org.threeten.bp.ZoneId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 *
 */
class TrackingCoordinator {
    private val db: DatabaseHelper by injectLazy()
    private val trackManager: TrackManager = Injekt.get()
    private val sourceManager: SourceManager by lazy { Injekt.get() }

    /**
     * Update tracker with new status
     */
    suspend fun updateTrackStatus(statusIndex: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        var track = trackAndService.track.copy(status = trackAndService.service.statusList[statusIndex])

        if (trackManager.getService(trackAndService.service.id)!!.isCompletedStatus(statusIndex) && track.totalChapters > 0) {
            track = track.copy(lastChapterRead = track.totalChapters.toFloat())
        }
        return updateTrackingService(track, trackAndService.service)
    }

    /**
     * Update tracker with new score
     */
    suspend fun updateTrackScore(scoreIndex: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        val trackItem = trackAndService.track.copy(
            score = trackAndService.service.indexToScore(scoreIndex),
        )

        return if (trackAndService.service.isMdList) {
            runCatching {
                trackManager.mdList.updateScore(trackItem.toDbTrack())
                TrackingUpdate.Success
            }.getOrElse {
                TrackingUpdate.Error("Error updating MangaDex Score", it)
            }
        } else {
            updateTrackingService(trackItem, trackAndService.service)
        }
    }

    /**
     * Update the tracker with the new chapter information
     */
    suspend fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        val track = trackAndService.track.copy(lastChapterRead = newChapterNumber.toFloat())
        return updateTrackingService(track, trackAndService.service)
    }

    /**
     * Update the tracker with the start/finished date
     */
    suspend fun updateTrackDate(trackDateChange: TrackingConstants.TrackDateChange): TrackingUpdate {
        val date = when (trackDateChange) {
            is TrackingConstants.TrackDateChange.EditTrackingDate -> {
                trackDateChange.newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            else -> 0L
        }
        val track =
            when (trackDateChange.readingDate) {
                TrackingConstants.ReadingDate.Start -> trackDateChange.trackAndService.track.copy(startedReadingDate = date)
                TrackingConstants.ReadingDate.Finish -> trackDateChange.trackAndService.track.copy(finishedReadingDate = date)
            }

        return updateTrackingService(track, trackDateChange.trackAndService.service)
    }

    /**
     * Register tracker
     */
    suspend fun registerTracking(trackAndService: TrackingConstants.TrackAndService, mangaId: Long): TrackingUpdate {
        return runCatching {
            val trackItem = trackAndService.track.copy(
                mangaId = mangaId,
            )

            val track = trackManager.getService(trackAndService.service.id)!!.bind(trackItem.toDbTrack())
            db.insertTrack(track).executeOnIO()
            TrackingUpdate.Success
        }.getOrElse { exception ->
            TrackingUpdate.Error("Error registering tacker", exception)
        }
    }

    /**
     * Remove a tracker with an option to remove it from the tracking service
     */
    suspend fun removeTracking(alsoRemoveFromTracker: Boolean, serviceItem: TrackServiceItem, mangaId: Long): TrackingUpdate {
        val service = trackManager.getService(serviceItem.id)!!
        val tracks = db.getTracks(mangaId).executeOnIO().filter { it.sync_id == service.id }
        db.deleteTrackForManga(mangaId, service).executeOnIO()
        if (alsoRemoveFromTracker && service.canRemoveFromService()) {
            launchIO {
                tracks.forEach {
                    runCatching {
                        service.removeFromService(it)
                    }.onFailure {
                        XLog.e("Unable to remove from service", it)
                    }
                }
            }
        }
        return TrackingUpdate.Success
    }

    /**
     * Updates the remote tracking service with tracking changes
     */
    suspend fun updateTrackingService(track: TrackItem, service: TrackServiceItem): TrackingUpdate {
        return runCatching {
            val updatedTrack = trackManager.getService(service.id)!!.update(track.toDbTrack())
            db.insertTrack(updatedTrack).executeOnIO()
            TrackingUpdate.Success
        }.getOrElse {
            TrackingUpdate.Error("Error updating tracker", it)
        }
    }

    /**
     * Search Tracker
     */
    suspend fun searchTracker(title: String, service: TrackServiceItem, manga: Manga, previouslyTracker: Boolean) = flow {
        emit(TrackingConstants.TrackSearchResult.Loading)
        val results = trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
        emit(
            when (results.isEmpty()) {
                true -> TrackingConstants.TrackSearchResult.NoResult
                false -> TrackingConstants.TrackSearchResult.Success(results.map { it.toTrackSearchItem() }.toImmutableList())
            },
        )
    }.catch {
        XLog.e("error searching tracker", it)
        emit(TrackingConstants.TrackSearchResult.Error(it.message ?: "Error searching tracker", service.nameRes))
    }

    /**
     * Search Tracker
     */
    suspend fun searchTrackerNonFlow(title: String, service: TrackServiceItem, manga: Manga, previouslyTracker: Boolean): TrackingConstants.TrackSearchResult {
        return kotlin.runCatching {
            val results = trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
            when (results.isEmpty()) {
                true -> TrackingConstants.TrackSearchResult.NoResult
                false -> TrackingConstants.TrackSearchResult.Success(results.map { it.toTrackSearchItem() }.toImmutableList())
            }
        }.getOrElse {
            XLog.e("error searching tracker", it)
            TrackingConstants.TrackSearchResult.Error(it.message ?: "Error searching tracker", service.nameRes)
        }
    }
}

sealed class TrackingUpdate {
    object Success : TrackingUpdate()
    data class Error(val message: String, val exception: Throwable) : TrackingUpdate()
}
