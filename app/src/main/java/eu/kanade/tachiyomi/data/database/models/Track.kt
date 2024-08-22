package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Track : Serializable {

    var id: Long?

    var manga_id: Long

    var sync_id: Int

    var media_id: Long

    var library_id: Long?

    var title: String

    var last_chapter_read: Float

    var total_chapters: Int

    var score: Float

    var status: Int

    var listIds: List<String>

    var tracking_url: String

    var started_reading_date: Long

    var finished_reading_date: Long

    fun copyPersonalFrom(other: Track) {
        last_chapter_read = other.last_chapter_read
        score = other.score
        status = other.status
        started_reading_date = other.started_reading_date
        finished_reading_date = other.finished_reading_date
        listIds = other.listIds
    }

    companion object {
        val LIST_SEPERATOR = "~||~"

        fun create(serviceId: Int): Track = TrackImpl().apply { sync_id = serviceId }
    }
}
