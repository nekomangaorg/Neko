package eu.kanade.tachiyomi.ui.source.latest

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.toDisplayManga
import okhttp3.internal.toImmutableList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LatestRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().getMangadex(),
    private val db: DatabaseHelper = Injekt.get(),
) {

    suspend fun getPage(page: Int): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.latestChapters(page).mapBoth(
            success = { mangaListPage ->
                val displayMangaList = mangaListPage.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(mangaListPage.hasNextPage to displayMangaList.toImmutableList())
            },
            failure = { Err(it) },
        )
    }
}
