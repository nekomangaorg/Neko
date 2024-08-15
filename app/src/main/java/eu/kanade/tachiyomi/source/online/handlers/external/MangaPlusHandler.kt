package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.extension.all.mangaplus.Language
import eu.kanade.tachiyomi.extension.all.mangaplus.MangaPlusResponse
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import java.util.UUID
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.nekomanga.core.network.GET
import tachiyomi.core.network.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaPlusHandler {
    val headers =
        Headers.Builder()
            .add("Origin", WEB_URL)
            .add("Referer", "$WEB_URL/")
            .add("User-Agent", USER_AGENT)
            .add("SESSION-TOKEN", UUID.randomUUID().toString())
            .build()

    private val json: Json by injectLazy()

    val client: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>()
            .client
            .newBuilder()
            .addInterceptor { imageIntercept(it) }
            .build()
    }

    suspend fun fetchPageList(chapterId: String): List<Page> {
        val response = client.newCall(pageListRequest(chapterId.substringAfterLast("/"))).await()
        return pageListParse(response)
    }

    private fun pageListRequest(chapterId: String): Request {
        return GET(
            "$API_URL/manga_viewer?chapter_id=$chapterId&split=yes&img_quality=super_high&format=json",
            headers,
        )
    }

    private fun Response.asMangaPlusResponse(): MangaPlusResponse = use {
        json.decodeFromString(body.string())
    }

    private fun pageListParse(response: Response): List<Page> {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            result.error!!.popups.firstOrNull { it.language == Language.ENGLISH }
                ?: "Error with MangaPlus"
        }

        return result.success.mangaViewer!!
            .pages
            .mapNotNull { it.mangaPage }
            .mapIndexed { i, page ->
                val encryptionKey = if (page.encryptionKey == null) "" else "#${page.encryptionKey}"
                Page(i, "", "${page.imageUrl}$encryptionKey")
            }
    }

    private fun imageIntercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val encryptionKey = request.url.fragment

        if (encryptionKey.isNullOrEmpty()) {
            return response
        }

        val contentType = response.headers["Content-Type"] ?: "image/jpeg"
        val image = response.body.bytes().decodeXorCipher(encryptionKey)
        val body = image.toResponseBody(contentType.toMediaTypeOrNull())

        return response.newBuilder().body(body).build()
    }

    private fun ByteArray.decodeXorCipher(key: String): ByteArray {
        val keyStream = key.chunked(2).map { it.toInt(16) }

        return mapIndexed { i, byte -> byte.toInt() xor keyStream[i % keyStream.size] }
            .map(Int::toByte)
            .toByteArray()
    }

    companion object {
        private const val WEB_URL = "https://mangaplus.shueisha.co.jp"
        val API_URL = "https://jumpg-webapi.tokyo-cdn.com/api"
        private val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"
    }
}
