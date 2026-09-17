package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate

class UpdateTrackStatus(
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService()
) {
    suspend fun await(
        statusIndex: Int,
        trackAndService: TrackingConstants.TrackAndService,
    ): TrackingUpdate {
        val track =
            trackAndService.track.copy(status = trackAndService.service.statusList[statusIndex])

        return updateTrackingService.await(track, trackAndService.service)
    }
}
