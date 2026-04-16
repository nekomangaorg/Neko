package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.data.database.models.MergeType

@Entity(
    tableName = "merge_manga",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["_id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["manga_id"], name = "merge_manga_manga_id_index")],
)
data class MergeMangaEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "mergeType") val mergeType: MergeType,
)
