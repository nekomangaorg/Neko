package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "similar")
data class MangaSimilarEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "manga_data") val data: String
)
