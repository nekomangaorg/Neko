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
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import okhttp3.internal.toImmutableList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    suspend fun getPage(page: Int, displayScreenType: DisplayScreenType): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return when (displayScreenType) {
            is DisplayScreenType.LatestChapters -> getLatestChapterPage(page)
            is DisplayScreenType.SubscriptionFeed -> getLatestChapterPage(page, feedType = MdConstants.FeedType.Subscription)
            is DisplayScreenType.List -> getListPage(displayScreenType.listUUID, displayScreenType.privateList)
            is DisplayScreenType.RecentlyAdded -> getRecentlyAddedPage(page)
            is DisplayScreenType.PopularNewTitles -> getPopularNewTitles(page)
        }
    }

    private suspend fun getLatestChapterPage(page: Int, feedType: MdConstants.FeedType = MdConstants.FeedType.Latest): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        val blockedScanlatorUUIDs = preferenceHelper.blockedScanlators().get().mapNotNull {
            var scanlatorImpl = db.getScanlatorByName(it).executeAsBlocking()
            if (scanlatorImpl == null) {
                mangaDex.getScanlator(scanlator = it).map { scanlator -> scanlatorImpl = scanlator.toScanlatorImpl() }
                db.insertScanlators(listOf(scanlatorImpl!!)).executeOnIO()
            }
            scanlatorImpl
        }.map {
            it.uuid
        }

        return mangaDex.latestChapters(page, blockedScanlatorUUIDs, feedType).mapBoth(
            success = { mangaListPage ->
                val displayMangaList = mangaListPage.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(mangaListPage.hasNextPage to displayMangaList.toImmutableList())
            },
            failure = { Err(it) },
        )
    }

    private suspend fun getListPage(listUUID: String, privateList: Boolean): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.fetchList(listUUID, privateList).mapBoth(
            success = { listResults ->
                val displayMangaList = listResults.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(false to displayMangaList.toImmutableList())
            },
            failure = { Err(it) },
        )
    }

    private suspend fun getRecentlyAddedPage(page: Int): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.recentlyAdded(page).mapBoth(
            success = { listResults ->
                val displayMangaList = listResults.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(listResults.hasNextPage to displayMangaList.toImmutableList())
            },
            failure = { Err(it) },
        )
    }

    private suspend fun getPopularNewTitles(page: Int): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.popularNewTitles(page).mapBoth(
            success = { listResults ->
                val displayMangaList = listResults.sourceManga.map { sourceManga ->
                    sourceManga.toDisplayManga(db, mangaDex.id)
                }
                Ok(listResults.hasNextPage to displayMangaList.toImmutableList())
            },
            failure = { Err(it) },
        )
    }
}
