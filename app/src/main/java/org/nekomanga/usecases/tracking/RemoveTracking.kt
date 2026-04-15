package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import org.nekomanga.data.database.model.toTrack
import org.nekomanga.data.database.repository.TrackRepositoryImpl
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RemoveTracking(
    private val trackRepository: TrackRepositoryImpl = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) {
    suspend fun await(
        alsoRemoveFromTracker: Boolean,
        serviceItem: TrackServiceItem,
        mangaId: Long,
    ): TrackingUpdate {
        val service =
            trackManager.getService(serviceItem.id)
                ?: return TrackingUpdate.Error(
                    "Service not found",
                    IllegalStateException("Service not found"),
                )
        val tracks = trackRepository.getTracksForMangaSync(mangaId).filter { it.syncId == service.id }
        trackRepository.deleteTrackByMangaIdAndSyncId(mangaId, service.id)
        if (alsoRemoveFromTracker && service.canRemoveFromService()) {
            withNonCancellableContext {
                tracks.forEach {
                    runCatching { service.removeFromService(it.toTrack()) }
                        .onFailure { TimberKt.e(it) { "Unable to remove from service" } }
                }
            }
        }
        return TrackingUpdate.Success
    }
}
