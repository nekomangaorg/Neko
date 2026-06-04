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

    private val trackingRestrictions =
        setOf(
            LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ,
            LibraryPreferences.MANGA_TRACKING_DROPPED,
            LibraryPreferences.MANGA_TRACKING_ON_HOLD,
            LibraryPreferences.MANGA_TRACKING_COMPLETED,
        )

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

        val hasActiveTrackingRestrictions = restrictions.any { it in trackingRestrictions }
        if (hasActiveTrackingRestrictions && tracks.isNotEmpty()) {
            for (track in tracks) {
                val service = trackManager.getService(track.sync_id) ?: continue
                val status = service.getGlobalStatus(track.status)
                if (status.isNullOrBlank()) continue
                val globalStatusId = trackManager.getGlobalStatusResId(status)

                val matchedRestriction =
                    when (globalStatusId) {
                        R.string.follows_plan_to_read ->
                            LibraryPreferences.MANGA_TRACKING_PLAN_TO_READ
                        R.string.follows_dropped -> LibraryPreferences.MANGA_TRACKING_DROPPED
                        R.string.follows_on_hold -> LibraryPreferences.MANGA_TRACKING_ON_HOLD
                        R.string.follows_completed -> LibraryPreferences.MANGA_TRACKING_COMPLETED
                        else -> null
                    }

                if (matchedRestriction != null && matchedRestriction in restrictions) {
                    return matchedRestriction
                }
            }
        }

        return null
    }
}
