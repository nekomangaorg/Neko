package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeRepositoryImpl
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Manga handling */
class MangaUseCases(
    mangaRepository: MangaRepositoryImpl = Injekt.get(),
    chapterRepository: ChapterRepositoryImpl = Injekt.get(),
    categoryRepository: CategoryRepositoryImpl = Injekt.get(),
    mergeRepository: MergeRepositoryImpl = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
    preferences: PreferencesHelper = Injekt.get(),
    storageManager: StorageManager = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(mangaRepository, chapterRepository, downloadManager)

    val modifyManga =
        ModifyMangaUseCase(
            mangaRepository,
            categoryRepository,
            preferences,
            downloadManager,
            storageManager,
        )

    val updateMangaAggregate = UpdateMangaAggregate(mergeRepository, sourceManager)
}
