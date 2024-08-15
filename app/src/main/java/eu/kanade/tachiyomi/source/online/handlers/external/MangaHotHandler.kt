package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import org.nekomanga.core.network.GET
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaHotHandler {
    val client: OkHttpClient by lazy { Injekt.get<NetworkHelper>().client }

    val baseUrl = "https://mangahot.jp"
    private val apiUrl = "https://api.mangahot.jp"
    val headers = Headers.Builder().add("User-Agent", HttpSource.USER_AGENT).build()

    suspend fun fetchPageList(externalUrl: String): List<Page> {
        val request =
            GET(
                externalUrl
                    .substringBefore("?")
                    .replace(baseUrl, apiUrl)
                    .replace("viewer", "v1/works/storyDetail"),
                headers,
            )
        return pageListParse(client.newCall(request).await())
    }

    fun pageListParse(response: Response): List<Page> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["content"]!!
            .jsonObject["contentUrls"]!!
            .jsonArray
            .mapIndexed { index, element ->
                val url = element.jsonPrimitive.content
                Page(index, url, url)
            }
    }
}
