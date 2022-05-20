package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import info.debatty.java.stringsimilarity.JaroWinkler
import info.debatty.java.stringsimilarity.Levenshtein
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class MergeSource : ReducedHttpSource() {
    override val name = "Merged Chapter"
    override val baseUrl = "https://manga4life.com"

    lateinit var directory: List<JsonElement>

    private val textDistance = Levenshtein()
    private val textDistance2 = JaroWinkler()

    val json: Json by injectLazy()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun searchManga(query: String): List<SManga> {
        return withContext(Dispatchers.IO) {
            if (this@MergeSource::directory.isInitialized.not()) {
                val response = client.newCall(GET("$baseUrl/search/", headers)).await()
                directory = directoryFromResponse(response)
            }

            val exactMatch = directory.filter { jsonElement ->
                jsonElement.getString("s") == query || jsonElement.getArray("al").any { altName -> altName.jsonPrimitive.content == query }
            }
            if (exactMatch.isNotEmpty()) {
                parseMangaList(exactMatch)
            } else {
                // take results that potentially start the same
                val results = directory.filter { jsonElement ->

                    val title = jsonElement.getString("s")
                    val query2 = query.take(7)

                    val titleMatches = jsonElement.getArray("al").map { altTitleJson ->
                        val altTitle = altTitleJson.jsonPrimitive.content
                        altTitle.startsWith(query2, true) || altTitle.contains(query2, true)
                    } + listOf((title.startsWith(query2, true)), title.contains(query2, true))

                    titleMatches.isNotEmpty()
                }.sortedBy { textDistance.distance(query, it.getString("s")) }

                // take similar results
                val results2 =
                    directory.map { Pair(textDistance2.distance(it.getString("s"), query), it) }
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
        return json.map { jsonElement ->
            SManga.create().apply {
                title = jsonElement.getString("s")
                url = "/manga/${jsonElement.getString("i")}"
                thumbnail_url = "https://cover.nep.li/cover/${jsonElement.getString("i")}.jpg"
            }
        }
    }

    suspend fun fetchChapters(mergeMangaUrl: String): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(GET("$baseUrl$mergeMangaUrl", headers)).await()
            val vmChapters =
                response.asJsoup().select("script:containsData(MainFunction)").first()!!.data()
                    .substringAfter("vm.Chapters = ").substringBefore(";")


            return@withContext json.parseToJsonElement(vmChapters).jsonArray.map { json ->
                val indexChapter = json.getString("Chapter")
                SChapter.create().apply {
                    val type = json.getString("Type")

                    name = when (json.getString("ChapterName").isEmpty()) {
                        true -> "$type ${chapterImage(indexChapter, true)}"
                        false -> json.getString("ChapterName")
                    }

                    val season = name.substringAfter("Volume ", "")
                    if (season.isNotEmpty()) {
                        vol = season.substringBefore(" ")
                    }

                    val seasonAnotherWay =
                        name.substringBefore(" - Chapter", "").substringAfter("S")

                    if (seasonAnotherWay.isNotEmpty()) {
                        vol = seasonAnotherWay
                    }

                    if (type != "Volume") {
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
                        .substringAfter("/manga/") + chapterURLEncode(indexChapter)
                    mangadex_chapter_id = url.substringAfter("/read-online/")
                    date_upload = runCatching {
                        val jsonDate = json.getString("Date")
                        when (jsonDate.isEmpty()) {
                            true -> 0L
                            false -> dateFormat.parse("$jsonDate +0600")?.time!!
                        }
                    }.getOrElse { 0L }

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
    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET("$baseUrl${chapter.url}", headers)).await()
        return pageListParse(response)
    }

    private fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val script = document.select("script:containsData(MainFunction)").first()!!.data()
        val curChapter = json.parseToJsonElement(
            script.substringAfter("vm.CurChapter = ")
                .substringBefore(";")
        ).jsonObject

        val pageTotal = curChapter.getString("Page").toInt()

        val host = "https://" + script.substringAfter("vm.CurPathName = \"").substringBefore("\"")
        val titleURI = script.substringAfter("vm.IndexName = \"").substringBefore("\"")
        val seasonURI = curChapter.getString("Directory")
            .let { if (it.isEmpty()) "" else "$it/" }
        val path = "$host/manga/$titleURI/$seasonURI"

        val chNum = chapterImage(curChapter.getString("Chapter"))

        return IntRange(1, pageTotal).mapIndexed { i, _ ->
            val imageNum = (i + 1).toString().let { "000$it" }.let { it.substring(it.length - 3) }
            Page(i, "", "$path$chNum-$imageNum.png")
        }
    }

    // don't use ";" for substringBefore() !
    private fun directoryFromResponse(response: Response): JsonArray {
        val jsonValue = response.asJsoup().select("script:containsData(MainFunction)").first()!!.data()
            .substringAfter("vm.Directory = ").substringBefore("vm.GetIntValue").trim()
            .replace(";", " ")

        return json.parseToJsonElement(jsonValue).jsonArray
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

    override suspend fun fetchImage(page: Page): Response {
        return client.newCallWithProgress(GET(page.imageUrl!!, headers), page).await()
    }

    // Convenience functions to shorten later code
    /** Returns value corresponding to given key as a string, or ""*/
    private fun JsonElement.getString(key: String): String {
        return this.jsonObject[key]!!.jsonPrimitive.contentOrNull ?: ""
    }

    /** Returns value corresponding to given key as a JsonArray */
    private fun JsonElement.getArray(key: String): JsonArray {
        return this.jsonObject[key]!!.jsonArray
    }

    companion object {
        const val name = "Merged Chapter"
    }
}
