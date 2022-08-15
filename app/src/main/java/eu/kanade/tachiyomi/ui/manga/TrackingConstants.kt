package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import org.threeten.bp.LocalDate
import java.text.DateFormat

object TrackingConstants {
    data class TrackItem(val track: Track?, val service: TrackService)

    data class TrackAndService(val track: Track, val service: TrackService)

    data class TrackingDate(val readingDate: ReadingDate, val currentDate: Long, val dateFormat: DateFormat)

    data class TrackingSuggestedDates(val startDate: Long, val finishedDate: Long)

    sealed class TrackDateChange(val readingDate: ReadingDate, val trackAndService: TrackAndService) {
        class RemoveTrackingDate(readingDate: ReadingDate, trackAndService: TrackAndService) : TrackDateChange(readingDate, trackAndService)
        class EditTrackingDate(readingDate: ReadingDate, val newDate: LocalDate, trackAndService: TrackAndService) : TrackDateChange(readingDate, trackAndService)
    }

    sealed class TrackSearchResult {
        object Loading : TrackSearchResult()
        object NoResult : TrackSearchResult()
        class Success(val trackSearchResult: List<TrackSearch>) : TrackSearchResult()
        class Error(val errorMessage: String) : TrackSearchResult()
    }

    enum class ReadingDate {
        Start,
        Finish
    }
}
