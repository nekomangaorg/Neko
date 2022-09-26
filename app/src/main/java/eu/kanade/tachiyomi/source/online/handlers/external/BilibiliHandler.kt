package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.Page
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.isomorphism.util.TokenBuckets
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BilibiliHandler {
    val baseUrl = "https://www.bilibilicomics.com"
    val headers = Headers.Builder()
        .add("Accept", ACCEPT_JSON)
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")
        .build()

    private val bucket = TokenBuckets.builder().withCapacity(1)
        .withFixedIntervalRefillStrategy(1, 1, TimeUnit.SECONDS).build()

    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>().cloudFlareClient.newBuilder()
            .addInterceptor(rateLimitInterceptor).build()
    }

    private fun getChapterUrl(externalUrl: String): String {
        val comicId = externalUrl.substringAfterLast("/mc")
            .substringBefore('/')
            .toInt()
        val episodeId = externalUrl.substringAfterLast('/')
            .substringBefore('?')
            .toInt()
        return "/mc$comicId/$episodeId"
    }

    suspend fun fetchPageList(chapterUrl: String): List<Page> {
        val response = client.newCall(pageListRequest(getChapterUrl(chapterUrl))).await()
        return pageListParse(response)
    }

    private fun pageListRequest(chapterUrl: String): Request {
        val chapterId = chapterUrl.substringAfterLast("/").toInt()

        val jsonPayload = buildJsonObject { put("ep_id", chapterId) }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headers
            .newBuilder()
            .set("Referer", baseUrl + chapterUrl)
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/GetImageIndex?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody,
        )
    }

    private suspend fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<BilibiliResultDto<BilibiliReader>>()

        if (result.message.contains("need buy episode")) {
            throw Exception("Chapter is unavailable, requires reading and/or purchasing on BililBili")
        }
        if (result.code != 0) {
            return emptyList()
        }
        val baseUrls = result.data!!.images.map { it.path }

        val imageResponse = client.newCall(imageUrlRequest(baseUrls)).await()

        return imageUrlParse(imageResponse)
    }

    private fun imageUrlParse(response: Response): List<Page> {
        val result = response.parseAs<BilibiliResultDto<List<BilibiliPageDto>>>()
        return result.data!!.mapIndexed { index, page ->
            Page(index = index, imageUrl = "${page.url}?token=${page.token}")
        }
    }

    private fun imageUrlRequest(baseUrls: List<String>): Request {
        val jsonPayload = buildJsonObject {
            put(
                "urls",
                buildJsonArray {
                    baseUrls.forEach { add(it) }
                }.toString(),
            )
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headers.newBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ImageToken?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody,
        )
    }

    @Serializable
    data class BilibiliPageDto(
        val token: String,
        val url: String,
    )

    @Serializable
    data class BilibiliResultDto<T>(
        val code: Int = 0,
        val data: T? = null,
        @SerialName("msg") val message: String = "",
    )

    @Serializable
    data class BilibiliReader(
        val images: List<BilibiliImageDto> = emptyList(),
    )

    @Serializable
    data class BilibiliImageDto(
        val path: String,
    )

    @Serializable
    data class BilibiliComicDto(
        @SerialName("author_name") val authorName: List<String> = emptyList(),
        @SerialName("classic_lines") val classicLines: String = "",
        @SerialName("comic_id") val comicId: Int = 0,
        @SerialName("ep_list") val episodeList: List<BilibiliEpisodeDto> = emptyList(),
        val id: Int = 0,
        @SerialName("is_finish") val isFinish: Int = 0,
        @SerialName("season_id") val seasonId: Int = 0,
        val styles: List<String> = emptyList(),
        val title: String,
        @SerialName("vertical_cover") val verticalCover: String = "",
    )

    @Serializable
    data class BilibiliEpisodeDto(
        val id: Int,
        @SerialName("is_locked") val isLocked: Boolean,
        @SerialName("ord") val order: Float,
        @SerialName("pub_time") val publicationTime: String,
        val title: String,
    )

    companion object {
        private const val BASE_API_ENDPOINT = "twirp/comic.v1.Comic"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()
    }
}
