package org.nekomanga.usecases.category

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import org.nekomanga.presentation.screens.library.LibrarySort
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga

class ModifyCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val mangaRepository: MangaRepository,
    private val libraryPreferences: LibraryPreferences,
) {

    suspend fun addNewCategory(newCategory: String) {
        val categories = categoryRepository.getCategories()
        val order = (categories.maxOfOrNull { it.order } ?: 0) + 1
        val category = Category.create(newCategory).apply {
            this.order = order
            this.mangaSort = LibrarySort.Title.categoryValue
        }
        categoryRepository.insertCategory(category)
    }

    suspend fun setMangaCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        val dbCategories = categories.map { it.toDbCategory() }

        val mangaIds = mangaList.map { it.mangaId }
        val dbMangas = mangaRepository.getMangaByIds(mangaIds)
        val mangaCategories = dbMangas.flatMap { dbManga ->
            dbCategories.map { MangaCategory.create(dbManga, it) }
        }
        categoryRepository.setMangaCategories(mangaCategories, dbMangas.mapNotNull { it.id })
    }

    suspend fun updateCategorySortAscending(category: CategoryItem) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortAscending().set(!category.isAscending)
        } else {
            val updatedDbCategory = category.toDbCategory(true)
            categoryRepository.insertCategory(updatedDbCategory)
        }
    }

    suspend fun updateCategoryLibrarySort(category: CategoryItem, librarySort: LibrarySort) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortingMode().set(librarySort.mainValue)
        } else {
            val updatedDbCategory =
                category.copy(isAscending = true, sortOrder = librarySort).toDbCategory()
            categoryRepository.insertCategory(updatedDbCategory)
        }
    }

    suspend fun updateMangaCategories(mangaId: Long, enabledCategories: List<CategoryItem>) {
        val dbManga = mangaRepository.getMangaById(mangaId) ?: return
        val categories = enabledCategories.map { MangaCategory.create(dbManga, it.toDbCategory()) }
        categoryRepository.setMangaCategories(categories, listOf(dbManga.id!!))
    }

    suspend fun deleteCategory(categoryId: Int) {
        categoryRepository.getCategoryById(categoryId)?.let {
            categoryRepository.deleteCategory(it)
        }
    }

    suspend fun updateCategoryName(categoryId: Int, newName: String) {
        categoryRepository.getCategoryById(categoryId)?.let {
            it.name = newName
            categoryRepository.insertCategory(it)
        }
    }

    suspend fun reorderCategories(category: CategoryItem, newIndex: Int) {
        val dbCategories = categoryRepository.getCategories().toMutableList()
        val currentIndex = dbCategories.indexOfFirst { category.id == it.id }
        if (currentIndex != -1) {
            dbCategories.add(newIndex, dbCategories.removeAt(currentIndex))
            dbCategories.forEachIndexed { index, dbCategory -> dbCategory.order = index + 1 }
            categoryRepository.insertCategories(dbCategories)
        }
    }
}
