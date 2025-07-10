package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.isomorphism.util.TokenBuckets
import org.nekomanga.core.network.GET
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class NamiComiHandler {
    val baseUrl = "https://namicomi.com"
    val apiUrl = "https://api.namicomi.com"
    val headers =
        Headers.Builder()
            .add("Accept", "application/json, text/plain, */*")
            .add("Origin", baseUrl)
            .add("Referer", "$baseUrl/")
            .add("User-Agent", HttpSource.USER_AGENT)
            .build()

    private val bucket =
        TokenBuckets.builder()
            .withCapacity(1)
            .withFixedIntervalRefillStrategy(1, 1, TimeUnit.SECONDS)
            .build()

    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>()
            .cloudFlareClient
            .newBuilder()
            .addInterceptor(rateLimitInterceptor)
            .build()
    }

    private val json: Json by injectLazy()

    suspend fun fetchPageList(chapterUrl: String): List<Page> {
        try {
            val response = client.newCall(pageListRequest(chapterUrl)).await()
            return pageListParse(response)
        } catch (e: Exception) {
            if (e.message == "HTTP error 402") {
                throw Exception("Chapter requires login and/or purchasing on NamiComi")
            }
            return emptyList()
        }
    }

    private fun pageListRequest(chapterUrl: String): Request {
        val chapterId = chapterUrl.substringAfterLast("/").substringBefore("?")
        val url = "$apiUrl/images/chapter/$chapterId?newQualities=true"
        return GET(url, headers)
    }

    private fun pageListParse(response: Response): List<Page> {
        val chapterId = response.request.url.pathSegments.last()
        val pageListDataDto = json.decodeFromString<ResultDto>(response.body.string()).data

        val hash = pageListDataDto.hash
        val prefix = "${pageListDataDto.baseUrl}/chapter/$chapterId/$hash"
        val urls = pageListDataDto.source.map { prefix + "/source/${it.filename}" }

        return urls.mapIndexed { index, url -> Page(index = index, imageUrl = url) }
    }

    @Serializable class ResultDto(val data: PageListDataDto)

    @Serializable
    class PageListDataDto(val baseUrl: String, val hash: String, val source: List<PageImageDto>)

    @Serializable class PageImageDto(val filename: String)
}
