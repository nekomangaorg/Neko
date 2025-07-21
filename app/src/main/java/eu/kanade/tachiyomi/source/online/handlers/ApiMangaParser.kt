package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.MangaTag
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.ChapterDataDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.lang.capitalized
import eu.kanade.tachiyomi.util.lang.toResultError
import kotlin.math.floor
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class ApiMangaParser {
    val network: NetworkHelper by injectLazy()
    val preferencesHelper: PreferencesHelper by injectLazy()

    /** Parse the manga details json into manga object */
    fun mangaDetailsParse(
        mangaDto: MangaDataDto,
        stats: Stats,
        simpleChapters: List<String>,
    ): Result<SManga, ResultError> {
        try {
            val mangaAttributesDto = mangaDto.attributes
            val manga = mangaDto.toBasicManga(preferencesHelper.thumbnailQuality().get())

            manga.rating = stats.rating
            manga.users = stats.follows
            manga.replies_count = stats.repliesCount
            manga.thread_id = stats.threadId

            manga.description = mangaAttributesDto.description.asMdMap<String>()["en"]

            manga.author =
                mangaDto.relationships
                    .filter { relationshipDto ->
                        relationshipDto.type.equals(MdConstants.Types.author, true)
                    }
                    .mapNotNull { it.attributes!!.name }
                    .distinct()
                    .joinToString(Constants.SEPARATOR)

            manga.artist =
                mangaDto.relationships
                    .filter { relationshipDto ->
                        relationshipDto.type.equals(MdConstants.Types.artist, true)
                    }
                    .mapNotNull { it.attributes!!.name }
                    .distinct()
                    .joinToString(Constants.SEPARATOR)

            val altTitles =
                mangaAttributesDto.altTitles?.map { it.asMdMap<String>().values }?.flatten()
            manga.setAltTitles(altTitles)

            manga.lang_flag = mangaAttributesDto.originalLanguage
            val lastChapter = mangaAttributesDto.lastChapter?.toFloatOrNull()
            lastChapter?.let { manga.last_chapter_number = floor(it).toInt() }
            val lastVolume = mangaAttributesDto.lastVolume?.toIntOrNull()
            manga.last_volume_number = lastVolume

            val otherUrls = mutableListOf<String>()
            mangaAttributesDto.links?.asMdMap<String>()?.let { linkMap ->
                linkMap["al"]?.let { id -> manga.anilist_id = id }
                linkMap["kt"]?.let { id -> manga.kitsu_id = id }
                linkMap["mal"]?.let { id -> manga.my_anime_list_id = id }
                linkMap["mu"]?.let { id -> manga.manga_updates_id = id }
                linkMap["ap"]?.let { id -> manga.anime_planet_id = id }
                linkMap["raw"]?.let { id -> otherUrls.add("raw~~$id") }
                linkMap["engtl"]?.let { id -> otherUrls.add("engtl~~$id") }
                linkMap["bw"]?.let { id -> otherUrls.add("bw~~$id") }
                linkMap["amz"]?.let { id -> otherUrls.add("amz~~$id") }
                linkMap["ebj"]?.let { id -> otherUrls.add("ebj~~$id") }
                linkMap["cdj"]?.let { id -> otherUrls.add("cdj~~$id") }
            }
            if (otherUrls.isNotEmpty()) {
                manga.other_urls = otherUrls.joinToString("||")
            }

            val tempStatus = parseStatus(mangaAttributesDto.status ?: "")
            val publishedOrCancelled =
                (tempStatus == SManga.PUBLICATION_COMPLETE || tempStatus == SManga.CANCELLED)
            if (publishedOrCancelled && simpleChapters.contains(mangaAttributesDto.lastChapter)) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {
                manga.status = tempStatus
            }

            val tempContentRating = mangaAttributesDto.contentRating?.capitalized()

            val contentRating =
                if (tempContentRating == null) {
                    null
                } else {
                    "Content rating: " + tempContentRating.capitalized()
                }

            val genres =
                (listOf(mangaAttributesDto.publicationDemographic?.capitalized()) +
                        mangaAttributesDto.tags
                            .map { it.id }
                            .map { dexTagId ->
                                MangaTag.values().firstOrNull { tag -> tag.uuid == dexTagId }
                            }
                            .map { tag -> tag?.prettyPrint } +
                        listOf(contentRating))
                    .filterNotNull()

            manga.genre = genres.joinToString(", ")

            return Ok(manga)
        } catch (e: Exception) {
            TimberKt.e(e) { "Unexpected manga parsing error" }
            return Err("Unexpected Manga parsing error".toResultError())
        }
    }

    private fun parseStatus(status: String) =
        when (status) {
            "ongoing" -> SManga.ONGOING
            "completed" -> SManga.PUBLICATION_COMPLETE
            "cancelled" -> SManga.CANCELLED
            "hiatus" -> SManga.HIATUS
            else -> SManga.UNKNOWN
        }

    fun chapterListParse(
        lastChapterNumber: Int?,
        lastVolumeNumber: Int?,
        chapterListResponse: List<ChapterDataDto>,
        groupMap: Map<String, String>,
        uploaderMap: Map<String, String>,
    ): Result<List<SChapter>, ResultError> {
        return runCatching {
                Ok(
                    chapterListResponse
                        .asSequence()
                        .mapNotNull {
                            mapChapter(
                                it,
                                lastChapterNumber,
                                lastVolumeNumber,
                                groupMap,
                                uploaderMap,
                            )
                        }
                        .toList()
                )
            }
            .getOrElse {
                val msg = "Exception parsing chapters"
                TimberKt.e(it) { msg }
                Err(msg.toResultError())
            }
    }

    private fun mapChapter(
        networkChapter: ChapterDataDto,
        lastChapterNumber: Int?,
        lastVolumeNumber: Int?,
        groups: Map<String, String>,
        uploaders: Map<String, String>,
    ): SChapter? {
        val chapter = SChapter.create()
        val attributes = networkChapter.attributes
        chapter.url = MdConstants.chapterSuffix + networkChapter.id

        chapter.name = networkChapter.buildChapterName(chapter, lastChapterNumber, lastVolumeNumber)
        // Convert from unix time

        chapter.date_upload = MdUtil.parseDate(attributes.readableAt)

        val scanlatorName =
            networkChapter.relationships
                .filter { it.type == MdConstants.Types.scanlator }
                .mapNotNull { groups[it.id] }
                .toMutableSet()

        val uploaderName =
            networkChapter.relationships
                .filter { it.type == MdConstants.Types.uploader }
                .map { uploaders[it.id] }
                .firstOrNull()

        if (scanlatorName.isEmpty()) {
            scanlatorName.add("No Group")
        }

        chapter.scanlator = MdUtil.cleanString(ChapterUtil.getScanlatorString(scanlatorName))

        chapter.uploader = uploaderName ?: ""

        chapter.mangadex_chapter_id = MdUtil.getChapterUUID(chapter.url)

        chapter.language = attributes.translatedLanguage

        chapter.isUnavailable = attributes.isUnavailable ?: return null

        return chapter
    }
}

fun ChapterDataDto.buildChapterName(
    chapter: SChapter? = null,
    lastChapterNumber: Int? = null,
    lastVolumeNumber: Int? = null,
): String {
    val chapterName = mutableListOf<String>()
    // Build chapter name

    if (attributes.volume != null) {
        chapterName.add("Vol.${attributes.volume}")
        chapter?.vol = attributes.volume.toString()
    }

    if (!attributes.chapter.isNullOrBlank()) {
        val chp = "Ch.${attributes.chapter}"
        chapterName.add(chp)
        chapter?.chapter_txt = chp
    }

    if (!attributes.title.isNullOrBlank()) {
        if (chapterName.isNotEmpty()) {
            chapterName.add("-")
        }
        chapterName.add(attributes.title!!)
        chapter?.chapter_title = MdUtil.cleanString(attributes.title)
    }

    // if volume, chapter and title is empty its a oneshot
    if (chapterName.isEmpty()) {
        chapterName.add("Oneshot")
    }

    val sameVolume =
        attributes.volume == null ||
            lastVolumeNumber == null ||
            attributes.volume == lastVolumeNumber.toString()

    if (
        lastChapterNumber != null &&
            attributes.chapter == lastChapterNumber.toString() &&
            sameVolume
    ) {
        chapterName.add("[END]")
    }

    return MdUtil.cleanString(chapterName.joinToString(" "))
}
