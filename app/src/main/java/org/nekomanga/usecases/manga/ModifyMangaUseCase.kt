package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import java.util.Date
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ModifyMangaUseCase(
    private val db: DatabaseHelper,
    private val preferences: PreferencesHelper,
    private val downloadManager: DownloadManager,
    private val storageManager: StorageManager,
) {
    suspend fun setAltTitle(
        mangaId: Long,
        title: String?,
    ): eu.kanade.tachiyomi.data.database.models.Manga? {
        val dbManga = db.getManga(mangaId).executeOnIO() ?: return null
        val previousEffectiveTitle = dbManga.user_title ?: dbManga.title
        val newEffectiveTitle = title ?: dbManga.title

        if (previousEffectiveTitle != newEffectiveTitle) {
            dbManga.user_title = title
            db.insertManga(dbManga).executeOnIO()

            val provider = DownloadProvider(preferences.context)
            provider.renameMangaFolder(previousEffectiveTitle, newEffectiveTitle)
            downloadManager.updateDownloadCacheForManga(dbManga)
            storageManager.renamePagesAndCoverDirectory(previousEffectiveTitle, newEffectiveTitle)
        }

        return dbManga
    }

    suspend fun toggleFavorite(mangaId: Long): eu.kanade.tachiyomi.data.database.models.Manga? {
        val editManga = db.getManga(mangaId).executeOnIO() ?: return null
        editManga.apply {
            favorite = !favorite
            date_added =
                when (favorite) {
                    true -> Date().time
                    false -> 0
                }
        }
        db.insertManga(editManga).executeOnIO()

        val mangaUseCases: MangaUseCases = Injekt.get()
        mangaUseCases.updateMangaAggregate(mangaId, editManga.url, editManga.favorite)

        return editManga
    }
}
