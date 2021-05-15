package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.AtHomeResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaResponse
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

class MdUtil {

    companion object {
        const val cdnUrl = "https://mangadex.org" // "https://s0.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val apiUrl = "https://api.mangadex.org"
        const val imageUrlCacheNotFound = "https://cdn.statically.io/img/raw.githubusercontent.com/CarlosEsco/Neko/master/.github/manga_cover_not_found.png"
        const val atHomeUrl = "$apiUrl/at-home/server"
        const val chapterUrl = "$apiUrl/chapter/"
        const val chapterSuffix = "/chapter/"
        const val checkTokenUrl = "$apiUrl/auth/check"
        const val refreshTokenUrl = "$apiUrl/auth/refresh"
        const val loginUrl = "$apiUrl/auth/login"
        const val logoutUrl = "$apiUrl/auth/logout"
        const val groupUrl = "$apiUrl/group"
        const val authorUrl = "$apiUrl/author"
        const val randomMangaUrl = "$apiUrl/manga/random"
        const val mangaUrl = "$apiUrl/manga"
        const val userFollowsUrl = "$apiUrl/user/follows/manga"
        const val readingStatusesUrl = "$apiUrl/manga/status"
        fun getReadingStatusUrl(id: String) = "$apiUrl/manga/$id/status"

        fun mangaFeedUrl(id: String, offset: Int, language: List<String>): String {
            return "$mangaUrl/$id/feed".toHttpUrl().newBuilder().apply {
                addQueryParameter("limit", "500")
                addQueryParameter("offset", offset.toString())
                addQueryParameter("order[volume]", "desc")
                addQueryParameter("order[chapter]", "desc")
                language.forEach {
                    addQueryParameter("locales[]", it)
                }
            }.build().toString()
        }

        const val similarCacheMapping = "https://api.similarmanga.com/mapping/mdex2search.csv"
        const val similarCacheMangas = "https://api.similarmanga.com/manga/"
        const val similarBaseApi = "https://api.similarmanga.com/similar/"

        const val apiCovers = "/covers"
        const val reportUrl = "https://api.mangadex.network/report"

        const val mdAtHomeTokenLifespan = 10 * 60 * 1000
        const val mangaLimit = 25

        /**
         * Get the manga offset pages are 1 based, so subtract 1
         */
        fun getMangaListOffset(page: Int): String = (mangaLimit * (page - 1)).toString()

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

        const val contentRatingSafe = "safe"
        const val contentRatingSuggestive = "suggestive"
        const val contentRatingErotica = "erotica"
        const val contentRatingPornographic = "pornographic"

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
            val id = url.trimEnd('/').substringAfterLast("/")
            return id
        }

        fun getChapterId(url: String) = url.substringAfterLast("/")

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

        fun atHomeUrlHostUrl(requestUrl: String, client: OkHttpClient, cacheControl: CacheControl): String {
            val atHomeRequest = GET(requestUrl, cache = cacheControl)
            val atHomeResponse = client.newCall(atHomeRequest).execute()
            return jsonParser.decodeFromString<AtHomeResponse>(atHomeResponse.body!!.string()).baseUrl
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSS", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

        fun parseDate(dateAsString: String): Long =
            dateFormatter.parse(dateAsString)?.time ?: 0

        fun createMangaEntry(json: MangaResponse, preferences: PreferencesHelper, v5DbHelper: V5DbHelper): SManga {
            return SManga.create().apply {
                url = "/manga/" + json.data.id
                title = cleanString(json.data.attributes.title["en"]!!)
                thumbnail_url = V5DbQueries.getAltCover(v5DbHelper.dbCovers, json.data.id) ?: imageUrlCacheNotFound
                //thumbnail_url = formThumbUrl(url, preferences.lowQualityCovers())
            }
        }

        fun getLangsToShow(preferences: PreferencesHelper) = preferences.langsToShow().get().split(",")

        fun getAuthHeaders(headers: Headers, preferences: PreferencesHelper) = headers.newBuilder().add("Authorization", "Bearer ${preferences.sessionToken()!!}").build()
    }
}
