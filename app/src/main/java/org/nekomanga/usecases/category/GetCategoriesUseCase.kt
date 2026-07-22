package org.nekomanga.usecases.category

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    suspend fun getCategoriesForManga(mangaId: Long): List<CategoryItem> {
        return categoryRepository.getCategoriesForManga(mangaId).map { it.toCategoryItem() }
    }

    fun observeCategoriesForManga(mangaId: Long): Flow<List<CategoryItem>> {
        return categoryRepository.observeCategoriesForManga(mangaId).map { list ->
            list.map { it.toCategoryItem() }
        }
    }

    suspend fun getMangaCategories(mangaIds: List<Long>): Map<Long, List<CategoryItem>> {
        val categories = categoryRepository.getCategories().associateBy { it.id }
        val mangaCategories = coroutineScope {
            mangaIds
                .chunked(900)
                .map { chunk -> async { categoryRepository.getMangaCategories(chunk) } }
                .toList()
                .awaitAll()
                .flatten()
        }
        return mangaCategories
            .groupBy { it.manga_id }
            .mapValues { (_, value) ->
                value.mapNotNull { categories[it.category_id]?.toCategoryItem() }
            }
    }
}

