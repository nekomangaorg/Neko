package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaListResponse
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * Returns the latest manga from the updates url since it actually respects the users settings
 */
class PopularHandler(val client: OkHttpClient, private val headers: Headers) {

    private val preferences: PreferencesHelper by injectLazy()

    fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return client.newCall(popularMangaRequest(page))
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

        return GET(tempUrl.build().toString(), headers, CacheControl.FORCE_NETWORK)
    }

    private fun popularMangaParse(response: Response): MangasPage {
        if (response.isSuccessful.not()) {
            throw Exception("Error getting search manga http code: ${response.code}")
        }

        val mlResponse = MdUtil.jsonParser.decodeFromString(MangaListResponse.serializer(), response.body!!.string())
        val hasMoreResults = mlResponse.limit + mlResponse.offset < mlResponse.total
        val mangaList = mlResponse.results.map { MdUtil.createMangaEntry(it, preferences) }
        return MangasPage(mangaList, hasMoreResults)
    }
}
