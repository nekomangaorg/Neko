package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.isUUID
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.throws
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SearchHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val filterHandler: FilterHandler by injectLazy()
    private val apiMangaParser: ApiMangaParser by injectLazy()
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun searchForManga(mangaUUID: String): Result<MangaListPage, ResultError> {
        return service.viewManga(mangaUUID)
            .getOrResultError("trying to view manga with UUID $mangaUUID")
            .andThen { mangaDto ->
                val sourceManga = persistentListOf(mangaDto.data.toSourceManga(preferencesHelper.thumbnailQuality(), false))
                Ok(MangaListPage(sourceManga = sourceManga, hasNextPage = false))
            }
    }

    suspend fun search2(page: Int, filters: DexFilters): Result<MangaListPage, ResultError> {
        return withContext(Dispatchers.IO) {
            val queryParameters = mutableMapOf<String, Any>()

            queryParameters["limit"] = MdUtil.mangaLimit.toString()
            queryParameters["offset"] = (MdUtil.getMangaListOffset(page))
            val actualQuery = filters.titleQuery.query.replace(WHITESPACE_REGEX, " ")
            if (actualQuery.isNotBlank()) {
                queryParameters[MdConstants.SearchParameters.titleParam] = actualQuery
            }

            val contentRating = filters.contentRatings.filter { it.state }.map { it.rating.key }

            if (contentRating.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.contentRatingParam] = contentRating
            }

            val originalLanguage = filters.originalLanguage.filter { it.state }.map { it.language.lang }
            if (originalLanguage.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.originalLanguageParam] = originalLanguage
            }

            //val additionalQueries = filterHandler.getQueryMap(filters)
            // queryParameters.putAll(additionalQueries)

            service.search(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("Trying to search")
                .andThen { response ->
                    searchMangaParse2(response)
                }
        }
    }

    suspend fun recentlyAdded(page: Int): Result<MangaListPage, ResultError> {
        return withContext(Dispatchers.IO) {
            val queryParameters = mutableMapOf<String, Any>()
            queryParameters["limit"] = MdUtil.mangaLimit.toString()
            queryParameters["offset"] = MdUtil.getMangaListOffset(page)
            val contentRatings = preferencesHelper.contentRatingSelections().toList()
            if (contentRatings.isNotEmpty()) {
                queryParameters["contentRating[]"] = contentRatings
            }
            val thumbQuality = preferencesHelper.thumbnailQuality()
            service.getRecentlyAdded(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("Error getting recently added")
                .andThen { mangaListDto ->
                    val hasMoreResults = mangaListDto.limit + mangaListDto.offset < mangaListDto.total

                    Ok(
                        MangaListPage(hasNextPage = hasMoreResults, sourceManga = mangaListDto.data.map { it.toSourceManga(thumbQuality) }.toImmutableList()),
                    )
                }
        }
    }

    suspend fun search(page: Int, query: String, filters: FilterList): MangaListPage {
        return withContext(Dispatchers.IO) {
            if (query.startsWith(MdUtil.PREFIX_ID_SEARCH)) {
                val realQuery = query.removePrefix(MdUtil.PREFIX_ID_SEARCH)
                val response = service.viewManga(realQuery)
                    .onFailure {
                        val type = "trying to view manga $realQuery"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()

                val details = apiMangaParser.mangaDetailsParse(response.data)
                MangaListPage(listOf(details), false)
            } else {
                val queryParameters = mutableMapOf<String, Any>()

                queryParameters["limit"] = MdUtil.mangaLimit.toString()
                queryParameters["offset"] = (MdUtil.getMangaListOffset(page))
                if (query.startsWith(MdUtil.PREFIX_GROUP_ID_SEARCH)) {
                    val groupId = query.removePrefix(MdUtil.PREFIX_GROUP_ID_SEARCH)
                    if (groupId.isUUID().not()) {
                        throw Exception("Invalid Group ID must be UUID")
                    }
                    queryParameters["group"] = groupId
                } else {
                    val actualQuery = query.replace(WHITESPACE_REGEX, " ")
                    if (actualQuery.isNotBlank()) {
                        queryParameters["title"] = actualQuery
                    }
                }

                val additionalQueries = filterHandler.getQueryMap(filters)
                queryParameters.putAll(additionalQueries)

                val response = service.search(ProxyRetrofitQueryMap(queryParameters))
                    .onFailure {
                        val type = "trying to search"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()

                searchMangaParse(response)
            }
        }
    }

    private fun searchMangaParse2(mangaListDto: MangaListDto): Result<MangaListPage, ResultError> {
        return com.github.michaelbull.result.runCatching {
            val hasMoreResults = mangaListDto.limit + mangaListDto.offset < mangaListDto.total
            val thumbQuality = preferencesHelper.thumbnailQuality()
            val mangaList = mangaListDto.data.map { it.toSourceManga(thumbQuality) }.toImmutableList()
            MangaListPage(hasNextPage = hasMoreResults, sourceManga = mangaList)
        }.mapError {
            XLog.e("error parsing search manga", it)
            "error parsing search manga".toResultError()
        }
    }

    private fun searchMangaParse(mangaListDto: MangaListDto): MangaListPage {
        val hasMoreResults = mangaListDto.limit + mangaListDto.offset < mangaListDto.total

        val thumbQuality = preferencesHelper.thumbnailQuality()

        val mangaList = mangaListDto.data.map {
            it.toBasicManga(thumbQuality)
        }

        return MangaListPage(mangaList, hasMoreResults)
    }

    companion object {
        val WHITESPACE_REGEX = "\\s".toRegex()
    }
}
