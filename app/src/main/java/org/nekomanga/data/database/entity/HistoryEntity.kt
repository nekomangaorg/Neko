package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    foreignKeys =
        [
            ForeignKey(
                entity = ChapterEntity::class,
                parentColumns = ["_id"],
                childColumns = ["history_chapter_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["history_chapter_id"], name = "history_history_chapter_id_index", unique = true)],)
data class HistoryEntity(
    // Changed column name to "history_id" and made non-nullable
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "history_id") val id: Long = 0L,
    @ColumnInfo(name = "history_chapter_id") val chapterId: Long,
    @ColumnInfo(name = "history_last_read") val lastRead: Long,
    @ColumnInfo(name = "history_time_read") val timeRead: Long,
)
