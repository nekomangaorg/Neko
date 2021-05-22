package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.handlers.serializers.CoverListResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaListResponse
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.v5.db.V5DbHelper
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
    private val preferences: PreferencesHelper by injectLazy()
    private val network: NetworkHelper by injectLazy()
    private val v5DbHelper: V5DbHelper by injectLazy()

    fun fetchPopularManga(page: Int): Observable<MangasPage> {
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

    private fun popularMangaParse(response: Response): MangasPage {
        if (response.isSuccessful.not()) {
            throw Exception("Error getting search manga http code: ${response.code}")
        }
        if (response.code == 204) {
            return MangasPage(emptyList(), false)
        }

        val mlResponse = MdUtil.jsonParser.decodeFromString<MangaListResponse>(response.body!!.string())
        val hasMoreResults = mlResponse.limit + mlResponse.offset < mlResponse.total
        val mangaList = mlResponse.results.map {
            var coverUrl = MdUtil.coverApi.replace("{uuid}", it.data.id)
            val coverUrlId = it.relationships.firstOrNull { it.type == "cover_art" }?.id
            if (coverUrlId != null) {
                runCatching {
                    val response = network.client.newCall(GET(MdUtil.coverUrl(it.data.id, coverUrlId))).execute()
                    val json = MdUtil.jsonParser.decodeFromString<CoverListResponse>(response.body!!.string())
                    json.results.firstOrNull()?.data?.attributes?.fileName?.let { fileName ->
                        coverUrl = "${MdUtil.cdnUrl}/covers/${it.data.id}/$fileName"
                    }
                }
            }
            MdUtil.createMangaEntry(it, coverUrl)
        }
        return MangasPage(mangaList, hasMoreResults)
    }
}
