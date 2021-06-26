package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SearchHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val filterHandler: FilterHandler by injectLazy()
    private val apiMangaParser: ApiMangaParser by injectLazy()

    suspend fun search(page: Int, query: String, filters: FilterList): MangaListPage {
        return withContext(Dispatchers.IO) {
            if (query.startsWith(PREFIX_ID_SEARCH)) {
                val realQuery = query.removePrefix(PREFIX_ID_SEARCH)
                val response = service.viewManga(realQuery)
                val details = apiMangaParser.mangaDetailsParse(response.body()!!)
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

                searchMangaParse(response)
            }
        }
    }

    private fun searchMangaParse(response: Response<MangaListDto>): MangaListPage {
        if (response.isSuccessful.not()) {
            throw Exception("Error getting search manga http code: ${response.code()}")
        }

        val mangaListDto = response.body()!!

        val hasMoreResults = mangaListDto.limit + mangaListDto.offset < mangaListDto.total

        val mangaList = mangaListDto.results.map {
            it.toBasicManga()
        }

        return MangaListPage(mangaList, hasMoreResults)
    }

    companion object {
        const val PREFIX_ID_SEARCH = "id:"
        val WHITESPACE_REGEX = "\\s".toRegex()
    }
}
