package eu.kanade.tachiyomi.source.online.utils

import org.jsoup.parser.Parser

class MdUtil {

    companion object {
        const val cdnUrl = "https://mangadex.org" // "https://s0.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val randMangaPage = "/manga/"
        const val apiManga = "/api/manga/"
        const val apiChapter = "/api/chapter/"
        const val apiChapterSuffix = "?mark_read=0"
        const val followsAllApi = "/api/?type=manga_follows"
        const val followsMangaApi = "/api/?type=manga_follows&manga_id="

        private const val scanlatorSeparator = " & "

        val validOneShotFinalChapters = listOf("0", "1")

        val englishDescriptionTags = listOf(
            "[b][u]English:[/u][/b]",
            "[b][u]English[/u][/b]",
            "[English]:",
            "[B][ENG][/B]"
        )

        val descriptionLanguages = listOf(
            "[b][u]French[/u][/b]",
            "[b][u]Russian / Русский[/u][/b]",
            "[b] [u] Russian / Русский [/ u] [/ b]",
            "[hr][u][b]Russian / Русский:[/b][/u]",
            "[u][b]Russian / Русский:[/b][/u]",
            "[hr][b][u]Russian / Русский:[/u][/b]",
            "[b][u]Russian/Русский:[/u][/b]",
            "RUS:",
            "German/Deutsch:",
            "[b][u]German / Deutsch[/u][/b]",
            "[b][u]Espa&ntilde;ol / Spanish:[/u][/b]",
            "[hr][u][b]Spanish / Espa&ntilde;ol:[/b][/u]",
            "[b] [u] Spanish / Espa & ntilde; ol: [/ u] [/ b]",
            "[Espa&ntilde;ol]:",
            "[b] Spanish: [/ b]",
            "[b][u]Espa&ntilde;ol / Spanish:[/b]",
            "[hr][b][u]Espa&ntilde;ol / Spanish:[/b]",
            "[b][u]Italian / Italiano[/u][/b]",
            "[b]Polish / polski[/b]",
            "[b][u]Polish / Polski:[/u][/b]",
            "[b][u]Polish / Polski[/u][/b]",
            "[b][u]Portuguese (BR) / Portugu&ecirc;s (BR)[/u][/b]",
            "[b]Portuguese (BR) / Portugu&ecirc;s (BR)[/b]",
            "[b][u]Português / Portuguese[/u][/b]",
            "[b][u]Portuguese / Portugu[/u][/b]",
            "[b][u]Portuguese / Portugu&ecirc;s:[/u][/b]",
            "[b] Portuguese (BR) / Portugu & ecirc; s (BR): [/ b]",
            "[b][u]Portuguese (BR) / Portugu&ecirc;s (BR):[/u][/b] ",
            "[hr][B][PTBR][/B]",
            "French - Français:",
            "R&eacute;sume Fran&ccedil;ais :[spoiler]",
            "[b][u]French[/u][/b]",
            "[b][u]French / Fran&ccedil;ais[/u][/b]",
            "[b][u]Turkish / T&uuml;rk&ccedil;e[/u][/b]",
            "[hr][u][b]Turkish/T&uuml;rk&ccedil;e[/b][/u]",
            "[b][u]Arabic / العربية[/u][/b]",
            "[hr][b]Links:[/b]",
            "Links:",
            "[b]External Links :[/b]"

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

        fun getChapterId(url: String) = url.trimEnd('/').substringAfterLast("/")

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
            return scanlators.split(scanlatorSeparator)
        }

        fun getScanlatorString(scanlators: List<String>): String {
            return scanlators.joinToString(scanlatorSeparator)
        }
    }
}
