package eu.kanade.tachiyomi.source.online

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import info.debatty.java.stringsimilarity.JaroWinkler
import info.debatty.java.stringsimilarity.Levenshtein
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class MergeSource : ReducedHttpSource() {
    override val name = "Merged Chapter"
    override val baseUrl = "https://manga4life.com"

    var directory: JsonArray? = null

    val gson = GsonBuilder().setLenient().create()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun searchManga(query: String): List<SManga> {
        return withContext(Dispatchers.IO) {
            if (directory == null) {
                val response = client.newCall(GET("$baseUrl/search/", headers)).execute()
                directory = gson.fromJson<JsonArray>(directoryFromResponse(response))
            }
            val textDistance = Levenshtein()
            val textDistance2 = JaroWinkler()

            val exactMatch = directory!!.firstOrNull { it["s"].string == query }
            if (exactMatch != null) {
                parseMangaList(listOf(exactMatch))
            } else {
                // take results that potentially start the same
                val results = directory!!.filter {
                    val title = it["s"].string
                    val query2 = query.take(7)
                    (
                        title.startsWith(query2, true) ||
                            title.contains(query2, true)
                        )
                }.sortedBy { textDistance.distance(query, it["s"].string) }

                // take similar results
                val results2 = directory!!.map { Pair(textDistance2.distance(it["s"].string, query), it) }
                    .filter { it.first < 0.3 }.sortedBy { it.first }.map { it.second }

                val combinedResults = results.union(results2)

                return@withContext if (combinedResults.isNotEmpty()) {
                    parseMangaList(combinedResults.toList())
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun parseMangaList(json: List<JsonElement>): List<SManga> {
        return json.map { it ->
            SManga.create().apply {
                title = it["s"].string
                url = "/manga/${it["i"].string}"
                thumbnail_url = "https://cover.mangabeast01.com/cover/${it["i"].string}.jpg"
            }
        }
    }

    suspend fun fetchChapters(mergeMangaUrl: String): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(GET("$baseUrl$mergeMangaUrl", headers)).execute()
            val vmChapters = response.asJsoup().select("script:containsData(MainFunction)").first().data()
                .substringAfter("vm.Chapters = ").substringBefore(";")

            return@withContext gson.fromJson<JsonArray>(vmChapters).map { json ->
                val indexChapter = json["Chapter"].string
                SChapter.create().apply {

                    val type = json["Type"].string

                    name = json["ChapterName"].nullString.let { if (it.isNullOrEmpty()) "$type ${chapterImage(indexChapter)}" else it }

                    val season = name.substringAfter("Volume ", "")
                    if (season.isNotEmpty()) {
                        vol = season.substringBefore(" ")
                    }

                    val seasonAnotherWay = name.substringBefore(" - Chapter", "").substringAfter("S")

                    if (seasonAnotherWay.isNotEmpty()) {
                        vol = seasonAnotherWay
                    }

                    if (json["Type"].string != "Volume") {
                        val splitName = name.substringAfter(" - Chapter").split(" ")
                        for (split in splitName) {
                            val splitFloat = split.toFloatOrNull()
                            if (splitFloat != null) {
                                chapter_txt = splitFloat.toString()
                                break
                            }
                        }
                    }

                    url = "/read-online/" + response.request.url.toString().substringAfter("/manga/") + chapterURLEncode(indexChapter)
                    mangadex_chapter_id = url.substringAfter("/read-online/")
                    date_upload = try {
                        json["Date"].nullString?.let { dateFormat.parse("$it +0600")?.time } ?: 0
                    } catch (_: Exception) {
                        0L
                    }
                    scanlator = this@MergeSource.name
                }
            }.asReversed()
        }
    }

    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(GET("$baseUrl${chapter.url}", headers))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response)
            }
    }

    fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val script = document.select("script:containsData(MainFunction)").first().data()
        val curChapter = gson.fromJson<JsonElement>(script.substringAfter("vm.CurChapter = ").substringBefore(";"))

        val pageTotal = curChapter["Page"].string.toInt()

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")
        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")
        val seasonURI = curChapter["Directory"].string
            .let { if (it.isEmpty()) "" else "$it/" }
        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = chapterImage(curChapter["Chapter"].string)

        return IntRange(1, pageTotal).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            Page(i, "", "$path$chNum-$imageNum.png")
        }
    }

    // don't use ";" for substringBefore() !
    private fun directoryFromResponse(response: Response): String {
        return response.asJsoup().select("script:containsData(MainFunction)").first().data()
            .substringAfter("vm.Directory = ").substringBefore("vm.GetIntValue").trim()
            .replace(";", " ")
    }

    private fun chapterImage(e: String): String {
        val a = e.substring(1, e.length - 1)
        val b = e.substring(e.length - 1).toInt()
        return if (b == 0) {
            a
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
        val n = e.substring(1, e.length - 1)

        var suffix = ""
        val path = e.substring(e.length - 1).toInt()
        if (0 != path) {
            suffix = ".$path"
        }
        return "-chapter-$n$suffix$index.html"
    }

    override fun fetchImage(page: Page): Observable<Response> {
        return client.newCallWithProgress(GET(page.imageUrl!!, headers), page)
            .asObservableSuccess()
    }

    companion object {
        const val name = "Merged Chapter"
    }
}
