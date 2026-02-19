package eu.kanade.tachiyomi.source.online.merged.weebdex.dto

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDexConstants
import eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDexHelper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.parser.Parser

@Serializable
class ChapterListDto(
    val data: List<ChapterDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 0,
    val total: Int = 0,
) {
    val hasNextPage: Boolean
        get() = page * limit < total

    fun toSChapterList(): List<SChapter> {
        val helper = WeebDexHelper()
        return data.map { it.toSChapter(helper) }
    }
}

@Serializable
class ChapterDto(
    val id: String,
    val title: String? = null,
    val chapter: String? = null,
    val volume: String? = null,
    @SerialName("published_at") val publishedAt: String = "",
    val language: String = "",
    val data: List<PageData>? = null,
    @SerialName("data_optimized") val dataOptimized: List<PageData>? = null,
    val relationships: ChapterRelationshipsDto? = null,
) {
    fun toSChapter(helper: WeebDexHelper): SChapter {
        val chapterName = mutableListOf<String>()
        // Build chapter name
        volume?.let {
            if (it.isNotEmpty()) {
                chapterName.add("Vol.$it")
            }
        }

        chapter?.let {
            if (it.isNotEmpty()) {
                chapterName.add("Ch.$it")
            }
        }

        title?.let {
            if (it.isNotEmpty()) {
                if (chapterName.isNotEmpty()) {
                    chapterName.add("-")
                }
                chapterName.add(it)
            }
        }

        // if volume, chapter and title is empty its a oneshot
        if (chapterName.isEmpty()) {
            chapterName.add("Oneshot")
        }

        return SChapter.create().apply {
            url = "/chapter/$id"
            name = Parser.unescapeEntities(chapterName.joinToString(" "), false)
            chapter_number = helper.parseChapterNumber(chapter)
            date_upload = helper.parseDate(publishedAt)
            scanlator = relationships?.groups?.joinToString(", ") { it.name }
        }
    }

    fun toPageList(dataSaver: Boolean): List<Page> {
        val pagesArray =
            if (dataSaver) {
                dataOptimized ?: data
            } else {
                data ?: dataOptimized
            } ?: emptyList()
        val pages = mutableListOf<Page>()

        pagesArray.forEachIndexed { index, pageData ->
            val filename = pageData.name
            val chapterId = id
            val imageUrl =
                filename
                    ?.takeIf { it.isNotBlank() && chapterId.isNotBlank() }
                    ?.let { "${WeebDexConstants.CDN_DATA_URL}/$chapterId/$it" }
            pages.add(Page(index, imageUrl = imageUrl))
        }
        return pages
    }
}

@Serializable class ChapterRelationshipsDto(val groups: List<NamedEntity> = emptyList())

@Serializable class PageData(val name: String? = null)
