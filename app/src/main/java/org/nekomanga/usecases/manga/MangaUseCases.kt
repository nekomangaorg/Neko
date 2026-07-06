package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaAggregateRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.site.MangaDexPreferences
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
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
    categoryRepository: CategoryRepository = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    scanlatorGroupRepository: ScanlatorGroupRepository = Injekt.get(),
    uploaderRepository: UploaderRepository = Injekt.get(),
    mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get(),
    mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(mangaRepository, chapterRepository, downloadManager)

    val modifyManga =
        ModifyMangaUseCase(mangaRepository, preferences, downloadManager, storageManager)

    val updateMangaAggregate = UpdateMangaAggregate(mangaAggregateRepository, sourceManager)

    val toggleMangaFavorite =
        ToggleMangaFavorite(mangaRepository, categoryRepository, libraryPreferences, updateMangaAggregate)

    val blockScanlator =
        BlockScanlator(
            scanlatorGroupRepository = scanlatorGroupRepository,
            uploaderRepository = uploaderRepository,
            mangaUpdateCoordinator = mangaUpdateCoordinator,
            mangaDexPreferences = mangaDexPreferences,
        )
}
