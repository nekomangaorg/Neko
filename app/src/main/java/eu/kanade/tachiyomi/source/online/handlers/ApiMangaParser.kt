package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.handlers.dto.MangaDto
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import kotlinx.serialization.decodeFromString
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.util.Date
import java.util.Locale
import kotlin.math.floor

class ApiMangaParser {
    val network: NetworkHelper by injectLazy()
    val filterHandler: FilterHandler by injectLazy()
    val v5DbHelper: V5DbHelper by injectLazy()

    /**
     * Parse the manga details json into manga object
     */
    fun mangaDetailsParse(jsonData: String): SManga {
        try {
            val mangaDto = MdUtil.jsonParser.decodeFromString<MangaDto>(jsonData)
            val mangaAttributesDto = mangaDto.data.attributes

            val manga = mangaDto.toBasicManga()

            manga.description = MdUtil.cleanDescription(mangaAttributesDto.description["en"]!!)

            val authors = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.author, true)
            }.mapNotNull { it.attributes!!.name }.distinct()

            val artists = mangaDto.relationships.filter { relationshipDto ->
                relationshipDto.type.equals(MdConstants.artist, true)
            }.mapNotNull { it.attributes!!.name }.distinct()


            manga.author = authors.joinToString()
            manga.artist = artists.joinToString()
            manga.lang_flag = mangaAttributesDto.originalLanguage
            val lastChapter = mangaAttributesDto.lastChapter?.toFloatOrNull()
            lastChapter?.let {
                manga.last_chapter_number = floor(it).toInt()
            }

            /*networkManga.rating?.let {
                manga.rating = it.bayesian ?: it.mean
                manga.users = it.users
            }*/

            mangaAttributesDto.links?.let {
                it["al"]?.let { manga.anilist_id = it }
                it["kt"]?.let { manga.kitsu_id = it }
                it["mal"]?.let { manga.my_anime_list_id = it }
                it["mu"]?.let { manga.manga_updates_id = it }
                it["ap"]?.let { manga.anime_planet_id = it }
            }
            // val filteredChapters = filterChapterForChecking(networkApiManga)

            val tempStatus = parseStatus(mangaAttributesDto.status ?: "")
            val publishedOrCancelled =
                tempStatus == SManga.PUBLICATION_COMPLETE || tempStatus == SManga.CANCELLED
            /*if (publishedOrCancelled && isMangaCompleted(networkApiManga, filteredChapters)) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {*/
            manga.status = tempStatus
            // }

            val tags = filterHandler.getTags()

            val tempContentRating = mangaAttributesDto.contentRating?.capitalize(Locale.US)

            val contentRating = if (tempContentRating == null || tempContentRating == "Safe") {
                null
            } else {
                "Content rating: " + tempContentRating.capitalize(Locale.US)
            }

            val genres = (
                listOf(mangaAttributesDto.publicationDemographic?.capitalize(Locale.US)) +
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

    /**
     * If chapter title is oneshot or a chapter exists which matches the last chapter in the required language
     * return manga is complete
     */
    /*private fun isMangaCompleted(
        serializer: ApiMangaSerializer,
        filteredChapters: List<ChapterSerializer>
    ): Boolean {
        if (filteredChapters.isEmpty() || serializer.data.manga.lastChapter.isNullOrEmpty()) {
            return false
        }
        val finalChapterNumber = serializer.data.manga.lastChapter!!
        if (MdUtil.validOneShotFinalChapters.contains(finalChapterNumber)) {
            filteredChapters.firstOrNull()?.let {
                if (isOneShot(it, finalChapterNumber)) {
                    return true
                }
            }
        }
        val removeOneshots = filteredChapters.asSequence()
            .map { it.chapter!!.toDoubleOrNull() }
            .filter { it != null }
            .map { floor(it!!).toInt() }
            .filter { it != 0 }
            .toList().distinctBy { it }
        return removeOneshots.toList().size == floor(finalChapterNumber.toDouble()).toInt()
    }*/

    /* private fun filterChapterForChecking(serializer: ApiMangaSerializer): List<ChapterSerializer> {
         serializer.data.chapters ?: return emptyList()
         return serializer.data.chapters.asSequence()
             .filter { langs.contains(it.language) }
             .filter {
                 it.chapter?.let { chapterNumber ->
                     if (chapterNumber.toDoubleOrNull() == null) {
                         return@filter false
                     }
                     return@filter true
                 }
                 return@filter false
             }.toList()
     }*/

    /*private fun isOneShot(chapter: ChapterSerializer, finalChapterNumber: String): Boolean {
        return chapter.title.equals("oneshot", true) ||
            ((chapter.chapter.isNullOrEmpty() || chapter.chapter == "0") && MdUtil.validOneShotFinalChapters.contains(finalChapterNumber))
    }*/

    private fun parseStatus(status: String) = when (status) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.PUBLICATION_COMPLETE
        "cancelled" -> SManga.CANCELLED
        "hiatus" -> SManga.HIATUS
        else -> SManga.UNKNOWN
    }

    /**
     * Parse for the random manga id from the [MdUtil.randMangaPage] response.
     */
    fun randomMangaIdParse(response: Response): String {
        val manga = MdUtil.jsonParser.decodeFromString(MangaDto.serializer(),
            response.body!!.string())
        return manga.data.id
    }

    fun chapterListParse(
        chapterListResponse: List<ChapterDto>,
        groupMap: Map<String, String>,
    ): List<SChapter> {
        val now = Date().time

        return chapterListResponse.asSequence()
            .map {
                mapChapter(it, groupMap)
            }.filter {
                it.date_upload <= now
            }.toList()
    }

    fun chapterParseForMangaId(response: Response): String {
        try {
            if (response.code != 200) throw Exception("HTTP error ${response.code}")
            val jsonBody = response.body?.string().orEmpty()
            if (jsonBody.isEmpty()) {
                throw Exception("Null Response")
            }

            val apiChapter =
                MdUtil.jsonParser.decodeFromString(ChapterDto.serializer(), jsonBody)
            return apiChapter.relationships.firstOrNull { it.type.equals("manga", true) }?.id
                ?: throw Exception("Not found")
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }

    private fun mapChapter(
        networkChapter: ChapterDto,
        groups: Map<String, String>,
    ): SChapter {
        val chapter = SChapter.create()
        val attributes = networkChapter.data.attributes
        chapter.url = MdUtil.chapterSuffix + networkChapter.data.id
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
        /*if ((status == 2 || status == 3)) {
            if (finalChapterNumber != null) {
                if ((isOneShot(networkChapter, finalChapterNumber) && totalChapterCount == 1) ||
                    networkChapter.chapter == finalChapterNumber && finalChapterNumber.toIntOrNull() != 0
                ) {
                    chapterName.add("[END]")
                }
            }
        }*/

        chapter.name = MdUtil.cleanString(chapterName.joinToString(" "))
        // Convert from unix time
        chapter.date_upload = MdUtil.parseDate(attributes.publishAt)

        val scanlatorName = networkChapter.relationships.filter { it.type == "scanlation_group" }
            .mapNotNull { groups[it.id] }.toSet()

        chapter.scanlator = MdUtil.cleanString(MdUtil.getScanlatorString(scanlatorName))

        chapter.mangadex_chapter_id = MdUtil.getChapterId(chapter.url)

        chapter.language = MdLang.fromIsoCode(attributes.translatedLanguage)?.prettyPrint ?: ""

        return chapter
    }
}
