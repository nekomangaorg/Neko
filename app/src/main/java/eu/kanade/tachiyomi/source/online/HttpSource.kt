package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.GET
import org.nekomanga.domain.chapter.SimpleChapter
import uy.kohesive.injekt.injectLazy

/** A simple implementation for sources from a website. */
abstract class HttpSource : Source {

    /** Network service. */
    protected val network: NetworkHelper by injectLazy()

    protected val networkServices: NetworkServices by injectLazy()

    //    /**
    //     * Preferences that a source may need.
    //     */
    //    val preferences: SharedPreferences by lazy {
    //        Injekt.get<Application>().getSharedPreferences("source_$id", Context.MODE_PRIVATE)
    //    }

    /** Base url of the website without the trailing slash, like: http://mysite.com */
    open val baseUrl = MdConstants.baseUrl

    override val name = "MangaDex"

    /**
     * Version id used to generate the source id. If the site completely changes and urls are
     * incompatible, you may increase this value and it'll be considered as a new source.
     */
    open val versionId = 1

    /**
     * Id of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string: sourcename/language/versionId Note the generated id sets the sign
     * bit to 0.
     */
    override val id by lazy {
        val key = "${name.lowercase(Locale.getDefault())}/en/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and
            Long.MAX_VALUE
    }

    /** Headers used for requests. */
    abstract val headers: Headers

    /** Default network client for doing requests. */
    open val client: OkHttpClient = network.client

    /** Visible name of the source. */
    override fun toString() = name

    // used to get the manga url instead of the api manga url
    open fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    protected open fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

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
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    abstract fun getChapterUrl(simpleChapter: SimpleChapter): String

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0"
    }
}
