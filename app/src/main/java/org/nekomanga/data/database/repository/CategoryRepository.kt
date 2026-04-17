package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    // =========================================================================
    // CATEGORY OPERATIONS
    // =========================================================================

    fun observeCategories(): Flow<List<Category>>

    suspend fun getCategories(): List<Category>

    suspend fun getCategoryById(id: Int): Category?

    fun observeCategoriesForManga(mangaId: Long): Flow<List<Category>>

    suspend fun getCategoriesForManga(mangaId: Long): List<Category>

    suspend fun insertCategory(category: Category): Int

    suspend fun insertCategories(categories: List<Category>)

    suspend fun deleteCategory(category: Category)

    suspend fun deleteCategories(categories: List<Category>)

    // =========================================================================
    // MANGA_CATEGORY (JOIN TABLE) OPERATIONS
    // =========================================================================

    fun observeMangaCategories(mangaIds: List<Long>): Flow<List<MangaCategory>>

    suspend fun getMangaCategories(mangaIds: List<Long>): List<MangaCategory>

    suspend fun getMangaCategoriesForCategory(categoryId: Int): List<MangaCategory>

    suspend fun insertMangaCategory(mangaCategory: MangaCategory)

    /** Replaces the categories for the given list of mangaIds in a single transaction. */
    suspend fun setMangaCategories(mangaCategories: List<MangaCategory>, mangaIds: List<Long>)
}
