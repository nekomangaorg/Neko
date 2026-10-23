package eu.kanade.tachiyomi.source.online.merged.kagane

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.formatChapterNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.nekomanga.constants.Constants
import org.nekomanga.logging.TimberKt

@Serializable class SourcesDto(val sources: List<SourceDto>)

@Serializable
data class SourceDto(
    @SerialName("source_id") val sourceId: String,
    @SerialName("source_type") val sourceType: String,
    val title: String,
)

@Serializable
class SearchDto(
    val content: List<SearchBook> = emptyList(),
    val last: Boolean = true,
) {
    @Serializable
    class SearchBook(
        @SerialName("series_id") val id: String,
        val title: String,
        @SerialName("source_id") val sourceId: String? = null,
        @SerialName("cover_image_id") val coverImage: String? = null,
        @SerialName("translated_language") val translatedLanguage: String? = null,
    ) {
        fun toSManga(apiUrl: String, sourceName: String?): SManga =
            SManga.create().apply {
                val lang = translatedLanguage?.takeIf { it.isNotBlank() }
                title =
                    buildString {
                        if (!sourceName.isNullOrBlank()) append("[$sourceName] ")
                        append(this@SearchBook.title.trim())
                    }
                        .trim()
                url = id
                lang_flag = lang?.let { KaganeLang.fromKaganeLang(it) }
                thumbnail_url = coverImage?.let { "$apiUrl/image/$it" }
            }
    }
}

@Serializable
class DetailsDto(
    val title: String,
    @SerialName("translated_language") val translatedLanguage: String? = null,
    @SerialName("source_id") val sourceId: String?,
)

@Serializable
class BookIndexDto(
    val count: Int,
    val fields: List<String>,
    val groups: List<ChapterBook.ChapterGroup>,
    val books: List<List<JsonElement>>,
) {
    fun toBooks(): List<ChapterBook> {
        // A partial chapter list would make the chapter sync delete the missing chapters.
        check(books.size == count) { "Incomplete Kagane chapter index" }
        // Each row holds one value per entry of fields, in the same order.
        return books.map { row ->
            val values = fields.zip(row).toMap()
            ChapterBook(
                id = checkNotNull(values.string("book_id")) { "Kagane book without book_id" },
                title = values.string("title").orEmpty(),
                releasedAt = values["released_at"]?.jsonPrimitive?.longOrNull,
                number = values["sort_no"]?.jsonPrimitive?.floatOrNull ?: 0f,
                chapterNo = values.string("chapter_no"),
                volumeNo = values.string("volume_no"),
                groups =
                    (values["group_ix"] as? JsonArray).orEmpty().mapNotNull { index ->
                        index.jsonPrimitive.intOrNull?.let { groups.getOrNull(it) }
                    },
            )
        }
    }

    private fun Map<String, JsonElement>.string(field: String): String? =
        this[field]?.jsonPrimitive?.contentOrNull
}

class ChapterBook(
    val id: String,
    val title: String,
    val releasedAt: Long?,
    val number: Float = 0f,
    val chapterNo: String?,
    val volumeNo: String?,
    val groups: List<ChapterGroup> = emptyList(),
) {
    fun toSChapter(
        actualSeriesId: String,
        sourceName: String,
        scanlatorSource: String?,
        language: String?,
    ): SChapter {
        // A "Chapter X [- Volume Y]" title, when present, is the source of truth.
        // Else, use the value parsed from "chapter_no" (that may contain letters).
        // Or "sort_no" as a hail mary lol
        val parsedTitle = getParsedTitle()
        TimberKt.d { "$parsedTitle" }
        val chnum = parsedTitle?.first ?: getChapterNumber() ?: number
        val chtxt = "Ch.${chnum.formatChapterNumber()}"
        val vol = parsedTitle?.second ?: volumeNo.orEmpty()
        val name = mutableListOf<String>()
        if (vol.isNotBlank()) {
            name.add("Vol.$vol")
        }
        name.add(chtxt)
        if (parsedTitle != null) {
            val rest = parsedTitle.third.trim()
            if (rest.isNotEmpty()) {
                name.add("-")
                name.add(rest)
            }
        } else if (title.isNotEmpty()) {
            name.add("-")
            name.add(title)
        }
        TimberKt.d { "$chnum | $chtxt | ${name.joinToString(" ")} | $title" }
        return SChapter.create().apply {
            url = "/series/$actualSeriesId/reader/$id"
            this.vol = vol
            chapter_number = chnum
            chapter_title = parsedTitle?.third?.trim() ?: title
            chapter_txt = chtxt
            this.name = name.joinToString(" ")
            date_upload = releasedAt?.let { it * 1000 } ?: 0L
            scanlator =
                (listOfNotNull(sourceName, scanlatorSource) + groups.map { it.title })
                    .filter { it.isNotBlank() }
                    .joinToString(Constants.SCANLATOR_SEPARATOR)
            this.language = language
        }
    }

    private fun getChapterNumber(): Float? {
        val raw = chapterNo?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = CHAPTER_NO_REGEX.find(raw) ?: return null
        return match.groupValues[1].toFloatOrNull()
    }

    private fun getParsedTitle(): Triple<Float, String?, String>? {
        val match = TITLE_NUMBER_REGEX.find(title.trim()) ?: return null
        val chnum = match.groupValues[1].toFloatOrNull() ?: return null
        return Triple(chnum, match.groupValues[2].takeIf { it.isNotEmpty() }, match.groupValues[3])
    }

    @Serializable class ChapterGroup(val title: String)

    companion object {
        private val CHAPTER_NO_REGEX = Regex("""^(\d+(?:\.\d+)?)""")
        private val TITLE_NUMBER_REGEX =
            Regex(
                """(?i)^ch(?:apter)?\.?\s*(\d+(?:\.\d+)?)\s*(?:-\s*vol(?:ume)?\.?\s*(\d+(?:\.\d+)?))?(.*)$"""
            )
    }
}

@Serializable
class ChallengeDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("cache_url") val cacheUrl: String,
    val manifest: ManifestDto? = null,
)

@Serializable class ManifestDto(val pages: List<PageDto> = emptyList())

@Serializable
class PageDto(
    @SerialName("page_no") val pageNumber: Int,
    @SerialName("page_id") val pageUuid: String,
    val ext: String? = null,
)

@Serializable class IntegrityDto(val token: String, val exp: Long)
