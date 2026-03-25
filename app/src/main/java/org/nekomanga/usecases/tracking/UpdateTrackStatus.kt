package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdateTrackStatus(
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService(),
    private val trackManager: TrackManager = Injekt.get(),
) {
    suspend fun await(
        statusIndex: Int,
        trackAndService: TrackingConstants.TrackAndService,
    ): TrackingUpdate {
        var track =
            trackAndService.track.copy(status = trackAndService.service.statusList[statusIndex])

        if (
            trackManager.getService(trackAndService.service.id)!!.isCompletedStatus(statusIndex) &&
                track.totalChapters > 0
        ) {
            track = track.copy(lastChapterRead = track.totalChapters.toFloat())
        }
        return updateTrackingService.await(track, trackAndService.service)
    }
}
