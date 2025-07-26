package eu.kanade.tachiyomi.ui.source.latest

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    suspend fun getPage(
        page: Int,
        displayScreenType: DisplayScreenType,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return when (displayScreenType) {
            is DisplayScreenType.LatestChapters -> getLatestChapterPage(page)
            is DisplayScreenType.List -> getListPage(displayScreenType.listUUID)
            is DisplayScreenType.RecentlyAdded -> getRecentlyAddedPage(page)
            is DisplayScreenType.PopularNewTitles -> getPopularNewTitles(page)
            is DisplayScreenType.FeedUpdates -> getFeedUpdatesPage(page)
        }
    }

    private suspend fun getFeedUpdatesPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        val blockedScanlatorUUIDs =
            preferenceHelper
                .blockedScanlators()
                .get()
                .mapNotNull {
                    var scanlatorImpl = db.getScanlatorByName(it).executeAsBlocking()
                    if (scanlatorImpl == null) {
                        mangaDex.getScanlator(scanlator = it).map { scanlator ->
                            scanlatorImpl = scanlator.toScanlatorImpl()
                        }
                        db.insertScanlators(listOf(scanlatorImpl!!)).executeOnIO()
                    }
                    scanlatorImpl
                }
                .map { it.uuid }
        return mangaDex
            .feedUpdates(page, blockedScanlatorUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(mangaListPage.hasNextPage to displayMangaList.toImmutableList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getLatestChapterPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        val blockedScanlatorUUIDs =
            preferenceHelper
                .blockedScanlators()
                .get()
                .mapNotNull {
                    var scanlatorImpl = db.getScanlatorByName(it).executeAsBlocking()
                    if (scanlatorImpl == null) {
                        mangaDex.getScanlator(scanlator = it).map { scanlator ->
                            scanlatorImpl = scanlator.toScanlatorImpl()
                        }
                        db.insertScanlators(listOf(scanlatorImpl!!)).executeOnIO()
                    }
                    scanlatorImpl
                }
                .map { it.uuid }
        return mangaDex
            .latestChapters(page, blockedScanlatorUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(mangaListPage.hasNextPage to displayMangaList.toImmutableList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getListPage(
        listUUID: String
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .fetchAllList(listUUID)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(false to displayMangaList.toImmutableList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getRecentlyAddedPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .recentlyAdded(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(listResults.hasNextPage to displayMangaList.toImmutableList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getPopularNewTitles(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .popularNewTitles(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(listResults.hasNextPage to displayMangaList.toImmutableList())
                },
                failure = { Err(it) },
            )
    }
}
