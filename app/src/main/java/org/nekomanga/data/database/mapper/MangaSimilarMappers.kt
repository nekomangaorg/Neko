package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.models.MangaSimilarImpl
import org.nekomanga.data.database.entity.MangaSimilarEntity

fun MangaSimilarEntity.toMangaSimilar(): MangaSimilar {
    return MangaSimilarImpl().apply {
        id = this@toMangaSimilar.id
        manga_id = this@toMangaSimilar.mangaId
        data = this@toMangaSimilar.data
    }
}

fun MangaSimilar.toEntity(): MangaSimilarEntity {
    return MangaSimilarEntity(id = this.id ?: 0L, mangaId = this.manga_id, data = this.data)
}
