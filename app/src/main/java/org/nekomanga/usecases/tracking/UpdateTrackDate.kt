package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import java.time.ZoneId

class UpdateTrackDate(
    private val updateTrackingService: UpdateTrackingService = UpdateTrackingService()
) {
    suspend fun await(trackDateChange: TrackingConstants.TrackDateChange): TrackingUpdate {
        val date =
            when (trackDateChange) {
                is TrackingConstants.TrackDateChange.EditTrackingDate -> {
                    trackDateChange.newDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                else -> 0L
            }
        val track =
            when (trackDateChange.readingDate) {
                TrackingConstants.ReadingDate.Start ->
                    trackDateChange.trackAndService.track.copy(startedReadingDate = date)
                TrackingConstants.ReadingDate.Finish ->
                    trackDateChange.trackAndService.track.copy(finishedReadingDate = date)
            }

        return updateTrackingService.await(track, trackDateChange.trackAndService.service)
    }
}
