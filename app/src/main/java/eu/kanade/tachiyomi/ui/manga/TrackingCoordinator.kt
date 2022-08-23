package eu.kanade.tachiyomi.ui.manga

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.threeten.bp.ZoneId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 *
 */
class TrackingCoordinator {
    private val db: DatabaseHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val mangaDex: MangaDex by lazy { sourceManager.getMangadex() }
    private val mergedSource: MangaLife by lazy { sourceManager.getMergeSource() }
    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    /**
     * Update tracker with new status
     */
    suspend fun updateTrackStatus(statusIndex: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        val track = trackAndService.track.apply {
            this.status = trackAndService.service.getStatusList()[statusIndex]
        }
        if (trackAndService.service.isCompletedStatus(statusIndex) && track.total_chapters > 0) {
            track.last_chapter_read = track.total_chapters.toFloat()
        }
        return updateTrackingService(track, trackAndService.service)
    }

    /**
     * Update tracker with new score
     */
    suspend fun updateTrackScore(scoreIndex: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        val track = trackAndService.track.apply {
            this.score = trackAndService.service.indexToScore(scoreIndex)
        }
        return if (trackAndService.service.isMdList()) {
            runCatching {
                (trackAndService.service as MdList).updateScore(track)
                TrackingUpdate.Success
            }.getOrElse {
                TrackingUpdate.Error("Error updating MangaDex Score", it)
            }
        } else {
            updateTrackingService(track, trackAndService.service)
        }
    }

    /**
     * Update the tracker with the new chapter information
     */
    suspend fun updateTrackChapter(newChapterNumber: Int, trackAndService: TrackingConstants.TrackAndService): TrackingUpdate {
        val track = trackAndService.track.apply {
            this.last_chapter_read = newChapterNumber.toFloat()
        }
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
        val track = trackDateChange.trackAndService.track.apply {
            when (trackDateChange.readingDate) {
                TrackingConstants.ReadingDate.Start -> this.started_reading_date = date
                TrackingConstants.ReadingDate.Finish -> this.finished_reading_date = date
            }
        }

        return updateTrackingService(track, trackDateChange.trackAndService.service)
    }

    /**
     * Register tracker
     */
    suspend fun registerTracking(trackAndService: TrackingConstants.TrackAndService, mangaId: Long): TrackingUpdate {
        return runCatching {
            XLog.e("ESCO tracking called for ${trackAndService.service.id} for track ${trackAndService.track.id}")
            val trackItem = trackAndService.track.apply {
                manga_id = mangaId
            }
            val track = trackAndService.service.bind(trackItem)
            db.insertTrack(track).executeOnIO()
            TrackingUpdate.Success
        }.getOrElse { exception ->
            TrackingUpdate.Error("Error registering tacker", exception)
        }
    }

    /**
     * Remove a tracker with an option to remove it from the tracking service
     */
    suspend fun removeTracking(alsoRemoveFromTracker: Boolean, service: TrackService, mangaId: Long): TrackingUpdate {
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
    suspend fun updateTrackingService(track: Track, service: TrackService): TrackingUpdate {
        return runCatching {
            val updatedTrack = service.update(track)
            db.insertTrack(updatedTrack).executeOnIO()
            TrackingUpdate.Success
        }.getOrElse {
            TrackingUpdate.Error("Error updating tracker", it)
        }
    }

    /**
     * Search Tracker
     */
    suspend fun searchTracker(title: String, service: TrackService, manga: Manga, previouslyTracker: Boolean) = flow {
        emit(TrackingConstants.TrackSearchResult.Loading)
        val results = service.search(title, manga, previouslyTracker)
        emit(
            when (results.isEmpty()) {
                true -> TrackingConstants.TrackSearchResult.NoResult
                false -> TrackingConstants.TrackSearchResult.Success(results)
            },
        )

    }.catch {
        emit(TrackingConstants.TrackSearchResult.Error(it.message ?: "Error searching tracker"))
    }
}

sealed class TrackingUpdate {
    object Success : TrackingUpdate()
    data class Error(val message: String, val exception: Throwable) : TrackingUpdate()
}

