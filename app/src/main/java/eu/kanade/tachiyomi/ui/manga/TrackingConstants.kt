package eu.kanade.tachiyomi.ui.manga

import androidx.annotation.StringRes
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackSearchItem
import org.nekomanga.domain.track.TrackServiceItem
import org.threeten.bp.LocalDate

object TrackingConstants {

    data class TrackAndService(val track: TrackItem, val service: TrackServiceItem)

    data class TrackingDate(val readingDate: ReadingDate, val currentDate: Long, val dateFormat: DateFormat)

    data class TrackingSuggestedDates(val startDate: Long, val finishedDate: Long)

    sealed class TrackDateChange(val readingDate: ReadingDate, val trackAndService: TrackAndService) {
        class RemoveTrackingDate(readingDate: ReadingDate, trackAndService: TrackAndService) : TrackDateChange(readingDate, trackAndService)
        class EditTrackingDate(readingDate: ReadingDate, val newDate: LocalDate, trackAndService: TrackAndService) : TrackDateChange(readingDate, trackAndService)
    }

    sealed class TrackSearchResult {
        object Loading : TrackSearchResult()
        object NoResult : TrackSearchResult()
        class Success(val trackSearchResult: ImmutableList<TrackSearchItem>) : TrackSearchResult()
        class Error(val errorMessage: String, @StringRes val trackerNameRes: Int) : TrackSearchResult()
    }

    enum class ReadingDate {
        Start,
        Finish,
    }
}
