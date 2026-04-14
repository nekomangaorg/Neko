package org.nekomanga.usecases.category

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.DisplayManga

class ModifyCategoryUseCase(
    private val db: DatabaseHelper,
    private val libraryPreferences: LibraryPreferences,
) {

    suspend fun addNewCategory(newCategory: String, order: Int) {
        val category = Category.create(newCategory).apply { this.order = order }
        db.insertCategory(category).executeOnIO()
    }

    suspend fun setMangaCategories(mangaList: List<DisplayManga>, categories: List<CategoryItem>) {
        val dbCategories = categories.map { it.toDbCategory() }

        val mangaIds = mangaList.map { it.mangaId }
        val dbMangas = db.getMangas(mangaIds).executeOnIO()
        val mangaCategories = dbMangas.flatMap { dbManga ->
            dbCategories.map { MangaCategory.create(dbManga, it) }
        }
        db.setMangaCategories(mangaCategories, dbMangas)
    }

    suspend fun updateCategorySortAscending(category: CategoryItem) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortAscending().set(!category.isAscending)
        } else {
            val updatedDbCategory = category.toDbCategory(true)
            db.insertCategory(updatedDbCategory).executeOnIO()
        }
    }

    suspend fun updateCategoryLibrarySort(category: CategoryItem, librarySort: LibrarySort) {
        if (category.isDynamic || category.isSystemCategory) {
            libraryPreferences.sortingMode().set(librarySort.mainValue)
        } else {
            val updatedDbCategory =
                category.copy(isAscending = true, sortOrder = librarySort).toDbCategory()
            db.insertCategory(updatedDbCategory).executeOnIO()
        }
    }

    suspend fun updateMangaCategories(mangaId: Long, enabledCategories: List<CategoryItem>) {
        val dbManga = db.getManga(mangaId).executeOnIO() ?: return
        val categories = enabledCategories.map { MangaCategory.create(dbManga, it.toDbCategory()) }
        db.setMangaCategories(categories, listOf(dbManga))
    }
}
