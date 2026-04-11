package eu.kanade.tachiyomi.source.online.merged.atsumaru.dto

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.merged.atsumaru.Atsumaru
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.nekomanga.constants.Constants

@Serializable class BrowseMangaDto(val items: List<MangaDto>)

@Serializable class MangaObjectDto(val mangaPage: MangaDto)

@Serializable
class SearchResultsDto(
    val page: Int,
    val found: Int,
    val hits: List<SearchMangaDto>,
    @SerialName("request_params") val requestParams: RequestParamsDto,
) {
    fun hasNextPage(): Boolean = page * requestParams.perPage < found

    @Serializable class SearchMangaDto(val document: MangaDto)

    @Serializable class RequestParamsDto(@SerialName("per_page") val perPage: Int)
}

@Serializable
class MangaDto(
    // Common
    private val id: String,
    private val title: String,
    @JsonNames("poster", "image") private val imagePath: JsonElement,

    // Details
    private val authors: List<AuthorDto>? = null,
    private val synopsis: String? = null,
    private val tags: List<TagDto>? = null,
    private val status: String? = null,
    private val type: String? = null,
    val scanlators: List<ScanlatorDto>? = null,

    // Chapters
    val chapters: List<ChapterDto>? = null,
) {
    private fun getImagePath(): String? {
        val url =
            when (imagePath) {
                is JsonPrimitive -> imagePath.content
                is JsonObject -> imagePath["image"]?.jsonPrimitive?.content
                else -> null
            }
        return url?.removePrefix("/")?.removePrefix("static/")
    }

    fun toSManga(baseUrl: String): SManga =
        SManga.create().apply {
            url = id
            title = this@MangaDto.title
            thumbnail_url =
                getImagePath()?.let {
                    val url =
                        when {
                            it.startsWith("http") -> it
                            it.startsWith("//") -> "https:$it"
                            else -> "$baseUrl/static/$it"
                        }
                    url.replaceFirst(Regex("^https?:?//"), "https://")
                }
            description = synopsis
            genre =
                buildList {
                        type?.let { add(it) }
                        tags?.forEach { add(it.name) }
                    }
                    .joinToString()

            authors?.let { author = it.joinToString { author -> author.name } }

            this@MangaDto.status?.let {
                status =
                    when (it.lowercase().trim()) {
                        "ongoing" -> SManga.ONGOING
                        "completed" -> SManga.COMPLETED
                        "hiatus" -> SManga.HIATUS
                        "canceled" -> SManga.CANCELLED
                        else -> SManga.UNKNOWN
                    }
            }
        }

    @Serializable class TagDto(val name: String)

    @Serializable class AuthorDto(val name: String)

    @Serializable class ScanlatorDto(val id: String, val name: String)
}

@Serializable class AllChaptersDto(val chapters: List<ChapterDto>)

@Serializable
class ChapterDto(
    val id: String,
    private val number: Float,
    private val title: String,
    val scanlationMangaId: String? = null,
    @SerialName("createdAt") private val date: JsonElement? = null,
) {
    fun toSChapter(slug: String, scanlatorName: String? = null): SChapter {

        val parsedName = parseTitle(title)

        val scanlatorList = mutableListOf(Atsumaru.name)
        if (scanlatorName != null && !scanlatorName.equals("alpha", true)) {
            scanlatorList.add(scanlatorName)
        }
        if (parsedName.tag != null) {
            scanlatorList.add(parsedName.tag)
        }

        return SChapter.create().apply {
            url = "$slug/$id"
            chapter_number = number
            chapter_txt = parsedName.chapterNum ?: ""
            name = parsedName.formattedName
            scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
            date?.let { date_upload = parseDate(it) }
        }
    }

    data class ParsedChapter(
        val tag: String?,        // Holds "Official Scans", "Redraw", or null
        val chapterNum: String?, // Holds strictly the "Ch.X" string
        val formattedName: String // Holds the full "Ch.X - Title" string
    )

    fun parseTitle(rawTitle: String): ParsedChapter {
        // Regex remains the same
        val regex = Regex("^(\\D*?)\\s*(\\d+(?:\\.\\d+)?)\\s*(.*)$")
        val match = regex.find(rawTitle)

        if (match != null) {
            val prefix = match.groupValues[1].trim()
            var number = match.groupValues[2]
            val suffix = match.groupValues[3].trim()

            // 1. Clean the number
            if (number.contains(".")) {
                number = number.trimEnd('0').trimEnd('.')
            }

            // 2. Check for "Official" or "Redraw"
            var tagString: String? = null
            val lowerPrefix = prefix.lowercase()
            if (lowerPrefix.contains("official") || lowerPrefix.contains("redraw")) {
                tagString = prefix
            }

            // 3. Create the standalone chapter string
            val chapterString = "Ch.$number"

            // 4. Build the full formatted name
            val chapterName = mutableListOf<String>()
            chapterName.add(chapterString)

            if (suffix.isNotBlank()) {
                chapterName.add("-")
                chapterName.add(suffix)
            }

            return ParsedChapter(
                tag = tagString,
                chapterNum = chapterString, // The new field is populated here
                formattedName = chapterName.joinToString(" ")
            )
        }

        // Fallback: If no number is found, tag and chapterNum remain null
        return ParsedChapter(null, null, rawTitle)
    }

    private fun parseDate(dateElement: JsonElement): Long =
        when (dateElement) {
            is JsonPrimitive -> {
                dateElement.longOrNull ?: tryParse(dateElement.content.replace("T ", "T"))
            }
            else -> 0L
        }

    private fun tryParse(dateStr: String): Long {
        return try {
            DATE_FORMAT.get()?.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        private val DATE_FORMAT: ThreadLocal<SimpleDateFormat> =
            object : ThreadLocal<SimpleDateFormat>() {
                override fun initialValue(): SimpleDateFormat {
                    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
            }
    }
}

@Serializable class PageObjectDto(val readChapter: PageDto)

@Serializable class PageDto(val pages: List<PageDataDto>)

@Serializable class PageDataDto(val image: String)

@Serializable internal class SearchRequest(val page: Int, val filter: SearchFilter)

@Serializable
internal class SearchFilter(
    val search: String? = null,
    val types: List<String>,
    val status: List<String>? = null,
    val includedTags: List<String>? = null,
    val year: Int? = null,
    val minChapters: Int? = null,
    val showAdult: Boolean = false,
    val officialTranslation: Boolean = false,
    val sortBy: String? = null,
)
