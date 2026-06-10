package eu.kanade.tachiyomi.source.online.merged.comix

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.text.DecimalFormat
import java.util.Calendar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nekomanga.constants.Constants

@Serializable
class Manga(
    @SerialName("hid") private val hashId: String,
    private val title: String,
    private val poster: Poster? = null,
    private val url: String? = null,
) {
    @Serializable
    class Poster(
        private val small: String? = null,
        private val medium: String? = null,
        private val large: String? = null,
    ) {
        fun from(quality: String? = "large") =
            when (quality) {
                "large" -> large ?: medium ?: small ?: ""
                "small" -> small ?: medium ?: large ?: ""
                else -> medium ?: large ?: small ?: ""
            }
    }

    fun toSManga() =
        SManga.create().apply {
            url = this@Manga.url?.substringAfter("/title") ?: "/$hashId"
            title = this@Manga.title
            thumbnail_url = this@Manga.poster?.from("large")
        }
}

@Serializable
class Meta(
    val page: Int = 1,
    val lastPage: Int = 1,
    val hasNext: Boolean = false,
)

@Serializable
class Pagination(
    val page: Int = 1,
    val lastPage: Int = 1,
)

@Serializable
class SearchResponse(
    val result: Items<Manga>,
)

@Serializable
class Items<T>(
    val items: List<T> = emptyList(),
    val meta: Meta? = null,
    private val pagination: Pagination? = null,
) {
    fun hasNextPage(): Boolean = when {
        meta != null -> meta.page < meta.lastPage
        pagination != null -> pagination.page < pagination.lastPage
        else -> false
    }
}

@Serializable
class Chapter(
    private val id: Int,
    val group: Group? = null,
    val url: String = "",
    val number: Double,
    private val name: String = "",
    val createdAtFormatted: String = "",
    val isOfficial: Boolean = false,
) {
    val createdAt: Long
        get() {
            if (createdAtFormatted.isEmpty()) return 0L
            val trimmed = createdAtFormatted.trim().lowercase().removeSuffix(" ago")
            val match = createdAtRegex.find(trimmed) ?: return 0L
            val amount = match.groupValues[1].toIntOrNull() ?: return 0L
            val unit = match.groupValues[2]
            val calendar = Calendar.getInstance()
            when (unit) {
                "s",
                "sec",
                "secs" -> calendar.add(Calendar.SECOND, -amount)
                "m",
                "min",
                "mins" -> calendar.add(Calendar.MINUTE, -amount)
                "h",
                "hr",
                "hrs" -> calendar.add(Calendar.HOUR_OF_DAY, -amount)
                "d",
                "day",
                "days" -> calendar.add(Calendar.DAY_OF_YEAR, -amount)
                "w",
                "week",
                "weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, -amount)
                "mo",
                "mos",
                "month",
                "months" -> calendar.add(Calendar.MONTH, -amount)
                "y",
                "yr",
                "yrs",
                "year",
                "years" -> calendar.add(Calendar.YEAR, -amount)
            }
            return calendar.timeInMillis
        }

    fun toSChapter(mangaSlug: String) =
        SChapter.create().apply {
            url = this@Chapter.url.indexOf("/title/").let { index ->
                if (index != -1) {
                    this@Chapter.url.substring(index + 1)
                } else {
                    "title/$mangaSlug/$id-chapter-${this@Chapter.number.toString().removeSuffix(".0")}"
                }
            }
            val chapterText = "Ch." + DecimalFormat("0.#").format(this@Chapter.number)
            chapter_txt = chapterText
            name = buildString {
                append(chapterText)
                this@Chapter.name.takeUnless { it.isEmpty() }?.let { append(" - $it") }
            }
            date_upload = this@Chapter.createdAt
            chapter_number = this@Chapter.number.toFloat()

            val scanlatorList = mutableListOf(Comix.name)
            val scanGroup = this@Chapter.group
            if (scanGroup != null) {
                // treat thinks they think might be Official as official
                if (scanGroup.name == "Official?") {
                    scanlatorList.add("Official")
                } else {
                    scanlatorList.add(scanGroup.name)
                }
            } else if (this@Chapter.isOfficial) {
                scanlatorList.add("Official")
            } else {
                scanlatorList.add("Unknown")
            }

            scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
        }

    companion object {
        val createdAtRegex =
            Regex(
                """^(\d+)\s*(s|m|h|d|w|mo|mos|y|yr|yrs|min|mins|sec|secs|hr|hrs|day|days|week|weeks|month|months|year|years)$"""
            )
    }
}

@Serializable class Group(val id: Int, val name: String)

@Serializable
class ChapterResponse(val result: ChapterResult? = null) {
    @Serializable
    class ChapterResult(
        val pages: Pages,
    )

    @Serializable
    class Pages(
        val baseUrl: String,
        val items: List<PageDto>,
    )

    @Serializable
    class PageDto(
        val url: String,
        val s: Int = 0,
    )
}
