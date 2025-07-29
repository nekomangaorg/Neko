package eu.kanade.tachiyomi.source.online.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.Json
import org.jsoup.parser.Parser
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.site.MangaDexPreferences

class MdUtil {

    companion object {

        /** Get the manga offset pages are 1 based, so subtract 1 */
        fun getMangaListOffset(page: Int): Int = (MdConstants.Limits.manga * (page - 1))

        /** Get the latest chapter offset pages are 1 based, so subtract 1 */
        fun getLatestChapterListOffset(page: Int): Int = (MdConstants.Limits.latest * (page - 1))

        val jsonParser = Json {
            isLenient = true
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = true
            prettyPrint = true
        }

        // Get the ID from the manga url
        fun getMangaUUID(url: String): String {
            return url.trimEnd('/').substringAfterLast("/")
        }

        fun getChapterUUID(url: String) = url.substringAfterLast("/")

        fun cleanString(dirtyString: String): String {
            return Parser.unescapeEntities(dirtyString, false)
        }

        val apiDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        val dateFormatter =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSS", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        fun parseDate(dateAsString: String): Long = dateFormatter.parse(dateAsString)?.time ?: 0

        fun cdnCoverUrl(dexId: String, fileName: String, quality: Int): String {
            val coverQualitySuffix =
                when (quality) {
                    1 -> ".512.jpg"
                    2 -> ".256.jpg"
                    else -> ""
                }
            return "${MdConstants.cdnUrl}/covers/$dexId/$fileName$coverQualitySuffix"
        }

        fun getLangsToShow(preferences: MangaDexPreferences) =
            // this prevents langauges that don't exist anymore from causing a parse exception
            preferences.enabledChapterLanguages().get().filter { enabledLang ->
                MdLang.entries.firstOrNull { mdLang -> enabledLang == mdLang.lang } != null
            }

        fun getTitle(titleMap: Map<String, String?>, originalLanguage: String): String {
            return titleMap[MdLang.ENGLISH.lang]
                ?: titleMap[originalLanguage]
                ?: titleMap["$originalLanguage-ro"]
                ?: titleMap[MdLang.JAPANESE.lang]
                ?: titleMap["${MdLang.JAPANESE.lang}-ro"]
                ?: titleMap[MdLang.KOREAN.lang]
                ?: titleMap["${MdLang.KOREAN.lang}-ro"]
                ?: titleMap[MdLang.CHINESE_TRAD.lang]
                ?: titleMap[MdLang.CHINESE_SIMPLIFIED.lang]
                ?: titleMap.entries.firstOrNull()?.value
                ?: ""
        }
    }
}
