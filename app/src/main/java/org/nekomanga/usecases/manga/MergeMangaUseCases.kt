package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MergeMangaUseCases(
    db: DatabaseHelper = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) {
    val searchMergedManga = SearchMergedManga(sourceManager)
    val removeMergedManga = RemoveMergedManga(db, downloadManager, libraryPreferences)
    val addMergedManga = AddMergedManga(db)
}
