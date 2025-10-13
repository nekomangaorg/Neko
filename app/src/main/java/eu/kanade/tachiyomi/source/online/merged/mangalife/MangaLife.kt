package eu.kanade.tachiyomi.source.online.merged.mangalife

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.lang.toResultError
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class MangaLife : ReducedHttpSource() {
    override val name = MangaLife.name
    override val baseUrl = "https://manga4life.com"

    override val client: OkHttpClient =
        network.cloudFlareClient
            .newBuilder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .rateLimit(2)
            .build()

    override val headers =
        Headers.Builder().add("Referer", baseUrl).add("User-Agent", userAgent).build()

    lateinit var directory: Map<String, MangaLifeMangaDto>

    val json: Json by injectLazy()

    private lateinit var thumbnailUrl: String

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z", Locale.getDefault())

    override suspend fun searchManga(query: String): List<SManga> {
        return withContext(Dispatchers.IO) {
            if (!this@MangaLife::directory.isInitialized) {
                val response = client.newCall(GET("$baseUrl/search/", headers)).await()
                val document = response.asJsoup()
                directory = directoryFromDocument(document).associateBy { it.name }
                thumbnailUrl =
                    document
                        .select(".SearchResult > .SearchResultCover img")
                        .first()!!
                        .attr("ng-src")
            }

            parseMangaList(emptyList())
        }
    }

    private fun parseMangaList(mangaList: List<MangaLifeMangaDto>): List<SManga> {
        return mangaList.map { manga ->
            SManga.create().apply {
                title = manga.name
                url = "/manga/${manga.id}"
                thumbnail_url = thumbnailUrl.replace("{{Result.i}}", manga.id)
            }
        }
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result
                .runCatching {
                    val response = client.newCall(GET("$baseUrl$mangaUrl", headers)).await()
                    val vmChapters =
                        response
                            .asJsoup()
                            .select("script:containsData(MainFunction)")
                            .first()!!
                            .data()
                            .substringAfter("vm.Chapters = ")
                            .substringBefore(";")

                    val mangaLifeChapters =
                        json.decodeFromString<List<MangaLifeChapterDto>>(vmChapters)
                    val uniqueTypes = arrayOf("Volume", "Special")
                    mangaLifeChapters.map { chp ->
                        SChapter.create().apply {
                            val chapterName = mutableListOf<String>()
                            // Build chapter name
                            if (chp.type in uniqueTypes) {
                                this.vol = calculateChapterNumber(chp.chapter, true)
                                val prefix =
                                    when (chp.type != "Volume") {
                                        true -> "${chp.type} "
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
                                var volResult = chp.type.substringBefore(" -", "")
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
                                this.chapter_txt = chp.chapterString()
                                chapterName.add(this.chapter_txt)
                            }

                            // get text
                            if (chp.chapterName?.isNotEmpty() == true) {
                                if (chapterName.isNotEmpty()) {
                                    chapterName.add("-")
                                }
                                chapterName.add(chp.chapterName)
                            }

                            this.name = chapterName.joinToString(" ")

                            url =
                                "/read-online/" +
                                    response.request.url.toString().substringAfter("/manga/") +
                                    chapterURLEncode(chp.chapter)
                            mangadex_chapter_id = url.substringAfter("/read-online/")
                            date_upload =
                                runCatching {
                                        when (chp.date.isEmpty()) {
                                            true -> 0L
                                            false -> dateFormat.parse("${chp.date} +0600")?.time!!
                                        }
                                    }
                                    .onFailure { TimberKt.e(it) }
                                    .getOrElse { 0L }

                            scanlator = this@MangaLife.name
                        } to false
                    }
                }
                .mapError {
                    TimberKt.e(it) { "Error merging with manga life" }
                    "Unknown Exception with merge".toResultError()
                }
        }
    }

    private fun MangaLifeChapterDto.chapterString(): String =
        "Ch.${calculateChapterNumber(this.chapter, true)}"

    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET("$baseUrl${chapter.url}", headers)).await()
        return pageListParse(response)
    }

    private fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val script = document.select("script:containsData(MainFunction)").first()!!.data()
        val chapterJson = script.substringAfter("vm.CurChapter = ").substringBefore(";")
        val curChapter = json.decodeFromString<MangaLifeChapterDto>(chapterJson)

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")

        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")

        val seasonURI =
            when (curChapter.directory.isNullOrBlank()) {
                true -> ""
                false -> "${curChapter.directory}/"
            }

        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = calculateChapterNumber(curChapter.chapter)

        return IntRange(1, curChapter.totalPages!!).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            Page(i, "", "$path$chNum-$imageNum.png")
        }
    }

    /** Get the entire manga life results from the json and store in memory */
    private fun directoryFromDocument(document: Document): List<MangaLifeMangaDto> {
        val jsonValue =
            document
                .select("script:containsData(MainFunction)")
                .first()!!
                .data()
                .substringAfter("vm.Directory = ")
                .substringBefore("vm.GetIntValue")
                .trim()
                .replace(";", " ")

        return json.decodeFromString(jsonValue)
    }

    private val chapterImageRegex = Regex("""^0+""")

    private fun calculateChapterNumber(e: String, cleanString: Boolean = false): String {
        // cleanString will result in an empty string if chapter number is 0, hence the else if
        // below
        val a =
            e.substring(1, e.length - 1).let {
                if (cleanString) it.replace(chapterImageRegex, "") else it
            }
        // If b is not zero, indicates chapter has decimal numbering
        val b = e.substring(e.length - 1).toInt()
        return if (b == 0 && a.isNotEmpty()) {
            a
        } else if (b == 0 && a.isEmpty()) {
            "0"
        } else {
            "$a.$b"
        }
    }

    private fun chapterURLEncode(e: String): String {
        var index = ""
        val t = e.substring(0, 1).toInt()
        if (1 != t) {
            index = "-index-$t"
        }
        val dgt =
            when {
                e.toInt() < 100100 -> 4
                e.toInt() < 101000 -> 3
                e.toInt() < 110000 -> 2
                else -> 1
            }
        val n = e.substring(dgt, e.length - 1)
        var suffix = ""
        val path = e.substring(e.length - 1).toInt()
        if (0 != path) {
            suffix = ".$path"
        }
        return "-chapter-$n$suffix$index.html"
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return baseUrl + simpleChapter.url
    }

    companion object {
        @Deprecated("deprecated") const val oldName = "Merged Chapter"
        const val name = "MangaLife"
    }
}
