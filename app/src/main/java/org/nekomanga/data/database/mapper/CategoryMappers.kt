package org.nekomanga.data.database.mapper
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import org.nekomanga.data.database.entity.CategoryEntity

fun CategoryEntity.toImpl(): CategoryImpl {
    return CategoryImpl().apply {
        id = this@toImpl.id
        name = this@toImpl.name
        order = this@toImpl.order
        flags = this@toImpl.flags

        // Replicating the exact logic from StorIO's CategoryGetResolver
        val orderString = this@toImpl.mangaOrder
        when {
            orderString.isBlank() -> {
                mangaSort = 'a'
                mangaOrder = emptyList()
            }
            orderString.firstOrNull()?.isLetter() == true -> {
                mangaSort = orderString.first()
                mangaOrder = emptyList()
            }
            else -> {
                mangaOrder = orderString.split("/").mapNotNull { it.toLongOrNull() }
            }
        }
    }
}

fun CategoryImpl.toEntity(): CategoryEntity {
    // Replicating the exact logic from StorIO's CategoryPutResolver
    val orderString = if (this.mangaSort != null) {
        this.mangaSort.toString()
    } else {
        this.mangaOrder.joinToString("/")
    }

    return CategoryEntity(
        id = this.id ?: 0, // Safely handles auto-generation for new inserts
        name = this.name,
        order = this.order,
        flags = this.flags,
        mangaOrder = orderString
    )
}
