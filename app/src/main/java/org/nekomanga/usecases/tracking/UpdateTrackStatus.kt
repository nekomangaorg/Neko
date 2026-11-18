package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import org.nekomanga.domain.track.toDbTrack
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
        val track =
            trackAndService.track.copy(status = trackAndService.service.statusList[statusIndex])

        // MdList.update does not send Unfollowed to MangaDex, see MdList.unfollow
        return if (trackAndService.service.isMdList && FollowStatus.isUnfollowed(track.status)) {
            runCatching {
                trackManager.mdList.unfollow(track.toDbTrack())
                TrackingUpdate.Success
            }
                .getOrElse { TrackingUpdate.Error("Error updating tracker", it) }
        } else {
            updateTrackingService.await(track, trackAndService.service)
        }
    }
}
