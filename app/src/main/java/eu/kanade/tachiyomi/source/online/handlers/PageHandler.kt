package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.CacheControl
import okhttp3.Request
import rx.Observable
import uy.kohesive.injekt.injectLazy

class PageHandler {

    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        if (chapter.scanlator.equals("MangaPlus")) {
            return network.client.newCall(pageListRequest(chapter))
                .asObservableSuccess()
                .map { response ->
                    val chapterId = ApiChapterParser().externalParse(response)
                    MangaPlusHandler(network.client).fetchPageList(chapterId)
                }
        }

        val atHomeRequestUrl = if (preferences.usePort443Only()) {
            "${MdUtil.atHomeUrl}/${chapter.mangadex_chapter_id}?forcePort443=true"
        } else {
            "${MdUtil.atHomeUrl}/${chapter.mangadex_chapter_id}"
        }

        return network.client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                val host = MdUtil.atHomeUrlHostUrl(atHomeRequestUrl, network.client, CacheControl.FORCE_NETWORK)
                ApiChapterParser().pageListParse(response, host, preferences.dataSaver())
            }
    }

    private fun pageListRequest(chapter: SChapter): Request {
        return GET("${MdUtil.chapterUrl}${chapter.mangadex_chapter_id}", network.headers, CacheControl.FORCE_NETWORK)
    }
}
