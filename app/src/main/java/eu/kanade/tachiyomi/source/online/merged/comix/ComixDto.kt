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
    private val poster: Poster,
) {
    @Serializable
    class Poster(
        private val small: String? = null,
        private val medium: String,
        private val large: String,
    ) {
        fun from(quality: String? = "large") =
            when (quality) {
                "large" -> large
                "small" -> small
                else -> medium
            }
    }

    fun toSManga() =
        SManga.create().apply {
            url = "/$hashId"
            title = this@Manga.title
            thumbnail_url = this@Manga.poster.from("large")
        }
}

@Serializable class Meta(val page: Int, val lastPage: Int)

@Serializable class SearchResponse(val result: Items<Manga>)

@Serializable class ChapterDetailsResponse(val result: Items<Chapter>)

@Serializable class Items<T>(val items: List<T>, val meta: Meta)

@Serializable
class Chapter(
    private val id: Int,
    val group: Group?,
    val number: Double,
    private val name: String,
    val createdAtFormatted: String,
    val isOfficial: Boolean,
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
            return calendar.timeInMillis / 1000
        }

    fun toSChapter(mangaId: String) =
        SChapter.create().apply {
            url = "title/$mangaId/$id"
            val chapterText = "Ch." + DecimalFormat("0.#").format(this@Chapter.number)
            chapter_txt = chapterText
            name = buildString {
                append(chapterText)
                this@Chapter.name.takeUnless { it.isEmpty() }?.let { append("- $it") }
            }
            date_upload = this@Chapter.createdAt * 1000
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
class ChapterResponse(val result: Items?) {
    @Serializable class Items(val id: Int, val pages: List<Page>)

    @Serializable class Page(val url: String)
}
