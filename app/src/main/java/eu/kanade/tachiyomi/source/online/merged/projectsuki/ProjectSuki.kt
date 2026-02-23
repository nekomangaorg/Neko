package eu.kanade.tachiyomi.source.online.merged.projectsuki

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.asJsoup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.nekomanga.constants.Constants
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await

class ProjectSuki : ReducedHttpSource() {
    override val name = ProjectSuki.name
    override val baseUrl = ProjectSuki.baseUrl
    override val headers: Headers = Headers.Builder().add("Referer", "$baseUrl/").build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }

    override suspend fun searchManga(query: String): List<SManga> {
        val apiUrl = "$baseUrl/api/book/search"
        val newHeaders =
            headers
                .newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Content-Type", "application/json;charset=UTF-8")
                .add("Referer", "$baseUrl/browse")
                .build()

        val body =
            json
                .encodeToString(SearchRequestData(null))
                .toRequestBody("application/json;charset=UTF-8".toMediaType())

        val request = POST(apiUrl, newHeaders, body)
        val response = client.newCall(request).await()

        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        val data = parseBookSearchResponse(response)
        return simpleSearchMangasPage(data, query)
    }

    override suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError> {
        return try {
            val response = client.newCall(GET(baseUrl + mangaUrl, headers)).await()
            if (!response.isSuccessful) {
                response.close()
                return Err(ResultError.HttpError(response.code, "HTTP ${response.code}"))
            }

            val document = response.asJsoup()
            val chapters = parseChapters(document)
            Ok(chapters.map { it to false })
        } catch (e: Exception) {
            Err(ResultError.Generic(e.message ?: "Unknown error"))
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val chapterUrl =
            (baseUrl + chapter.url).toHttpUrlOrNull() ?: throw Exception("Invalid chapter URL")

        // Parse bookid and chapterid from URL
        // pattern: /read/<bookid>/<chapterid>/<startpage>
        val pathSegments = chapterUrl.pathSegments
        // expected: ["read", bookid, chapterid, startpage]
        if (pathSegments.size < 3 || pathSegments[0] != "read") {
            throw Exception("Invalid chapter URL format")
        }
        val bookId = pathSegments[1]
        val chapterId = pathSegments[2]

        val apiUrl = "$baseUrl/callpage"
        val newHeaders =
            headers
                .newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Content-Type", "application/json;charset=UTF-8")
                .build()

        val body =
            json
                .encodeToString(PagesRequestData(bookId, chapterId, "true"))
                .toRequestBody("application/json;charset=UTF-8".toMediaType())

        val response = client.newCall(POST(apiUrl, newHeaders, body)).await()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }

        return parseChapterPagesResponse(response)
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return baseUrl + simpleChapter.url
    }

    // --- Helper Methods & DTOs ---

    @Serializable data class SearchRequestData(@SerialName("hash") val hash: String?)

    @Serializable
    data class PagesRequestData(
        @SerialName("bookid") val bookID: String,
        @SerialName("chapterid") val chapterID: String,
        @SerialName("first") val first: String,
    )

    private fun parseBookSearchResponse(response: Response): Map<String, String> {
        val responseBody = response.body?.string() ?: return emptyMap()
        val jsonObj = json.decodeFromString<JsonObject>(responseBody)
        val data = jsonObj["data"]?.jsonObject ?: return emptyMap()

        return data.entries.associate { (id, element) ->
            val title = element.jsonObject["value"]?.jsonPrimitive?.contentOrNull ?: ""
            id to title
        }
    }

    private fun simpleSearchMangasPage(data: Map<String, String>, query: String): List<SManga> {
        val words = query.lowercase(Locale.US).split(Regex("\\s+"))

        return data.entries
            .mapNotNull { (id, title) ->
                var count = 0
                val lowerTitle = title.lowercase(Locale.US)
                for (word in words) {
                    if (lowerTitle.contains(word)) {
                        count++
                    }
                }
                if (count > 0) Triple(id, title, count) else null
            }
            .sortedWith(
                compareByDescending<Triple<String, String, Int>> { it.third }.thenBy { it.second }
            )
            .map { (id, title, _) ->
                SManga.create().apply {
                    this.url = "/book/$id"
                    this.title = title
                    this.thumbnail_url = "$baseUrl/images/gallery/$id/thumb"
                }
            }
    }

    private fun parseChapterPagesResponse(response: Response): List<Page> {
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        val jsonObj = json.decodeFromString<JsonObject>(responseBody)
        val rawSrc =
            jsonObj["src"]?.jsonPrimitive?.contentOrNull
                ?: throw Exception("Chapter pages not found")

        val document = Jsoup.parseBodyFragment(rawSrc, baseUrl)
        val images = document.select("img")

        return images.mapIndexedNotNull { index, img ->
            val url =
                img.attr("abs:src").takeIf { it.isNotBlank() }
                    ?: img.attr("abs:data-src").takeIf { it.isNotBlank() }
                    ?: img.attr("abs:data-lazy-src").takeIf { it.isNotBlank() }

            if (url != null) {
                Page(index, imageUrl = url)
            } else {
                null
            }
        }
    }

    private fun parseChapters(document: Document): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        val tables = document.select("table")

        val regex = Regex("""Chapter\s*(\d+)\s*[-_]?\s*(.*)""", RegexOption.IGNORE_CASE)

        for (table in tables) {
            val headers =
                table.select("thead tr th, thead tr td").map { it.text().lowercase(Locale.US) }
            val chapterIdx = headers.indexOfFirst { it.contains("chapter") }
            val groupIdx = headers.indexOfFirst { it.contains("group") }
            val dateIdx = headers.indexOfFirst { it.contains("date") || it.contains("added") }

            if (chapterIdx == -1) continue

            val rows = table.select("tbody tr")
            for (row in rows) {
                val cols = row.children()
                if (cols.size <= chapterIdx) continue

                val chapterCol = cols[chapterIdx]
                val link = chapterCol.selectFirst("a") ?: continue
                val url = link.attr("abs:href")

                val (chpText, chpName) =
                    regex.find(link.text())?.destructured?.let { (num, desc) ->
                        val short = "Ch.$num"
                        val full = if (desc.isNotBlank()) "$short - ${desc.trim()}" else short
                        short to full
                    } ?: ("" to link.text())

                // Date parsing
                val dateText =
                    if (dateIdx != -1 && cols.size > dateIdx) cols[dateIdx].text() else ""
                val date = parseDate(dateText)

                val scanlatorList = mutableListOf(ProjectSuki.name)
                if (groupIdx != -1 && cols.size > groupIdx) {
                    scanlatorList.add(cols[groupIdx].text())
                }

                val chapter =
                    SChapter.create().apply {
                        this.url = url.replace(baseUrl, "")
                        this.name = chpName
                        this.chapter_txt = chpText
                        this.date_upload = date
                        this.scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
                    }
                chapters.add(chapter)
            }
        }
        return chapters
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            if (dateStr.contains("ago", ignoreCase = true)) {
                val number = Regex("""(\d+)""").find(dateStr)?.groupValues?.get(1)?.toInt() ?: 0
                val cal = Calendar.getInstance()
                when {
                    dateStr.contains("year", ignoreCase = true) -> cal.add(Calendar.YEAR, -number)
                    dateStr.contains("month", ignoreCase = true) -> cal.add(Calendar.MONTH, -number)
                    dateStr.contains("week", ignoreCase = true) ->
                        cal.add(Calendar.WEEK_OF_YEAR, -number)
                    dateStr.contains("day", ignoreCase = true) ->
                        cal.add(Calendar.DAY_OF_YEAR, -number)
                    dateStr.contains("hour", ignoreCase = true) -> cal.add(Calendar.HOUR, -number)
                    dateStr.contains("min", ignoreCase = true) -> cal.add(Calendar.MINUTE, -number)
                    dateStr.contains("sec", ignoreCase = true) -> cal.add(Calendar.SECOND, -number)
                }
                cal.timeInMillis
            } else {
                SimpleDateFormat("MMMM dd, yyyy", Locale.US).parse(dateStr)?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        const val name = "Project Suki"
        const val baseUrl = "https://projectsuki.com"
    }
}
