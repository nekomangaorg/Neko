package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import org.nekomanga.data.database.entity.ArtworkEntity

fun ArtworkEntity.toImpl(): ArtworkImpl {
    return ArtworkImpl(
        id = this.id,
        mangaId = this.mangaId,
        fileName = this.fileName,
        volume = this.volume,
        locale = this.locale,
        description = this.description
    )
}

fun ArtworkImpl.toEntity(): ArtworkEntity {
    return ArtworkEntity(
        id = this.id ?: 0L, // Safely handles new inserts
        mangaId = this.mangaId,
        fileName = this.fileName,
        volume = this.volume,
        locale = this.locale,
        description = this.description
    )
}
