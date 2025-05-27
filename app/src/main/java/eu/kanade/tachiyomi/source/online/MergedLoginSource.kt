package eu.kanade.tachiyomi.source.online

abstract class MergedLoginSource : ReducedHttpSource() {

    abstract fun requiresCredentials(): Boolean

    abstract fun hostUrl(): String

    open fun getMangaUrl(url: String): String = hostUrl() + url

    abstract suspend fun loginWithUrl(username: String, password: String, url: String): Boolean

    abstract fun hasCredentials(): Boolean

    abstract suspend fun isLoggedIn(): Boolean
}
