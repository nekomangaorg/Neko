package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import org.jsoup.parser.Parser
import kotlin.math.floor

class MdUtil {

    companion object {
        const val cdnUrl = "https://mangadex.org" // "https://s0.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val randMangaPage = "/manga/"
        const val apiUrl = "https://api.mangadex.org"
        const val apiManga = "/v2/manga/"
        const val includeChapters = "?include=chapters"
        const val oldApiChapter = "/api/chapter/"
        const val newApiChapter = "/v2/chapter/"
        const val apiChapterSuffix = "?mark_read=0"
        const val groupSearchUrl = "$baseUrl/groups/0/1/"
        const val followsAllApi = "/v2/user/me/followed-manga"
        const val isLoggedInApi = "/v2/user/me"
        const val followsMangaApi = "/v2/user/me/manga/"
        const val apiCovers = "/covers"
        const val reportUrl = "https://api.mangadex.network/report"
        const val imageUrl = "$baseUrl/data"

        val jsonParser =
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                allowSpecialFloatingPointValues = true
                useArrayPolymorphism = true
                prettyPrint = true
            }

        private const
        val scanlatorSeparator = " & "

        val validOneShotFinalChapters = listOf("0", "1")

        val englishDescriptionTags = listOf(
            "[b][u]English:",
            "[b][u]English",
            "English:",
            "English :",
            "[English]:",
            "English Translaton:",
            "[B][ENG][/B]"
        )

        val bbCodeToRemove = listOf(
            "list", "*", "hr", "u", "b", "i", "s", "center", "spoiler="
        )
        val descriptionLanguages = listOf(
            "=FRANCAIS=",
            "[b] Spanish: [/ b]",
            "[b][u]Chinese",
            "[b][u]French",
            "[b][u]German / Deutsch",
            "[b][u]Russian",
            "[b][u]Spanish",
            "[b][u]Vietnamese",
            "[b]External Links",
            "[b]Link[/b]",
            "[b]Links:",
            "[Espa&ntilde;ol]:",
            "[hr]Fr:",
            "[hr]TH",
            "[INDO]",
            "[PTBR]",
            "[right][b][u]Persian",
            "[RUS]",
            "[u]Russian",
            "\r\n\r\nItalian\r\n",
            "Arabic /",
            "Descriptions in Other Languages",
            "Espanol",
            "[Espa&ntilde;",
            "Espa&ntilde;",
            "Farsi/",
            "Fran&ccedil;ais",
            "French - ",
            "Francois",
            "French:",
            "French/",
            "French /",
            "German/",
            "German /",
            "Hindi /",
            "Bahasa Indonesia",
            "Indonesia:",
            "Indonesian:",
            "Indonesian :",
            "Indo:",
            "[u]Indonesian",
            "Italian / ",
            "Italian Summary:",
            "Italian/",
            "Italiano",
            "Italian:",
            "Italian summary:",
            "Japanese /",
            "Original Japanese",
            "Official Japanese Translation",
            "Official Chinese Translation",
            "Official French Translation",
            "Official Indonesian Translation",
            "Links:",
            "Pasta-Pizza-Mandolino/Italiano",
            "Persian/فارسی",
            "Persian /فارسی",
            "Polish /",
            "Polish Summary /",
            "Polish/",
            "Polski",
            "Portugu&ecirc;s",
            "Portuguese (BR)",
            "PT/BR:",
            "Pt/Br:",
            "Pt-Br:",
            "Portuguese /",
            "[right]",
            "R&eacute;sum&eacute; Fran&ccedil;ais",
            "R&eacute;sume Fran&ccedil;ais",
            "R&Eacute;SUM&Eacute; FRANCAIS :",
            "RUS:",
            "Ru/Pyc",
            "\\r\\nRUS\\r\\n",
            "Russia/",
            "Russian /",
            "Spanish:",
            "Spanish /",
            "Spanish Summary:",
            "Spanish/",
            "T&uuml;rk&ccedil;e",
            "Thai:",
            "Turkish /",
            "Turkish/",
            "Turkish:",
            "Русский",
            "العربية",
            "정보",
            "(zh-Hant)",
        )

        // guess the thumbnail url is .jpg  this has a ~80% success rate
        fun formThumbUrl(mangaUrl: String, lowQuality: Boolean): String {
            var ext = ".jpg"
            if (lowQuality) {
                ext = ".thumb$ext"
            }
            return cdnUrl + "/images/manga/" + getMangaId(mangaUrl) + ext
        }

        // Get the ID from the manga url
        fun getMangaId(url: String): String {
            val lastSection = url.trimEnd('/').substringAfterLast("/")
            return if (lastSection.toIntOrNull() != null) {
                lastSection
            } else {
                // this occurs if person has manga from before that had the id/name/
                url.trimEnd('/').substringBeforeLast("/").substringAfterLast("/")
            }
        }

        fun getChapterId(url: String) = url.substringBeforeLast(apiChapterSuffix).substringAfterLast("/")

        // creates the manga url from the browse for the api
        fun modifyMangaUrl(url: String): String =
            url.replace("/title/", "/manga/").substringBeforeLast("/") + "/"

        fun cleanString(string: String): String {
            var cleanedString = string

            bbCodeToRemove.forEach {
                cleanedString = cleanedString.replace("[$it]", "", true)
                    .replace("[/$it]", "", true)
            }

            val bbRegex =
                """\[(\w+)[^]]*](.*?)\[/\1]""".toRegex()

            // Recursively remove nested bbcode
            while (bbRegex.containsMatchIn(cleanedString)) {
                cleanedString = cleanedString.replace(bbRegex, "$2")
            }

            return Parser.unescapeEntities(cleanedString, false)
        }

        fun cleanDescription(string: String): String {
            var newDescription = string
            descriptionLanguages.forEach { it ->
                newDescription = newDescription.substringBefore(it)
            }

            englishDescriptionTags.forEach { it ->
                newDescription = newDescription.replace(it, "")
            }
            return cleanString(newDescription)
        }

        fun getImageUrl(attr: String): String {
            // Some images are hosted elsewhere
            if (attr.startsWith("http")) {
                return attr
            }
            return baseUrl + attr
        }

        fun getScanlators(scanlators: String): List<String> {
            if (scanlators.isBlank()) return emptyList()
            return scanlators.split(scanlatorSeparator).distinct()
        }

        fun getScanlatorString(scanlators: Set<String>): String {
            return scanlators.toList().sorted().joinToString(scanlatorSeparator)
        }

        fun getMissingChapterCount(chapters: List<SChapter>, mangaStatus: Int): String? {
            if (mangaStatus == SManga.COMPLETED) return null

            val remove0ChaptersFromCount = chapters.distinctBy {
                if (it.chapter_txt.isNotEmpty()) {
                    it.vol + it.chapter_txt
                } else {
                    it.name
                }
            }.sortedByDescending { it.chapter_number }

            remove0ChaptersFromCount.firstOrNull()?.let {
                val chpNumber = floor(it.chapter_number).toInt()
                val allChapters = (1..chpNumber).toMutableSet()

                remove0ChaptersFromCount.forEach {
                    allChapters.remove(floor(it.chapter_number).toInt())
                }

                if (allChapters.size <= 0) return null
                return allChapters.size.toString()
            }
            return null
        }
    }
}
