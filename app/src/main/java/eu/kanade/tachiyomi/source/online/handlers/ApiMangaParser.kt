package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.AggregateVolume
import eu.kanade.tachiyomi.source.online.models.dto.ChapterDataDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.lang.capitalized
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlin.math.floor
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.injectLazy

class ApiMangaParser {
    val network: NetworkHelper by injectLazy()
    val filterHandler: FilterHandler by injectLazy()
    val preferencesHelper: PreferencesHelper by injectLazy()

    /**
     * Parse the manga details json into manga object
     */
    fun mangaDetailsParse(mangaDto: MangaDataDto, stats: Pair<String?, String?>, simpleChapters: List<String>): Result<SManga, ResultError> {
        try {
            val mangaAttributesDto = mangaDto.attributes
            val manga = mangaDto.toBasicManga(preferencesHelper.thumbnailQuality())

            manga.rating = stats.first
            manga.users = stats.second

            manga.description = mangaAttributesDto.description.asMdMap<String>()["en"]

            val authors = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.Types.author, true)
            }.mapNotNull { it.attributes!!.name }.distinct()

            val artists = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.Types.artist, true)
            }.mapNotNull { it.attributes!!.name }.distinct()

            val altTitles = mangaAttributesDto.altTitles?.map { it.asMdMap<String>().values }?.flatten()
            manga.setAltTitles(altTitles)

            manga.author = authors.joinToString()
            manga.artist = artists.joinToString()
            manga.lang_flag = mangaAttributesDto.originalLanguage
            val lastChapter = mangaAttributesDto.lastChapter?.toFloatOrNull()
            lastChapter?.let {
                manga.last_chapter_number = floor(it).toInt()
            }

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
            if (publishedOrCancelled && simpleChapters.contains(mangaAttributesDto.lastChapter)
            ) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {
                manga.status = tempStatus
            }

            val tags = filterHandler.getTags()

            val tempContentRating = mangaAttributesDto.contentRating?.capitalized()

            val contentRating = if (tempContentRating == null) {
                null
            } else {
                "Content rating: " + tempContentRating.capitalized()
            }

            val genres = (
                listOf(mangaAttributesDto.publicationDemographic?.capitalized()) +
                    mangaAttributesDto.tags.map { it.id }
                        .map { dexTagId -> tags.firstOrNull { tag -> tag.id == dexTagId } }
                        .map { tag -> tag?.name } +
                    listOf(contentRating)
                )
                .filterNotNull()

            manga.genre = genres.joinToString(", ")

            return Ok(manga)
        } catch (e: Exception) {
            XLog.e(e)
            return Err("Unexpected Manga parsing error".toResultError())
        }
    }

    /**
     * Parse the manga details json into manga object
     */
    suspend fun mangaDetailsParse(mangaDto: MangaDataDto): SManga {
        try {
            val mangaAttributesDto = mangaDto.attributes
            val manga = mangaDto.toBasicManga(preferencesHelper.thumbnailQuality())

            val simpleChapters = withIOContext {
                val aggregateDto = network.service.aggregateChapters(
                    mangaDto.id,
                    MdUtil.getLangsToShow(preferencesHelper),
                )
                    .onFailure {
                        this.log("trying to aggregate for ${mangaDto.id}")
                    }.getOrNull()

                aggregateDto?.volumes?.asMdMap<AggregateVolume>()?.values
                    ?.flatMap { it.chapters.values }
                    ?.map { it.chapter }
                    ?: emptyList()
            }

            withIOContext {
                val statResult = network.service.mangaStatistics(mangaDto.id)
                    .onFailure {
                        this.log("trying to get rating for ${mangaDto.id}")
                    }.getOrNull()
                statResult?.statistics?.get(mangaDto.id)?.let { stats ->
                    val rating = stats.rating.bayesian ?: 0.0
                    if (rating > 0) {
                        manga.rating = rating.toString()
                    }

                    manga.users = stats.follows?.toString()
                }
            }

            manga.description = mangaAttributesDto.description.asMdMap<String>()["en"]

            val authors = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.Types.author, true)
            }.mapNotNull { it.attributes!!.name }.distinct()

            val artists = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.Types.artist, true)
            }.mapNotNull { it.attributes!!.name }.distinct()

            val altTitles = mangaAttributesDto.altTitles?.map { it.asMdMap<String>().values }?.flatten()
            manga.setAltTitles(altTitles)

            manga.author = authors.joinToString()
            manga.artist = artists.joinToString()
            manga.lang_flag = mangaAttributesDto.originalLanguage
            val lastChapter = mangaAttributesDto.lastChapter?.toFloatOrNull()
            lastChapter?.let {
                manga.last_chapter_number = floor(it).toInt()
            }

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
            if (publishedOrCancelled && simpleChapters.contains(mangaAttributesDto.lastChapter)
            ) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {
                manga.status = tempStatus
            }

            val tags = filterHandler.getTags()

            val tempContentRating = mangaAttributesDto.contentRating?.capitalized()

            val contentRating = if (tempContentRating == null) {
                null
            } else {
                "Content rating: " + tempContentRating.capitalized()
            }

            val genres = (
                listOf(mangaAttributesDto.publicationDemographic?.capitalized()) +
                    mangaAttributesDto.tags.map { it.id }
                        .map { dexTagId -> tags.firstOrNull { tag -> tag.id == dexTagId } }
                        .map { tag -> tag?.name } +
                    listOf(contentRating)
                )
                .filterNotNull()

            manga.genre = genres.joinToString(", ")

            return manga
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }

    private fun parseStatus(status: String) = when (status) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.PUBLICATION_COMPLETE
        "cancelled" -> SManga.CANCELLED
        "hiatus" -> SManga.HIATUS
        else -> SManga.UNKNOWN
    }

    fun chapterListParse(
        lastChapterNumber: Int?,
        chapterListResponse: List<ChapterDataDto>,
        groupMap: Map<String, String>,
    ): Result<List<SChapter>, ResultError> {
        return runCatching {
            Ok(
                chapterListResponse.asSequence()
                    .map {
                        mapChapter(it, lastChapterNumber, groupMap)
                    }.toList(),
            )
        }.getOrElse {
            val msg = "Exception parsing chapters"
            XLog.e(msg, it)
            Err(msg.toResultError())
        }
    }

    private fun mapChapter(
        networkChapter: ChapterDataDto,
        lastChapterNumber: Int?,
        groups: Map<String, String>,
    ): SChapter {
        val chapter = SChapter.create()
        val attributes = networkChapter.attributes
        chapter.url = MdUtil.chapterSuffix + networkChapter.id

        val chapterName = mutableListOf<String>()
        // Build chapter name

        if (attributes.volume != null) {
            chapterName.add("Vol.${attributes.volume}")
            chapter.vol = attributes.volume.toString()
        }

        if (attributes.chapter.isNullOrBlank().not()) {
            val chp = "Ch.${attributes.chapter}"
            chapterName.add(chp)
            chapter.chapter_txt = chp
        }

        if (attributes.title.isNullOrBlank().not()) {
            if (chapterName.isNotEmpty()) {
                chapterName.add("-")
            }
            chapterName.add(attributes.title!!)
            chapter.chapter_title = MdUtil.cleanString(attributes.title)
        }

        // if volume, chapter and title is empty its a oneshot
        if (chapterName.isEmpty()) {
            chapterName.add("Oneshot")
        }
        if (lastChapterNumber != null && attributes.chapter == lastChapterNumber.toString()) {
            chapterName.add("[END]")
        }

        chapter.name = MdUtil.cleanString(chapterName.joinToString(" "))
        // Convert from unix time

        chapter.date_upload = MdUtil.parseDate(attributes.readableAt)

        val scanlatorName =
            networkChapter.relationships.filter { it.type == MdConstants.Types.scanlator }
                .mapNotNull { groups[it.id] }.toMutableSet()

        if (scanlatorName.contains("no group") || scanlatorName.isEmpty()) {
            scanlatorName.remove("no group")
            scanlatorName.add("No Group")
        }

        chapter.scanlator = MdUtil.cleanString(ChapterUtil.getScanlatorString(scanlatorName))

        chapter.mangadex_chapter_id = MdUtil.getChapterUUID(chapter.url)

        chapter.language = attributes.translatedLanguage

        return chapter
    }
}
