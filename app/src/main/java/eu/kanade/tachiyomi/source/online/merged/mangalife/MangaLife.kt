package eu.kanade.tachiyomi.source.online.merged.mangalife

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.lang.toResultError
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.xdrop.fuzzywuzzy.FuzzySearch
import okhttp3.Response
import org.jsoup.nodes.Document
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.injectLazy

class MangaLife : ReducedHttpSource() {
    override val name = "Merged Chapter"
    override val baseUrl = "https://manga4life.com"

    lateinit var directory: Map<String, MangaLifeMangaDto>

    val json: Json by injectLazy()

    private lateinit var thumbnailUrl: String

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun searchManga(query: String): List<SManga> {
        return withContext(Dispatchers.IO) {
            if (this@MangaLife::directory.isInitialized.not()) {
                val response = client.newCall(GET("$baseUrl/search/", headers)).await()
                val document = response.asJsoup()
                directory = directoryFromDocument(document).associateBy { it.name }
                thumbnailUrl = document.select(".SearchResult > .SearchResultCover img").first()!!.attr("ng-src")
            }

            // val searchResults =
            val results = FuzzySearch.extractSorted(query, directory.keys, 88).mapNotNull {
                directory[it.string]
            }

            parseMangaList(results)
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

    suspend fun fetchChapters(mergeMangaUrl: String): Result<List<SChapter>, ResultError> {
        return withContext(Dispatchers.IO) {
            com.github.michaelbull.result.runCatching {
                val response = client.newCall(GET("$baseUrl$mergeMangaUrl", headers)).await()
                val vmChapters =
                    response.asJsoup().select("script:containsData(MainFunction)").first()!!.data()
                        .substringAfter("vm.Chapters = ").substringBefore(";")

                val mangaLifeChapters = json.decodeFromString<List<MangaLifeChapterDto>>(vmChapters)

                mangaLifeChapters.map { chp ->
                    SChapter.create().apply {
                        name = when (chp.chapterName == null || chp.chapterName.isEmpty()) {
                            true -> "${chp.type} ${chapterImage(chp.chapter, true)}"
                            false -> chp.chapterName
                        }

                        // get the seasons
                        val season1 = name.substringAfter("Volume ", "")
                        val season2 = name.substringBefore(" - Chapter", "").substringAfter("S")
                        if (season1.isNotEmpty() && season2.isEmpty()) {
                            vol = season1
                        } else if (season2.isNotEmpty()) {
                            vol = season2
                        }

                        // set the chapter text
                        if (chp.type != "Volume") {
                            val splitName = name.substringAfter(" - Chapter").split(" ")
                            for (split in splitName) {
                                val splitFloat = split.toFloatOrNull()
                                if (splitFloat != null) {
                                    chapter_txt = splitFloat.toString()
                                    break
                                }
                            }
                        }

                        url = "/read-online/" + response.request.url.toString()
                            .substringAfter("/manga/") + chapterURLEncode(chp.chapter)
                        mangadex_chapter_id = url.substringAfter("/read-online/")
                        date_upload = runCatching {
                            when (chp.date.isEmpty()) {
                                true -> 0L
                                false -> dateFormat.parse("$chp.date +0600")?.time!!
                            }
                        }.getOrElse { 0L }

                        scanlator = this@MangaLife.name
                    }
                }.asReversed()
            }.mapError {
                XLog.e(it)
                "Unknown Exception with merge".toResultError()
            }
        }
    }

    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET("$baseUrl${chapter.url}", headers)).await()
        return pageListParse(response)
    }

    private fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val script = document.select("script:containsData(MainFunction)").first()!!.data()
        val chapterJson = script.substringAfter("vm.CurChapter = ")
            .substringBefore(";")
        val curChapter = json.decodeFromString<MangaLifeChapterDto>(chapterJson)

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")

        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")

        val seasonURI = when (curChapter.directory.isNullOrBlank()) {
            true -> ""
            false -> "${curChapter.directory}/"
        }

        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = chapterImage(curChapter.chapter)

        return IntRange(1, curChapter.totalPages!!).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            Page(i, "", "$path$chNum-$imageNum.png")
        }
    }

    /**
     * Get the entire manga life results from the json and store in memory
     */
    private fun directoryFromDocument(document: Document): List<MangaLifeMangaDto> {
        val jsonValue = document.select("script:containsData(MainFunction)").first()!!.data()
            .substringAfter("vm.Directory = ").substringBefore("vm.GetIntValue").trim()
            .replace(";", " ")

        return json.decodeFromString(jsonValue)
    }

    private val chapterImageRegex = Regex("""^0+""")

    private fun chapterImage(e: String, cleanString: Boolean = false): String {
        // cleanString will result in an empty string if chapter number is 0, hence the else if below
        val a = e.substring(1, e.length - 1).let { if (cleanString) it.replace(chapterImageRegex, "") else it }
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
        val dgt = when {
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

    companion object {
        const val name = "Merged Chapter"
    }
}
