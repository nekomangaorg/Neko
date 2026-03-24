package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackUseCases(trackManager: TrackManager = Injekt.get()) {
    val loginToTrackService = LoginToTrackService(trackManager)
    val refreshTracking = RefreshTrackingUseCase()
}
