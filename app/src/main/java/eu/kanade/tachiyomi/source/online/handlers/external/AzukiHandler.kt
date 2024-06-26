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
import okhttp3.Request
import okhttp3.Response
import org.nekomanga.core.network.GET
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AzukiHandler {

    val client: OkHttpClient by lazy { Injekt.get<NetworkHelper>().client }

    val baseUrl = "https://www.azuki.co"
    private val apiUrl = "https://production.api.azuki.co"
    val headers = Headers.Builder().add("User-Agent", HttpSource.USER_AGENT).build()

    suspend fun fetchPageList(externalUrl: String): List<Page> {
        val chapterId = externalUrl.substringAfterLast("/").substringBefore("?")
        val request = pageListRequest(chapterId)
        return pageListParse(client.newCall(request).await())
    }

    private fun pageListRequest(chapterId: String): Request {
        return GET("$apiUrl/chapter/$chapterId/pages/v0", headers)
    }

    fun pageListParse(response: Response): List<Page> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["pages"]!!
            .jsonArray
            .mapIndexed { index, element ->
                val array = element.jsonObject["image_wm"]!!.jsonObject["webp"]!!.jsonArray

                val position =
                    when (array.size) {
                        3 -> 2
                        2 -> 1
                        else -> 0
                    }

                val url = array[position].jsonObject["url"]!!.jsonPrimitive.content
                Page(index, url, url)
            }
    }
}
