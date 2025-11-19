package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Manga handling */
class MangaUseCases(
    db: DatabaseHelper = Injekt.get(),
    downloadManager: DownloadManager = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(db, downloadManager)
}
