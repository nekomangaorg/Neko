package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.track.TrackManager
import org.nekomanga.domain.track.TrackServiceItem

class LoginToTrackService(private val trackManager: TrackManager) {
    suspend operator fun invoke(
        trackServiceItem: TrackServiceItem,
        username: String,
        password: String,
    ): Boolean {
        return trackManager.getService(trackServiceItem.id)?.login(username, password) ?: false
    }
}
