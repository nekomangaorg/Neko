package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_aggregate")
data class MangaAggregateEntity(
    @PrimaryKey @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "volumes") val volumes: String,
)
