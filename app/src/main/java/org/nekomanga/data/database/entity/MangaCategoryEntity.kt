package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_categories",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = CategoryEntity::class,
                parentColumns = ["id"],
                childColumns = ["category_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index(value = ["manga_id"]), Index(value = ["category_id"])],
)
data class MangaCategoryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Int,
)
