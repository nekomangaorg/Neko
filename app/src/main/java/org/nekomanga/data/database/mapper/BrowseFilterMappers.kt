package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import org.nekomanga.data.database.entity.BrowseFilterEntity

fun BrowseFilterEntity.toImpl(): BrowseFilterImpl {
    return BrowseFilterImpl(
        id = this.id,
        name = this.name,
        default = this.isDefault,
        dexFilters = this.filters,
    )
}

fun BrowseFilterImpl.toEntity(): BrowseFilterEntity {
    return BrowseFilterEntity(
        id = this.id ?: 0L,
        name = this.name,
        isDefault = this.default,
        filters = this.dexFilters,
    )
}
