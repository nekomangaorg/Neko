package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().getMangadex(),
    private val db: DatabaseHelper = Injekt.get(),
) {

    suspend fun getSearchPage(page: Int, query: String, filter: FilterList): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.search2(page, query, filter)
            .andThen { mangaListPage ->
                when (mangaListPage.sourceManga.isEmpty()) {
                    true -> Err(ResultError.Generic(errorRes = R.string.no_results_found))
                    false -> {
                        val displayMangaList = mangaListPage.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                        Ok(mangaListPage.hasNextPage to displayMangaList)
                    }
                }
            }
    }

    suspend fun getInitialPage(): Result<List<HomePageManga>, ResultError> {
        return mangaDex.fetchList(MdConstants.currentSeasonalId).andThen { listResult ->
            Ok(
                listOf(
                    HomePageManga(
                        title = listResult.name,
                        displayManga = listResult.sourceManga.map { it.toDisplayManga(db, mangaDex.id) }.toPersistentList(),
                    ),
                ),
            )
        }
    }
}
