package eu.kanade.tachiyomi.source.online

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MergeSource : HttpSource() {
    override val name = "Merged Chapter"
    override val baseUrl = "https://manga4life.com"
    override val client: OkHttpClient = network.cloudFlareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build()

    var directory: JsonArray? = null

    val gson = GsonBuilder().setLenient().create()

    override val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:71.0) Gecko/20100101 Firefox/77.0").build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun searchManga(query: String): List<SManga> {
        return withContext(Dispatchers.IO) {
            if (directory == null) {
                val response = client.newCall(GET("$baseUrl/search/", headers)).execute()
                directory = gson.fromJson<JsonArray>(directoryFromResponse(response))
            }
            val results = directory!!.filter { it["s"].string.contains(query, ignoreCase = true) }
            return@withContext if (results.isNotEmpty()) {
                parseMangaList(results)
            } else {
                emptyList()
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
                    name = json["ChapterName"].nullString.let { if (it.isNullOrEmpty()) "${json["Type"].string} ${chapterImage(indexChapter)}" else it }

                    val season = name.substringBefore(" - Chapter", "")
                    if (season.isNotEmpty()) {
                        vol = season.substring(1)
                    }
                    chapter_txt = name.substringAfter("Chapter ")

                    url = "/read-online/" + response.request.url.toString().substringAfter("/manga/") + chapterURLEncode(indexChapter)
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
        return "-chapter-$n$index$suffix.html"
    }

    override fun fetchImage(page: Page): Observable<Response> {
        return client.newCallWithProgress(GET(page.imageUrl!!, headers), page)
            .asObservableSuccess()
    }

    override fun isLogged(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(username: String, password: String, twoFactorCode: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): Boolean {
        TODO("Not yet implemented")
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override fun fetchFollows(page: Int): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        TODO("Not yet implemented")
    }

    override suspend fun updateReadingProgress(track: Track): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        TODO("Not yet implemented")
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        TODO("Not yet implemented")
    }

    override fun fetchMangaSimilarObservable(manga: Manga): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        TODO("Not yet implemented")
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        TODO("Not yet implemented")
    }

    override suspend fun getLatestCoverUrl(manga: SManga): String {
        TODO("Not yet implemented")
    }

    override suspend fun getAllCovers(manga: SManga): List<String> {
        TODO("Not yet implemented")
    }

    companion object {
        const val name = "Merged Chapter"
    }
}