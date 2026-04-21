package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import org.nekomanga.data.database.entity.MergeMangaEntity

fun MergeMangaEntity.toMergeMangaImpl(): MergeMangaImpl {
    return MergeMangaImpl(
        id = this.id,
        mangaId = this.mangaId,
        coverUrl = this.coverUrl,
        url = this.url,
        title = this.title,
        mergeType = this.mergeType,
    )
}

fun MergeMangaImpl.toEntity(): MergeMangaEntity {
    return MergeMangaEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        mangaId = this.mangaId,
        coverUrl = this.coverUrl,
        url = this.url,
        title = this.title,
        mergeType = this.mergeType,
    )
}
