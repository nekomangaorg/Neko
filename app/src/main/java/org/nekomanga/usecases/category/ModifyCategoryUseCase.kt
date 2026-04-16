package org.nekomanga.usecases.category

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.ui.library.LibrarySort
import org.nekomanga.data.database.model.toCategory
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga

class ModifyCategoryUseCase(
    private val mangaRepositoryImpl: MangaRepositoryImpl,
    private val categoryRepositoryImpl: CategoryRepositoryImpl,
    private val libraryPreferences: LibraryPreferences,
) {

    suspend fun addNewCategory(newCategory: String) {
        categoryRepositoryImpl.getAllCategoriesList()
        val categories = categoryRepositoryImpl.getAllCategoriesList().map { it.toCategory() }
        val order = (categories.maxOfOrNull { it.order } ?: 0) + 1
        val category = Category.create(newCategory).apply { this.order = order }
        categoryRepositoryImpl.insertCategory(category.toEntity())
    }

    suspend fun setMangaCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        val dbCategories = categories.map { it.toDbCategory() }

        val mangaIds = mangaList.map { it.mangaId }
        val dbMangas = mangaRepositoryImpl.getMangas(mangaIds).map { it.toManga() }
        val mangaCategories =
            dbMangas
                .flatMap { dbManga -> dbCategories.map { MangaCategory.create(dbManga, it) } }
                .map { it.toEntity() }

        categoryRepositoryImpl.setMangaCategories(mangaCategories, dbMangas.mapNotNull { it.id })
    }

    suspend fun updateCategorySortAscending(category: CategoryItem) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortAscending().set(!category.isAscending)
        } else {
            val updatedDbCategory = category.toDbCategory(true).toEntity()
            categoryRepositoryImpl.insertCategory(updatedDbCategory)
        }
    }

    suspend fun updateCategoryLibrarySort(category: CategoryItem, librarySort: LibrarySort) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortingMode().set(librarySort.mainValue)
        } else {
            val updatedDbCategory =
                category.copy(isAscending = true, sortOrder = librarySort).toDbCategory().toEntity()
            categoryRepositoryImpl.insertCategory(updatedDbCategory)
        }
    }

    suspend fun updateMangaCategories(mangaId: Long, enabledCategories: List<CategoryItem>) {
        val dbManga = mangaRepositoryImpl.getMangaByIdSync(mangaId)?.toManga() ?: return
        val categories =
            enabledCategories
                .map { MangaCategory.create(dbManga, it.toDbCategory()) }
                .map { it.toEntity() }
        categoryRepositoryImpl.setMangaCategories(categories, listOf(mangaId))
    }
}
