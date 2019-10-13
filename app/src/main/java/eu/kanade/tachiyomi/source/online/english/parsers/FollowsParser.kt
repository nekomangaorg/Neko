package eu.kanade.tachiyomi.source.online.english.parsers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.english.utils.MdUtil
import eu.kanade.tachiyomi.source.online.english.utils.MdUtil.Companion.getMangaId
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.setUrlWithoutDomain
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import rx.Observable

class FollowsParser(val client: OkHttpClient, val baseUrl: String, val headers: Headers) {

    fun fetchFollows(page: Int): Observable<MangasPage> {
        return client.newCall(followsListRequest(page))
                .asObservable()
                .map { response ->
                    followsParse(response)
                }
    }

    private fun followsParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val follows = document
                .select(followSelector)
                .map { followFromElement(it) }

        val hasNextPage = document
                .select(followsNextPageSelector)
                .isNotEmpty()

        val estimatedTotalFollows = document
                .select(estimatedTotalFollowsSelector)
                .text()
                .split(' ')
                .last { it.toIntOrNull() is Int } // TODO: Try to deduplicate toInt()
                .toInt()

        return MangasPage(follows, hasNextPage, estimatedTotalFollows)
    }

    protected fun followsListRequest(page: Int): Request {

        // Format `/follows/manga/$status[/$sort[/$page]]`
        val url = "$baseUrl/follows/manga/0".toHttpUrlOrNull()!!.newBuilder() // Gets regardless of follow status.
                .addQueryParameter("p", page.toString())

        return GET(url.toString(), headers)
    }

    private fun followFromElement(element: Element): SManga {
        val manga = SManga.create()

        element.select("a.manga_title").first().let {
            val url = MdUtil.modifyMangaUrl(it.attr("href"))
            manga.setUrlWithoutDomain(url)
            manga.title = it.text().trim()
        }

        manga.thumbnail_url = MdUtil.formThumbUrl(manga.url)

        manga.follow_status = getFollowStatusFromElement(element).let { SManga.FollowStatus.fromMangadex(it!!) }

        return manga
    }

    private fun getFollowStatusFromElement(element: Element): String? {
        var dropdownElement = element.select("button.btn.btn-success.dropdown-toggle").first()
                ?: element.select("button.btn.btn-warning.dropdown-toggle").first()
                ?: element.select("button.btn.btn-danger.dropdown-toggle").first()
                ?: element.select("button.btn.btn-info.dropdown-toggle").first()
                ?: element.select("button.btn.btn-secondary.dropdown-toggle").first()
                ?: element.select("button.btn.btn-primary.dropdown-toggle").first() //This should be last because it can match the Rating dropdown. The Follow Status dropdown should be what will be returned by `first()` since it should appear earlier in the HTML

        if (dropdownElement.select("span.fa-star").first() != null) { // Well, FUCK
            //Contingency. May not work if the earlier code didn't
            dropdownElement = element.select("button.btn.btn-success.dropdown-toggle:has(span.fas.fa-fw:not(.fa-star))").first()
        }

        return dropdownElement?.text()?.trim()
    }


    fun changeFollowStatus(manga: SManga): Observable<Boolean> {
        manga.follow_status ?: throw IllegalArgumentException("Cannot tell MD server to set an null follow status")

        val mangaID = getMangaId(manga.url)
        val status = manga.follow_status!!.toMangadexInt()

        return client.newCall(GET("$baseUrl/ajax/actions.ajax.php?function=manga_follow&id=$mangaID&type=$status", headers))
                .asObservable()
                .map { it.body!!.string().isEmpty() }
    }


    companion object {
        val followSelector = "div.manga-entry"
        val estimatedTotalFollowsSelector = "div.manga-entry:last-of-type + *" // The element immediately following the last follow entry
        val followsNextPageSelector = ".pagination li:not(.disabled) span[title*=last page]:not(disabled)"
        private val FOLLOW_STATUS_LIST = listOf(
                Triple(0, SManga.FollowStatus.UNFOLLOWED, "Unfollowed"),
                Triple(1, SManga.FollowStatus.READING, "Reading"),
                Triple(2, SManga.FollowStatus.COMPLETED, "Completed"),
                Triple(3, SManga.FollowStatus.ON_HOLD, "On hold"),
                Triple(4, SManga.FollowStatus.PLAN_TO_READ, "Plan to read"),
                Triple(5, SManga.FollowStatus.DROPPED, "Dropped"),
                Triple(6, SManga.FollowStatus.RE_READING, "Re-reading"))

        fun SManga.FollowStatus.Companion.fromMangadex(x: Int) = FOLLOW_STATUS_LIST.first { it.first == x }.second
        fun SManga.FollowStatus.Companion.fromMangadex(MangadexFollowString: String) = FOLLOW_STATUS_LIST.first { it.third == MangadexFollowString }.second
        fun SManga.FollowStatus.toMangadexInt() = FOLLOW_STATUS_LIST.first { it.second == this }.first
        fun SManga.FollowStatus.toMangadexString() = FOLLOW_STATUS_LIST.first { it.second == this }.third
    }
}