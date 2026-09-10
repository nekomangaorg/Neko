package eu.kanade.tachiyomi.source.online.merged.kagane

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val description: String?,
    @SerialName("translated_language") val translatedLanguage: String? = null,
    val format: String?,
    @SerialName("source_id") val sourceId: String?,
    @SerialName("series_books") val seriesBooks: List<ChapterBook> = emptyList(),
    @SerialName("series_covers") val covers: List<SeriesCover> = emptyList(),
) {

    @Serializable class SeriesCover(@SerialName("image_id") val imageId: String)
}

@Serializable
class ChapterBook(
    @SerialName("book_id") val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("sort_no") val number: Float = 0f,
    @SerialName("chapter_no") val chapterNo: String?,
    @SerialName("volume_no") val volumeNo: String?,
    val groups: List<ChapterGroup> = emptyList(),
) {
    fun toSChapter(actualSeriesId: String, sourceName: String, language: String?): SChapter {
        // A "Chapter X [- Volume Y]" title, when present, is the source of truth.
        // Else, use the value parsed from "chapter_no" (that may contain letters).
        // Or "sort_no" as a hail mary lol
        val parsedTitle = getParsedTitle()
        TimberKt.d { "$parsedTitle" }
        val chnum = parsedTitle?.first ?: getChapterNumber() ?: number
        val chtxt = "Ch.${chnum.formatFloat()}"
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
            date_upload = parseDate(createdAt)
            scanlator =
                (listOf(sourceName) + groups.map { it.title })
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

    fun Float.formatFloat(): String {
        val df = DecimalFormat("#.###")
        df.minimumFractionDigits = 0
        df.maximumFractionDigits = 3
        df.isGroupingUsed = false
        return df.format(this.toBigDecimal().stripTrailingZeros())
    }

    @Serializable class ChapterGroup(val title: String)

    companion object {
        private val CHAPTER_NO_REGEX = Regex("""^(\d+(?:\.\d+)?)""")
        private val TITLE_NUMBER_REGEX =
            Regex(
                """(?i)^ch(?:apter)?\.?\s*(\d+(?:\.\d+)?)\s*(?:-\s*vol(?:ume)?\.?\s*(\d+(?:\.\d+)?))?(.*)$"""
            )

        private val dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)

        fun parseDate(createdAt: String?): Long {
            if (createdAt.isNullOrBlank()) return 0L
            runCatching {
                return OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
            }
            return runCatching {
                    LocalDateTime.parse(createdAt.take(19), dateFormatter)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                }
                .getOrDefault(0L)
        }
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
