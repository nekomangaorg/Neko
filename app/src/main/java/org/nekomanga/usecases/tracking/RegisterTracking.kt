package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.ui.manga.TrackingUpdate
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RegisterTracking(
    private val db: DatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) {
    suspend fun await(
        trackAndService: TrackingConstants.TrackAndService,
        mangaId: Long,
    ): TrackingUpdate {
        return runCatching {
                val trackItem = trackAndService.track.copy(mangaId = mangaId)

                val track =
                    trackManager.getService(trackAndService.service.id)?.bind(trackItem.toDbTrack())
                        ?: throw IllegalStateException("Service not found")
                db.insertTrack(track).executeOnIO()
                TrackingUpdate.Success
            }
            .getOrElse { exception ->
                TimberKt.e(exception) { "Error registering tracker" }
                TrackingUpdate.Error("Error registering tracker", exception)
            }
    }
}
