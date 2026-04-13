package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toDbTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdateTrackingService(
    private val db: DatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) {
    suspend fun await(track: TrackItem, service: TrackServiceItem): TrackingUpdate {
        return runCatching {
                val trackService =
                    trackManager.getService(service.id)
                        ?: throw IllegalStateException("Service not found")
                val updatedTrack = trackService.update(track.toDbTrack())
                db.insertTrack(updatedTrack).executeOnIO()
                TrackingUpdate.Success
            }
            .getOrElse { TrackingUpdate.Error("Error updating tracker", it) }
    }
}
