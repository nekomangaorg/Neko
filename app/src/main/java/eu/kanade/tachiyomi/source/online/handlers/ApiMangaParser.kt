package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.salomonbrys.kotson.nullInt
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.network.consumeBody
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.ApiMangaSerializer
import eu.kanade.tachiyomi.source.online.handlers.serializers.ChapterSerializer
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.Response
import org.jsoup.Jsoup
import java.util.Date
import kotlin.math.floor

class ApiMangaParser(val langs: List<String>) {

    fun mangaDetailsParse(response: Response, coverUrls: List<String>): SManga {
        return mangaDetailsParse(response.body!!.string(), coverUrls)
    }

    /**
     * Parse the manga details json into manga object
     */
    fun mangaDetailsParse(jsonData: String, coverUrls: List<String>): SManga {
        try {
            val manga = SManga.create()
            val networkApiManga = MdUtil.jsonParser.decodeFromString(ApiMangaSerializer.serializer(), jsonData)
            val networkManga = networkApiManga.data.manga
            manga.title = MdUtil.cleanString(networkManga.title)

            manga.thumbnail_url =
                if (coverUrls.isNotEmpty()) {
                    coverUrls.last()
                } else {
                    networkManga.mainCover
                }

            manga.description = MdUtil.cleanDescription(networkManga.description)
            manga.author = MdUtil.cleanString(networkManga.author.joinToString())
            manga.artist = MdUtil.cleanString(networkManga.artist.joinToString())
            manga.lang_flag = networkManga.publication?.language
            val lastChapter = networkManga.lastChapter?.toFloatOrNull()
            lastChapter?.let {
                manga.last_chapter_number = floor(it).toInt()
            }

            networkManga.rating?.let {
                manga.rating = it.bayesian ?: it.mean
                manga.users = it.users
            }
            networkManga.links?.let {
                it.al?.let { manga.anilist_id = it }
                it.kt?.let { manga.kitsu_id = it }
                it.mal?.let { manga.my_anime_list_id = it }
                it.mu?.let { manga.manga_updates_id = it }
                it.ap?.let { manga.anime_planet_id = it }
            }
            val filteredChapters = filterChapterForChecking(networkApiManga)

            val tempStatus = parseStatus(networkManga.publication!!.status)
            val publishedOrCancelled =
                tempStatus == SManga.PUBLICATION_COMPLETE || tempStatus == SManga.CANCELLED
            if (publishedOrCancelled && isMangaCompleted(networkApiManga, filteredChapters)) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {
                manga.status = tempStatus
            }

            val genres = networkManga.tags.mapNotNull { FilterHandler.allTypes[it.toString()] }
                .toMutableList()

            networkManga.publication?.demographic?.let { demographicInt ->
                val demographic = FilterHandler.demographics().firstOrNull { it.id.toInt() == demographicInt }

                if (demographic != null) {
                    genres.add(0, demographic.name)
                }

            }

            if (networkManga.isHentai) {
                genres.add("Hentai")
            }

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
    private fun isMangaCompleted(
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
    }

    private fun filterChapterForChecking(serializer: ApiMangaSerializer): List<ChapterSerializer> {
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
    }

    private fun isOneShot(chapter: ChapterSerializer, finalChapterNumber: String): Boolean {
        return chapter.title.equals("oneshot", true) ||
            ((chapter.chapter.isNullOrEmpty() || chapter.chapter == "0") && MdUtil.validOneShotFinalChapters.contains(finalChapterNumber))
    }

    private fun parseStatus(status: Int) = when (status) {
        1 -> SManga.ONGOING
        2 -> SManga.PUBLICATION_COMPLETE
        3 -> SManga.CANCELLED
        4 -> SManga.HIATUS
        else -> SManga.UNKNOWN
    }

    /**
     * Parse for the random manga id from the [MdUtil.randMangaPage] response.
     */
    fun randomMangaIdParse(response: Response): String {
        val randMangaUrl = Jsoup.parse(response.consumeBody())
            .select("link[rel=canonical]")
            .attr("href")
        return MdUtil.getMangaId(randMangaUrl)
    }

    fun chapterListParse(response: Response): List<SChapter> {
        return chapterListParse(response.body!!.string())
    }

    fun chapterListParse(jsonData: String): List<SChapter> {
        val now = Date().time
        val networkApiManga = MdUtil.jsonParser.decodeFromString(ApiMangaSerializer.serializer(), jsonData)
        val networkManga = networkApiManga.data.manga
        val networkChapters = networkApiManga.data.chapters
        val groups = networkApiManga.data.groups.mapNotNull {
            if (it.name == null) {
                null
            } else {
                it.id to it.name
            }
        }.toMap()

        val status = networkManga.publication!!.status

        val finalChapterNumber = networkManga.lastChapter

        // Skip chapters that don't match the desired language, or are future releases

        val chapLangs = MdLang.values().filter { langs.contains(it.dexLang) }



        return networkChapters.asSequence()
            .filter { langs.contains(it.language) && (it.timestamp * 1000) <= now }
            .map { mapChapter(it, finalChapterNumber, status, chapLangs, networkChapters.size, groups) }.toList()
    }

    fun chapterParseForMangaId(response: Response): Int {
        try {
            if (response.code != 200) throw Exception("HTTP error ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isEmpty()) {
                throw Exception("Null Response")
            }

            val jsonObject = JsonParser.parseString(body).obj
            return jsonObject["data"].asJsonObject["mangaId"]?.nullInt ?: throw Exception("No manga associated with chapter")
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }

    private fun mapChapter(
        networkChapter: ChapterSerializer,
        finalChapterNumber: String?,
        status: Int,
        chapLangs: List<MdLang>,
        totalChapterCount: Int,
        groups: Map<Long, String>,
    ): SChapter {
        val chapter = SChapter.create()
        chapter.url = MdUtil.oldApiChapter + networkChapter.id
        val chapterName = mutableListOf<String>()
        // Build chapter name

        if (!networkChapter.volume.isNullOrBlank()) {
            val vol = "Vol." + networkChapter.volume
            chapterName.add(vol)
            chapter.vol = vol
        }

        if (!networkChapter.chapter.isNullOrBlank()) {
            val chp = "Ch." + networkChapter.chapter
            chapterName.add(chp)
            chapter.chapter_txt = chp
        }
        if (!networkChapter.title.isNullOrBlank()) {
            if (chapterName.isNotEmpty()) {
                chapterName.add("-")
            }
            chapterName.add(networkChapter.title)
            chapter.chapter_title = MdUtil.cleanString(networkChapter.title)
        }

        // if volume, chapter and title is empty its a oneshot
        if (chapterName.isEmpty()) {
            chapterName.add("Oneshot")
        }
        if ((status == 2 || status == 3)) {
            if (finalChapterNumber != null) {
                if ((isOneShot(networkChapter, finalChapterNumber) && totalChapterCount == 1) ||
                    networkChapter.chapter == finalChapterNumber && finalChapterNumber.toIntOrNull() != 0
                ) {
                    chapterName.add("[END]")
                }
            }
        }

        chapter.name = MdUtil.cleanString(chapterName.joinToString(" "))
        // Convert from unix time
        chapter.date_upload = networkChapter.timestamp * 1000
        val scanlatorName = mutableSetOf<String>()

        networkChapter.groups.mapNotNull { groups.get(it) }.forEach { scanlatorName.add(it) }

        chapter.scanlator = MdUtil.cleanString(MdUtil.getScanlatorString(scanlatorName))

        chapter.mangadex_chapter_id = MdUtil.getChapterId(chapter.url)

        chapter.language = chapLangs.firstOrNull { it.dexLang == networkChapter.language }?.name

        return chapter
    }
}
