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
        fun apiUrl(useNew: Boolean): String {
            return when (useNew) {
                false -> oldApiUrl
                true -> newApiUrl
            }
        }

        private const val oldApiUrl = "https://mangadex.org/api"
        private const val newApiUrl = "https://api.mangadex.org"
        const val apiManga = "/v2/manga/"
        const val includeChapters = "?include=chapters"
        const val oldApiChapter = "/api/chapter/"
        const val newApiChapter = "/v2/chapter/"
        const val apiChapterSuffix = "?mark_read=0"
        const val groupSearchUrl = "$baseUrl/groups/0/1/"
        val followsAllApi = "/v2/user/me/followed-manga"
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
            "[English]:",
            "[B][ENG][/B]"
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
            "Espa&ntilde;ol /",
            "Espa&ntilde;ol:",
            "Farsi/",
            "Fran&ccedil;ais",
            "French - ",
            "Francois",
            "French:",
            "French / ",
            "R&Eacute;SUM&Eacute; FRANCAIS :",
            "German/",
            "German /",
            "Hindi /",
            "Indonesia:",
            "Indonesian:",
            "[u]Indonesian",
            "Italian / ",
            "Italian Summary:",
            "Italian/",
            "Italian:",
            "Japanese /",
            "Links:",
            "Pasta-Pizza-Mandolino/Italiano",
            "Persian /فارسی",
            "Polish /",
            "Polish Summary /",
            "Polish/",
            "Polski",
            "Portugu&ecirc;s",
            "Portuguese (BR)",
            "Pt-Br:",
            "Portuguese /",
            "R&eacute;sum&eacute; Fran&ccedil;ais",
            "R&eacute;sume Fran&ccedil;ais",
            "RUS:",
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
            val bbRegex =
                """\[(\w+)[^]]*](.*?)\[/\1]""".toRegex()
            var intermediate = string
                .replace("[list]", "", true)
                .replace("[/list]", "", true)
                .replace("[*]", "")
                .replace("[hr]", "", true)
                .replace("[u]", "", true)
                .replace("[/u]", "", true)
                .replace("[b]", "", true)
                .replace("[/b]", "", true)

            // Recursively remove nested bbcode
            while (bbRegex.containsMatchIn(intermediate)) {
                intermediate = intermediate.replace(bbRegex, "$2")
            }
            return Parser.unescapeEntities(intermediate, false)
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
