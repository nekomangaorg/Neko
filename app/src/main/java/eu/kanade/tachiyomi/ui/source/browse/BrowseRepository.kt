package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.filterValues
import com.github.michaelbull.result.map
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    val loginHelper: MangaDexLoginHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {

    fun isLoggedIn() = loginHelper.isLoggedIn()

    suspend fun getRandomManga(): Result<DisplayManga, ResultError> {
        return mangaDex.getRandomManga().andThen {
            val displayManga = it.toDisplayManga(db, mangaDex.id)
            Ok(displayManga)
        }
    }

    suspend fun getSearchPage(
        page: Int,
        filters: DexFilters,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.search(page, filters).andThen { mangaListPage ->
            val displayMangaList =
                mangaListPage.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
            Ok(Pair(mangaListPage.hasNextPage, displayMangaList))
        }
    }

    suspend fun getHomePage(): Result<List<HomePageManga>, ResultError> {
        val blockedGroupUUIDs =
            mangaDexPreferences.blockedGroups().get().map {
                var scanlatorGroupImpl = db.getScanlatorGroupByName(it).executeAsBlocking()
                if (scanlatorGroupImpl == null) {
                    mangaDex.getScanlatorGroup(group = it).map { group ->
                        scanlatorGroupImpl = group.toScanlatorGroupImpl()
                        db.insertScanlatorGroups(listOf(scanlatorGroupImpl!!)).executeOnIO()
                        scanlatorGroupImpl!!
                    }
                } else {
                    Ok(scanlatorGroupImpl!!)
                }
            }
        val blockedUploaderUUIDs =
            mangaDexPreferences.blockedUploaders().get().map {
                var uploaderImpl = db.getUploaderByName(it).executeAsBlocking()
                if (uploaderImpl == null) {
                    mangaDex.getUploader(uploader = it).map { uploader ->
                        uploaderImpl = uploader.toUploaderImpl()
                        db.insertUploader(listOf(uploaderImpl!!)).executeOnIO()
                        uploaderImpl!!
                    }
                } else {
                    Ok(uploaderImpl!!)
                }
            }

        val scanlatorUUIDs = blockedGroupUUIDs.filterValues().map { it.uuid }
        val uploaderUUIDs = blockedUploaderUUIDs.filterValues().map { it.uuid }
        return mangaDex.fetchHomePageInfo(scanlatorUUIDs, uploaderUUIDs).andThen { listResults ->
            Ok(
                listResults.map { listResult ->
                    HomePageManga(
                        displayScreenType = listResult.displayScreenType,
                        displayManga =
                            listResult.sourceManga
                                .map { it.toDisplayManga(db, mangaDex.id) }
                                .distinctBy { it.url }
                                .toPersistentList(),
                    )
                }
            )
        }
    }

    suspend fun getFollows(): Result<PersistentList<DisplayManga>, ResultError> {
        return mangaDex.fetchAllFollows().andThen { sourceManga ->
            Ok(sourceManga.map { it.toDisplayManga(db, mangaDex.id) }.toPersistentList())
        }
    }
}
