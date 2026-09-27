package eu.kanade.tachiyomi.source.online.merged.mangaball

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nekomanga.constants.Constants

@Serializable class SearchResponse(val data: List<SearchTitle> = emptyList())

@Serializable
class SearchTitle(
    val id: String,
    val name: String,
    val slug: String? = null,
    val image: TitleImage? = null,
) {
    fun toSManga(): SManga =
        SManga.create().apply {
            url = slug?.takeIf { it.isNotBlank() }?.let { "$it-$id" } ?: id
            title = name
            thumbnail_url = image?.coverUrl()
        }
}

@Serializable
class TitleImage(
    val cover: Cover? = null,
    val file: String? = null,
    @SerialName("cdn_mangadex") val cdnMangadex: String? = null,
    @SerialName("cdn_mangaupdate") val cdnMangaupdate: String? = null,
) {
    /** Same order the site uses to pick a cover. Relative paths live on its cover host. */
    fun coverUrl(): String? =
        listOf(cover?.path, file, cdnMangadex, cdnMangaupdate)
            .firstNotNullOfOrNull { it?.trim()?.replace('\\', '/')?.takeIf { s -> s.isNotEmpty() } }
            ?.let {
                if (it.startsWith("https://") || it.startsWith("http://")) it else COVER_URL + it
            }
}

@Serializable class Cover(val path: String? = null)

@Serializable class ChapterListResponse(val data: List<ChapterDto>)

@Serializable
class ChapterDto(
    val id: String,
    val lang: String? = null,
    val name: String? = null,
    val number: Float? = null,
    val volume: Float? = null,
    val group: Group? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toSChapter(enabledSiteLangs: List<String>): SChapter? {
        if (lang == null || lang !in enabledSiteLangs) return null
        val language = MangaBallLang.fromMangaBallLang(lang) ?: return null

        val volumeText =
            volume?.takeIf { it > 0 }?.let { "Vol.${it.toString().removeSuffix(".0")}" }
        val numberText = number?.let { "Ch.${it.toString().removeSuffix(".0")}" }
        val nameParts = listOfNotNull(volumeText, numberText).toMutableList()
        if (number != null) {
            val title = normalizeChapterName(name.orEmpty(), number)
            if (title.isNotBlank()) nameParts += listOf("-", title)
        } else {
            // Without a number, ChapterRecognition reads it from the name, so keep all of it.
            name?.trim()?.takeIf { it.isNotEmpty() }?.let { nameParts += it }
        }

        return SChapter.create().apply {
            url = id
            this.name = nameParts.joinToString(" ")
            vol = volumeText.orEmpty()
            chapter_txt = numberText.orEmpty()
            number?.let { chapter_number = it }
            date_upload = parseDate(createdAt)
            scanlator =
                listOfNotNull(
                        MangaBall.name,
                        group?.name,
                        group?.id?.takeUnless { groupIdRegex.matches(it) }?.let { "($it)" },
                    )
                    .joinToString(Constants.SCANLATOR_SEPARATOR)
            this.language = language
        }
    }
}

@Serializable class Group(val id: String? = null, val name: String? = null)

@Serializable
class ChapterDetailResponse(val data: ChapterDetailData? = null) {
    fun toPageList(): List<Page> =
        data?.chapter?.pages.orEmpty().mapIndexed { index, url -> Page(index, imageUrl = url) }
}

@Serializable class ChapterDetailData(val chapter: ChapterDetail)

@Serializable class ChapterDetail(val pages: List<String> = emptyList())

private const val COVER_URL = "https://bulbasaur.poke-black-and-white.net/covers/"

private val groupIdRegex = Regex("""[a-z0-9]{24}""")

/** The site reads these timestamps as UTC. */
private fun parseDate(createdAt: String?): Long {
    createdAt ?: return 0L
    return runCatching { LocalDateTime.parse(createdAt).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrDefault(0L)
}

private fun normalizeChapterName(name: String, number: Float): String {
    val trimmedName = name.trim()

    // Regex to find "Chapter XXX:", "Ch. 12:", "Chapter 12.1 - ", etc.
    // This matches the prefix AND a separator (like ':', '-', '—')
    // and returns *only* the text after it.
    val prefixRegex = Regex("""^(?i)(Chapter|Ch\.?)\s+\d+(\.\d+)?\s*[:\-–—]\s*(.*)$""")
    val prefixMatch = prefixRegex.find(trimmedName)
    if (prefixMatch != null) {
        // Found a prefix and a title. Return just the title part.
        // groupValues[3] is the (.*) part
        return prefixMatch.groupValues[3].trim()
    }

    // Regex to find if the *entire string* is just a chapter identifier
    // (e.f., "Chapter 1506", "Ch. 12", "Chapter 04")
    val fullMatchRegex = Regex("""^(?i)(Chapter|Ch\.?)\s+\d+(\.\d+)?\s*$""")
    if (fullMatchRegex.matches(trimmedName)) {
        // The name is just "Chapter XXX", no title.
        return ""
    }

    // Check if the name is *just* the number (e.g., "77.1")
    val numAsStr =
        if (number == number.toInt().toFloat()) {
            number.toInt().toString() // "77"
        } else {
            number.toString() // "77.1"
        }

    if (trimmedName == numAsStr) {
        return ""
    }

    // If no patterns matched, the name is already a valid title.
    // (e.g., "Official Translation", "The Island of Destiny")
    return trimmedName
}
