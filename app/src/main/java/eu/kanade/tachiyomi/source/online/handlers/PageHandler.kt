package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable

class PageHandler(val client: OkHttpClient, private val imageServer: String) {

    fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
                .asObservableSuccess()
                .map { response ->
                    ApiChapterParser().pageListParse(response)
                }
    }

    private fun pageListRequest(chapter: SChapter): Request {
        return GET("${MdUtil.baseUrl}${chapter.url}?server=$imageServer")
    }

}