package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getAll
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.map
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.domain.DisplayResult
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.toDisplayResult
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val loginHelper: MangaDexLoginHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    fun isLoggedIn() = loginHelper.isLoggedIn()

    suspend fun getRandomManga(): Result<DisplayManga, ResultError> {
        return mangaDex.getRandomManga().andThen {
            val displayManga = it.toDisplayManga(db, mangaDex.id)
            Ok(displayManga)
        }
    }

    suspend fun getSearchPage(page: Int, filters: DexFilters): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.search(page, filters)
            .andThen { mangaListPage ->
                val displayMangaList = mangaListPage.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(Pair(mangaListPage.hasNextPage, displayMangaList))
            }
    }

    suspend fun getDeepLinkManga(uuid: String): Result<DisplayManga, ResultError> {
        return mangaDex.searchForManga(uuid).andThen { mangaListPage ->
            val displayManga = mangaListPage.sourceManga.first().toDisplayManga(db, mangaDex.id)
            Ok(displayManga)
        }
    }

    suspend fun getList(listUuid: String): Result<List<DisplayManga>, ResultError> {
        return mangaDex.fetchList(listUuid).andThen { resultListPage ->
            val displayManga = resultListPage.sourceManga.map { it.toDisplayManga(db, sourceId = mangaDex.id) }.toImmutableList()
            Ok(displayManga)
        }
    }

    suspend fun getAuthors(authorQuery: String): Result<List<DisplayResult>, ResultError> {
        return mangaDex.searchForAuthor(authorQuery).andThen { resultListPage ->
            val displayManga = resultListPage.results.map { it.toDisplayResult() }
            Ok(displayManga)
        }
    }

    suspend fun getGroups(groupQuery: String): Result<List<DisplayResult>, ResultError> {
        return mangaDex.searchForGroup(groupQuery).andThen { resultListPage ->
            val displayManga = resultListPage.results.map { it.toDisplayResult() }
            Ok(displayManga)
        }
    }

    suspend fun getHomePage(): Result<List<HomePageManga>, ResultError> {
        val blockedScanlatorUUIDs = preferenceHelper.blockedScanlators().get().map {
            var scanlatorImpl = db.getScanlatorByName(it).executeAsBlocking()
            if (scanlatorImpl == null) {
                mangaDex.getScanlator(scanlator = it).map { scanlator ->
                    scanlatorImpl = scanlator.toScanlatorImpl()
                    db.insertScanlators(listOf(scanlatorImpl!!)).executeOnIO()
                    scanlatorImpl!!
                }
            } else {
                Ok(scanlatorImpl!!)
            }
        }
        return if (blockedScanlatorUUIDs.getAllErrors().isNotEmpty()) {
            Err(blockedScanlatorUUIDs.getAllErrors().first())
        } else {
            val uuids = blockedScanlatorUUIDs.getAll().map { it.uuid }
            mangaDex.fetchHomePageInfo(MdConstants.currentSeasonalId, uuids)
                .andThen { listResults ->
                    Ok(
                        listResults.map { listResult ->
                            HomePageManga(
                                displayScreenType = listResult.displayScreenType,
                                displayManga = listResult.sourceManga.map { it.toDisplayManga(db, mangaDex.id) }.toPersistentList(),
                            )
                        },
                    )
                }
        }
    }

    suspend fun getFollows(): Result<ImmutableList<DisplayManga>, ResultError> {
        return mangaDex.fetchAllFollows()
            .andThen { sourceManga ->
                Ok(sourceManga.map { it.toDisplayManga(db, mangaDex.id) }.toImmutableList())
            }
    }
}

enum class DeepLinkType {
    Author,
    Group,
    Error,
    Manga,
    List,
    None,
    ;

    companion object {
        fun getDeepLinkType(query: String): DeepLinkType {
            return when {
                query.startsWith(MdConstants.DeepLinkPrefix.author) -> Author
                query.startsWith(MdConstants.DeepLinkPrefix.group) -> Group
                query.startsWith(MdConstants.DeepLinkPrefix.manga) -> Manga
                query.startsWith(MdConstants.DeepLinkPrefix.error) -> Error
                query.startsWith(MdConstants.DeepLinkPrefix.list) -> List
                else -> None
            }
        }

        fun removePrefix(query: String, deepLinkType: DeepLinkType): String {
            return when (deepLinkType) {
                Author -> query.removePrefix(MdConstants.DeepLinkPrefix.author)
                Group -> query.removePrefix(MdConstants.DeepLinkPrefix.group)
                Manga -> query.removePrefix(MdConstants.DeepLinkPrefix.manga)
                List -> query.removePrefix(MdConstants.DeepLinkPrefix.list)
                Error -> query.removePrefix(MdConstants.DeepLinkPrefix.error)
                None -> query
            }
        }
    }
}
