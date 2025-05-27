package eu.kanade.tachiyomi.source.online

abstract class MergedLoginSource : ReducedHttpSource() {

    abstract fun requiresCredentials(): Boolean

    abstract fun hostUrl(): String

    abstract suspend fun loginWithUrl(username: String, password: String, url: String): Boolean

    abstract fun hasCredentials(): Boolean

    abstract suspend fun isLoggedIn(): Boolean
}
