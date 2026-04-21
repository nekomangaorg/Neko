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
                parentColumns = ["id"],
                childColumns = ["chapter_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["chapter_id"], unique = true)],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "chapter_id") val chapterId: Long,
    @ColumnInfo(name = "last_read") val lastRead: Long,
    @ColumnInfo(name = "time_read") val timeRead: Long,
)
