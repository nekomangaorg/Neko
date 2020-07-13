package eu.kanade.tachiyomi.source.online.utils

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jsoup.parser.Parser

class MdUtil {

    companion object {
        const val cdnUrl = "https://mangadex.org" // "https://s0.mangadex.org"
        const val baseUrl = "https://mangadex.org"
        const val randMangaPage = "/manga/"
        const val apiManga = "/api/manga/"
        const val apiChapter = "/api/chapter/"
        const val apiChapterSuffix = "?mark_read=0"
        const val groupSearchUrl = "$baseUrl/groups/0/1/"
        const val followsAllApi = "/api/?type=manga_follows"
        const val followsMangaApi = "/api/?type=manga_follows&manga_id="
        const val coversApi = "/api/index.php?type=covers&id="
        const val reportUrl = "https://api.mangadex.network/report"
        const val imageUrl = "$baseUrl/data"

        @OptIn(UnstableDefault::class)
        val jsonParser =
            Json(
                JsonConfiguration(
                    isLenient = true, ignoreUnknownKeys = true, serializeSpecialFloatingPointValues = true,
                    useArrayPolymorphism = true, prettyPrint = true
                )
            )

        private const
        val scanlatorSeparator = " & "

        val validOneShotFinalChapters = listOf("0", "1")

        val englishDescriptionTags = listOf(
            "[b][u]English:[/u][/b]",
            "[b][u]English[/u][/b]",
            "[English]:",
            "[B][ENG][/B]"
        )

        val descriptionLanguages = listOf(
            "[b][u]Russian / Русский[/u][/b]",
            "[b] [u] Russian / Русский [/ u] [/ b]",
            "[u][b]Russian / Русский:[/b][/u]",
            "[u][b]Russian / Русский:[/b][/u]",
            "[u]Russian:[/u]",
            "[b][u]Russian / Русский:[/u][/b]",
            "[b][u]Russian/Русский:[/u][/b]",
            "[b][u]Русский[/u][/b]",
            "Russian / Русский",
            "RUS:",
            "[b][u]German / Deutsch[/u][/b]",
            "German/Deutsch:",
            "[b][u]Espa&ntilde;ol / Spanish:[/u][/b]",
            "[u][b]Spanish / Espa&ntilde;ol:[/b][/u]",
            "[b] [u] Spanish / Espa & ntilde; ol: [/ u] [/ b]",
            "[b][u]Spanish / Espa&ntilde;ol:[/u][/b]",
            "[Espa&ntilde;ol]:",
            "[b] Spanish: [/ b]",
            "[b][u]Espa&ntilde;ol / Spanish:[/b]",
            "[b][u]Espa&ntilde;ol / Spanish:[/b]",
            "[b][u]Italian / Italiano[/u][/b]",
            "Pasta-Pizza-Mandolino/Italiano",
            "[b]Polish / polski[/b]",
            "[b][u]Polish / Polski:[/u][/b]",
            "[b][u]Polish / Polski[/u][/b]",
            "[b][u]Polish Summary / Polski Opis[/u][/b]",
            "[b][u]Polski[/u][/b]",
            "[b][u]Portuguese (BR) / Portugu&ecirc;s (BR)[/u][/b]",
            "[b]Portuguese (BR) / Portugu&ecirc;s (BR)[/b]",
            "[u][b]Portuguese / Portugu&ecirc;s:[/b][/u]",
            "[b][u]Português / Portuguese[/u][/b]",
            "[b][u]Portuguese / Portugu[/u][/b]",
            "[b][u]Portuguese / Portugu&ecirc;s:[/u][/b]",
            "[b][u]Portugu&ecirc;s[/u][/b]",
            "[b] Portuguese (BR) / Portugu & ecirc; s (BR): [/ b]",
            "[b][u]Portuguese (BR) / Portugu&ecirc;s (BR):[/u][/b] ",
            "[B][PTBR][/B]",
            "R&eacute;sume Fran&ccedil;ais :[spoiler]",
            "R&eacute;sum&eacute; Fran&ccedil;ais",
            "[b][u]French[/u][/b]",
            "[b][u]French / Fran&ccedil;ais[/u][/b]",
            "[u][b]French / Fran&ccedil;ais :[/b][/u]",
            "b][u]Fran&ccedil;ais[/u][/b]",
            "[hr]Fr:",
            "French - Français:",
            "[b][u]Turkish / T&uuml;rk&ccedil;e[/u][/b]",
            "[u][b]Turkish/T&uuml;rk&ccedil;e[/b][/u]",
            "[b][u]Chinese[/u][/b]",
            "[b][u]Arabic / العربية[/u][/b]",
            "[b][u]العربية[/u][/b]",
            "[hr]TH",
            "[b][u]Vietnamese[/u][/b]",
            "[b]Links:[/b]",
            "[b]Link[/b]",
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

        fun getChapterId(url: String) = url.substringBeforeLast(apiChapterSuffix).substringAfterLast("/")

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
                .replace("[u]", "")
                .replace("[/u]", "")

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
    }
}
