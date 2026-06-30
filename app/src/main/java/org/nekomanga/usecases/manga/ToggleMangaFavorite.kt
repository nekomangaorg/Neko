package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.models.MangaCategory
import java.util.Date
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.library.LibraryPreferences

class ToggleMangaFavorite(
    private val mangaRepository: MangaRepository,
    private val categoryRepository: CategoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val updateMangaAggregate: UpdateMangaAggregate,
) {
    suspend operator fun invoke(
        mangaId: Long,
        categoryItems: List<CategoryItem>,
        categoriesProvider: () -> List<CategoryItem>,
    ): Boolean? {
        val editManga = mangaRepository.getMangaById(mangaId) ?: return null
        editManga.apply {
            favorite = !favorite
            date_added =
                when (favorite) {
                    true -> Date().time
                    false -> 0
                }
        }
        mangaRepository.updateManga(editManga)

        updateMangaAggregate(mangaId, editManga.url, editManga.favorite)

        if (editManga.favorite) {
            val defaultCategory = libraryPreferences.defaultCategory().get()

            if (categoryItems.isEmpty() && defaultCategory != -1) {
                categoriesProvider()
                    .firstOrNull { defaultCategory == it.id }
                    ?.let {
                        val categories =
                            listOf(MangaCategory.create(editManga, it.toDbCategory()))
                        categoryRepository.setMangaCategories(
                            categories,
                            listOf(editManga.id!!),
                        )
                    }
            } else if (categoryItems.isNotEmpty()) {
                val categories = categoryItems.map {
                    MangaCategory.create(editManga, it.toDbCategory())
                }
                categoryRepository.setMangaCategories(categories, listOf(editManga.id!!))
            }
        }

        return editManga.favorite
    }
}
