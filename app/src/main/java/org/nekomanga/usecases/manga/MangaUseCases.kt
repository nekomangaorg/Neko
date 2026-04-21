package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaAggregateRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Manga handling */
class MangaUseCases(
    chapterRepository: ChapterRepository = Injekt.get(),
    mangaAggregateRepository: MangaAggregateRepository = Injekt.get(),
    mangaRepository: MangaRepository = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
    preferences: PreferencesHelper = Injekt.get(),
    storageManager: StorageManager = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(mangaRepository, chapterRepository, downloadManager)

    val modifyManga =
        ModifyMangaUseCase(mangaRepository, preferences, downloadManager, storageManager)

    val updateMangaAggregate = UpdateMangaAggregate(mangaAggregateRepository, sourceManager)
}
