package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LatestChapterHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    private val authService: MangaDexAuthorizedUserService by lazy { Injekt.get<NetworkServices>().authService }

    private val preferencesHelper: PreferencesHelper by injectLazy()

    private val uniqueManga = mutableSetOf<String>()

    suspend fun getPage(
        page: Int = 1,
        blockedScanlatorUUIDs: List<String>,
        limit: Int = MdConstants.Limits.latest,
        feedType: MdConstants.FeedType = MdConstants.FeedType.Latest,
    ): Result<MangaListPage, ResultError> {
        if (page == 1) uniqueManga.clear()
        return withContext(Dispatchers.IO) {
            val offset = MdUtil.getLatestChapterListOffset(page)

            val langs = MdUtil.getLangsToShow(preferencesHelper)

            val contentRatings = preferencesHelper.contentRatingSelections().get().toList()

            return@withContext when (feedType) {
                MdConstants.FeedType.Latest -> service.latestChapters(limit, offset, langs, contentRatings, blockedScanlatorUUIDs)
                    .getOrResultError("getting latest chapters")

                MdConstants.FeedType.Subscription -> authService.subscriptionFeed(limit, offset, langs, contentRatings, blockedScanlatorUUIDs)
                    .getOrResultError("getting subscription feed")
            }.andThen {
                latestChapterParse(it)
            }
        }
    }

    private suspend fun latestChapterParse(
        chapterListDto: ChapterListDto
    ): Result<MangaListPage, ResultError> {
        return runCatching {
                val result =
                    chapterListDto.data
                        .groupBy { chapterListDto ->
                            chapterListDto.relationships
                                .first { relationshipDto ->
                                    relationshipDto.type == MdConstants.Types.manga
                                }
                                .id
                        }
                        .filterNot { uniqueManga.contains(it.key) }

                val mangaIds = result.keys.toList()

                uniqueManga.addAll(mangaIds)

                val allContentRating =
                    listOf(
                        MdConstants.ContentRating.safe,
                        MdConstants.ContentRating.suggestive,
                        MdConstants.ContentRating.erotica,
                        MdConstants.ContentRating.pornographic,
                    )

                val queryParameters =
                    mutableMapOf(
                        "ids[]" to mangaIds,
                        "limit" to mangaIds.size,
                        "contentRating[]" to allContentRating,
                    )

                service
                    .search(ProxyRetrofitQueryMap(queryParameters))
                    .getOrResultError("trying to search manga from latest chapters")
                    .andThen { mangaListDto ->
                        val hasMoreResults =
                            chapterListDto.limit + chapterListDto.offset < chapterListDto.total

                        val mangaDtoMap = mangaListDto.data.associateBy({ it.id }, { it })

                        val thumbQuality = preferencesHelper.thumbnailQuality().get()
                        val mangaList =
                            mangaIds
                                .mapNotNull { mangaDtoMap[it] }
                                .sortedByDescending {
                                    result[it.id]!!.first().attributes.readableAt
                                }
                                .map {
                                    val chapterName =
                                        result[it.id]?.firstOrNull()?.buildChapterName() ?: ""
                                    it.toSourceManga(
                                        coverQuality = thumbQuality, displayText = chapterName)
                                }

                        Ok(
                            MangaListPage(
                                sourceManga = mangaList.toPersistentList(),
                                hasNextPage = hasMoreResults))
                    }
            }
            .getOrElse { e ->
                if (e !is CancellationException) {
                    TimberKt.e(e) { "Error parsing latest chapters" }
                }
                Err(ResultError.Generic(errorString = "Error parsing latest chapters response"))
            }
    }
}
