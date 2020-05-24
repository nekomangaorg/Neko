package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.consumeBody
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.ApiMangaSerializer
import eu.kanade.tachiyomi.source.online.handlers.serializers.ChapterSerializer
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.serialization.json.Json
import okhttp3.Response
import org.jsoup.Jsoup
import timber.log.Timber
import java.util.Date

class ApiMangaParser(val langs: List<String>) {
    fun mangaDetailsParse(response: Response): SManga {
        return mangaDetailsParse(response.body!!.string())
    }

    /**
     * Parse the manga details json into manga object
     */
    fun mangaDetailsParse(jsonData: String): SManga {
        try {
            val manga = SManga.create()
            val networkApiManga = Json.nonstrict.parse(ApiMangaSerializer.serializer(), jsonData)
            val networkManga = networkApiManga.manga
            manga.title = MdUtil.cleanString(networkManga.title)
            manga.thumbnail_url = MdUtil.cdnUrl + MdUtil.removeTimeParamUrl(networkManga.cover_url)
            manga.description = MdUtil.cleanDescription(networkManga.description)
            manga.author = MdUtil.cleanString(networkManga.author)
            manga.artist = MdUtil.cleanString(networkManga.artist)
            manga.lang_flag = networkManga.lang_flag

            networkManga.links?.let {
                it.al?.let { manga.anilist_id = it }
                it.kt?.let { manga.kitsu_id = it }
                it.mal?.let { manga.my_anime_list_id = it }
                it.mu?.let { manga.manga_updates_id = it }
                it.ap?.let { manga.anime_planet_id = it }
            }
            val filteredChapters = filterChapterForChecking(networkApiManga)

            val tempStatus = parseStatus(networkManga.status)
            val publishedOrCancelled =
                tempStatus == SManga.PUBLICATION_COMPLETE || tempStatus == SManga.CANCELLED
            if (publishedOrCancelled && isMangaCompleted(networkApiManga, filteredChapters)) {
                manga.status = SManga.COMPLETED
                manga.missing_chapters = null
            } else {
                manga.missing_chapters = getMissingChapterCount(filteredChapters)
                manga.status = tempStatus
            }

            val genres = networkManga.genres.mapNotNull { FilterHandler.allTypes[it.toString()] }
                .toMutableList()

            if (networkManga.hentai == 1) {
                genres.add("Hentai")
            }

            manga.genre = genres.joinToString(", ")

            return manga
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    /**
     * If chapter title is oneshot or a chapter exists which matches the last chapter in the required language
     * return manga is complete
     */
    private fun isMangaCompleted(
        serializer: ApiMangaSerializer,
        filteredChapters: List<Map.Entry<String, ChapterSerializer>>
    ): Boolean {
        if (serializer.chapter.isNullOrEmpty() || serializer.manga.last_chapter.isNullOrEmpty()) {
            return false
        }
        val finalChapterNumber = serializer.manga.last_chapter!!
        if (MdUtil.validOneShotFinalChapters.contains(finalChapterNumber)) {
            filteredChapters.firstOrNull()?.let {
                if (isOneShot(it.value, finalChapterNumber)) {
                    return true
                }
            }
        }
        val removeOneshots = filteredChapters.filter { !it.value.chapter.isNullOrBlank() }
        return removeOneshots.size.toString() == finalChapterNumber
    }

    private fun getMissingChapterCount(filteredChapters: List<Map.Entry<String, ChapterSerializer>>): String? {

        val remove0ChaptersFromCount = filteredChapters.filter { it.value?.chapter?.toInt() != 0 }

        remove0ChaptersFromCount.firstOrNull()?.value?.chapter?.let {
            val result = it.toInt() - remove0ChaptersFromCount.size
            if (result <= 0) return null
            return result.toString()
        }
        return null
    }

    private fun filterChapterForChecking(serializer: ApiMangaSerializer): List<Map.Entry<String, ChapterSerializer>> {
        serializer.chapter ?: return emptyList()
        val filteredChapters = serializer.chapter.entries
            .filter { langs.contains(it.value.lang_code) }
            .filter {
                it.value.chapter?.let { chapterNumber ->
                    if (chapterNumber.isBlank() || chapterNumber.contains("-") || chapterNumber.contains(".")) {
                        return@filter false
                    }
                    return@filter true
                }
                return@filter false
            }.distinctBy { it.value.chapter }
        return filteredChapters
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
        val networkApiManga = Json.nonstrict.parse(ApiMangaSerializer.serializer(), jsonData)
        val networkManga = networkApiManga.manga
        val networkChapters = networkApiManga.chapter
        if (networkChapters.isNullOrEmpty()) {
            return listOf()
        }
        val status = networkManga.status

        val finalChapterNumber = networkManga.last_chapter!!

        val chapters = mutableListOf<SChapter>()

        // Skip chapters that don't match the desired language, or are future releases

        val chapLangs = MdLang.values().filter { langs.contains(it.dexLang) }

        networkChapters.forEach {
            if (langs.contains(it.value.lang_code) && (it.value.timestamp * 1000) <= now) {
                chapters.add(mapChapter(it.key, it.value, finalChapterNumber, status, chapLangs))
            }
        }

        return chapters
    }

    private fun mapChapter(
        chapterId: String,
        networkChapter: ChapterSerializer,
        finalChapterNumber: String,
        status: Int,
        chapLangs: List<MdLang>
    ): SChapter {
        val chapter = SChapter.create()
        chapter.url = MdUtil.apiChapter + chapterId
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
            if (isOneShot(networkChapter, finalChapterNumber) || networkChapter.chapter == finalChapterNumber && chapter.chapter_title.contains("prologue", true)) {
                chapterName.add("[END]")
            }
        }

        chapter.name = MdUtil.cleanString(chapterName.joinToString(" "))
        // Convert from unix time
        chapter.date_upload = networkChapter.timestamp * 1000
        val scanlatorName = mutableListOf<String>()

        networkChapter.group_name?.let {
            scanlatorName.add(it)
        }
        networkChapter.group_name_2?.let {
            scanlatorName.add(it)
        }
        networkChapter.group_name_3?.let {
            scanlatorName.add(it)
        }

        chapter.scanlator = MdUtil.cleanString(MdUtil.getScanlatorString(scanlatorName))

        chapter.mangadex_chapter_id = MdUtil.getChapterId(chapter.url)

        chapter.language = chapLangs.firstOrNull { it.dexLang.equals(networkChapter.lang_code) }?.name

        return chapter
    }
}
