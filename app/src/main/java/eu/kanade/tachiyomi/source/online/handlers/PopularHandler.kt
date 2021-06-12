package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.serialization.decodeFromString
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * Returns the latest manga from the updates url since it actually respects the users settings
 */
class PopularHandler {

    private val filterHandler: FilterHandler by injectLazy()
    private val network: NetworkHelper by injectLazy()

    fun fetchPopularManga(page: Int): Observable<MangaListPage> {
        return network.client.newCall(popularMangaRequest(page))
            .asObservableSuccess()
            .map { response ->
                popularMangaParse(response)
            }
    }

    private fun popularMangaRequest(page: Int): Request {
        val tempUrl = MdUtil.mangaUrl.toHttpUrlOrNull()!!.newBuilder()

        tempUrl.apply {
            addQueryParameter("limit", MdUtil.mangaLimit.toString())
            addQueryParameter("offset", (MdUtil.getMangaListOffset(page)))
        }
        val finalUrl = filterHandler.addFiltersToUrl(tempUrl, filterHandler.getMDFilterList())

        return GET(finalUrl, network.headers, CacheControl.FORCE_NETWORK)
    }

    private fun popularMangaParse(response: Response): MangaListPage {
        if (response.isSuccessful.not()) {
            throw Exception("Error getting search manga http code: ${response.code}")
        }
        if (response.code == 204) {
            return MangaListPage(emptyList(), false)
        }

        val mlResponse =
            MdUtil.jsonParser.decodeFromString<MangaListDto>(response.body!!.string())
        val hasMoreResults = mlResponse.limit + mlResponse.offset < mlResponse.total

        val coverMap = MdUtil.getCoversFromMangaList(mlResponse.results, network.client)

        val mangaList = mlResponse.results.map {
            val coverUrl = coverMap[it.data.id]
            MdUtil.createMangaEntry(it, coverUrl)
        }
        return MangaListPage(mangaList, hasMoreResults)
    }
}
