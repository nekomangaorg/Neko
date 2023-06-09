package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource.Companion.USER_AGENT
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.nekomanga.core.network.GET
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ComikeyHandler {
    val baseUrl = "https://comikey.com"
    private val apiUrl = "$baseUrl/sapi"
    val headers = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .build()

    val client: OkHttpClient by lazy { Injekt.get<NetworkHelper>().client }

    private val urlForbidden =
        "https://fakeimg.pl/1800x2252/FFFFFF/000000/?font_size=120&text=This%20chapter%20is%20not%20available%20for%20free.%0A%0AIf%20you%20have%20purchased%20this%20chapter%2C%20please%20%0Aopen%20the%20website%20in%20web%20view%20and%20log%20in."

    suspend fun fetchPageList(externalUrl: String): List<Page> {
        val httpUrl = externalUrl.toHttpUrl()
        val mangaId = getMangaId(httpUrl.pathSegments[1])
        val response = client.newCall(pageListRequest(mangaId, httpUrl.pathSegments[2])).await()
        val request =
            getActualPageList(response) ?: return listOf(Page(0, urlForbidden, urlForbidden))
        return pageListParse(client.newCall(request).await())
    }

    suspend fun getMangaId(mangaUrl: String): Int {
        val response = client.newCall(GET("$baseUrl/read/$mangaUrl")).await()
        val url = response.asJsoup().selectFirst("meta[property=og:url]")!!.attr("content")
        return url.trimEnd('/').substringAfterLast('/').toInt()
    }

    private fun pageListRequest(mangaId: Int, chapterGuid: String): Request {
        return GET("$apiUrl/comics/$mangaId/read?format=json&content=EPI-$chapterGuid", headers)
    }

    private fun getActualPageList(response: Response): Request? {
        val element = Json.parseToJsonElement(response.body!!.string()).jsonObject
        val ok = element["ok"]?.jsonPrimitive?.booleanOrNull ?: false
        if (ok.not()) {
            return null
        }
        val url = element["href"]?.jsonPrimitive!!.content
        return GET(url, headers)
    }

    fun pageListParse(response: Response): List<Page> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["readingOrder"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonObject["href"]!!.jsonPrimitive.content
                Page(index, url, url)
            }
    }
}
