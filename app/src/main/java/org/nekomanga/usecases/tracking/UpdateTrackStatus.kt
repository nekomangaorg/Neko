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

        val service = trackManager.getService(trackAndService.service.id)
        val isCompleted = service?.isCompletedStatus(statusIndex) == true
        if (isCompleted) {
            if (track.totalChapters > 0) {
                track = track.copy(lastChapterRead = track.totalChapters.toFloat())
            }
            if (trackAndService.service.supportsReadingDates) {
                if (track.finishedReadingDate <= 0L) {
                    track = track.copy(finishedReadingDate = System.currentTimeMillis())
                }
                if (track.startedReadingDate <= 0L) {
                    track = track.copy(startedReadingDate = System.currentTimeMillis())
                }
            }
        }
        return updateTrackingService.await(track, trackAndService.service)
    }
}
