package eu.kanade.tachiyomi.source.online.merged.mangaball

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.system.tryParse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.nodes.Document
import org.nekomanga.constants.Constants
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MangaBall : ReducedHttpSource() {

    override val name = MangaBall.name
    override val baseUrl = MangaBall.baseUrl

    private val json: Json by injectLazy()

    override val headers: Headers = Headers.Builder().apply { add("Referer", "$baseUrl/") }.build()

    override val client =
        network.cloudFlareClient
            .newBuilder()
            .addInterceptor { chain ->
                var request = chain.request()
                if (request.url.pathSegments[0] == "api") {
                    request =
                        request
                            .newBuilder()
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("X-CSRF-TOKEN", getCSRF())
                            .build()

                    val response = chain.proceed(request)
                    if (!response.isSuccessful && response.code == 403) {
                        response.close()
                        request =
                            request
                                .newBuilder()
                                .header("X-CSRF-TOKEN", getCSRF(forceReset = true))
                                .build()

                        chain.proceed(request)
                    } else {
                        response
                    }
                } else {
                    chain.proceed(request)
                }
            }
            .build()

    private var _csrf: String? = null

    @Synchronized
    private fun getCSRF(document: Document? = null, forceReset: Boolean = false): String {
        if (_csrf == null || document != null || forceReset) {
            val doc = document ?: client.newCall(GET(baseUrl, headers)).execute().asJsoup()

            doc.selectFirst("meta[name=csrf-token]")
                ?.attr("content")
                ?.takeIf { it.isNotBlank() }
                ?.also { _csrf = it }
        }

        return _csrf ?: throw Exception("CSRF token not found")
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val body =
            FormBody.Builder()
                .apply {
                    add("search_input", query.trim())
                    add("filters[translatedLanguage][]", "en")
                }
                .build()

        val response =
            client.newCall(POST("$baseUrl/api/v1/title/search-advanced/", headers, body)).await()
        return parseSearchManga(response)
    }

    private fun parseSearchManga(response: Response): List<SManga> {
        val data = with(json) { response.parseAs<SearchResponse>() }

        val mangaList =
            data.data.map {
                SManga.create().apply {
                    url = it.url.toHttpUrl().pathSegments[1]
                    title = it.name
                    thumbnail_url = it.cover
                }
            }
        return mangaList
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        val id = mangaUrl.substringAfterLast("-")
        val body = FormBody.Builder().add("title_id", id).build()

        val response =
            client
                .newCall(
                    POST("$baseUrl/api/v1/chapter/chapter-listing-by-title-id/", headers, body)
                )
                .await()

        return parseChapters(response)
    }

    private fun parseChapters(response: Response): Result<List<SChapterStatusPair>, ResultError> {

        val data = with(json) { response.parseAs<ChapterListResponse>() }

        val chapters =
            data.chapters.flatMap { chapter ->
                chapter.translations.mapNotNull { translation ->
                    if (translation.language == "en") {
                        SChapter.create().apply {
                            url = translation.id
                            val chapterName = mutableListOf<String>()
                            if (translation.volume > 0) {
                                val volume = "Vol.${translation.volume}"
                                vol = volume
                                chapterName.add(volume)
                            }

                            val number = chapter.number.toString().removeSuffix(".0")

                            val chapterNum = "Ch.$number"

                            chapterName.add(chapterNum)

                            chapterName.add(translation.name.trim())

                            name = chapterName.joinToString(" ")

                            chapter_number = chapter.number
                            date_upload = dateFormat.tryParse(translation.date)
                            val scanlatorList = mutableListOf(MangaBall.name)
                            scanlatorList.add(translation.group.name)
                            if (groupIdRegex.matchEntire(translation.group.id) == null) {
                                scanlatorList.add("(${translation.group.id})")
                            }

                            scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
                        }
                    } else {
                        null
                    }
                }
            }

        return Ok(chapters.map { it to false })
    }

    private val groupIdRegex = Regex("""[a-z0-9]{24}""")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return getChapterUrl(simpleChapter.url)
    }

    private fun getChapterUrl(url: String): String {
        return "$baseUrl/chapter-detail/$url/"
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET(getChapterUrl(chapter.url), headers)).await()
        val document = response.asJsoup()
        response.closeQuietly()
        getCSRF(document)

        val script =
            document.select("script:containsData(chapterImages)").joinToString(";") { it.data() }
        val images =
            with(json) {
                imagesRegex.find(script)?.groupValues?.get(1)?.parseAs<List<String>>().orEmpty()
            }

        return images.mapIndexed { idx, img -> Page(idx, imageUrl = img) }
    }

    private val imagesRegex = Regex("""const\s+chapterImages\s*=\s*JSON\.parse\(`([^`]+)`\)""")

    companion object {
        const val name = "Manga Ball"
        const val baseUrl = "https://mangaball.net"
    }
}
