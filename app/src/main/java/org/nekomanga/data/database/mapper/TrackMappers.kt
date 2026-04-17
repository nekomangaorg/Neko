package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import org.nekomanga.data.database.entity.TrackEntity

// =========================================================================
// TRACKING / MANGA_SYNC MAPPERS
// =========================================================================

fun TrackEntity.toTrack(): TrackImpl {
    return TrackImpl().apply {
        id = this@toTrack.id
        manga_id = this@toTrack.mangaId
        sync_id = this@toTrack.trackServiceId
        media_id = this@toTrack.mediaId
        library_id = this@toTrack.libraryId
        title = this@toTrack.title
        last_chapter_read = this@toTrack.lastChapterRead
        total_chapters = this@toTrack.totalChapters
        status = this@toTrack.status
        score = this@toTrack.score
        tracking_url = this@toTrack.trackingUrl
        started_reading_date = this@toTrack.startedReadingDate
        finished_reading_date = this@toTrack.finishedReadingDate
    }
}

fun Track.toEntity(): TrackEntity {
    return TrackEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        mangaId = this.manga_id,
        trackServiceId = this.sync_id,
        mediaId = this.media_id,
        libraryId = this.library_id,
        title = this.title,
        lastChapterRead = this.last_chapter_read,
        totalChapters = this.total_chapters,
        status = this.status,
        score = this.score,
        trackingUrl = this.tracking_url,
        startedReadingDate = this.started_reading_date,
        finishedReadingDate = this.finished_reading_date,
    )
}
