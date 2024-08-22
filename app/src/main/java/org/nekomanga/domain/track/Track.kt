package org.nekomanga.domain.track

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackList
import eu.kanade.tachiyomi.data.track.TrackListService
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.TrackStatusService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

data class TrackItem(
    val id: Long?,
    val mangaId: Long,
    val trackServiceId: Int,
    val mediaId: Long,
    val libraryId: Long?,
    val title: String,
    val lastChapterRead: Float,
    val totalChapters: Int,
    val score: Float,
    val status: Int,
    val listIds: List<String>,
    val trackingUrl: String,
    val startedReadingDate: Long,
    val finishedReadingDate: Long,
)

data class TrackServiceItem(
    val id: Int,
    val nameRes: Int,
    val logoRes: Int,
    val logoColor: Int,
    val statusList: ImmutableList<Int>?,
    val lists: ImmutableList<TrackList>?,
    val supportsReadingDates: Boolean,
    val canRemoveFromService: Boolean,
    val isAutoAddTracker: Boolean,
    val isMdList: Boolean,
    val status: (Int) -> String?,
    val currentList: (String) -> TrackList?,
    val displayScore: (TrackItem) -> String,
    val scoreList: ImmutableList<String>,
    val indexToScore: (Int) -> Float,
)

fun TrackService.toTrackServiceItem(): TrackServiceItem {
    return TrackServiceItem(
        id = this.id,
        nameRes = this.nameRes(),
        logoRes = this.getLogo(),
        logoColor = this.getLogoColor(),
        statusList =
            when (this) {
                is TrackListService -> null
                is TrackStatusService -> this.getStatusList().toImmutableList()
            },
        lists =
            when (this) {
                is TrackListService -> this.viewLists().toPersistentList()
                is TrackStatusService -> null
            },
        supportsReadingDates = this.supportsReadingDates,
        canRemoveFromService = this.canRemoveFromService(),
        isAutoAddTracker = this.isAutoAddTracker(),
        isMdList = this.isMdList(),
        status = { num ->
            when (this) {
                is TrackListService -> null
                is TrackStatusService -> this.getStatus(num)
            }
        },
        currentList = { listId ->
            when (this) {
                is TrackListService -> this.viewLists().find { it.id == listId }
                is TrackStatusService -> null
            }
        },
        displayScore = { track -> this.displayScore(track.toDbTrack()) },
        scoreList = this.getScoreList().toImmutableList(),
        indexToScore = { index -> this.indexToScore(index) },
    )
}

fun TrackItem.toDbTrack(): Track {
    return Track.create(this.trackServiceId).apply {
        this.id = this@toDbTrack.id
        this.manga_id = this@toDbTrack.mangaId
        this.sync_id = this@toDbTrack.trackServiceId
        this.media_id = this@toDbTrack.mediaId
        this.library_id = this@toDbTrack.libraryId
        this.title = this@toDbTrack.title
        this.last_chapter_read = this@toDbTrack.lastChapterRead
        this.total_chapters = this@toDbTrack.totalChapters
        this.score = this@toDbTrack.score
        this.status = this@toDbTrack.status
        this.tracking_url = this@toDbTrack.trackingUrl
        this.started_reading_date = this@toDbTrack.startedReadingDate
        this.finished_reading_date = this@toDbTrack.finishedReadingDate
        this.listIds = this@toDbTrack.listIds
    }
}

fun Track.toTrackItem(): TrackItem {

    return TrackItem(
        id = this.id,
        mangaId = this.manga_id,
        trackServiceId = this.sync_id,
        mediaId = this.media_id,
        libraryId = this.library_id,
        title = this.title,
        lastChapterRead = this.last_chapter_read,
        totalChapters = this.total_chapters,
        score = this.score,
        status = this.status,
        listIds = this.listIds,
        trackingUrl = this.tracking_url,
        startedReadingDate = this.started_reading_date,
        finishedReadingDate = this.finished_reading_date,
    )
}

data class TrackSearchItem(
    val coverUrl: String,
    val summary: String,
    val publishingStatus: String,
    var publishingType: String,
    var startDate: String,
    val trackItem: TrackItem,
)

fun TrackSearch.toTrackSearchItem(): TrackSearchItem {
    return TrackSearchItem(
        coverUrl = this.cover_url,
        summary = this.summary,
        publishingStatus = this.publishing_status,
        publishingType = this.publishing_type,
        startDate = this.start_date,
        trackItem = this.toTrackItem(),
    )
}
