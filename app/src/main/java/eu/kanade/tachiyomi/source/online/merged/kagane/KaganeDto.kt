package eu.kanade.tachiyomi.source.online.merged.kagane

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.tryParse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
class GenreDto(
    val id: String,
    @SerialName("genre_name") val genreName: String,
)

@Serializable
class TagDto(
    val id: String,
    @SerialName("tag_name") val tagName: String,
)

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
    @SerialName("total_elements") val totalElements: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
) {
    @Serializable
    class SearchBook(
        @SerialName("series_id") val id: String,
        val title: String,
        @SerialName("source_id") val sourceId: String? = null,
        @SerialName("current_books") val booksCount: Int = 0,
        @SerialName("start_year") val startYear: Int? = null,
        @SerialName("cover_image_id") val coverImage: String? = null,
        @SerialName("alternate_titles") val alternateTitles: List<String> = emptyList(),
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
                lang_flag = KaganeLang.fromKaganeLang(lang ?: "en")
                thumbnail_url = coverImage?.let { "$apiUrl/image/$it" }
            }
    }
}

@Serializable
class DetailsDto(
    val title: String,
    val description: String?,
    @SerialName("upload_status") val publicationStatus: String,
    @SerialName("translated_language") val translatedLanguage: String? = null,
    val format: String?,
    @SerialName("source_id") val sourceId: String?,
    @SerialName("series_staff") val seriesStaff: List<SeriesStaff> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val tags: List<Tag> = emptyList(),
    @SerialName("series_alternate_titles")
    val seriesAlternateTitles: List<AlternateTitle> = emptyList(),
    @SerialName("series_books") val seriesBooks: List<ChapterBook> = emptyList(),
    @SerialName("edition_info") val editionInfo: String? = null,
    @SerialName("tracker_id") val trackerId: String? = null,
    @SerialName("series_covers") val covers: List<SeriesCover> = emptyList(),
) {
    @Serializable class SeriesStaff(val name: String, val role: String)

    @Serializable class Genre(@SerialName("genre_name") val genreName: String)

    @Serializable class Tag(@SerialName("tag_name") val tagName: String)

    @Serializable class AlternateTitle(val title: String, val label: String? = null)

    @Serializable class SeriesCover(@SerialName("image_id") val imageId: String)

    fun toSManga(apiUrl: String, sourceName: String? = null, baseUrl: String = ""): SManga =
        SManga.create().apply {
            title = this@DetailsDto.title.trim()
            thumbnail_url = covers.firstOrNull()?.imageId?.let { "$apiUrl/image/$it" }
            val desc = StringBuilder()
            this@DetailsDto.description
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    desc.append(Jsoup.parse(it.trim().replace("\n", "<br>")).wholeText())
                    desc.append("\n")
                }
            if (sourceName != null && this@DetailsDto.sourceId != null) {
                if (desc.isNotEmpty()) desc.append("\n")
                desc.append("Source: [$sourceName]($baseUrl/sources/${this@DetailsDto.sourceId})\n")
            }
            if (seriesAlternateTitles.isNotEmpty()) {
                if (desc.isNotEmpty()) desc.append("\n")
                desc.append("Associated Name(s):\n")
                seriesAlternateTitles.forEach { desc.append("• ${it.title}\n") }
            }
            val authors =
                seriesStaff
                    .filter {
                        it.role.contains("Author", ignoreCase = true) ||
                            it.role.contains("Story", ignoreCase = true)
                    }
                    .map { it.name }
                    .distinct()
            val artists =
                seriesStaff
                    .filter {
                        it.role.contains("Artist", ignoreCase = true) ||
                            it.role.contains("Art", ignoreCase = true)
                    }
                    .map { it.name }
                    .distinct()
                    .joinToString(", ")
            artist = artists
            author = authors.joinToString()
            description = desc.toString().trim()
            genre =
                buildList {
                    this@DetailsDto.format?.takeIf { it.isNotBlank() }?.let { add(it) }
                    addAll(genres.map { it.genreName })
                }
                    .joinToString()
            status = this@DetailsDto.publicationStatus.toStatus()
        }

    private fun String.toStatus(): Int =
        when (this.uppercase()) {
            "ONGOING" -> SManga.ONGOING
            "COMPLETED" -> SManga.COMPLETED
            "HIATUS" -> SManga.HIATUS
            "ABANDONED" -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
}

@Serializable
class ChapterBook(
    @SerialName("book_id") val id: String,
    @SerialName("series_id") val seriesId: String? = null,
    val title: String,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("page_count") val pagesCount: Int = 0,
    @SerialName("sort_no") val number: Float = 0f,
    @SerialName("chapter_no") val chapterNo: String?,
    @SerialName("volume_no") val volumeNo: String?,
    val groups: List<ChapterGroup> = emptyList(),
) {
    fun toSChapter(actualSeriesId: String, sourceName: String, language: String?): SChapter =
        SChapter.create().apply {
            url = "/series/$actualSeriesId/reader/$id"
            name = buildChapterName()
            date_upload = dateFormat.tryParse(createdAt)
            scanlator =
                (listOf(sourceName) + groups.map { it.title })
                    .filter { it.isNotBlank() }
                    .joinToString(org.nekomanga.constants.Constants.SCANLATOR_SEPARATOR)
            this.language = language
        }

    private fun buildChapterName(): String {
        val trimmedTitle = title.trim()
        return when {
            trimmedTitle.isEmpty() && chapterNo.isNullOrBlank() && !volumeNo.isNullOrBlank() ->
                "Vol.$volumeNo"
            trimmedTitle.isEmpty() && !chapterNo.isNullOrBlank() -> "Ch.$chapterNo"
            else -> trimmedTitle
        }
    }

    @Serializable class ChapterGroup(val title: String)

    companion object {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
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
