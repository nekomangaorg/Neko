package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import org.nekomanga.data.database.entity.CategoryEntity

fun CategoryEntity.toCategory(): Category {
    return CategoryImpl().apply {
        id = this@toCategory.id
        name = this@toCategory.name
        order = this@toCategory.order
        flags = this@toCategory.flags

        // Replicating the exact logic from StorIO's CategoryGetResolver
        val orderString = this@toCategory.mangaOrder
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

fun Category.toEntity(): CategoryEntity {
    // Replicating the exact logic from StorIO's CategoryPutResolver
    val orderString =
        if (this.mangaSort != null) {
            this.mangaSort.toString()
        } else {
            this.mangaOrder.joinToString("/")
        }

    return CategoryEntity(
        id = this.id ?: 0, // Safely handles auto-generation for new inserts
        name = this.name,
        order = this.order,
        flags = this.flags,
        mangaOrder = orderString,
    )
}
