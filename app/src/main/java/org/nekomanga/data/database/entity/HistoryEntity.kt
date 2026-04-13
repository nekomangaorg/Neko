package org.nekomanga.data.database.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "history_chapter_id") val chapterId: Long,
    @ColumnInfo(name = "history_last_read") val lastRead: Long,
    @ColumnInfo(name = "history_time_read") val timeRead: Long
)
