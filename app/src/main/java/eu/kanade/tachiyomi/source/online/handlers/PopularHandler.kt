package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.setUrlWithoutDomain
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import rx.Observable

class PopularHandler(val client: OkHttpClient, private val headers: Headers) {


    fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return client.newCall(popularMangaRequest(page))
                .asObservableSuccess()
                .map { response ->
                    popularMangaParse(response)
                }
    }

    private fun popularMangaRequest(page: Int): Request {
        return GET("${MdUtil.baseUrl}/titles/0/$page/", headers)
    }

    private fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector).map { element ->
            popularMangaFromElement(element)
        }.distinct()

        val hasNextPage = popularMangaNextPageSelector.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    private fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a.manga_title").first().let {
            val url = MdUtil.modifyMangaUrl(it.attr("href"))
            manga.setUrlWithoutDomain(url)
            manga.title = it.text().trim()
        }
        manga.thumbnail_url = MdUtil.formThumbUrl(manga.url)

        return manga
    }

    companion object {
        const val popularMangaSelector = "div.manga-entry"
        const val popularMangaNextPageSelector = ".pagination li:not(.disabled) span[title*=last page]:not(disabled)"

    }
}