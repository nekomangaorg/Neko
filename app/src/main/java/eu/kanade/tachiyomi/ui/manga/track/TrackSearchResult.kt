package eu.kanade.tachiyomi.ui.manga.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch

/**
 * Sealed class containing the results from searching a tracker
 */
sealed class TrackSearchResult {
    object Loading : TrackSearchResult()
    object NoResult : TrackSearchResult()
    class Success(val trackSearchResult: List<TrackSearch>) : TrackSearchResult()
    class Error(val errorMessage: String) : TrackSearchResult()
}
