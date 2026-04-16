package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mangas_categories",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["_id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = CategoryEntity::class,
                parentColumns = ["_id"],
                childColumns = ["category_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices =
        [
            Index(value = ["manga_id"], name = "mangas_categories_manga_id_index"),
            Index(value = ["category_id"], name = "mangas_categories_category_id_index"),
        ],
)
data class MangaCategoryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Int,
)
