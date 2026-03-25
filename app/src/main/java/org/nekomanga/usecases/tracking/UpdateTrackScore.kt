package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import org.nekomanga.domain.track.toDbTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdateTrackScore(
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService(),
    private val trackManager: TrackManager = Injekt.get(),
) {
    suspend fun await(
        scoreIndex: Int,
        trackAndService: TrackingConstants.TrackAndService,
    ): TrackingUpdate {
        val trackItem =
            trackAndService.track.copy(score = trackAndService.service.indexToScore(scoreIndex))

        return if (trackAndService.service.isMdList) {
            runCatching {
                    trackManager.mdList.updateScore(trackItem.toDbTrack())
                    TrackingUpdate.Success
                }
                .getOrElse { TrackingUpdate.Error("Error updating MangaDex Score", it) }
        } else {
            updateTrackingService.await(trackItem, trackAndService.service)
        }
    }
}
