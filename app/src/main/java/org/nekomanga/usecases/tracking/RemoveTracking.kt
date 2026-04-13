package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RemoveTracking(
    private val db: DatabaseHelper = Injekt.get(),
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
        val tracks = db.getTracks(mangaId).executeOnIO().filter { it.sync_id == service.id }
        db.deleteTrackForManga(mangaId, service).executeOnIO()
        if (alsoRemoveFromTracker && service.canRemoveFromService()) {
            withNonCancellableContext {
                tracks.forEach {
                    runCatching { service.removeFromService(it) }
                        .onFailure { TimberKt.e(it) { "Unable to remove from service" } }
                }
            }
        }
        return TrackingUpdate.Success
    }
}
