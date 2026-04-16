package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import org.nekomanga.data.database.entity.MangaAggregateEntity

fun MangaAggregateEntity.toMangaAggregate(): MangaAggregate {
    return MangaAggregate(
        mangaId = this.mangaId,
        volumes = this.volumes
    )
}

fun MangaAggregate.toEntity(): MangaAggregateEntity {
    return MangaAggregateEntity(
        mangaId = this.mangaId,
        volumes = this.volumes
    )
}
