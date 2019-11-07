package eu.kanade.tachiyomi.source.online.handlers

import com.github.salomonbrys.kotson.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.network.consumeBody
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.Response
import org.jsoup.Jsoup
import java.util.*

class ApiMangaParser(val lang: String) {
    fun mangaDetailsParse(response: Response): SManga {
        val manga = SManga.create()
        val jsonData = response.body!!.string()
        val json = JsonParser().parse(jsonData).asJsonObject
        val mangaJson = json.getAsJsonObject("manga")
        val chapterJson = json.getAsJsonObject("chapter")
        manga.title = MdUtil.cleanString(mangaJson.get("title").string)
        manga.thumbnail_url = MdUtil.cdnUrl + mangaJson.get("cover_url").string
        manga.description = MdUtil.cleanString(mangaJson.get("description").string)
        manga.author = mangaJson.get("author").string
        manga.artist = mangaJson.get("artist").string
        manga.lang_flag = mangaJson.get("lang_flag").string
        val status = mangaJson.get("status").int
        val tempStatus = parseStatus(status)
        val finalChapterNumber = getFinalChapter(mangaJson)
        if ((tempStatus == SManga.PUBLICATION_COMPLETE || tempStatus == SManga.CANCELLED) && chapterJson != null && isMangaCompleted(chapterJson, finalChapterNumber)) {
            manga.status = SManga.COMPLETED
        } else if (tempStatus == SManga.PUBLICATION_COMPLETE && chapterJson != null && isOneshot(chapterJson, finalChapterNumber)) {
            manga.status = SManga.COMPLETED
        } else {
            manga.status = tempStatus
        }

        val genres = (if (mangaJson.get("hentai").int == 1) listOf("Hentai") else listOf()) +
                mangaJson.get("genres").asJsonArray.mapNotNull { FilterHandler.allTypes[it.toString()] }
        manga.genre = genres.joinToString(", ")

        return manga
    }

    private fun isMangaCompleted(chapterJson: JsonObject, finalChapterNumber: String): Boolean {
        val count = chapterJson.entrySet()
                .filter { it.value.asJsonObject.get("lang_code").string == lang }
                .filter { doesFinalChapterExist(finalChapterNumber, it.value) }.count()
        return count != 0
    }

    private fun parseStatus(status: Int) = when (status) {
        1 -> SManga.ONGOING
        2 -> SManga.PUBLICATION_COMPLETE
        3 -> SManga.CANCELLED
        4 -> SManga.HIATUS
        else -> SManga.UNKNOWN

    }

    private fun isOneshot(chapterJson: JsonObject, lastChapter: String): Boolean {
        val chapter = chapterJson.takeIf { it.size() > 0 }?.get(chapterJson.keys().elementAt(0))?.obj?.get("title")?.string
        return if (chapter != null) {
            chapter == "Oneshot" || chapter.isEmpty() && lastChapter == "0"
        } else {
            false
        }
    }

    /**
     * Parse for the random manga id from the [MdUtil.randMangaPage] response.
     */
    fun randomMangaIdParse(response : Response) : String{
        val randMangaUrl = Jsoup.parse(response.consumeBody())
                .select("link[rel=canonical]")
                .attr("href")
        return MdUtil.getMangaId(randMangaUrl)
    }

    fun chapterListParse(response: Response): List<SChapter> {
        val now = Date().time
        val jsonData = response.body!!.string()
        val json = JsonParser().parse(jsonData).asJsonObject
        val mangaJson = json.getAsJsonObject("manga")
        val status = mangaJson.get("status").int

        val finalChapterNumber = getFinalChapter(mangaJson)
        val chapterJson = json.getAsJsonObject("chapter")
        val chapters = mutableListOf<SChapter>()

        // Skip chapters that don't match the desired language, or are future releases
        chapterJson?.forEach { key, jsonElement ->
            val chapterElement = jsonElement.asJsonObject
            if (chapterElement.get("lang_code").string == lang && (chapterElement.get("timestamp").asLong * 1000) <= now) {
                chapters.add(chapterFromJson(key, chapterElement, finalChapterNumber, status))
            }
        }
        return chapters
    }

    private fun chapterFromJson(chapterId: String, chapterJson: JsonObject, finalChapterNumber: String, status: Int): SChapter {
        val chapter = SChapter.create()
        chapter.url = MdUtil.apiChapter + chapterId
        val chapterName = mutableListOf<String>()
        // Build chapter name
        if (chapterJson.get("volume").string.isNotBlank()) {
            chapterName.add("Vol." + chapterJson.get("volume").string)
        }
        if (chapterJson.get("chapter").string.isNotBlank()) {
            chapterName.add("Ch." + chapterJson.get("chapter").string)
        }
        if (chapterJson.get("title").string.isNotBlank()) {
            if (chapterName.isNotEmpty()) {
                chapterName.add("-")
            }
            chapterName.add(chapterJson.get("title").string)
        }
        //if volume, chapter and title is empty its a oneshot
        if (chapterName.isEmpty()) {
            chapterName.add("Oneshot")
        }
        if ((status == 2 || status == 3) && doesFinalChapterExist(finalChapterNumber, chapterJson)) {
            chapterName.add("[END]")
        }

        chapter.name = MdUtil.cleanString(chapterName.joinToString(" "))
        // Convert from unix time
        chapter.date_upload = chapterJson.get("timestamp").long * 1000
        val scanlatorName = mutableListOf<String>()
        if (!chapterJson.get("group_name").nullString.isNullOrBlank()) {
            scanlatorName.add(chapterJson.get("group_name").string)
        }
        if (!chapterJson.get("group_name_2").nullString.isNullOrBlank()) {
            scanlatorName.add(chapterJson.get("group_name_2").string)
        }
        if (!chapterJson.get("group_name_3").nullString.isNullOrBlank()) {
            scanlatorName.add(chapterJson.get("group_name_3").string)
        }
        chapter.scanlator = MdUtil.cleanString(scanlatorName.joinToString(" & "))

        return chapter
    }


    private fun doesFinalChapterExist(finalChapterNumber: String, chapterJson: JsonElement) = finalChapterNumber.isNotEmpty() && finalChapterNumber == chapterJson.get("chapter").string.trim()


    private fun getFinalChapter(jsonObj: JsonObject): String = jsonObj.get("last_chapter").string.trim()

}