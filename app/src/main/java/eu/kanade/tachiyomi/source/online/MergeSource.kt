package eu.kanade.tachiyomi.source.online

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MergeSource {
    val name = "Merged Chapter"
    val baseUrl = "https://manga4life.com"
    val network: NetworkHelper by injectLazy()
    val client: OkHttpClient = network.cloudFlareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build()

    var directory: JsonArray? = null

    val gson = GsonBuilder().setLenient().create()

    val headers = Headers.Builder()
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
                    url = "$baseUrl/read-online/" + response.request.url.toString().substringAfter("/manga/") + chapterURLEncode(indexChapter)
                    date_upload = try {
                        dateFormat.parse(json["Date"].string.substringBefore(" "))?.time ?: 0
                    } catch (_: Exception) {
                        0L
                    }
                    scanlator = this@MergeSource.name
                }
            }.asReversed()
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

    companion object {
        const val name = "Merged Chapter"
    }
}