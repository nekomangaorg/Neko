package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import org.nekomanga.data.database.entity.ScanlatorGroupEntity

fun ScanlatorGroupEntity.toImpl(): ScanlatorGroupImpl {
    return ScanlatorGroupImpl(
        id = this.id,
        name = this.name,
        uuid = this.uuid,
        description = this.description,
    )
}

fun ScanlatorGroupImpl.toEntity(): ScanlatorGroupEntity {
    return ScanlatorGroupEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        name = this.name,
        uuid = this.uuid,
        description = this.description,
    )
}
