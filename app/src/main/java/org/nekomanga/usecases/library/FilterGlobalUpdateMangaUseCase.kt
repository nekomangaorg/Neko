package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.model.SManga
import org.nekomanga.domain.library.LibraryPreferences

class FilterGlobalUpdateMangaUseCase(private val trackManager: TrackManager) {

    operator fun invoke(
        libraryManga: List<LibraryManga>,
        includedCategories: List<Int>,
        excludedCategories: List<Int>,
        restrictions: Set<String>,
        tracksByMangaId: Map<Long, List<Track>>,
        planToReadStatus: String,
        droppedStatus: String,
        onHoldStatus: String,
        completedStatus: String,
    ): Map<Long?, List<LibraryManga>> {
        return libraryManga
            .groupBy { it.id }
            .filterNot { it.value.any { manga -> manga.category in excludedCategories } }
            .filter {
                includedCategories.isEmpty() ||
                    it.value.any { manga -> manga.category in includedCategories }
            }
            .filter {
                val manga = it.value.first()
                when {
                    LibraryPreferences.MANGA_HAS_UNREAD in restrictions && manga.unread != 0 -> true
                    LibraryPreferences.MANGA_NOT_STARTED in restrictions &&
                        manga.totalChapters > 0 &&
                        !manga.hasStarted -> true
                    LibraryPreferences.MANGA_NOT_COMPLETED in restrictions &&
                        manga.status == SManga.COMPLETED -> true
                    LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ in restrictions &&
                        hasTrackWithGivenStatus(manga, planToReadStatus, tracksByMangaId) -> false
                    LibraryPreferences.MANGA_TRACKING_DROPPED in restrictions &&
                        hasTrackWithGivenStatus(manga, droppedStatus, tracksByMangaId) -> false
                    LibraryPreferences.MANGA_TRACKING_ON_HOLD in restrictions &&
                        hasTrackWithGivenStatus(manga, onHoldStatus, tracksByMangaId) -> false
                    LibraryPreferences.MANGA_TRACKING_COMPLETED in restrictions &&
                        hasTrackWithGivenStatus(manga, completedStatus, tracksByMangaId) -> false
                    else -> true
                }
            }
    }

    private fun hasTrackWithGivenStatus(
        libraryManga: LibraryManga,
        globalStatus: String,
        tracksByMangaId: Map<Long, List<Track>>,
    ): Boolean {
        val tracks = tracksByMangaId[libraryManga.id] ?: return false
        return tracks.any { track ->
            val status = trackManager.getService(track.sync_id)?.getGlobalStatus(track.status)
            if (status.isNullOrBlank()) {
                false
            } else {
                status == globalStatus
            }
        }
    }
}
