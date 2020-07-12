package eu.kanade.tachiyomi.source.online

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.string
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

class MergeSource {
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
                url = "${baseUrl}/manga/${it["i"].string}"
                thumbnail_url = "https://cover.mangabeast01.com/cover/${it["i"].string}.jpg"
            }
        }
    }
}

// don't use ";" for substringBefore() !
private fun directoryFromResponse(response: Response): String {
    return response.asJsoup().select("script:containsData(MainFunction)").first().data()
        .substringAfter("vm.Directory = ").substringBefore("vm.GetIntValue").trim()
        .replace(";", " ")
}