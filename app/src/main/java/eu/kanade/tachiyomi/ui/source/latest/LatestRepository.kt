package eu.kanade.tachiyomi.ui.source.latest

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.toLocalManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LatestRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().getMangadex(),
    private val db: DatabaseHelper = Injekt.get(),
) {

    suspend fun getPage(page: Int): Pair<Boolean, List<DisplayManga>> {
        val results = mangaDex.latestChapters(page)
        val displayMangaList = results.displayManga.map {
            DisplayManga(
                it.sManga.toLocalManga(db, mangaDex.id),
                it.displayText,
            )
        }

        return results.hasNextPage to displayMangaList
    }
}