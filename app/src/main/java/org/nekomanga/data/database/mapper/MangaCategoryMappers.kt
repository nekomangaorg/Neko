package org.nekomanga.data.database.mapper
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import org.nekomanga.data.database.entity.MangaCategoryEntity

fun MangaCategoryEntity.toMangaCategory(): MangaCategory {
    return MangaCategory().apply {
        id = this@toMangaCategory.id // No need for ?.toLong() since it's already a Long
        manga_id = this@toMangaCategory.mangaId
        category_id = this@toMangaCategory.categoryId
    }
}

fun MangaCategory.toEntity(): MangaCategoryEntity {
    return MangaCategoryEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        mangaId = this.manga_id,
        categoryId = this.category_id
    )
}
