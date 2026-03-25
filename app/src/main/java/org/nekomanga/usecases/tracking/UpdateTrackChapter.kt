package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate

class UpdateTrackChapter(
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService()
) {
    suspend fun await(
        newChapterNumber: Int,
        trackAndService: TrackingConstants.TrackAndService,
    ): TrackingUpdate {
        val track = trackAndService.track.copy(lastChapterRead = newChapterNumber.toFloat())
        return updateTrackingService.await(track, trackAndService.service)
    }
}
