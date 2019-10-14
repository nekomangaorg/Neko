package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.setUrlWithoutDomain
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import rx.Observable

class SearchHandler(val client: OkHttpClient, private val headers: Headers, val lang: String) {

    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(PREFIX_ID_SEARCH)) {
            val realQuery = query.removePrefix(PREFIX_ID_SEARCH)
            client.newCall(searchMangaByIdRequest(realQuery))
                    .asObservableSuccess()
                    .map { response ->
                        val details = ApiMangaParser(lang).mangaDetailsParse(response)
                        details.url = "/manga/$realQuery/"
                        MangasPage(listOf(details), false)
                    }
        } else {
            client.newCall(searchMangaRequest(page, query, filters))
                    .asObservableSuccess()
                    .map { response ->
                        searchMangaParse(response)
                    }
        }
    }

    private fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector).map { element ->
            searchMangaFromElement(element)
        }

        val hasNextPage = searchMangaNextPageSelector.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    private fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {

        val genresToInclude = mutableListOf<String>()
        val genresToExclude = mutableListOf<String>()

        // Do traditional search
        val url = "${MdUtil.baseUrl}/?page=search".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("p", page.toString())
                .addQueryParameter("title", query.replace(WHITESPACE_REGEX, " "))

        filters.forEach { filter ->
            when (filter) {
                is FilterHandler.TextField -> url.addQueryParameter(filter.key, filter.state)
                is FilterHandler.Demographic -> {
                    if (filter.state != 0) {
                        url.addQueryParameter("demo_id", filter.state.toString())
                    }
                }
                is FilterHandler.PublicationStatus -> {
                    if (filter.state != 0) {
                        url.addQueryParameter("status_id", filter.state.toString())
                    }
                }
                is FilterHandler.OriginalLanguage -> {
                    if (filter.state != 0) {
                        val number: String = FilterHandler.sourceLang.first { it -> it.first == filter.values[filter.state] }.second
                        url.addQueryParameter("lang_id", number)
                    }
                }
                is FilterHandler.TagInclusionMode -> {
                    url.addQueryParameter("tag_mode_inc", arrayOf("all", "any")[filter.state])
                }
                is FilterHandler.TagExclusionMode -> {
                    url.addQueryParameter("tag_mode_exc", arrayOf("all", "any")[filter.state])
                }
                is FilterHandler.ContentList -> {
                    filter.state.forEach { content ->
                        if (content.isExcluded()) {
                            genresToExclude.add(content.id)
                        } else if (content.isIncluded()) {
                            genresToInclude.add(content.id)
                        }
                    }
                }
                is FilterHandler.FormatList -> {
                    filter.state.forEach { format ->
                        if (format.isExcluded()) {
                            genresToExclude.add(format.id)
                        } else if (format.isIncluded()) {
                            genresToInclude.add(format.id)
                        }
                    }
                }
                is FilterHandler.GenreList -> {
                    filter.state.forEach { genre ->
                        if (genre.isExcluded()) {
                            genresToExclude.add(genre.id)
                        } else if (genre.isIncluded()) {
                            genresToInclude.add(genre.id)
                        }
                    }
                }
                is FilterHandler.ThemeList -> {
                    filter.state.forEach { theme ->
                        if (theme.isExcluded()) {
                            genresToExclude.add(theme.id)
                        } else if (theme.isIncluded()) {
                            genresToInclude.add(theme.id)
                        }
                    }
                }
                is FilterHandler.SortFilter -> {
                    if (filter.state != null) {
                        if (filter.state!!.ascending) {
                            url.addQueryParameter("s", FilterHandler.sortables[filter.state!!.index].second.toString())
                        } else {
                            url.addQueryParameter("s", FilterHandler.sortables[filter.state!!.index].third.toString())
                        }
                    }
                }
            }
        }
        // Manually append genres list to avoid commas being encoded
        var urlToUse = url.toString()
        if (genresToInclude.isNotEmpty()) {
            urlToUse += "&tags_inc=" + genresToInclude.joinToString(",")
        }
        if (genresToExclude.isNotEmpty()) {
            urlToUse += "&tags_exc=" + genresToExclude.joinToString(",")
        }

        return GET(urlToUse, headers)
    }

    private fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        element.select("a.manga_title").first().let {
            val url = MdUtil.modifyMangaUrl(it.attr("href"))
            manga.setUrlWithoutDomain(url)
            manga.title = it.text().trim()
        }

        manga.thumbnail_url = MdUtil.formThumbUrl(manga.url)


        return manga
    }

    private fun searchMangaByIdRequest(id: String): Request {
        return GET(MdUtil.baseUrl + MdUtil.apiManga + id, headers)
    }

    companion object {
        const val PREFIX_ID_SEARCH = "id:"
        val WHITESPACE_REGEX = "\\s".toRegex()
        const val searchMangaNextPageSelector = ".pagination li:not(.disabled) span[title*=last page]:not(disabled)"
        const val searchMangaSelector = "div.manga-entry"
    }
}