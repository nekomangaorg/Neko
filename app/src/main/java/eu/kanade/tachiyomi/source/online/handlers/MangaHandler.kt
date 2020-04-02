package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable

class MangaHandler(val client: OkHttpClient, val headers: Headers, val lang: String) {

    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            val parser = ApiMangaParser(lang)

            val jsonData = response.body!!.string()

            val detailsManga = parser.mangaDetailsParse(jsonData)
            detailsManga.apply { initialized = true }
            val chapterList = parser.chapterListParse(jsonData)
            Pair(detailsManga,
                    chapterList)
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            ApiMangaParser(lang).mangaDetailsParse(response).apply { initialized = true }
        }
    }

    fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        return client.newCall(apiRequest(manga))
                .asObservableSuccess()
                .map { response ->
                    ApiMangaParser(lang).mangaDetailsParse(response).apply { initialized = true }
                }
    }

    fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(apiRequest(manga))
                .asObservableSuccess()
                .map { response ->

                    ApiMangaParser(lang).chapterListParse(response)
                }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(apiRequest(manga)).execute()
            ApiMangaParser(lang).chapterListParse(response)
        }
    }

    fun fetchRandomMangaId(): Observable<String> {
        return client.newCall(randomMangaRequest())
                .asObservableSuccess()
                .map { response ->
                    ApiMangaParser(lang).randomMangaIdParse(response)
                }
    }

    private fun randomMangaRequest(): Request {
        return GET(MdUtil.baseUrl + MdUtil.randMangaPage)
    }

    private fun apiRequest(manga: SManga): Request {
        return GET(MdUtil.baseUrl + MdUtil.apiManga + MdUtil.getMangaId(manga.url), headers, CacheControl.FORCE_NETWORK)
    }

    companion object {
    }
}
