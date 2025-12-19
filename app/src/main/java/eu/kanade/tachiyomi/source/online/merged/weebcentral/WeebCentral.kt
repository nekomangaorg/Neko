package eu.kanade.tachiyomi.source.online.merged.weebcentral

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.system.tryParse
import java.text.SimpleDateFormat
import java.util.Locale
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await

class WeebCentral : ReducedHttpSource() {
    override val name = WeebCentral.name

    override val baseUrl = WeebCentral.baseUrl

    override val client = network.cloudFlareClient.newBuilder().rateLimit(2).build()

    override val headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return baseUrl + simpleChapter.url
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
                .addPathSegment("simple")
                .build()
                .toString()

        val formBody = FormBody.Builder().add("text", query).build()

        val response = client.newCall(POST(url, headers, body = formBody)).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        return parseSearchManga(response)
    }

    private fun parseSearchManga(response: Response): List<SManga> {
        val document = response.asJsoup()
        response.closeQuietly()
        return document.select("a.btn.join-item").mapNotNull { link ->
            MangaImpl().apply {
                title = link.select("div.flex-1").text()
                setUrlWithoutDomain(link.attr("abs:href"))

                val source = link.selectFirst("picture source")
                thumbnail_url = source?.attr("srcset") ?: link.select("img").attr("src")
            }
        }
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
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
            return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
        }

        return parseChapters(response)
    }

    private fun parseChapters(response: Response): Result<List<SChapterStatusPair>, ResultError> {
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
                                when (chapterText.startsWith("Special")) {
                                    true -> "${chapterText.substringBefore(" ")} "
                                    false -> "Vol."
                                }
                            chapterName.add("$prefix ${this.vol}")
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

                            val text =
                                when {
                                    chapterPrefixes.none { prefix ->
                                        chapterText.startsWith(prefix)
                                    } -> "Ch.${chapterText.substringAfterLast(" ")}"
                                    else -> chapterText
                                }

                            chapterName.add(text)
                        }

                        this.name = chapterName.joinToString(" ")
                        this.chapter_txt = chapterName.find { it.startsWith("Ch.") } ?: this.name

                        date_upload =
                            dateFormat.tryParse(
                                element.selectFirst("time[datetime]")?.attr("datetime")
                            )
                        scanlator = WeebCentral.name
                        mangadex_chapter_id = url.substringAfter("chapters/")
                    } to false
                }
            }
        return Ok(chapters)
    }

    companion object {
        const val name = "Weeb Central"
        const val baseUrl = "https://weebcentral.com"
    }
}
