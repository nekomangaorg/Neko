package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.util.Date
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ModifyMangaUseCase(
    private val mangaRepository: MangaRepositoryImpl,
    private val categoryRepository: CategoryRepositoryImpl,
    private val preferences: PreferencesHelper,
    private val downloadManager: DownloadManager,
    private val storageManager: StorageManager,
) {
    suspend fun addNewCategory(newCategory: String, order: Int) {
        val category = Category.create(newCategory).apply { this.order = order }
        categoryRepository.insertCategory(category.toEntity())
    }

    suspend fun updateMangaCategories(mangaId: Long, enabledCategories: List<CategoryItem>) {
        val dbManga = mangaRepository.getMangaById(mangaId)?.toManga() ?: return
        val categories = enabledCategories.map { MangaCategory.create(dbManga, it.toDbCategory()).toEntity() }
        categoryRepository.setMangaCategories(categories, listOf(mangaId))
    }

    suspend fun setAltTitle(
        mangaId: Long,
        title: String?,
    ): eu.kanade.tachiyomi.data.database.models.Manga? {
        val dbManga = mangaRepository.getMangaById(mangaId)?.toManga() ?: return null
        val previousEffectiveTitle = dbManga.user_title ?: dbManga.title
        val newEffectiveTitle = title ?: dbManga.title

        if (previousEffectiveTitle != newEffectiveTitle) {
            dbManga.user_title = title
            mangaRepository.insertManga(dbManga.toEntity())

            val provider = DownloadProvider(preferences.context)
            provider.renameMangaFolder(previousEffectiveTitle, newEffectiveTitle)
            downloadManager.updateDownloadCacheForManga(dbManga)
            storageManager.renamePagesAndCoverDirectory(previousEffectiveTitle, newEffectiveTitle)
        }

        return dbManga
    }

    suspend fun toggleFavorite(mangaId: Long): eu.kanade.tachiyomi.data.database.models.Manga? {
        val editManga = mangaRepository.getMangaById(mangaId)?.toManga() ?: return null
        editManga.apply {
            favorite = !favorite
            date_added =
                when (favorite) {
                    true -> Date().time
                    false -> 0
                }
        }
        mangaRepository.insertManga(editManga.toEntity())

        val mangaUseCases: MangaUseCases = Injekt.get()
        mangaUseCases.updateMangaAggregate(mangaId, editManga.url, editManga.favorite)

        return editManga
    }
}
