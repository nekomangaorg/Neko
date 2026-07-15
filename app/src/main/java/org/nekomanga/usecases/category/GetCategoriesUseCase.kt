package org.nekomanga.usecases.category

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem

class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun get(): List<CategoryItem> {
        return categoryRepository.getCategories().map { it.toCategoryItem() }
    }

    fun observe(): Flow<List<CategoryItem>> {
        return categoryRepository.observeCategories().map { list ->
            list.map { it.toCategoryItem() }
        }
    }
}
