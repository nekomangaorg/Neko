package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource.Companion.USER_AGENT
import eu.kanade.tachiyomi.source.online.models.dto.Language
import eu.kanade.tachiyomi.source.online.models.dto.MangaPlusResponse
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaPlusHandler {
    val baseUrl = "https://jumpg-webapi.tokyo-cdn.com/api"
    val headers = Headers.Builder()
        .add("Origin", WEB_URL)
        .add("Referer", WEB_URL)
        .add("User-Agent", USER_AGENT)
        .add("SESSION-TOKEN", UUID.randomUUID().toString()).build()

    private val json: Json by injectLazy()

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>().nonRateLimitedClient.newBuilder()
            .addInterceptor { imageIntercept(it) }
            .build()
    }

    suspend fun fetchPageList(chapterId: String): List<Page> {
        val response = client.newCall(pageListRequest(chapterId.substringAfterLast("/"))).await()
        return pageListParse(response)
    }

    private fun pageListRequest(chapterId: String): Request {
        return GET(
            "$baseUrl/manga_viewer?chapter_id=$chapterId&split=yes&img_quality=super_high&format=json",
            headers,
        )
    }

    private fun Response.asMangaPlusResponse(): MangaPlusResponse = use {
        json.decodeFromString(body!!.string())
    }

    private fun pageListParse(response: Response): List<Page> {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) { result.error!!.popups.firstOrNull { it.language == Language.ENGLISH } ?: "Error with MangaPlus" }

        return result.success.mangaViewer!!.pages
            .mapNotNull { it.mangaPage }
            .mapIndexed { i, page ->
                val encryptionKey =
                    if (page.encryptionKey == null) "" else "&encryptionKey=${page.encryptionKey}"
                Page(i, "", "${page.imageUrl}$encryptionKey")
            }
    }

    private fun imageIntercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!request.url.queryParameterNames.contains("encryptionKey")) {
            return chain.proceed(request)
        }

        val encryptionKey = request.url.queryParameter("encryptionKey")!!

        // Change the url and remove the encryptionKey to avoid detection.
        val newUrl = request.url.newBuilder().removeAllQueryParameters("encryptionKey").build()
        request = request.newBuilder().url(newUrl).build()

        val response = chain.proceed(request)

        val image = decodeImage(encryptionKey, response.body!!.bytes())

        val body = image.toResponseBody("image/jpeg".toMediaTypeOrNull())
        return response.newBuilder().body(body).build()
    }

    private fun decodeImage(encryptionKey: String, image: ByteArray): ByteArray {
        val keyStream = HEX_GROUP
            .findAll(encryptionKey)
            .map { it.groupValues[1].toInt(16) }
            .toList()

        val content = image
            .map { it.toInt() }
            .toMutableList()

        val blockSizeInBytes = keyStream.size

        for ((i, value) in content.iterator().withIndex()) {
            content[i] = value xor keyStream[i % blockSizeInBytes]
        }

        return ByteArray(content.size) { pos -> content[pos].toByte() }
    }

    companion object {
        private const val WEB_URL = "https://mangaplus.shueisha.co.jp"
        private val HEX_GROUP = "(.{1,2})".toRegex()
    }
}
