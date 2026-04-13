package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merge_manga")
data class MergeMangaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "merge_type") val mergeType: Int
)
