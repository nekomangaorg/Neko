package eu.kanade.tachiyomi.ui.similar

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.toDisplayManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SimilarRepository(
    private val db: DatabaseHelper = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {

    suspend fun fetchSimilar(
        mangaId: String,
        forceRefresh: Boolean
    ): List<DisplayManga> {
        return withContext(Dispatchers.IO) {
            val manga = db.getManga(mangaId, sourceManager.mangaDex.id).executeAsBlocking()!!

            val similar = async {
                sourceManager.mangaDex.getSimilar(manga, forceRefresh)
            }
            val recommended = async {
                sourceManager.mangaDex.getRecommended(manga, forceRefresh)
            }

            val external = async {
                sourceManager.mangaDex.getExternal(manga)
            }

            val similarList = similar.await().map { it.toDisplayManga(db, sourceManager.mangaDex.id) }
            val recommendedList = recommended.await().map { it.toDisplayManga(db, sourceManager.mangaDex.id) }
            val externalList = external.await().map { it.toDisplayManga(db, sourceManager.mangaDex.id) }
            (similarList + recommendedList + externalList).distinctBy { it.mangaId }
        }
    }
}
