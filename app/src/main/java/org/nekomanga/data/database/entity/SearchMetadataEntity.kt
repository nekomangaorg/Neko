package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_metadata")
data class SearchMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "manga_id") val mangaId: Long = 0L,
    @ColumnInfo(name = "uploader") val uploader: String?,
    @ColumnInfo(name = "extra") val extra: String,
    @ColumnInfo(name = "indexed_extra") val indexedExtra: String?,
    @ColumnInfo(name = "extra_version") val extraVersion: Int,
)
