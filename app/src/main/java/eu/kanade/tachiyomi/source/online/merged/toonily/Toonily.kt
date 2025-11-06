package eu.kanade.tachiyomi.source.online.merged.toonily

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.nodes.Element
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await
import tachiyomi.core.network.interceptors.CookieInterceptor

class Toonily : ReducedHttpSource() {
    override val name = Toonily.name
    override val baseUrl = Toonily.baseUrl
    val dateFormat = SimpleDateFormat("MMM d, yy", Locale.US)

    override val client: OkHttpClient =
        network.cloudFlareClient
            .newBuilder()
            .rateLimit(2)
            .addNetworkInterceptor(CookieInterceptor(domain, "toonily-mature" to "1"))
            .build()

    override val headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val searchHeaders =
        headers.newBuilder().add("X-Requested-With", "XMLHttpRequest").build()

    override suspend fun searchManga(query: String): List<SManga> {
        val sanitizedQuery = query.replace(titleSpecialCharactersRegex, " ").trim()

        val response =
            client
                .newCall(
                    POST(
                        "$baseUrl/wp-admin/admin-ajax.php",
                        searchHeaders,
                        searchFormBuilder(sanitizedQuery).build(),
                    )
                )
                .await()
        if (!response.isSuccessful) {
            response.close()
            // Error message for exceeding last page
            if (response.code == 404) {
                error("Already on the Last Page!")
            } else {
                throw Exception("HTTP error ${response.code}")
            }
        }

        return parseSearchManga(response)
    }

    fun searchFormBuilder(query: String): FormBody.Builder =
        FormBody.Builder().apply {
            add("action", "madara_load_more")
            add("page", "0")
            add("template", "madara-core/content/content-search")
            add("vars[paged]", "1")
            add("vars[template]", "archive")
            add("vars[sidebar]", "right")
            add("vars[post_type]", "wp-manga")
            add("vars[post_status]", "publish")
            add("vars[manga_archives_item_layout]", "big_thumbnail")
            add("vars[s]", query)
        }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val response =
            client.newCall(POST("${baseUrl}${mangaUrl}ajax/chapters", searchHeaders)).await()

        if (!response.isSuccessful) {
            response.close()
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }
        return parseChapterList(response)
    }

    private fun parseChapterList(
        response: Response
    ): Result<List<SChapterStatusPair>, ResultError> {
        val document = response.asJsoup()
        response.closeQuietly()
        var currentVolume = 1
        var previousChapterNumber = "1"
        var currentExtraNumber = 0

        val elements = document.select("li.wp-manga-chapter").reversed()

        val chapters =
            elements.map { element ->
                val chapter = SChapter.create()
                val chapterName = mutableListOf<String>()

                var toonilyChapterName = ""
                element.select("a").first()?.let { urlElement ->
                    chapter.url =
                        urlElement.attr("abs:href").let {
                            it.substringBefore("?style=paged") +
                                if (!it.endsWith("?style=list")) "?style=list" else ""
                        }
                    toonilyChapterName = urlElement.text()
                }

                if (toonilyChapterName.startsWith("S${currentVolume + 1}")) {
                    currentVolume++
                }

                // edge case where there is a Season finale then an epilogue after it in the same
                // season
                if (toonilyChapterName.endsWith("Season ${currentVolume - 1} Epilogue")) {
                    val previousVolume = currentVolume - 1
                    chapterName.add("Vol.$previousVolume")
                    chapter.vol = previousVolume.toString()
                } else {
                    chapterName.add("Vol.$currentVolume")
                    chapter.vol = currentVolume.toString()
                }

                val endingList = listOf("Season $currentVolume END", "Season $currentVolume Finale")

                if (endingList.any { toonilyChapterName.endsWith(it, true) }) {
                    currentVolume++
                }

                val chapterNumber =
                    toonilyChapterName.substringAfter("Chapter ", "").substringBefore("-")

                val actualChapterNumberToUse =
                    when (chapterNumber.isBlank()) {
                        true -> {
                            currentExtraNumber++
                            "$previousChapterNumber.$currentExtraNumber"
                        }
                        false -> {
                            previousChapterNumber = chapterNumber.trim()
                            currentExtraNumber = 0
                            previousChapterNumber
                        }
                    }
                val chpText = "Ch.$actualChapterNumberToUse"
                chapterName.add(chpText)
                chapter.chapter_txt = chpText

                val chpName =
                    toonilyChapterName
                        .substringAfter("Chapter $chapterNumber")
                        .substringAfterLast("-")

                if (chpName.isNotBlank()) {
                    chapterName.add("-")
                    chapterName.add(chpName)
                }

                chapter.name = chapterName.joinToString(" ")

                chapter.scanlator = Toonily.name

                chapter.date_upload =
                    element.select("img:not(.thumb)").firstOrNull()?.attr("alt")?.let {
                        parseChapterDate(it)
                    }
                        ?: element.select("span a").firstOrNull()?.attr("title")?.let {
                            parseChapterDate(it)
                        }
                        ?: parseChapterDate(
                            element.select("span.chapter-release-date").firstOrNull()?.text()
                        )

                chapter to false
            }
        return Ok(chapters.reversed())
    }

    private fun parseChapterDate(date: String?): Long {
        date ?: return 0

        return when (date.contains("UP")) {
            true -> {
                Calendar.getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    .timeInMillis
            }
            false -> {
                runCatching { dateFormat.parse(date)?.time ?: 0 }.getOrElse { 0L }
            }
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET(chapter.url, headers)).await()
        val document = response.asJsoup()
        response.closeQuietly()
        return document
            .select(
                "div.page-break, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) img"
            )
            .mapIndexed { index, element ->
                Page(
                    index,
                    document.location(),
                    element.select("img").first()?.let {
                        it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
                    },
                )
            }
    }

    private fun parseSearchManga(response: Response): List<SManga> {
        val document = response.asJsoup()
        response.closeQuietly()
        return document.select("div.page-item-detail.manga").map { element ->
            val manga = SManga.create()
            element.select("div.post-title a").first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.ownText()
            }
            element.select("img").first()?.let { manga.thumbnail_url = imageFromElement(it) }
            manga
        }
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers.newBuilder().set("Referer", page.url).build())
    }

    private fun imageFromElement(element: Element): String? {
        return when {
            element.hasAttr("data-src") -> element.attr("abs:data-src")
            element.hasAttr("data-lazy-src") -> element.attr("abs:data-lazy-src")
            element.hasAttr("srcset") -> element.attr("abs:srcset").substringBefore(" ")
            else -> element.attr("abs:src")
        }
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return simpleChapter.url
    }

    companion object {
        const val name = "Toonily"
        const val domain = "toonily.com"
        const val baseUrl = "https://$domain"
        val titleSpecialCharactersRegex = "[^a-z0-9]+".toRegex()
    }
}
