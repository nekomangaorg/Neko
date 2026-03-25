package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Manga handling */
class MangaUseCases(
    db: DatabaseHelper = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
    preferences: PreferencesHelper = Injekt.get(),
    storageManager: StorageManager = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(db, downloadManager)

    val modifyManga = ModifyMangaUseCase(db, preferences, downloadManager, storageManager)

    val updateMangaAggregate = UpdateMangaAggregate(db, sourceManager)
}
