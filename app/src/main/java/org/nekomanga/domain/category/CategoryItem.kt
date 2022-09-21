package org.nekomanga.domain.category

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class CategoryItem(
    val id: Int,
    val name: String,
    val order: Int = 0,
    val flags: Int = 0,
    val mangaOrder: ImmutableList<Long> = persistentListOf(),
    val mangaSort: Char? = null,
    val isAlone: Boolean = true,
    val isHidden: Boolean = false,
    val isDynamic: Boolean = false,
    val sourceId: Long? = null,
)

fun CategoryItem.toDbCategory(): CategoryImpl {
    return CategoryImpl().apply {
        id = this@toDbCategory.id
        name = this@toDbCategory.name
        order = this@toDbCategory.order
        flags = this@toDbCategory.flags
        mangaOrder = this@toDbCategory.mangaOrder.toList()
        mangaSort = this@toDbCategory.mangaSort
        isAlone = this@toDbCategory.isAlone
        isHidden = this@toDbCategory.isHidden
        isDynamic = this@toDbCategory.isDynamic
        sourceId = this@toDbCategory.sourceId
    }
}

fun Category.toCategoryItem(): CategoryItem {
    return CategoryItem(
        id = this.id!!,
        name = this.name,
        order = this.order,
        flags = this.flags,
        mangaOrder = this.mangaOrder.toImmutableList(),
        mangaSort = this.mangaSort,
        isAlone = this.isAlone,
        isHidden = this.isHidden,
        isDynamic = this.isDynamic,
        sourceId = this.sourceId,
    )
}
