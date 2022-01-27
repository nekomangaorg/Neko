package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SearchHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val filterHandler: FilterHandler by injectLazy()
    private val apiMangaParser: ApiMangaParser by injectLazy()
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun search(page: Int, query: String, filters: FilterList): MangaListPage {
        return withContext(Dispatchers.IO) {
            if (query.startsWith(PREFIX_ID_SEARCH)) {
                val realQuery = query.removePrefix(PREFIX_ID_SEARCH)
                val response = service.viewManga(realQuery)
                    .onError {
                        val type = "trying to view manga $realQuery"
                        this.log(type)
                        this.throws(type)
                    }.onException {
                        val type = "trying to view manga $realQuery"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()

                val details = apiMangaParser.mangaDetailsParse(response.data)
                MangaListPage(listOf(details), false)
            } else {
                val queryParamters = mutableMapOf<String, Any>()

                queryParamters["limit"] = MdUtil.mangaLimit.toString()
                queryParamters["offset"] = (MdUtil.getMangaListOffset(page))
                val actualQuery = query.replace(WHITESPACE_REGEX, " ")

                if (actualQuery.isNotBlank()) {
                    queryParamters["title"] = actualQuery
                }
                val additionalQueries = filterHandler.getQueryMap(filters)
                queryParamters.putAll(additionalQueries)

                val response = service.search(ProxyRetrofitQueryMap(queryParamters))
                    .onError {
                        val type = "trying to search"
                        this.log(type)
                        this.throws(type)
                    }.onException {
                        val type = "trying to search"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()

                searchMangaParse(response)
            }
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
        const val PREFIX_ID_SEARCH = "id:"
        val WHITESPACE_REGEX = "\\s".toRegex()
    }
}
