package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.ApiCovers
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable

class MangaHandler(val client: OkHttpClient, val headers: Headers, val langs: List<String>, val useNewApiServer: Boolean, val forceLatestCovers: Boolean = false) {

    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            val covers = getCovers(manga, forceLatestCovers)
            val parser = ApiMangaParser(langs)

            val jsonData = response.body!!.string()
            if (response.code != 200) {
                XLog.e("error from MangaDex with response code ${response.code} \n body: \n$jsonData")
                throw Exception("Error from MangaDex Response code ${response.code} ")
            }

            val detailsManga = parser.mangaDetailsParse(jsonData, covers)
            manga.copyFrom(detailsManga)
            val chapterList = parser.chapterListParse(jsonData)
            Pair(
                manga,
                chapterList
            )
        }
    }

    suspend fun getCovers(manga: SManga, forceLatestCovers: Boolean): List<String> {
        if (forceLatestCovers) {
            val response = client.newCall(coverRequest(manga)).execute()
            val covers = MdUtil.jsonParser.decodeFromString(ApiCovers.serializer(), response.body!!.string())
            return covers.data.map { it.url }
        } else {
            return emptyList<String>()
        }
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): Int {
        return withContext(Dispatchers.IO) {
            val request = GET(MdUtil.apiUrl(useNewApiServer) + MdUtil.newApiChapter + urlChapterId + MdUtil.apiChapterSuffix, headers, CacheControl.FORCE_NETWORK)
            val response = client.newCall(request).execute()
            ApiMangaParser(langs).chapterParseForMangaId(response)
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            val covers = getCovers(manga, forceLatestCovers)
            ApiMangaParser(langs).mangaDetailsParse(response, covers).apply { initialized = true }
        }
    }

    fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {

        return client.newCall(apiRequest(manga))
            .asObservableSuccess()
            .map { response ->
                ApiMangaParser(langs).mangaDetailsParse(response, emptyList()).apply { initialized = true }
            }
    }

    fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(apiRequest(manga))
            .asObservableSuccess()
            .map { response ->

                ApiMangaParser(langs).chapterListParse(response)
            }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            ApiMangaParser(langs).chapterListParse(response)
        }
    }

    fun fetchRandomMangaId(): Observable<String> {
        return client.newCall(randomMangaRequest())
            .asObservableSuccess()
            .map { response ->
                ApiMangaParser(langs).randomMangaIdParse(response)
            }
    }

    private fun randomMangaRequest(): Request {
        return GET(MdUtil.baseUrl + MdUtil.randMangaPage)
    }

    private fun apiRequest(manga: SManga): Request {
        return GET(MdUtil.apiUrl(useNewApiServer) + MdUtil.apiManga + MdUtil.getMangaId(manga.url) + MdUtil.includeChapters, headers, CacheControl.FORCE_NETWORK)
    }

    private fun coverRequest(manga: SManga): Request {
        return GET(MdUtil.apiUrl(useNewApiServer) + MdUtil.apiManga + MdUtil.getMangaId(manga.url) + MdUtil.apiCovers, headers, CacheControl.FORCE_NETWORK)
    }

    companion object {
    }
}
