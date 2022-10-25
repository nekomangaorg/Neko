package eu.kanade.tachiyomi.source.online.merged.komga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

class Komga : ReducedHttpSource() {

    override val baseUrl: String = "https://demo.komga.org"

    private val json: Json by injectLazy()

    override val client: OkHttpClient =
        network.client.newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up, we've already failed to authenticate.
                } else {
                    response.request.newBuilder()
                        .addHeader("Authorization", Credentials.basic("demo@komga.org", "komga-demo"))
                        .build()
                }
            }
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()

    override suspend fun searchManga(query: String): List<SManga> {
        val url = "$baseUrl/api/v1/series?search=$query&unpaged=true&deleted=false".toHttpUrlOrNull()!!.newBuilder()
        val response = client.newCall(GET(url.toString(), headers)).await()
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        return responseBody.use { body ->
            with(json.decodeFromString<KomgaPageWrapperDto<KomgaSeriesDto>>(body.string())) {
                content.map { series ->
                    SManga.create().apply {
                        this.title = series.metadata.title
                        this.url = "$baseUrl/api/v1/series/${series.id}"
                        this.thumbnail_url = "${this.url}/thumbnail"
                    }
                }
            }
        }
    }

    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET("$baseUrl${chapter.url}", headers)).await()
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        val pages = responseBody.use { body -> json.decodeFromString<List<KomgaPageDto>>(body.string()) }
        return pages.map {
            val url = "${response.request.url}/${it.number}" +
                if (!supportedImageTypes.contains(it.mediaType)) {
                    "?convert=png"
                } else {
                    ""
                }
            Page(
                index = it.number - 1,
                imageUrl = url,
            )
        }
    }

    companion object {
        private val supportedImageTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/jxl")
    }
}


