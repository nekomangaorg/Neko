package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.toDisplayManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().getMangadex(),
    private val db: DatabaseHelper = Injekt.get(),
) {

    suspend fun getPage(page: Int): Pair<Boolean, List<DisplayManga>> {
        val results = mangaDex.latestChapters(page)
        val displayMangaList = results.displayManga.map { sourceManga ->
            sourceManga.toDisplayManga(db, mangaDex.id)
        }

        return results.hasNextPage to displayMangaList
    }
}
