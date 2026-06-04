package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.model.SManga
import org.nekomanga.R
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ShouldUpdateMangaUseCase(private val trackManager: TrackManager = Injekt.get()) {
    /**
     * Checks if the given manga should be updated based on active restrictions. Returns the
     * restriction key (e.g. [LibraryPreferences.MANGA_HAS_UNREAD]) that matched and caused the
     * manga to be skipped, or null if the manga should be updated.
     */
    operator fun invoke(
        libraryManga: LibraryManga,
        restrictions: Set<String>,
        tracks: List<Track>,
    ): String? {
        if (LibraryPreferences.MANGA_HAS_UNREAD in restrictions && libraryManga.unread != 0) {
            return LibraryPreferences.MANGA_HAS_UNREAD
        }
        if (
            LibraryPreferences.MANGA_NOT_STARTED in restrictions &&
                libraryManga.totalChapters > 0 &&
                !libraryManga.hasStarted
        ) {
            return LibraryPreferences.MANGA_NOT_STARTED
        }
        if (
            LibraryPreferences.MANGA_NOT_COMPLETED in restrictions &&
                libraryManga.status == SManga.COMPLETED
        ) {
            return LibraryPreferences.MANGA_NOT_COMPLETED
        }

        if (
            LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ in restrictions &&
                hasTrackWithGivenStatus(tracks, R.string.follows_plan_to_read)
        ) {
            return LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ
        }
        if (
            LibraryPreferences.MANGA_TRACKING_DROPPED in restrictions &&
                hasTrackWithGivenStatus(tracks, R.string.follows_dropped)
        ) {
            return LibraryPreferences.MANGA_TRACKING_DROPPED
        }
        if (
            LibraryPreferences.MANGA_TRACKING_ON_HOLD in restrictions &&
                hasTrackWithGivenStatus(tracks, R.string.follows_on_hold)
        ) {
            return LibraryPreferences.MANGA_TRACKING_ON_HOLD
        }
        if (
            LibraryPreferences.MANGA_TRACKING_COMPLETED in restrictions &&
                hasTrackWithGivenStatus(tracks, R.string.follows_completed)
        ) {
            return LibraryPreferences.MANGA_TRACKING_COMPLETED
        }

        return null
    }

    private fun hasTrackWithGivenStatus(tracks: List<Track>, globalStatusId: Int): Boolean {
        return tracks.any { track ->
            val status = trackManager.getService(track.sync_id)?.getGlobalStatus(track.status)
            if (status.isNullOrBlank()) {
                false
            } else {
                globalStatusId == trackManager.getGlobalStatusResId(status)
            }
        }
    }
}
