package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_aggregate",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class MangaAggregateEntity(
    @PrimaryKey @ColumnInfo(name = "manga_id") val mangaId: Long = 0L,
    @ColumnInfo(name = "volumes") val volumes: String,
)
