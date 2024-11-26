package eu.kanade.tachiyomi.source.online.merged.weebcentral

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await

class WeebCentral : ReducedHttpSource() {
    override val name = WeebCentral.name

    override val baseUrl = "https://weebcentral.com"

    override val client = network.cloudFlareClient.newBuilder().rateLimit(2).build()

    override val headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return simpleChapter.url
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val response =
            client
                .newCall(GET("$baseUrl${chapter.url}/images?reading_style=long_strip", headers))
                .await()
        val document = response.asJsoup()
        response.closeQuietly()
        return document.select("section img").mapIndexed { index, element ->
            Page(index, document.location(), element.select("img").first()?.absUrl("src"))
        }
    }

    override fun imageRequest(page: Page): Request {
        val imgHeaders =
            headers
                .newBuilder()
                .apply {
                    add("Accept", "image/avif,image/webp,*/*")
                    add("Host", page.imageUrl!!.toHttpUrl().host)
                }
                .build()

        return GET(page.imageUrl!!, imgHeaders)
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val url =
            baseUrl
                .toHttpUrl()
                .newBuilder()
                .addPathSegment("search")
                .addPathSegment("data")
                .addQueryParameter("author", "")
                .addQueryParameter("text", query)
                .addQueryParameter("sort", "Best Match")
                .addQueryParameter("order", "Ascending")
                .addQueryParameter("official", "Any")
                .addQueryParameter("display_mode", "Full Display")
                .build()
                .toString()

        val response = client.newCall(GET(url, headers)).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        return parseSearchManga(response)
    }

    private fun parseSearchManga(response: Response): List<SManga> {
        val document = response.asJsoup()
        response.closeQuietly()
        return document.select("article").mapNotNull { element ->
            if (element.select("a.link").first() == null) {
                null
            } else {
                MangaImpl().apply {
                    element.select("a.link").first()?.let { link ->
                        title = link.text()
                        setUrlWithoutDomain(link.attr("abs:href"))
                    }
                    element.select("picture source").first()?.let { picture ->
                        thumbnail_url = picture.attr("srcset")
                    }
                }
            }
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError> {
        val url =
            (baseUrl + mangaUrl)
                .toHttpUrl()
                .newBuilder()
                .removePathSegment(2) // remove the text
                .addPathSegment("full-chapter-list")
                .build()
                .toString()

        val response = client.newCall(GET(url, headers)).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        return parseChapters(response)
    }

    private fun parseChapters(response: Response): Result<List<SChapter>, ResultError> {
        val document = response.asJsoup()
        response.closeQuietly()

        val chapterPrefixes = arrayOf("Epilogue", "Side Story")
        val volumePrefixes = arrayOf("Volume", "Special")

        val chapters =
            document.select("a").mapNotNull { element ->
                val chapterText = element.selectFirst("span[class~=^\$]")?.text()
                if (chapterText == null) {
                    null
                } else {
                    SChapter.create().apply {
                        val chapterName = mutableListOf<String>()

                        setUrlWithoutDomain(element.attr("href"))

                        if (volumePrefixes.any { prefix -> chapterText.startsWith(prefix) }) {
                            this.vol = chapterText.substringAfter(" ")
                            val prefix =
                                when (chapterText.startsWith("Volume")) {
                                    true -> "${chapterText.substringBefore(" ")} "
                                    false -> "Vol."
                                }
                            chapterName.add("$prefix${this.vol}")
                        } else {
                            // The old logic would apply the name from either the "ChapterName"
                            // or Type + chapterNumber
                            // To match dex more this doesn't use name as it doesnt seem used
                            // often see Gantz (which doesnt make it here anyways cause its
                            // a manga)) and the text the extension shows for that would be ex.
                            // Special Osaka 1 vs Neko Special 1 - Special Osaka 1
                            // get volume
                            var volResult = chapterText.substringBefore(" -", "")
                            if (volResult.startsWith("S")) {
                                volResult = volResult.substringAfter("S")
                            } else {
                                volResult = volResult.substringAfter(" ")
                            }
                            if (volResult.isNotEmpty()) {
                                this.vol = volResult
                                chapterName.add("Vol.$volResult")
                            }

                            // get chapter

                            this.chapter_txt =
                                when {
                                    chapterPrefixes.none { prefix ->
                                        chapterText.startsWith(prefix)
                                    } -> "Ch.${chapterText.substringAfter(" ")}"
                                    else -> chapterText
                                }

                            chapterName.add(this.chapter_txt)
                        }

                        this.name = chapterName.joinToString(" ")

                        date_upload =
                            element.selectFirst("time[datetime]")?.attr("datetime").parseDate()
                        scanlator = WeebCentral.name
                        mangadex_chapter_id = url.substringAfter("chapters/")
                    }
                }
            }
        return Ok(chapters)
    }

    private fun String?.parseDate(): Long {
        return try {
            when (this == null) {
                true -> 0L
                false -> dateFormat.parse(this).time
            }
        } catch (_: ParseException) {
            0L
        }
    }

    companion object {
        const val name = "Weeb Central"
    }
}
