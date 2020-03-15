package eu.kanade.tachiyomi.source.online.utils

import org.jsoup.parser.Parser

class MdUtil {

    companion object {
        const val cdnUrl = "https://mangadex.org" // "https://s0.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val randMangaPage = "/manga/"
        const val apiManga = "/api/manga/"
        const val apiChapter = "/api/chapter/"
        const val followsAllApi = "/api/?type=manga_follows"
        const val followsMangaApi = "/api/?type=manga_follows&manga_id="

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

        // creates the manga url from the browse for the api
        fun modifyMangaUrl(url: String): String =
            url.replace("/title/", "/manga/").substringBeforeLast("/") + "/"

        // Removes the ?timestamp from image urls
        fun removeTimeParamUrl(url: String): String = url.substringBeforeLast("?")

        fun cleanString(string: String): String {
            val bbRegex = """\[(\w+)[^]]*](.*?)\[/\1]""".toRegex()
            var intermediate = string
                .replace("[list]", "")
                .replace("[/list]", "")
                .replace("[*]", "")
                .replace("[hr]", "")

            // Recursively remove nested bbcode
            while (bbRegex.containsMatchIn(intermediate)) {
                intermediate = intermediate.replace(bbRegex, "$2")
            }
            return Parser.unescapeEntities(intermediate, false)
        }

        fun cleanDescription(string: String): String {
            val description = cleanString(string)
            return description
                .substringBefore("Russian / Русский")
                .substringBefore("German / Deutsch")
                .substringBefore("Italian / Italiano")
                .substringBefore("Portuguese (BR) / Portugu")
                .substringBefore("Portuguese / Portugu")
                .substringBefore("Turkish/ T&uuml")
        }

        fun getImageUrl(attr: String): String {
            // Some images are hosted elsewhere
            if (attr.startsWith("http")) {
                return attr
            }
            return baseUrl + attr
        }
    }
}
