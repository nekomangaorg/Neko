package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates.Companion.READING_LIST
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Context(
    @SerialName("session_token") val sessionToken: String,
    val uid: Long,
)

@Serializable
data class Image(
    val url: Url? = null,
    val height: Int? = null,
    val width: Int? = null,
)

@Serializable
data class ListItem(
    val series: Series? = null,
    @SerialName("list_id") val listId: Int? = null,
    val status: Status? = null,
    val priority: Int? = null,
)

fun ListItem.copyTo(track: Track): Track {
    return track.apply {
        this.status = listId ?: READING_LIST
        this.last_chapter_read = this@copyTo.status?.chapter?.toFloat() ?: 0f
    }
}

@Serializable
data class Rating(
    val rating: Float? = null,
)

fun Rating.copyTo(track: Track): Track {
    return track.apply { this.score = rating ?: 0f }
}

@Serializable
data class Record(
    @SerialName("series_id") val seriesId: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val description: String? = null,
    val image: Image? = null,
    val type: String? = null,
    val year: String? = null,
    @SerialName("bayesian_rating") val bayesianRating: Double? = null,
    @SerialName("rating_votes") val ratingVotes: Int? = null,
    @SerialName("latest_chapter") val latestChapter: Int? = null,
)

fun Record.toTrackSearch(id: Int): TrackSearch {
    return TrackSearch.create(id).apply {
        media_id = this@toTrackSearch.seriesId ?: 0L
        title = this@toTrackSearch.title?.htmlDecode() ?: ""
        total_chapters = 0
        cover_url = this@toTrackSearch.image?.url?.original ?: ""
        summary = this@toTrackSearch.description?.htmlDecode() ?: ""
        tracking_url = this@toTrackSearch.url ?: ""
        publishing_status = ""
        publishing_type = this@toTrackSearch.type.toString()
        start_date = this@toTrackSearch.year.toString()
    }
}

@Serializable
data class Series(
    val id: Long? = null,
    val title: String? = null,
)

@Serializable
data class Status(
    val volume: Int? = null,
    val chapter: Int? = null,
)

@Serializable
data class Url(
    val original: String? = null,
    val thumb: String? = null,
)
