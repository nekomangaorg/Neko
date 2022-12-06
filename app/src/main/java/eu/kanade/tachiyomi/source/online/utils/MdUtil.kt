package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.Json
import org.jsoup.parser.Parser

class MdUtil {

    companion object {
        const val cdnUrl = "https://uploads.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val apiUrl = "https://api.mangadex.org"
        const val imageUrlCacheNotFound =
            "https://cdn.statically.io/img/raw.githubusercontent.com/CarlosEsco/Neko/master/.github/manga_cover_not_found.png"
        const val chapterSuffix = "/chapter/"

        const val mangaUrl = "$apiUrl/manga"

        const val similarCacheMangaList = "https://api.similarmanga.com/manga/"

        /**
         * Get the manga offset pages are 1 based, so subtract 1
         */
        fun getMangaListOffset(page: Int): Int = (MdConstants.Limits.manga * (page - 1))

        /**
         * Get the latest chapter  offset pages are 1 based, so subtract 1
         */
        fun getLatestChapterListOffset(page: Int): Int = (MdConstants.Limits.latest * (page - 1))

        val jsonParser =
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                allowSpecialFloatingPointValues = true
                useArrayPolymorphism = true
                prettyPrint = true
            }

        val validOneShotFinalChapters = listOf("0", "1")

        val englishDescriptionTags = listOf(
            "[b][u]English:",
            "[b][u]English",
            "English:",
            "English :",
            "[English]:",
            "English Translaton:",
            "[B][ENG][/B]",
        )

        val bbCodeToRemove = listOf(
            "list", "*", "hr", "u", "b", "i", "s", "center", "spoiler=",
        )

        // Get the ID from the manga url
        fun getMangaUUID(url: String): String {
            return url.trimEnd('/').substringAfterLast("/")
        }

        fun getChapterUUID(url: String) = url.substringAfterLast("/")

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

        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSS", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

        fun parseDate(dateAsString: String): Long =
            dateFormatter.parse(dateAsString)?.time ?: 0

        fun cdnCoverUrl(dexId: String, fileName: String, quality: Int): String {
            val coverQualitySuffix = when (quality) {
                1 -> ".512.jpg"
                2 -> ".256.jpg"
                else -> ""
            }
            return "$cdnUrl/covers/$dexId/$fileName$coverQualitySuffix"
        }

        fun getLangsToShow(preferences: PreferencesHelper) =
            preferences.langsToShow().get().split(",")

        fun getTitle(
            titleMap: Map<String, String?>,
            originalLanguage: String,
        ): String {
            return titleMap["en"]
                ?: titleMap[originalLanguage]
                ?: titleMap["$originalLanguage-ro"]
                ?: titleMap["jp"]
                ?: titleMap["ja"]
                ?: titleMap["kr"]
                ?: titleMap["zh"]
                ?: titleMap.entries.firstOrNull()?.value ?: ""
        }
    }
}
