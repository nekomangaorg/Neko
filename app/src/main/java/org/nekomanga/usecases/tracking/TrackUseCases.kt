package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackUseCases(trackManager: TrackManager = Injekt.get()) {
    val loginToTrackService = LoginToTrackService(trackManager)
    val refreshTracking = RefreshTrackingUseCase()
    val updateTrackStatus = UpdateTrackStatus()
    val updateTrackScore = UpdateTrackScore()
    val updateTrackChapter = UpdateTrackChapter()
    val updateTrackDate = UpdateTrackDate()
    val registerTracking = RegisterTracking()
    val removeTracking = RemoveTracking()
    val updateTrackingService = UpdateTrackingService()
    val searchTracker = SearchTracker()
}
