package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import org.nekomanga.data.database.entity.UploaderEntity

fun UploaderEntity.toImpl(): UploaderImpl {
    return UploaderImpl(id = this.id, username = this.username, uuid = this.uuid)
}

fun UploaderImpl.toEntity(): UploaderEntity {
    return UploaderEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        username = this.username,
        uuid = this.uuid,
    )
}

// Optional List extensions for cleaner Repository code
fun List<UploaderEntity>.toImplList(): List<UploaderImpl> {
    return this.map { it.toImpl() }
}

fun List<UploaderImpl>.toEntityList(): List<UploaderEntity> {
    return this.map { it.toEntity() }
}
