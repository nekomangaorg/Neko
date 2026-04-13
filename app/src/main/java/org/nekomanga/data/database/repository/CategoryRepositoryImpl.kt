package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.CategoryDao
import org.nekomanga.data.database.dao.MangaCategoryDao
import org.nekomanga.data.database.entity.CategoryEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val mangaCategoryDao: MangaCategoryDao
) {

    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getCategoryById(id: Int): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    fun getCategoriesForManga(mangaId: Long): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesForManga(mangaId)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategories(categories: List<CategoryEntity>) {
        categoryDao.deleteCategories(categories)
    }

    fun getMangaCategories(mangaIds: List<Long>): Flow<List<MangaCategoryEntity>> {
        return mangaCategoryDao.getMangaCategories(mangaIds)
    }

    suspend fun insertMangaCategory(mangaCategory: MangaCategoryEntity) {
        mangaCategoryDao.insertMangaCategory(mangaCategory)
    }

    suspend fun insertMangaListCategories(mangaListCategories: List<MangaCategoryEntity>) {
        mangaCategoryDao.insertMangaListCategories(mangaListCategories)
    }

    suspend fun deleteOldMangaListCategories(mangaIds: List<Long>) {
        mangaCategoryDao.deleteOldMangaListCategories(mangaIds)
    }

    suspend fun setMangaCategories(mangaListCategories: List<MangaCategoryEntity>, mangaIds: List<Long>) {
        mangaCategoryDao.setMangaCategories(mangaListCategories, mangaIds)
    }

    suspend fun deleteMangaFromAllCategories(mangaId: Long) {
        categoryDao.deleteMangaFromAllCategories(mangaId)
    }
}
