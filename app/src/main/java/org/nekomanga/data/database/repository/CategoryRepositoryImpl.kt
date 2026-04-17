package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.CategoryDao
import org.nekomanga.data.database.dao.MangaCategoryDao
import org.nekomanga.data.database.mapper.toCategory
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toMangaCategory

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val mangaCategoryDao: MangaCategoryDao,
) : CategoryRepository {

    // --- Category Operations ---

    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun getCategories(): List<Category> {
        return categoryDao.getAllCategories().map { it.toCategory() }
    }

    override suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)?.toCategory()
    }

    override fun observeCategoriesForManga(mangaId: Long): Flow<List<Category>> {
        return categoryDao.observeCategoriesForManga(mangaId).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun getCategoriesForManga(mangaId: Long): List<Category> {
        return categoryDao.getCategoriesForManga(mangaId).map { it.toCategory() }
    }

    override suspend fun insertCategory(category: Category): Int {
        return categoryDao.insertCategory(category.toEntity()).toInt()
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun deleteCategories(categories: List<Category>) {
        categoryDao.deleteCategories(categories.map { it.toEntity() })
    }

    // --- MangaCategory (Join) Operations ---

    override fun observeMangaCategories(mangaIds: List<Long>): Flow<List<MangaCategory>> {
        return mangaCategoryDao.observeMangaCategories(mangaIds).map { entities ->
            entities.map { it.toMangaCategory() }
        }
    }

    override suspend fun getMangaCategories(mangaIds: List<Long>): List<MangaCategory> {
        return mangaCategoryDao.getMangaCategories(mangaIds).map { it.toMangaCategory() }
    }

    override suspend fun getMangaCategoriesForCategory(categoryId: Int): List<MangaCategory> {
        return mangaCategoryDao.getMangaCategoriesForCategory(categoryId).map {
            it.toMangaCategory()
        }
    }

    override suspend fun insertMangaCategory(mangaCategory: MangaCategory) {
        mangaCategoryDao.insertMangaCategory(mangaCategory.toEntity())
    }

    override suspend fun setMangaCategories(
        mangaCategories: List<MangaCategory>,
        mangaIds: List<Long>,
    ) {
        mangaCategoryDao.setMangaCategories(
            mangaListCategories = mangaCategories.map { it.toEntity() },
            mangaIds = mangaIds,
        )
    }
}
