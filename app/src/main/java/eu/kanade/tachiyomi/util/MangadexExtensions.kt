package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.net.URI
import java.net.URISyntaxException

/**
 * Assigns the url of the chapter without the scheme and domain. It saves some redundancy from
 * database and the urls could still work after a domain change.
 *
 * @param url the full url to the chapter.
 */
fun SChapter.setUrlWithoutDomain(url: String) {
    this.url = getUrlWithoutDomain(url)
}

/**
 * Assigns the url of the manga without the scheme and domain. It saves some redundancy from
 * database and the urls could still work after a domain change.
 *
 * @param url the full url to the manga.
 */
fun SManga.setUrlWithoutDomain(url: String) {
    this.url = getUrlWithoutDomain(url)
}

/**
 * Returns the url of the given string without the scheme and domain.
 *
 * @param orig the full url.
 */
private fun getUrlWithoutDomain(orig: String): String {
    return try {
        val uri = URI(orig)
        var out = uri.path
        if (uri.query != null)
            out += "?" + uri.query
        if (uri.fragment != null)
            out += "#" + uri.fragment
        out
    } catch (e: URISyntaxException) {
        orig
    }
}
