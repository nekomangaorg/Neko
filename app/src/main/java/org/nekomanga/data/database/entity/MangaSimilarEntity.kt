package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_related")
data class MangaSimilarEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "matched_ids") val data: String,
)
