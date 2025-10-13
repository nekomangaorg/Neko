package eu.kanade.tachiyomi.util.manga

import eu.kanade.tachiyomi.data.external.Amazon
import eu.kanade.tachiyomi.data.external.BookWalker
import eu.kanade.tachiyomi.data.external.CdJapan
import eu.kanade.tachiyomi.data.external.EBookJapan
import eu.kanade.tachiyomi.data.external.Engtl
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.external.Raw
import org.nekomanga.constants.Constants.ALT_TITLES_SEPARATOR

class MangaUtil {
    companion object {

        fun getAltTitles(altTitle: String?, originalTitle: String): List<String> {
            return altTitle
                ?.split(ALT_TITLES_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.filter { it != originalTitle } ?: emptyList()
        }

        fun altTitlesToString(altTitles: List<String>): String {
            return altTitles.joinToString(ALT_TITLES_SEPARATOR)
        }

        fun externalLinksToOtherString(externalLinks: List<ExternalLink>): String {
            return externalLinks
                .mapNotNull { externalLink ->
                    when (externalLink) {
                        is Raw -> "raw~~${externalLink.id}"
                        is Engtl -> "engtl~~${externalLink.id}"
                        is BookWalker -> "bw~~${externalLink.id}"
                        is CdJapan -> "cdj~~${externalLink.id}"
                        is EBookJapan -> "ebj~~${externalLink.id}"
                        is Amazon -> "amz~~${externalLink.id}"
                        else -> null
                    }
                }
                .joinToString("||")
        }

        fun getGenres(genres: String?, filterOutSafe: Boolean = false): List<String> {
            return genres
                ?.split(",")
                ?.mapNotNull { tag -> tag.trim().takeUnless { it.isBlank() } }
                ?.filter {
                    if (filterOutSafe) {
                        !it.equals("Content rating: safe", true)
                    } else {
                        true
                    }
                } ?: emptyList()
        }

        fun genresToString(genres: List<String>, contentRating: String?): String {
            val set = genres.toMutableSet()
            if (!contentRating.isNullOrBlank()) {
                set.add("Content rating: $contentRating")
            }
            return set.joinToString(",")
        }

        fun getContentRating(genres: List<String>): String {
            return genres
                .firstOrNull { it.startsWith("Content rating: ") }
                ?.substringAfter("Content rating: ") ?: ""
        }
    }
}
