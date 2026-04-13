package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "track_manga_id") val mangaId: Long,
    @ColumnInfo(name = "track_sync_id") val syncId: Int,
    @ColumnInfo(name = "track_media_id") val mediaId: Long,
    @ColumnInfo(name = "track_library_id") val libraryId: Long,
    @ColumnInfo(name = "track_title") val title: String,
    @ColumnInfo(name = "track_last_chapter_read") val lastChapterRead: Float,
    @ColumnInfo(name = "track_total_chapters") val totalChapters: Int,
    @ColumnInfo(name = "track_status") val status: Int,
    @ColumnInfo(name = "track_score") val score: Float,
    @ColumnInfo(name = "track_tracking_url") val trackingUrl: String,
    @ColumnInfo(name = "track_start_date") val startedReadingDate: Long,
    @ColumnInfo(name = "track_finish_date") val finishedReadingDate: Long
)
