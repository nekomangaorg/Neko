package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artwork",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["manga_id"])],
)
data class ArtworkEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "volume") val volume: String,
    @ColumnInfo(name = "locale") val locale: String,
    @ColumnInfo(name = "description") val description: String,
)
