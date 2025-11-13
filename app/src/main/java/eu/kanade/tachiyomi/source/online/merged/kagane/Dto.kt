package eu.kanade.tachiyomi.extension.en.kagane

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.merged.kagane.Kagane
import eu.kanade.tachiyomi.source.online.merged.kagane.normalizeTitle
import eu.kanade.tachiyomi.util.system.tryParse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nekomanga.constants.Constants

@Serializable
class SearchDto(val content: List<Book>, val last: Boolean) {
    fun hasNextPage() = !last

    @Serializable
    class Book(val name: String, val id: String) {

        fun toSManga(domain: String): SManga =
            SManga.create().apply {
                title = name
                url = id
                thumbnail_url = "$domain/api/v1/series/$id/thumbnail"
            }
    }
}

@Serializable class DetailsDto(val source: String)

@Serializable
class ChapterDto(val content: List<Book>) {
    @Serializable
    class Book(
        val id: String,
        @SerialName("series_id") val seriesId: String,
        val title: String,
        @SerialName("release_date") val releaseDate: String?,
        @SerialName("pages_count") val pagesCount: Int,
        @SerialName("number_sort") val number: Float,
    ) {
        fun toSChapter(useSourceChapterNumber: Boolean = false, source: String): SChapter =
            SChapter.create().apply {
                url = "$seriesId;$id;$pagesCount"

                val normalized = normalizeTitle(title)
                vol = normalized.first ?: ""
                chapter_txt = normalized.second ?: normalized.third
                name = normalized.third

                date_upload = dateFormat.tryParse(releaseDate)
                if (useSourceChapterNumber) {
                    chapter_number = number
                }

                val scanlatorList = listOf(Kagane.name) + listOf(source)
                scanlator = scanlatorList.joinToString(Constants.SCANLATOR_SEPARATOR)
            }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    }
}

@Serializable
class ChallengeDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("cache_url") val cacheUrl: String,
)
