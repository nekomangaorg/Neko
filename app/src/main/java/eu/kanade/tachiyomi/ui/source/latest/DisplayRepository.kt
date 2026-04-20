package eu.kanade.tachiyomi.ui.source.latest

import androidx.compose.ui.state.ToggleableState
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.getBlockedScanlatorGroupUUIDs
import eu.kanade.tachiyomi.source.online.utils.getBlockedUploaderUUIDs
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.toDisplayResult
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val db: DatabaseHelper = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {

    suspend fun getPage(
        page: Int,
        displayScreenType: DisplayScreenType,
    ): Result<DisplayPageResult, ResultError> {
        return when (displayScreenType) {
            is DisplayScreenType.LatestChapters -> getLatestChapterPage(page)
            is DisplayScreenType.List -> getListPage(displayScreenType.listUUID)
            is DisplayScreenType.RecentlyAdded -> getRecentlyAddedPage(page)
            is DisplayScreenType.PopularNewTitles -> getPopularNewTitles(page)
            is DisplayScreenType.FeedUpdates -> getFeedUpdatesPage(page)
            is DisplayScreenType.Tag -> getTagPage(displayScreenType.title.str, page)
            is DisplayScreenType.AuthorByName -> getAuthorPage(displayScreenType.title.str)
            is DisplayScreenType.AuthorWithUuid ->
                getAuthorManga(page, displayScreenType.authorUUID)
            is DisplayScreenType.GroupByName -> getGroupPage(displayScreenType.title.str)
            is DisplayScreenType.GroupByUuid -> getGroupManga(page, displayScreenType.groupUUID)
        }
    }

    private fun createContentRatingFilter(): DexFilters {
        val enabledContentRatings = mangaDexPreferences.visibleContentRatings().get()
        val contentRatings =
            MangaContentRating.getOrdered()
                .map { Filter.ContentRating(it, enabledContentRatings.contains(it.key)) }
                .toPersistentList()

        return DexFilters(
            contentRatings = contentRatings,
            contentRatingVisible = mangaDexPreferences.showContentRatingFilter().get(),
        )
    }

    private suspend fun getAuthorPage(
        authorSearch: String
    ): Result<DisplayPageResult, ResultError> {
        return mangaDex.searchForAuthor(authorSearch).map { resultListPage ->
            val displayResult = resultListPage.results.map { it.toDisplayResult() }
            DisplayPageResult(displayResult = displayResult.toPersistentList())
        }
    }

    private suspend fun getAuthorManga(
        page: Int,
        authorUUID: String,
    ): Result<DisplayPageResult, ResultError> {

        return mangaDex
            .search(page, createContentRatingFilter().copy(authorId = Filter.AuthorId(authorUUID)))
            .map { mangaListPage ->
                val displayMangaList =
                    mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                DisplayPageResult(
                    hasNextPage = mangaListPage.hasNextPage,
                    displayManga = displayMangaList.toPersistentList(),
                )
            }
    }

    private suspend fun getGroupPage(groupName: String): Result<DisplayPageResult, ResultError> {
        return mangaDex.searchForGroup(groupName).map { resultListPage ->
            val displayResult = resultListPage.results.map { it.toDisplayResult() }
            DisplayPageResult(displayResult = displayResult.toPersistentList())
        }
    }

    private suspend fun getGroupManga(
        page: Int,
        groupUUID: String,
    ): Result<DisplayPageResult, ResultError> {
        return mangaDex
            .search(page, createContentRatingFilter().copy(groupId = Filter.GroupId(groupUUID)))
            .map { mangaListPage ->
                val displayMangaList =
                    mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                DisplayPageResult(
                    hasNextPage = mangaListPage.hasNextPage,
                    displayManga = displayMangaList.toPersistentList(),
                )
            }
    }

    private suspend fun getFeedUpdatesPage(page: Int): Result<DisplayPageResult, ResultError> {
        val blockedGroupUUIDs = getBlockedScanlatorGroupUUIDs(mangaDexPreferences, db, mangaDex)
        val blockedUploaderUUIDs = getBlockedUploaderUUIDs(mangaDexPreferences, db, mangaDex)

        return mangaDex
            .feedUpdates(page, blockedGroupUUIDs, blockedUploaderUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(
                        DisplayPageResult(
                            hasNextPage = mangaListPage.hasNextPage,
                            displayManga = displayMangaList.toPersistentList(),
                        )
                    )
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getLatestChapterPage(page: Int): Result<DisplayPageResult, ResultError> {
        val blockedGroupUUIDs = getBlockedScanlatorGroupUUIDs(mangaDexPreferences, db, mangaDex)
        val blockedUploaderUUIDs = getBlockedUploaderUUIDs(mangaDexPreferences, db, mangaDex)

        return mangaDex
            .latestChapters(page, blockedGroupUUIDs, blockedUploaderUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(
                        DisplayPageResult(
                            hasNextPage = mangaListPage.hasNextPage,
                            displayManga = displayMangaList.toPersistentList(),
                        )
                    )
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getListPage(listUUID: String): Result<DisplayPageResult, ResultError> {
        return mangaDex
            .fetchAllList(listUUID)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(DisplayPageResult(displayManga = displayMangaList.toPersistentList()))
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getRecentlyAddedPage(page: Int): Result<DisplayPageResult, ResultError> {
        return mangaDex
            .recentlyAdded(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(
                        DisplayPageResult(
                            hasNextPage = listResults.hasNextPage,
                            displayManga = displayMangaList.toPersistentList(),
                        )
                    )
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getTagPage(tag: String, page: Int): Result<DisplayPageResult, ResultError> {

        val enabledContentRatings = mangaDexPreferences.visibleContentRatings().get()
        val contentRatings =
            MangaContentRating.getOrdered()
                .map { Filter.ContentRating(it, enabledContentRatings.contains(it.key)) }
                .toPersistentList()

        val blankFilter = DexFilters(contentRatings = contentRatings)

        val filters =
            if (tag.startsWith("Content rating: ")) {
                val rating =
                    MangaContentRating.getContentRating(tag.substringAfter("Content rating: "))
                blankFilter.copy(
                    contentRatings =
                        blankFilter.contentRatings
                            .map {
                                if (it.rating == rating) it.copy(state = true)
                                else it.copy(state = false)
                            }
                            .toPersistentList()
                )
            } else {
                blankFilter.copy(
                    tags =
                        blankFilter.tags
                            .map {
                                if (it.tag.prettyPrint.equals(tag, true))
                                    it.copy(state = ToggleableState.On)
                                else it
                            }
                            .toPersistentList()
                )
            }

        return mangaDex
            .search(page, filters)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(
                        DisplayPageResult(
                            hasNextPage = mangaListPage.hasNextPage,
                            displayManga = displayMangaList.toPersistentList(),
                        )
                    )
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getPopularNewTitles(page: Int): Result<DisplayPageResult, ResultError> {
        return mangaDex
            .popularNewTitles(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
                    Ok(
                        DisplayPageResult(
                            hasNextPage = listResults.hasNextPage,
                            displayManga = displayMangaList.toPersistentList(),
                        )
                    )
                },
                failure = { Err(it) },
            )
    }
}
