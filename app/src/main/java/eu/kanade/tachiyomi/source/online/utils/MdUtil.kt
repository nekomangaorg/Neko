package eu.kanade.tachiyomi.source.online.utils

import org.jsoup.parser.Parser

class MdUtil {
    companion object {
        const val cdnUrl = "https://cdndex.com"
        const val baseUrl = "https://mangadex.org"
        const val randMangaPage = "/manga/"
        const val apiManga = "/api/manga/"
        const val apiChapter = "/api/chapter/"

        //guess the thumbnail url is .jpg  this has a ~80% success rate
        fun formThumbUrl(mangaUrl: String): String {
            return cdnUrl + "/images/manga/" + getMangaId(mangaUrl) + ".jpg"
        }

        //Get the ID from the manga url
        fun getMangaId(url: String): String {
            val lastSection = url.trimEnd('/').substringAfterLast("/")
            return if (lastSection.toIntOrNull() != null) {
                lastSection
            } else {
                //this occurs if person has manga from before that had the id/name/
                url.trimEnd('/').substringBeforeLast("/").substringAfterLast("/")
            }
        }

        //creates the manga url from the browse for the api
        fun modifyMangaUrl(url: String): String = url.replace("/title/", "/manga/").substringBeforeLast("/") + "/"

        fun cleanString(string: String): String {
            val bbRegex = """\[(\w+)[^]]*](.*?)\[/\1]""".toRegex()
            var intermediate = string
                    .replace("[list]", "")
                    .replace("[/list]", "")
                    .replace("[*]", "")
            // Recursively remove nested bbcode
            while (bbRegex.containsMatchIn(intermediate)) {
                intermediate = intermediate.replace(bbRegex, "$2")
            }
            return Parser.unescapeEntities(intermediate, false)
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