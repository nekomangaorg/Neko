package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource.Companion.USER_AGENT
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.interceptor.rateLimit
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ProjectSukiHandler {
    val baseUrl = "https://projectsuki.com"
    private val callPageUrl = "$baseUrl/callpage"

    val headers =
        Headers.Builder().add("User-Agent", USER_AGENT).add("Referer", "$baseUrl/").build()

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>().cloudFlareClient.newBuilder()
            .rateLimit(2, 1, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }

    suspend fun fetchPageList(externalUrl: String): List<Page> {
        val chapterUrl = externalUrl.toHttpUrlOrNull() ?: throw Exception("Invalid URL")
        val pathSegments = chapterUrl.pathSegments

        // Expected URL: https://projectsuki.com/read/bookid/chapterid/startpage
        if (pathSegments.size < 3 || !pathSegments[0].equals("read", ignoreCase = true)) {
            throw Exception("Invalid ProjectSuki URL format")
        }

        val bookId = pathSegments[1]
        val chapterId = pathSegments[2]

        val newHeaders =
            headers.newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Content-Type", "application/json;charset=UTF-8")
                .build()

        val bodyData = PagesRequestData(bookId, chapterId, "true")
        val body =
            json.encodeToString(bodyData)
                .toRequestBody("application/json;charset=UTF-8".toMediaType())

        val request = POST(callPageUrl, newHeaders, body)
        val response = client.newCall(request).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP Error ${response.code}")
        }

        val responseString = response.body.string()
        val jsonObject = json.decodeFromString<JsonObject>(responseString)

        val rawSrc =
            jsonObject["src"]?.jsonPrimitive?.content ?: throw Exception("No source found in response")

        val document = Jsoup.parseBodyFragment(rawSrc, baseUrl)
        val imageUrls =
            document.select("img").mapNotNull { it.imageSrc()?.toString() }.distinct()

        if (imageUrls.isEmpty()) {
            throw Exception("No pages found")
        }

        return imageUrls.mapIndexed { index, url -> Page(index, imageUrl = url) }
    }

    private fun Element.imageSrc(): HttpUrl? {
        val simpleSrcVariants = listOf("data-lazy-src", "data-src", "src")
        simpleSrcVariants.forEach { variant ->
            if (hasAttr(variant)) {
                return attr("abs:$variant").toHttpUrlOrNull()
            }
        }

        if (hasAttr("srcset")) {
            return attr("abs:srcset").substringBefore(" ").toHttpUrlOrNull()
        }

        return null
    }

    @Serializable
    data class PagesRequestData(
        @SerialName("bookid") val bookID: String,
        @SerialName("chapterid") val chapterID: String,
        @SerialName("first") val first: String,
    )
}
