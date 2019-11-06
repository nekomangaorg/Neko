package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable

class MangaHandler(val client: OkHttpClient, val headers: Headers, val lang: String) {

    fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(apiRequest(manga))
                .asObservableSuccess()
                .map { response ->
                    ApiMangaParser(lang).mangaDetailsParse(response).apply { initialized = true }
                }
    }

    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(apiRequest(manga))
                .asObservableSuccess()
                .map { response ->

                    ApiMangaParser(lang).chapterListParse(response)
                }
    }

    fun fetchRandomMangaId() : Observable<String>{
        return client.newCall(randomMangaRequest())
                .asObservableSuccess()
                .map {response ->
                    ApiMangaParser(lang).randomMangaIdParse(response)
                }
    }

    private fun randomMangaRequest(): Request{
        return GET(MdUtil.baseUrl + MdUtil.randMangaPage)
    }

    private fun apiRequest(manga: SManga): Request {
        return GET(MdUtil.baseUrl + MdUtil.apiManga + MdUtil.getMangaId(manga.url), headers)
    }

    companion object {

    }
}