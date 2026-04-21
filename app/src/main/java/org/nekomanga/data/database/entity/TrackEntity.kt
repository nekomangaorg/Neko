package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices =
        [
            Index(value = ["manga_id"]),
            Index(value = ["manga_id", "track_service_id"], unique = true),
        ],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "track_service_id") val trackServiceId: Int,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "library_id") val libraryId: Long?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "last_chapter_read") val lastChapterRead: Float,
    @ColumnInfo(name = "total_chapters") val totalChapters: Int,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "score") val score: Float,
    @ColumnInfo(name = "tracking_url") val trackingUrl: String,
    @ColumnInfo(name = "start_date") val startedReadingDate: Long,
    @ColumnInfo(name = "finish_date") val finishedReadingDate: Long,
)
