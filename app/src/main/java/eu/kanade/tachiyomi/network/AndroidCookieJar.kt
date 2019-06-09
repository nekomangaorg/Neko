package eu.kanade.tachiyomi.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class AndroidCookieJar : CookieJar {

    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
        val urlString = url.toString()

        for (cookie in cookies) {
            manager.setCookie(urlString, cookie.toString())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())

        return if (cookies != null && !cookies.isEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else {
            emptyList()
        }
    }

    fun remove(url: HttpUrl) {
        val urlString = url.toString()
        val cookies = manager.getCookie(urlString) ?: return

        cookies.split(";")
            .map { it.substringBefore("=") }
            .onEach { manager.setCookie(urlString, "$it=;Max-Age=-1") }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            syncManager.sync()
        }
    }

    fun removeAll() {
            manager.removeAllCookies {}
    }

}
