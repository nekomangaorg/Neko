package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeRepositoryImpl
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MergeMangaUseCases(
    mangaRepository: MangaRepositoryImpl = Injekt.get(),
    chapterRepository: ChapterRepositoryImpl = Injekt.get(),
    mergeRepository: MergeRepositoryImpl = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) {
    val searchMergedManga = SearchMergedManga(sourceManager)
    val removeMergedManga =
        RemoveMergedManga(
            mangaRepository,
            chapterRepository,
            mergeRepository,
            downloadManager,
            libraryPreferences,
        )
    val addMergedManga = AddMergedManga(mergeRepository)
}
